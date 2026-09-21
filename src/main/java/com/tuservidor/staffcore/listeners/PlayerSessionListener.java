package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

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
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        support.clearPlayerState(player.getUniqueId());
        plugin.staffChatManager().disable(player.getUniqueId());
        plugin.reportManager().clearRejectFlow(player.getUniqueId());
        plugin.reportManager().clearCooldown(player.getUniqueId());
        plugin.freezeManager().handleStaffQuit(player);
        plugin.freezeManager().handleQuit(player);
        if (plugin.staffHudManager() != null) {
            plugin.staffHudManager().removePlayer(player.getUniqueId());
        }
        if (plugin.staffManager().isStaff(player)) {
            plugin.staffManager().disable(player);
        }
    }
}
