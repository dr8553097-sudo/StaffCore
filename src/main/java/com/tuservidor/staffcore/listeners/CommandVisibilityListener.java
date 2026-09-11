package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Locale;

public final class CommandVisibilityListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public CommandVisibilityListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (support.isStaffCoreNamespaceCall(event.getMessage()) && !Permissions.has(player, "staffcore.admin")) {
            event.setCancelled(true);
            plugin.messages().send(player, "command-hidden");
            return;
        }
        if (!support.isSecurityHideEnabled()) {
            return;
        }
        if (support.shouldHideCommandFrom(player, event.getMessage())) {
            event.setCancelled(true);
            plugin.messages().send(player, "command-hidden");
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!support.isSecurityHideEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        event.getCommands().removeIf(command -> command.toLowerCase(Locale.ROOT).startsWith("staffcore:"));
        event.getCommands().removeIf(command -> support.shouldHideCommandFrom(player, command));
    }
}
