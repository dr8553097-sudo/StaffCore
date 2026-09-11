package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ListenerSupport {

    private static final DateTimeFormatter BAN_SCREEN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final StaffCore plugin;
    private final NamespacedKey toolKey;
    private final Map<UUID, Long> freezeReminderAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> chatMuteNoticeAt = new ConcurrentHashMap<>();
    private final Map<String, Long> actionCooldown = new ConcurrentHashMap<>();
    private final Map<String, String[]> hiddenCommandPermissions;

    public ListenerSupport(StaffCore plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "staffcore-tool");
        this.hiddenCommandPermissions = CommandVisibilityPolicy.defaultHiddenRules();
    }

    void clearPlayerState(UUID uuid) {
        freezeReminderAt.remove(uuid);
        chatMuteNoticeAt.remove(uuid);
    }

    String toolName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    boolean canRunToolAction(String key) {
        long now = System.currentTimeMillis();
        long last = actionCooldown.getOrDefault(key, 0L);
        long cooldown = Math.max(120L, plugin.getConfig().getLong("staff-tools.action-cooldown-ms", 300L));
        if (now - last < cooldown) {
            return false;
        }
        actionCooldown.put(key, now);
        return true;
    }

    Player teleportTarget(Player staff) {
        int maxDistance = Math.max(5, plugin.getConfig().getInt("staff-tools.teleporter.max-distance", 120));
        Entity lookedEntity = staff.getTargetEntity(maxDistance);
        if (lookedEntity instanceof Player directTarget && !directTarget.getUniqueId().equals(staff.getUniqueId())) {
            return directTarget;
        }
        RayTraceResult rayTrace = staff.getWorld().rayTraceEntities(
            staff.getEyeLocation(),
            staff.getEyeLocation().getDirection(),
            maxDistance,
            entity -> entity instanceof Player player && !player.getUniqueId().equals(staff.getUniqueId())
        );
        if (rayTrace != null && rayTrace.getHitEntity() instanceof Player lookedPlayer) {
            return lookedPlayer;
        }

        double bestScore = -1.0D;
        Player best = null;
        Vector direction = staff.getEyeLocation().getDirection().normalize();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(staff.getUniqueId())) {
                continue;
            }
            Vector delta = player.getEyeLocation().toVector().subtract(staff.getEyeLocation().toVector());
            double distance = delta.length();
            if (distance <= 0.01D || distance > maxDistance) {
                continue;
            }
            Vector toTarget = delta.clone().normalize();
            double dot = direction.dot(toTarget);
            if (dot < 0.95D) {
                continue;
            }
            double score = dot - (distance / (double) maxDistance) * 0.15D;
            if (score > bestScore) {
                bestScore = score;
                best = player;
            }
        }
        if (best != null) {
            return best;
        }

        List<Player> online = Bukkit.getOnlinePlayers().stream()
            .filter(player -> !player.getUniqueId().equals(staff.getUniqueId()))
            .collect(Collectors.toList());
        if (online.size() == 1) {
            return online.get(0);
        }
        return null;
    }

    boolean shouldBlockWorldInteraction(Action action) {
        return action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK || action == Action.PHYSICAL;
    }

    Player damagerAsPlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (!(entity instanceof Projectile projectile)) {
            return null;
        }
        ProjectileSource source = projectile.getShooter();
        return source instanceof Player player ? player : null;
    }

    boolean isStaffDamageBypassedByOp(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return false;
        }
        Player attacker = damagerAsPlayer(byEntity.getDamager());
        return attacker != null && attacker.isOp();
    }

    void sendStaffWorldBlocked(Player player) {
        long cooldown = Math.max(250L, plugin.getConfig().getLong("staff-mode.interaction-notice-cooldown-ms", 1200L));
        String key = "notice:staff-world:" + player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = actionCooldown.getOrDefault(key, 0L);
        if (now - last < cooldown) {
            return;
        }
        actionCooldown.put(key, now);
        plugin.messages().send(player, "staff-world-blocked");
    }

    void sendVanishWorldBlocked(Player player) {
        long cooldown = Math.max(250L, plugin.getConfig().getLong("vanish.interaction-notice-cooldown-ms", 1200L));
        String key = "notice:vanish-world:" + player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = actionCooldown.getOrDefault(key, 0L);
        if (now - last < cooldown) {
            return;
        }
        actionCooldown.put(key, now);
        plugin.messages().send(player, "vanish-world-blocked");
    }

    boolean isFrozenRestricted(Player player) {
        if (!plugin.featureEnabled("freeze")) {
            return false;
        }
        return plugin.freezeManager().isFrozen(player) && !Permissions.has(player, "staffcore.freeze.bypass");
    }

    boolean isVanishWorldModificationRestricted(Player player) {
        if (!plugin.featureEnabled("vanish")
            || !plugin.vanishManager().isVanished(player)) {
            return false;
        }
        return plugin.getConfig().getBoolean("vanish.prevent-world-modification-while-vanish", true);
    }

    boolean isVanishInteractionRestricted(Player player) {
        if (!plugin.featureEnabled("vanish")
            || !plugin.vanishManager().isVanished(player)) {
            return false;
        }
        return plugin.getConfig().getBoolean("vanish.prevent-world-interactions-while-vanish", true);
    }

    boolean shouldBlockFrozenTeleport(PlayerTeleportEvent.TeleportCause cause) {
        return cause != PlayerTeleportEvent.TeleportCause.UNKNOWN;
    }

    boolean isSecurityHideEnabled() {
        return plugin.getConfig().getBoolean("security.hide-staff-commands", true);
    }

    boolean shouldHideCommandFrom(Player player, String rawCommand) {
        return CommandVisibilityPolicy.shouldHideCommandFrom(player, rawCommand, hiddenCommandPermissions);
    }

    boolean isStaffCoreNamespaceCall(String rawMessage) {
        return CommandVisibilityPolicy.isStaffCoreNamespaceCall(rawMessage);
    }

    String normalizeCommand(String rawCommand) {
        return FreezeCommandPolicy.normalizeCommand(rawCommand);
    }

    boolean isAllowedCommandWhileFrozen(String command) {
        return FreezeCommandPolicy.isAllowedWhileFrozen(
            command,
            plugin.getConfig().getStringList("freeze.allowed-commands-while-frozen"),
            plugin.getConfig().getStringList("freeze.blocked-commands-while-frozen")
        );
    }

    boolean shouldBlockByGlobalChatMute(Player player) {
        if (!plugin.isChatMuted()) {
            return false;
        }
        return !canBypassGlobalChatMute(player);
    }

    void sendChatMutedNotice(Player player) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(250L, plugin.getConfig().getLong("chat-moderation.blocked-notice-cooldown-ms", 900L));
        long last = chatMuteNoticeAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldown) {
            return;
        }
        chatMuteNoticeAt.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().send(player, "chatmute-chat-blocked"));
    }

    void sendFreezeReminder(Player player) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, plugin.getConfig().getLong("freeze.reminder-cooldown-ms", 1200L));
        long last = freezeReminderAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldown) {
            return;
        }
        freezeReminderAt.put(player.getUniqueId(), now);
        plugin.messages().send(player, "freeze-reminder");
    }

    boolean isPrivateMessageCommand(String command) {
        return command.equals("msg")
            || command.equals("m")
            || command.equals("message")
            || command.equals("pm")
            || command.equals("dm")
            || command.equals("tell")
            || command.equals("t")
            || command.equals("w")
            || command.equals("whisper")
            || command.equals("msgto")
            || command.equals("etell");
    }

    boolean tryRouteFreezePrivateChat(Player sender, String rawMessage) {
        if (!plugin.featureEnabled("freeze")) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("freeze.private-chat-enabled", true)) {
            return false;
        }
        if (rawMessage == null) {
            return false;
        }
        String message = rawMessage.trim();
        if (message.isEmpty()) {
            return false;
        }

        if (isFrozenRestricted(sender)) {
            Optional<Player> staff = plugin.freezeManager().assignedStaff(sender);
            if (staff.isEmpty() || !staff.get().isOnline()) {
                plugin.messages().send(sender, "freeze-private-chat-unavailable");
                return true;
            }
            Player receiver = staff.get();
            plugin.messages().send(sender, "freeze-private-chat-player-sent", Map.of(
                "staff", receiver.getName(),
                "message", message
            ));
            plugin.messages().send(receiver, "freeze-private-chat-player-received", Map.of(
                "player", sender.getName(),
                "message", message
            ));
            plugin.staffLogManager().log(sender.getName(), "FREEZE_CHAT", receiver.getName(), message);
            return true;
        }

        if (!plugin.getConfig().getBoolean("freeze.private-chat-auto-route-staff", true)) {
            return false;
        }
        if (plugin.staffChatManager().isEnabled(sender.getUniqueId()) || message.startsWith("@")) {
            return false;
        }
        if (!ModerationGuard.isStaffMember(sender)) {
            return false;
        }

        Optional<Player> assignedFrozen = plugin.freezeManager().assignedFrozen(sender);
        if (assignedFrozen.isPresent()) {
            Player receiver = assignedFrozen.get();
            if (!isFrozenRestricted(receiver)) {
                return false;
            }
            plugin.messages().send(sender, "freeze-private-chat-staff-sent", Map.of(
                "player", receiver.getName(),
                "message", message
            ));
            plugin.messages().send(receiver, "freeze-private-chat-staff-received", Map.of(
                "staff", sender.getName(),
                "message", message
            ));
            plugin.staffLogManager().log(sender.getName(), "FREEZE_CHAT", receiver.getName(), message);
            return true;
        }

        if (plugin.freezeManager().activeAssignedFrozenCount(sender) > 1) {
            plugin.messages().send(sender, "freeze-private-chat-multiple");
        }
        return false;
    }

    String moderationLink(String key, String fallback) {
        String value = plugin.getConfig().getString("moderation.links." + key, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    String formatBanDate(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(BAN_SCREEN_DATE_FORMAT);
    }

    private boolean canBypassGlobalChatMute(Player player) {
        if (player.isOp()) {
            return true;
        }
        String bypassPermission = plugin.getConfig().getString("chat-moderation.bypass-permission", "staffcore.chatmute.bypass");
        if (bypassPermission == null || bypassPermission.isBlank()) {
            return false;
        }
        return Permissions.has(player, bypassPermission);
    }
}
