package com.tuservidor.staffcore.data;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.DurationParser;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class PunishmentManager extends AsyncYamlPersistence {

    private static final DateTimeFormatter SCREEN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final Plugin plugin;
    private final Messages messages;
    private final StaffLogManager staffLogManager;
    private final File file;
    private final List<PunishmentEntry> entries = new ArrayList<>();
    private int nextId = 1;

    public PunishmentManager(Plugin plugin, Messages messages, StaffLogManager staffLogManager) {
        super(plugin, "punishments.yml");
        this.plugin = plugin;
        this.messages = messages;
        this.staffLogManager = staffLogManager;
        this.file = new File(plugin.getDataFolder(), "punishments.yml");
        reload();
    }

    public PunishmentEntry warn(CommandSender sender, OfflinePlayer target, String targetName, String reason) {
        String cleanReason = normalizeReason(reason);
        PunishmentEntry entry = add(PunishmentType.WARN, sender, target, targetName, cleanReason, null, false);
        notifyTarget(target, "punishment-warned", Map.of(
            "reason", cleanReason,
            "staff", sender.getName(),
            "target", targetName
        ));
        notifyStaffPunishmentCard(sender, PunishmentType.WARN, targetName, cleanReason, "-");
        staffLogManager.log(sender.getName(), "WARN", targetName, cleanReason);
        return entry;
    }

    public PunishmentEntry kick(CommandSender sender, Player target, String reason) {
        String cleanReason = normalizeReason(reason);
        PunishmentEntry entry = add(PunishmentType.KICK, sender, target, target.getName(), cleanReason, null, false);
        String kickMessage = messages.resolve(target, "punishment-kick-screen", Map.of(
            "reason", cleanReason,
            "staff", sender.getName(),
            "duration", "n/a",
            "date", nowDateString(),
            "appeal", moderationLink("appeal-url", "-"),
            "store", moderationLink("store-url", "-"),
            "server", plugin.getServer().getName(),
            "target", target.getName()
        ));
        target.kickPlayer(kickMessage);
        notifyStaffPunishmentCard(sender, PunishmentType.KICK, target.getName(), cleanReason, "-");
        staffLogManager.log(sender.getName(), "KICK", target.getName(), cleanReason);
        return entry;
    }

    public PunishmentEntry mute(CommandSender sender, OfflinePlayer target, String targetName, String reason, Duration duration) {
        String cleanReason = normalizeReason(reason);
        Long expiresAt = duration == null ? null : System.currentTimeMillis() + duration.toMillis();
        PunishmentEntry entry = add(PunishmentType.MUTE, sender, target, targetName, cleanReason, expiresAt, true);
        String durationText = duration == null ? "permanent" : DurationParser.format(duration);
        notifyTarget(target, "punishment-muted", Map.of(
            "reason", cleanReason,
            "staff", sender.getName(),
            "duration", durationText,
            "target", targetName
        ));
        notifyStaffPunishmentCard(sender, PunishmentType.MUTE, targetName, cleanReason, durationText);
        staffLogManager.log(sender.getName(), "MUTE", targetName, cleanReason + " [" + durationText + "]");
        return entry;
    }

    public boolean unmute(CommandSender sender, OfflinePlayer target, String targetName) {
        Optional<PunishmentEntry> active = activeMute(target.getUniqueId());
        if (active.isEmpty()) {
            return false;
        }
        markInactive(active.get().id());
        notifyTarget(target, "punishment-unmuted", Map.of("staff", sender.getName()));
        staffLogManager.log(sender.getName(), "UNMUTE", targetName, "Manual unmute");
        return true;
    }

    public PunishmentEntry ban(CommandSender sender, OfflinePlayer target, String targetName, String reason, Duration duration) {
        String cleanReason = normalizeReason(reason);
        if (plugin instanceof StaffCore staffCore) {
            staffCore.freezeManager().clearForBan(target.getUniqueId(), targetName, sender.getName());
        }
        Long expiresAt = duration == null ? null : System.currentTimeMillis() + duration.toMillis();
        PunishmentType type = duration == null ? PunishmentType.BAN : PunishmentType.TEMPBAN;
        PunishmentEntry entry = add(type, sender, target, targetName, cleanReason, expiresAt, true);
        Date expiration = expiresAt == null ? null : new Date(expiresAt);

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, cleanReason, expiration, sender.getName());

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            String durationText = duration == null ? "permanent" : DurationParser.format(duration);
            onlineTarget.kickPlayer(messages.resolve(onlineTarget, "punishment-ban-screen", Map.of(
                "reason", cleanReason,
                "staff", sender.getName(),
                "duration", durationText,
                "date", nowDateString(),
                "appeal", moderationLink("appeal-url", "-"),
                "store", moderationLink("store-url", "-"),
                "server", plugin.getServer().getName(),
                "target", targetName
            )));
        }

        String durationText = duration == null ? "permanent" : DurationParser.format(duration);
        notifyStaffPunishmentCard(sender, type, targetName, cleanReason, durationText);
        staffLogManager.log(sender.getName(), type.name(), targetName, cleanReason);
        return entry;
    }

    public boolean unban(CommandSender sender, String targetName) {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        boolean deactivated = deactivateActiveBansByTargetName(targetName);
        if (!banList.isBanned(targetName) && !deactivated) {
            return false;
        }
        banList.pardon(targetName);
        clearFreezeForTargetName(targetName);

        staffLogManager.log(sender.getName(), "UNBAN", targetName, "Manual unban");
        return true;
    }

    public PunishmentEntry banIp(CommandSender sender, OfflinePlayer target, String targetName, String ipAddress, String reason, Duration duration) {
        String cleanReason = normalizeReason(reason);
        if (plugin instanceof StaffCore staffCore) {
            staffCore.freezeManager().clearForBan(target.getUniqueId(), targetName, sender.getName());
        }
        Long expiresAt = duration == null ? null : System.currentTimeMillis() + duration.toMillis();
        PunishmentType type = duration == null ? PunishmentType.BAN_IP : PunishmentType.TEMPBAN_IP;
        PunishmentEntry entry = add(type, sender, target, targetName, cleanReason, expiresAt, true);
        Date expiration = expiresAt == null ? null : new Date(expiresAt);

        if (ipAddress != null && !ipAddress.isBlank()) {
            Bukkit.getBanList(BanList.Type.IP).addBan(ipAddress, cleanReason, expiration, sender.getName());
        }
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, cleanReason, expiration, sender.getName());

        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
        if (onlineTarget != null) {
            String durationText = duration == null ? "permanent" : DurationParser.format(duration);
            onlineTarget.kickPlayer(messages.resolve(onlineTarget, "punishment-ban-screen", Map.of(
                "reason", cleanReason,
                "staff", sender.getName(),
                "duration", durationText,
                "date", nowDateString(),
                "appeal", moderationLink("appeal-url", "-"),
                "store", moderationLink("store-url", "-"),
                "server", plugin.getServer().getName(),
                "target", targetName
            )));
        }

        String durationText = duration == null ? "permanent" : DurationParser.format(duration);
        notifyStaffPunishmentCard(sender, type, targetName, cleanReason, durationText);
        staffLogManager.log(sender.getName(), type.name(), targetName, cleanReason + (ipAddress != null ? " [IP: " + ipAddress + "]" : ""));
        return entry;
    }

    public boolean unbanIp(CommandSender sender, String targetOrIp) {
        BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
        BanList nameBanList = Bukkit.getBanList(BanList.Type.NAME);
        boolean unbanned = false;

        if (ipBanList.isBanned(targetOrIp)) {
            ipBanList.pardon(targetOrIp);
            unbanned = true;
        }
        if (nameBanList.isBanned(targetOrIp)) {
            nameBanList.pardon(targetOrIp);
            unbanned = true;
        }
        boolean deactivated = deactivateActiveBansByTargetName(targetOrIp);
        if (unbanned || deactivated) {
            clearFreezeForTargetName(targetOrIp);
            staffLogManager.log(sender.getName(), "UNBAN_IP", targetOrIp, "Manual IP unban");
            return true;
        }
        return false;
    }

    public Optional<PunishmentEntry> activeMute(UUID targetUuid) {
        expireEntries();
        return entries.stream()
                .filter(PunishmentEntry::active)
                .filter(entry -> entry.type() == PunishmentType.MUTE)
                .filter(entry -> entry.targetUuid().equals(targetUuid))
                .sorted(Comparator.comparingInt(PunishmentEntry::id).reversed())
                .findFirst();
    }

    public Optional<PunishmentEntry> activeBanByIdentity(UUID targetUuid, String targetName) {
        expireEntries();
        String normalizedName = targetName == null ? "" : targetName.toLowerCase(Locale.ROOT);
        return entries.stream()
            .filter(PunishmentEntry::active)
            .filter(entry -> entry.type() == PunishmentType.BAN || entry.type() == PunishmentType.TEMPBAN
                || entry.type() == PunishmentType.BAN_IP || entry.type() == PunishmentType.TEMPBAN_IP)
            .filter(entry -> {
                if (targetUuid != null && entry.targetUuid().equals(targetUuid)) {
                    return true;
                }
                return !normalizedName.isBlank() && entry.targetName().toLowerCase(Locale.ROOT).equals(normalizedName);
            })
            .sorted(Comparator.comparingLong(PunishmentEntry::createdAt).reversed())
            .findFirst();
    }

    public boolean isMuted(UUID targetUuid) {
        return activeMute(targetUuid).isPresent();
    }

    public List<PunishmentEntry> history(UUID targetUuid, int limit) {
        expireEntries();
        return entries.stream()
            .filter(entry -> entry.targetUuid().equals(targetUuid))
            .sorted(Comparator.comparingLong(PunishmentEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public List<PunishmentEntry> historyByIdentity(UUID targetUuid, String targetName, int limit) {
        expireEntries();
        String normalizedName = targetName == null ? "" : targetName.toLowerCase(Locale.ROOT);
        return entries.stream()
            .filter(entry -> {
                if (targetUuid != null && entry.targetUuid().equals(targetUuid)) {
                    return true;
                }
                return !normalizedName.isBlank() && entry.targetName().toLowerCase(Locale.ROOT).equals(normalizedName);
            })
            .sorted(Comparator.comparingLong(PunishmentEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public Optional<UUID> latestKnownTargetUuidByName(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return Optional.empty();
        }
        String normalized = targetName.toLowerCase(Locale.ROOT);
        return entries.stream()
            .filter(entry -> entry.targetName().toLowerCase(Locale.ROOT).equals(normalized))
            .sorted(Comparator.comparingLong(PunishmentEntry::createdAt).reversed())
            .map(PunishmentEntry::targetUuid)
            .findFirst();
    }

    public List<PunishmentEntry> activePunishments(int limit) {
        expireEntries();
        return entries.stream()
                .filter(PunishmentEntry::active)
                .sorted(Comparator.comparingLong(PunishmentEntry::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    public void reload() {
        prepareForReload();
        entries.clear();
        nextId = 1;
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("punishments");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection row = section.getConfigurationSection(key);
            if (row == null) {
                continue;
            }

            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }

            String type = row.getString("type");
            String targetUuid = row.getString("target-uuid");
            String staffUuid = row.getString("staff-uuid");
            if (type == null || targetUuid == null || staffUuid == null) {
                continue;
            }

            try {
                String cleanReason = normalizeReason(row.getString("reason", ""));
                PunishmentEntry entry = new PunishmentEntry(
                        id,
                        PunishmentType.valueOf(type),
                        UUID.fromString(targetUuid),
                        row.getString("target-name", "Unknown"),
                        UUID.fromString(staffUuid),
                        row.getString("staff-name", "Unknown"),
                        cleanReason,
                        row.getLong("created-at"),
                        row.contains("expires-at") ? row.getLong("expires-at") : null,
                        row.getBoolean("active")
                );
                entries.add(entry);
                nextId = Math.max(nextId, id + 1);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid punishment row #" + id);
            }
        }
        expireEntries();
        syncBanStateWithEntries();
    }

    public void save() {
        flushNow();
    }

    public int totalPunishments() {
        return entries.size();
    }

    public int activePunishmentsCount() {
        int count = 0;
        for (PunishmentEntry entry : entries) {
            if (entry.active()) {
                count++;
            }
        }
        return count;
    }

    public boolean saveQueued() {
        return super.isSaveQueued();
    }

    public boolean asyncWriteInProgress() {
        return super.isAsyncWriteInProgress();
    }

    @Override
    protected YamlConfiguration buildSnapshot() {
        YamlConfiguration config = new YamlConfiguration();
        for (PunishmentEntry entry : entries) {
            String path = "punishments." + entry.id() + ".";
            config.set(path + "type", entry.type().name());
            config.set(path + "target-uuid", entry.targetUuid().toString());
            config.set(path + "target-name", entry.targetName());
            config.set(path + "staff-uuid", entry.staffUuid().toString());
            config.set(path + "staff-name", entry.staffName());
            config.set(path + "reason", entry.reason());
            config.set(path + "created-at", entry.createdAt());
            config.set(path + "expires-at", entry.expiresAt());
            config.set(path + "active", entry.active());
        }
        return config;
    }

    private PunishmentEntry add(PunishmentType type, CommandSender sender, OfflinePlayer target, String targetName, String reason, Long expiresAt, boolean active) {
        String cleanReason = normalizeReason(reason);
        UUID senderUuid = sender instanceof Player player
            ? player.getUniqueId()
            : UUID.nameUUIDFromBytes(("console:" + sender.getName()).getBytes(StandardCharsets.UTF_8));
        PunishmentEntry entry = new PunishmentEntry(
                nextId++,
                type,
                target.getUniqueId(),
                targetName,
                senderUuid,
                sender.getName(),
                cleanReason,
                System.currentTimeMillis(),
                expiresAt,
                active
        );
        entries.add(entry);
        queueSave();
        return entry;
    }

    private void notifyTarget(OfflinePlayer target, String messageKey, Map<String, String> placeholders) {
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            messages.send(online, messageKey, placeholders);
        }
    }

    private void expireEntries() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        List<PunishmentEntry> updated = new ArrayList<>(entries.size());
        for (PunishmentEntry entry : entries) {
            if (entry.active() && entry.expiresAt() != null && entry.expiresAt() <= now) {
                updated.add(new PunishmentEntry(entry.id(), entry.type(), entry.targetUuid(), entry.targetName(), entry.staffUuid(), entry.staffName(), entry.reason(), entry.createdAt(), entry.expiresAt(), false));
                changed = true;
            } else {
                updated.add(entry);
            }
        }
        if (changed) {
            entries.clear();
            entries.addAll(updated);
            queueSave();
        }
        syncBanStateWithEntries();
    }

    private void markInactive(int id) {
        List<PunishmentEntry> updated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (PunishmentEntry entry : entries) {
            if (entry.id() == id && entry.active()) {
                updated.add(new PunishmentEntry(entry.id(), entry.type(), entry.targetUuid(), entry.targetName(), entry.staffUuid(), entry.staffName(), entry.reason(), entry.createdAt(), entry.expiresAt(), false));
                changed = true;
            } else {
                updated.add(entry);
            }
        }
        if (changed) {
            entries.clear();
            entries.addAll(updated);
            queueSave();
        }
    }

    private void syncBanStateWithEntries() {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        long now = System.currentTimeMillis();
        boolean changed = false;
        List<PunishmentEntry> updated = new ArrayList<>(entries.size());

        for (PunishmentEntry entry : entries) {
            if (!entry.active() || (entry.type() != PunishmentType.BAN && entry.type() != PunishmentType.TEMPBAN)) {
                updated.add(entry);
                continue;
            }

            if (entry.expiresAt() != null && entry.expiresAt() <= now) {
                if (!hasOtherActiveBan(entry.targetName(), entry.id())) {
                    banList.pardon(entry.targetName());
                }
                updated.add(new PunishmentEntry(
                    entry.id(),
                    entry.type(),
                    entry.targetUuid(),
                    entry.targetName(),
                    entry.staffUuid(),
                    entry.staffName(),
                    entry.reason(),
                    entry.createdAt(),
                    entry.expiresAt(),
                    false
                ));
                changed = true;
                continue;
            }

            Date expiration = entry.expiresAt() == null ? null : new Date(entry.expiresAt());
            banList.addBan(entry.targetName(), entry.reason(), expiration, entry.staffName());
            updated.add(entry);
        }

        if (changed) {
            entries.clear();
            entries.addAll(updated);
            queueSave();
        }
    }

    private boolean hasOtherActiveBan(String targetName, int ignoreId) {
        return entries.stream()
            .filter(PunishmentEntry::active)
            .filter(entry -> entry.id() != ignoreId)
            .filter(entry -> entry.type() == PunishmentType.BAN || entry.type() == PunishmentType.TEMPBAN)
            .anyMatch(entry -> entry.targetName().equalsIgnoreCase(targetName));
    }

    private boolean deactivateActiveBansByTargetName(String targetName) {
        boolean changed = false;
        List<PunishmentEntry> updated = new ArrayList<>(entries.size());
        for (PunishmentEntry entry : entries) {
            boolean isBan = entry.type() == PunishmentType.BAN || entry.type() == PunishmentType.TEMPBAN;
            if (entry.active() && isBan && entry.targetName().equalsIgnoreCase(targetName)) {
                updated.add(new PunishmentEntry(
                    entry.id(),
                    entry.type(),
                    entry.targetUuid(),
                    entry.targetName(),
                    entry.staffUuid(),
                    entry.staffName(),
                    entry.reason(),
                    entry.createdAt(),
                    entry.expiresAt(),
                    false
                ));
                changed = true;
                continue;
            }
            updated.add(entry);
        }
        if (changed) {
            entries.clear();
            entries.addAll(updated);
            queueSave();
        }
        return changed;
    }

    private void clearFreezeForTargetName(String targetName) {
        if (!(plugin instanceof StaffCore staffCore)) {
            return;
        }
        Set<UUID> uuids = new HashSet<>();
        for (PunishmentEntry entry : entries) {
            if (entry.targetName().equalsIgnoreCase(targetName)) {
                uuids.add(entry.targetUuid());
            }
        }
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            uuids.add(online.getUniqueId());
        }
        for (UUID uuid : uuids) {
            staffCore.freezeManager().remove(uuid);
        }
        staffCore.freezeManager().removeByName(targetName);
    }

    private String moderationLink(String key, String fallback) {
        String value = plugin.getConfig().getString("moderation.links." + key, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String nowDateString() {
        return LocalDateTime.now().format(SCREEN_DATE_FORMAT);
    }

    private String normalizeReason(String reason) {
        return ModerationGuard.sanitizeReason(reason);
    }

    private void notifyStaffPunishmentCard(CommandSender sender, PunishmentType type, String targetName, String reason, String durationText) {
        String safeDuration = durationText == null || durationText.isBlank() ? "-" : durationText;
        String safeReason = normalizeReason(reason);
        String staffName = sender.getName();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!Permissions.hasAny(
                online,
                "staffcore.staff",
                "staffcore.freeze",
                "staffcore.punish.warn",
                "staffcore.punish.mute",
                "staffcore.punish.kick",
                "staffcore.punish.ban",
                "staffcore.punish.tempban"
            )) {
                continue;
            }
            online.sendMessage(messages.resolve(online, "punishment-staff-card", Map.of(
                "action", messages.resolve(online, type.translationKey()),
                "staff", staffName,
                "target", targetName,
                "duration", safeDuration,
                "reason", safeReason
            )));
        }
    }
}
