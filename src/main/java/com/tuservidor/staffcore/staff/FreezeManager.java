package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.SafeYamlIO;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public final class FreezeManager {

    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private final StaffCore plugin;
    private final Messages messages;
    private final Set<UUID> frozen = new HashSet<>();
    private final Map<UUID, SpeedData> speeds = new HashMap<>();
    private final Map<UUID, UUID> freezeSessions = new HashMap<>();
    private final Map<UUID, UUID> freezeOrigins = new HashMap<>();
    private final Map<UUID, String> freezeReasons = new HashMap<>();
    private final Map<UUID, BukkitTask> freezeActionbarTasks = new HashMap<>();
    private final File file;
    private final NamespacedKey freezeAppliedKey;

    public FreezeManager(StaffCore plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.file = new File(plugin.getDataFolder(), "frozen.yml");
        this.freezeAppliedKey = plugin.key("freeze-applied");
        reload();
    }

    public void toggle(Player staff, Player target) {
        toggle(staff, target, null);
    }

    public void toggle(Player staff, Player target, String reason) {
        if (isFrozen(target)) {
            unfreeze(staff, target);
            return;
        }
        freeze(staff, target, reason);
    }

    public void freeze(Player staff, Player target) {
        freeze(staff, target, null);
    }

    public void freeze(Player staff, Player target, String reason) {
        if (!frozen.add(target.getUniqueId())) {
            return;
        }
        UUID targetUuid = target.getUniqueId();
        String normalizedReason = normalizeFreezeReason(staff, reason);
        freezeSessions.put(targetUuid, staff.getUniqueId());
        freezeOrigins.put(targetUuid, staff.getUniqueId());
        freezeReasons.put(targetUuid, normalizedReason);
        speeds.put(target.getUniqueId(), new SpeedData(normalSpeed(target.getWalkSpeed(), DEFAULT_WALK_SPEED), normalSpeed(target.getFlySpeed(), DEFAULT_FLY_SPEED)));
        target.setWalkSpeed(0f);
        target.setFlySpeed(0f);
        target.setVelocity(new Vector(0, 0, 0));
        applyFreezeEffects(target);
        sendFreezeScreen(staff, target, normalizedReason);
        messages.send(staff, "freeze-staff", Map.of("player", target.getName(), "reason", normalizedReason));
        messages.send(target, "freeze-target", Map.of("staff", staff.getName(), "reason", normalizedReason));
        notifyStaff("freeze-alert", Map.of("staff", staff.getName(), "player", target.getName(), "reason", normalizedReason));
        plugin.staffLogManager().log(staff.getName(), "FREEZE", target.getName(), normalizedReason);
        save();
    }

    public ClaimResult claim(Player staff, Player target) {
        if (!isFrozen(target)) {
            return ClaimResult.NOT_FROZEN;
        }
        UUID staffUuid = staff.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        UUID previousStaff = freezeSessions.get(targetUuid);
        if (staffUuid.equals(previousStaff)) {
            return ClaimResult.ALREADY_ASSIGNED_TO_YOU;
        }

        freezeSessions.put(targetUuid, staffUuid);
        int responseSeconds = Math.max(3, plugin.getConfig().getInt("freeze.response-window-seconds", 8));
        if (plugin.getConfig().getBoolean("freeze.visual-actionbar-enabled", true)) {
            startFreezeActionbar(target, staff.getName(), responseSeconds);
        }
        messages.send(target, "freeze-claim-target", Map.of("staff", staff.getName()));
        notifyStaff("freeze-claim-alert", Map.of("staff", staff.getName(), "player", target.getName()));

        String previousName = previousStaff == null ? "none" : staffName(previousStaff);
        plugin.staffLogManager().log(staff.getName(), "FREEZE_CLAIM", target.getName(), "Reassigned from " + previousName);
        save();
        return ClaimResult.CLAIMED;
    }

    public void unfreeze(Player staff, Player target) {
        if (!frozen.remove(target.getUniqueId())) {
            return;
        }
        freezeSessions.remove(target.getUniqueId());
        freezeOrigins.remove(target.getUniqueId());
        freezeReasons.remove(target.getUniqueId());
        stopFreezeActionbar(target.getUniqueId());
        restoreSpeed(target);
        clearFreezeEffects(target);
        messages.send(staff, "unfreeze-staff", Map.of("player", target.getName()));
        messages.send(target, "unfreeze-target", Map.of("staff", staff.getName()));
        notifyStaff("unfreeze-alert", Map.of("staff", staff.getName(), "player", target.getName()));
        plugin.staffLogManager().log(staff.getName(), "UNFREEZE", target.getName(), "Player unfrozen");
        save();
    }

    public void remove(Player player) {
        if (frozen.remove(player.getUniqueId())) {
            freezeSessions.remove(player.getUniqueId());
            freezeOrigins.remove(player.getUniqueId());
            freezeReasons.remove(player.getUniqueId());
            stopFreezeActionbar(player.getUniqueId());
            restoreSpeed(player);
            clearFreezeEffects(player);
            save();
        }
    }

    public void remove(UUID targetUuid) {
        if (!frozen.remove(targetUuid)) {
            return;
        }
        freezeSessions.remove(targetUuid);
        freezeOrigins.remove(targetUuid);
        freezeReasons.remove(targetUuid);
        stopFreezeActionbar(targetUuid);
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            restoreSpeed(online);
            clearFreezeEffects(online);
        } else {
            speeds.remove(targetUuid);
        }
        save();
    }

    public boolean clearForBan(UUID targetUuid, String targetName, String staffName) {
        boolean hadFreeze = frozen.contains(targetUuid);
        if (!hadFreeze && targetName != null && !targetName.isBlank()) {
            String normalized = targetName.toLowerCase(Locale.ROOT);
            hadFreeze = frozen.stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .filter(name -> name != null)
                .anyMatch(name -> name.toLowerCase(Locale.ROOT).equals(normalized));
        }

        remove(targetUuid);
        if (targetName != null && !targetName.isBlank()) {
            removeByName(targetName);
        }

        if (hadFreeze) {
            notifyStaff("freeze-ban-alert", Map.of(
                "player", targetName == null || targetName.isBlank() ? targetUuid.toString() : targetName,
                "staff", staffName == null || staffName.isBlank() ? "Unknown" : staffName
            ));
        }
        return hadFreeze;
    }

    public int removeByName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return 0;
        }
        String normalized = playerName.toLowerCase(Locale.ROOT);
        int removed = 0;
        for (UUID uuid : Set.copyOf(frozen)) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName();
            if (name == null || !name.toLowerCase(Locale.ROOT).equals(normalized)) {
                continue;
            }
            remove(uuid);
            removed++;
        }
        return removed;
    }

    public void handleQuit(Player player) {
        if (!isFrozen(player)) {
            return;
        }
        stopFreezeActionbar(player.getUniqueId());
        notifyStaff("freeze-quit-alert", Map.of("player", player.getName()));
        // Security-first behavior: frozen state persists across reconnect by default.
        // This avoids "disconnect to bypass freeze" abuse.
    }

    public void handleStaffQuit(Player staff) {
        // Keep assignment by default so if the same staff member reconnects,
        // the freeze private-chat link restores automatically.
        if (plugin.getConfig().getBoolean("freeze.keep-assignment-when-staff-quits", true)) {
            return;
        }
        UUID staffUuid = staff.getUniqueId();
        if (freezeSessions.containsValue(staffUuid)) {
            freezeSessions.entrySet().removeIf(entry -> entry.getValue().equals(staffUuid));
            save();
        }
    }

    public void handleJoin(Player player) {
        if (!isFrozen(player)) {
            cleanupResidualFreezeState(player);
            return;
        }
        int responseSeconds = Math.max(3, plugin.getConfig().getInt("freeze.response-window-seconds", 8));
        UUID targetUuid = player.getUniqueId();
        String staffDisplay = resolveAssignedStaffDisplay(targetUuid);
        String reason = resolveFreezeReason(player);
        speeds.put(player.getUniqueId(), new SpeedData(normalSpeed(player.getWalkSpeed(), DEFAULT_WALK_SPEED), normalSpeed(player.getFlySpeed(), DEFAULT_FLY_SPEED)));
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.setVelocity(new Vector(0, 0, 0));
        applyFreezeEffects(player);
        Map<String, String> placeholders = Map.of(
            "staff", staffDisplay,
            "seconds", String.valueOf(responseSeconds),
            "reason", reason
        );
        player.sendTitle(
            messages.resolve(player, "freeze-visual-title", placeholders),
            messages.resolve(player, "freeze-visual-subtitle", placeholders),
            5,
            50,
            10
        );
        if (plugin.getConfig().getBoolean("freeze.visual-actionbar-enabled", true)) {
            startFreezeActionbar(player, staffDisplay, 0);
        }
        messages.send(player, "freeze-reminder", placeholders);
        notifyStaff("freeze-join-alert", Map.of(
            "player", player.getName(),
            "staff", staffDisplay,
            "reason", reason
        ));
    }

    public boolean isFrozen(Player player) {
        return frozen.contains(player.getUniqueId());
    }

    public Optional<Player> assignedStaff(Player frozenPlayer) {
        if (!isFrozen(frozenPlayer)) {
            return Optional.empty();
        }
        UUID staffUuid = freezeSessions.get(frozenPlayer.getUniqueId());
        if (staffUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(staffUuid));
    }

    public Optional<Player> assignedFrozen(Player staff) {
        List<Player> onlineFrozen = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : freezeSessions.entrySet()) {
            if (!entry.getValue().equals(staff.getUniqueId())) {
                continue;
            }
            UUID frozenUuid = entry.getKey();
            if (!frozen.contains(frozenUuid)) {
                continue;
            }
            Player online = Bukkit.getPlayer(frozenUuid);
            if (online != null) {
                onlineFrozen.add(online);
            }
        }
        if (onlineFrozen.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(onlineFrozen.get(0));
    }

    public int activeAssignedFrozenCount(Player staff) {
        int count = 0;
        for (Map.Entry<UUID, UUID> entry : freezeSessions.entrySet()) {
            if (!entry.getValue().equals(staff.getUniqueId())) {
                continue;
            }
            if (frozen.contains(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    public Set<UUID> frozenPlayers() {
        return Collections.unmodifiableSet(frozen);
    }

    public int activeSessionCount() {
        return freezeSessions.size();
    }

    public void reload() {
        for (UUID uuid : Set.copyOf(freezeActionbarTasks.keySet())) {
            stopFreezeActionbar(uuid);
        }
        frozen.clear();
        speeds.clear();
        freezeSessions.clear();
        freezeOrigins.clear();
        freezeReasons.clear();
        boolean crashSafeEnabled = plugin.getConfig().getBoolean("storage.crash-safe.enabled", true);
        boolean recoverTemp = plugin.getConfig().getBoolean("storage.crash-safe.recover-temp-on-load", true);
        boolean forceSync = plugin.getConfig().getBoolean("storage.crash-safe.force-file-sync", false);
        SafeYamlIO.recoverTempIfNeeded(plugin, file, crashSafeEnabled && recoverTemp, forceSync);
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String raw : config.getStringList("frozen")) {
            try {
                frozen.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!config.isConfigurationSection("data")) {
            return;
        }
        for (String key : config.getConfigurationSection("data").getKeys(false)) {
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!frozen.contains(targetUuid)) {
                continue;
            }
            String base = "data." + key + ".";
            tryLoadUuid(config.getString(base + "assigned-staff")).ifPresent(uuid -> freezeSessions.put(targetUuid, uuid));
            tryLoadUuid(config.getString(base + "origin-staff")).ifPresent(uuid -> freezeOrigins.put(targetUuid, uuid));
            String reason = ModerationGuard.sanitizeReason(config.getString(base + "reason", ""));
            if (!reason.isBlank()) {
                freezeReasons.put(targetUuid, reason);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("frozen", frozen.stream().map(UUID::toString).toList());
        for (UUID targetUuid : frozen) {
            String base = "data." + targetUuid + ".";
            UUID assigned = freezeSessions.get(targetUuid);
            UUID origin = freezeOrigins.get(targetUuid);
            String reason = freezeReasons.get(targetUuid);
            if (assigned != null) {
                config.set(base + "assigned-staff", assigned.toString());
            }
            if (origin != null) {
                config.set(base + "origin-staff", origin.toString());
            }
            if (reason != null && !reason.isBlank()) {
                config.set(base + "reason", reason);
            }
        }
        boolean crashSafeEnabled = plugin.getConfig().getBoolean("storage.crash-safe.enabled", true);
        boolean forceSync = plugin.getConfig().getBoolean("storage.crash-safe.force-file-sync", false);
        boolean keepBackup = plugin.getConfig().getBoolean("storage.crash-safe.keep-backup", true);
        try {
            SafeYamlIO.writeSnapshot(plugin, file, config, crashSafeEnabled, forceSync, keepBackup);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save frozen.yml: " + exception.getMessage());
        }
    }

    public int cleanupOfflineFrozen() {
        Set<UUID> removed = new HashSet<>();
        frozen.removeIf(uuid -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            boolean offline = !player.isOnline();
            if (offline) {
                removed.add(uuid);
            }
            return offline;
        });
        if (!removed.isEmpty()) {
            removed.forEach(freezeSessions::remove);
            removed.forEach(freezeOrigins::remove);
            removed.forEach(freezeReasons::remove);
            save();
        }
        return removed.size();
    }

    private void restoreSpeed(Player player) {
        SpeedData speed = speeds.remove(player.getUniqueId());
        player.setWalkSpeed(speed == null ? DEFAULT_WALK_SPEED : normalSpeed(speed.walkSpeed(), DEFAULT_WALK_SPEED));
        player.setFlySpeed(speed == null ? DEFAULT_FLY_SPEED : normalSpeed(speed.flySpeed(), DEFAULT_FLY_SPEED));
    }

    private void applyFreezeEffects(Player player) {
        int duration = Integer.MAX_VALUE;
        player.getPersistentDataContainer().set(freezeAppliedKey, PersistentDataType.BYTE, (byte) 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 200, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 1, false, false, false));
        player.setFreezeTicks(300);
    }

    private void clearFreezeEffects(Player player) {
        player.getPersistentDataContainer().remove(freezeAppliedKey);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.setFreezeTicks(0);

        // Some clients keep visual effect transitions for a short moment.
        // Force-clear again on next ticks so unfreeze feels instant.
        Bukkit.getScheduler().runTask(plugin, () -> forceClear(player));
        Bukkit.getScheduler().runTaskLater(plugin, () -> forceClear(player), 2L);
    }

    private void sendFreezeScreen(Player staff, Player target, String reason) {
        int responseSeconds = Math.max(3, plugin.getConfig().getInt("freeze.response-window-seconds", 8));
        Map<String, String> placeholders = Map.of(
            "staff", staff.getName(),
            "seconds", String.valueOf(responseSeconds),
            "reason", reason
        );
        target.sendTitle(
            messages.resolve(target, "freeze-visual-title", placeholders),
            messages.resolve(target, "freeze-visual-subtitle", placeholders),
            10,
            120,
            20
        );
        if (plugin.getConfig().getBoolean("freeze.visual-chat-card-enabled", true)) {
            List<String> lines = List.of(
                messages.resolve(target, "freeze-visual-line-1", placeholders),
                messages.resolve(target, "freeze-visual-line-2", placeholders),
                messages.resolve(target, "freeze-visual-line-3", placeholders),
                messages.resolve(target, "freeze-visual-line-4", placeholders),
                messages.resolve(target, "freeze-visual-line-5", placeholders),
                messages.resolve(target, "freeze-visual-line-6", placeholders)
            );
            for (String line : lines) {
                target.sendMessage(line);
            }
        }
        if (plugin.getConfig().getBoolean("freeze.visual-actionbar-enabled", true)) {
            startFreezeActionbar(target, staff.getName(), responseSeconds);
        }
        try {
            target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
        } catch (Throwable ignored) {}
        target.playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 1.2f, 0.8f);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.2f, 0.6f);
    }

    private void notifyStaff(String messageKey, Map<String, String> placeholders) {
        String consoleLine = messages.resolve(messageKey, placeholders);
        Bukkit.getConsoleSender().sendMessage(consoleLine);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Permissions.has(player, "staffcore.freeze")) {
                player.sendMessage(messages.resolve(player, messageKey, placeholders));
            }
        }
    }

    private void forceClear(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.getPersistentDataContainer().remove(freezeAppliedKey);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.setFreezeTicks(0);
    }

    private void cleanupResidualFreezeState(Player player) {
        boolean tagged = player.getPersistentDataContainer().has(freezeAppliedKey, PersistentDataType.BYTE);
        boolean legacyEffects = hasLegacyFreezeEffects(player);
        boolean zeroSpeed = player.getWalkSpeed() <= 0.001f || player.getFlySpeed() <= 0.001f;

        if (!tagged && !legacyEffects && !zeroSpeed) {
            return;
        }

        clearFreezeEffects(player);
        if (player.getWalkSpeed() <= 0.001f) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
        }
        if (player.getFlySpeed() <= 0.001f) {
            player.setFlySpeed(DEFAULT_FLY_SPEED);
        }
        speeds.remove(player.getUniqueId());
        stopFreezeActionbar(player.getUniqueId());
    }

    private boolean hasLegacyFreezeEffects(Player player) {
        PotionEffect slow = player.getPotionEffect(PotionEffectType.SLOWNESS);
        PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        PotionEffect blindness = player.getPotionEffect(PotionEffectType.BLINDNESS);
        PotionEffect darkness = player.getPotionEffect(PotionEffectType.DARKNESS);

        boolean strongSlow = slow != null && slow.getAmplifier() >= 200;
        boolean strongJump = jump != null && jump.getAmplifier() >= 150;
        boolean freezeBlindness = blindness != null && blindness.getAmplifier() <= 1 && blindness.getDuration() > 1200;
        boolean freezeDarkness = darkness != null && darkness.getAmplifier() <= 1 && darkness.getDuration() > 1200;

        return strongSlow || strongJump || freezeBlindness || freezeDarkness;
    }

    private float normalSpeed(float value, float fallback) {
        return value <= 0.001f ? fallback : value;
    }

    private String resolveAssignedStaffDisplay(UUID frozenUuid) {
        UUID assigned = freezeSessions.get(frozenUuid);
        if (assigned != null) {
            return staffName(assigned);
        }
        UUID origin = freezeOrigins.get(frozenUuid);
        if (origin != null) {
            return staffName(origin);
        }
        return "STAFF";
    }

    private String resolveFreezeReason(Player viewer) {
        String configured = freezeReasons.get(viewer.getUniqueId());
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return messages.resolve(viewer, "freeze-reason-not-specified");
    }

    private String normalizeFreezeReason(Player source, String reason) {
        String normalized = ModerationGuard.sanitizeReason(reason);
        if (!normalized.isBlank()) {
            return normalized;
        }
        String fallback = ModerationGuard.sanitizeReason(messages.resolve(source, "freeze-tool-default-reason"));
        if (!fallback.isBlank() && !fallback.toLowerCase(Locale.ROOT).contains("missing message")) {
            return fallback;
        }
        return "Suspicious activity";
    }

    private Optional<UUID> tryLoadUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String staffName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null && !offline.getName().isBlank()) {
            return offline.getName();
        }
        return uuid.toString();
    }

    private void startFreezeActionbar(Player target, String staffName, int countdownSeconds) {
        stopFreezeActionbar(target.getUniqueId());
        final int[] seconds = {Math.max(0, countdownSeconds)};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!target.isOnline() || !isFrozen(target)) {
                stopFreezeActionbar(target.getUniqueId());
                return;
            }
            if (seconds[0] > 0) {
                sendActionbar(target, "freeze-visual-actionbar", Map.of(
                    "staff", staffName,
                    "seconds", String.valueOf(seconds[0])
                ));
                seconds[0]--;
                return;
            }
            sendActionbar(target, "freeze-visual-actionbar-expired", Map.of(
                "staff", staffName,
                "seconds", "0"
            ));
        }, 0L, 20L);
        freezeActionbarTasks.put(target.getUniqueId(), task);
    }

    private void sendActionbar(Player target, String key, Map<String, String> placeholders) {
        String line = messages.resolve(target, key, placeholders);
        target.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(line));
    }

    private void stopFreezeActionbar(UUID uuid) {
        BukkitTask task = freezeActionbarTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private record SpeedData(float walkSpeed, float flySpeed) {
    }

    public enum ClaimResult {
        CLAIMED,
        NOT_FROZEN,
        ALREADY_ASSIGNED_TO_YOU
    }
}
