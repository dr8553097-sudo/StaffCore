package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.PunishmentEntry;
import com.tuservidor.staffcore.util.DurationParser;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.ModerationGuard;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerSessionListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public PlayerSessionListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.vanishManager().handleJoin(player);
        plugin.freezeManager().handleJoin(player);
        plugin.xrayAlertManager().trackJoin(player.getUniqueId());
        plugin.updateChecker().handleJoin(player);

        if (plugin.firstJoinManager().markIfFirstJoin(player)) {
            List<String> lines = plugin.uiStringList("welcome.first-join-lines");
            if (lines.isEmpty()) {
                lines = List.of(
                    "&8[&bStaffCore&8] &aThanks for joining this server.",
                    "&8[&bStaffCore&8] &7Have a great time."
                );
            }
            for (String line : lines) {
                player.sendMessage(Messages.color(line.replace("{player}", player.getName())));
            }
            player.sendTitle(
                Messages.color(plugin.uiString("welcome.first-join-title", "&bWelcome")),
                Messages.color(plugin.uiString("welcome.first-join-subtitle", "&7Thanks for joining us")),
                10,
                60,
                15
            );
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.1f);
        }

        Optional<PunishmentEntry> mute = plugin.featureEnabled("punishments")
            ? plugin.punishmentManager().activeMute(player.getUniqueId())
            : Optional.empty();
        if (mute.isPresent()) {
            String duration = mute.get().expiresAt() == null
                ? "permanent"
                : DurationParser.format(Duration.ofMillis(Math.max(0L, mute.get().expiresAt() - System.currentTimeMillis())));
            plugin.messages().send(player, "mute-reminder", Map.of(
                "duration", duration,
                "reason", ModerationGuard.sanitizeReason(mute.get().reason())
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginBanScreen(PlayerLoginEvent event) {
        if (!plugin.featureEnabled("punishments")) {
            return;
        }
        Player player = event.getPlayer();
        String playerName = player.getName();
        Optional<PunishmentEntry> activeBan = plugin.punishmentManager().activeBanByIdentity(player.getUniqueId(), playerName);

        if (activeBan.isPresent()) {
            PunishmentEntry entry = activeBan.get();
            String duration = entry.expiresAt() == null
                ? plugin.messages().resolve(player, "history-duration-permanent")
                : DurationParser.format(Duration.ofMillis(Math.max(0L, entry.expiresAt() - System.currentTimeMillis())));
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, plugin.messages().resolve(player, "punishment-ban-screen", Map.of(
                "reason", ModerationGuard.sanitizeReason(entry.reason()),
                "staff", entry.staffName(),
                "duration", duration,
                "date", support.formatBanDate(entry.createdAt()),
                "appeal", support.moderationLink("appeal-url", "-"),
                "store", support.moderationLink("store-url", "-"),
                "server", plugin.getServer().getName(),
                "target", playerName
            )));
            return;
        }

        if (event.getResult() != PlayerLoginEvent.Result.KICK_BANNED) {
            return;
        }
        String fallbackReason = ModerationGuard.sanitizeReason(event.getKickMessage());
        if (fallbackReason.isBlank()) {
            fallbackReason = "Banned";
        }
        event.disallow(PlayerLoginEvent.Result.KICK_BANNED, plugin.messages().resolve(player, "punishment-ban-screen", Map.of(
            "reason", fallbackReason,
            "staff", "System",
            "duration", plugin.messages().resolve(player, "history-duration-permanent"),
            "date", support.formatBanDate(System.currentTimeMillis()),
            "appeal", support.moderationLink("appeal-url", "-"),
            "store", support.moderationLink("store-url", "-"),
            "server", plugin.getServer().getName(),
            "target", playerName
        )));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        support.clearPlayerState(player.getUniqueId());
        plugin.staffChatManager().disable(player.getUniqueId());
        plugin.reportManager().clearRejectFlow(player.getUniqueId());
        plugin.reportManager().clearCooldown(player.getUniqueId());
        plugin.xrayAlertManager().clearSnapshot(player.getUniqueId());
        plugin.freezeManager().handleStaffQuit(player);
        plugin.freezeManager().handleQuit(player);
        if (plugin.staffManager().isStaff(player)) {
            plugin.staffManager().disable(player);
        }
    }
}
