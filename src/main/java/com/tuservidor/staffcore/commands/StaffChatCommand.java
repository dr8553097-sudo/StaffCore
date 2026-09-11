package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class StaffChatCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffChatCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.staffchat")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("staff-chat")) {
            plugin.messages().send(sender, "module-disabled", java.util.Map.of("module", "staff-chat"));
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "players-only");
                return true;
            }
            boolean enabled = plugin.staffChatManager().toggle(player.getUniqueId());
            plugin.messages().send(player, enabled ? "staffchat-enabled" : "staffchat-disabled");
            return true;
        }

        if (sender instanceof Player player && args.length == 1 && args[0].equalsIgnoreCase("off")) {
            plugin.staffChatManager().disable(player.getUniqueId());
            plugin.messages().send(player, "staffchat-disabled");
            return true;
        }
        if (sender instanceof Player player && args.length == 1 && args[0].equalsIgnoreCase("on")) {
            if (!plugin.staffChatManager().isEnabled(player.getUniqueId())) {
                plugin.staffChatManager().toggle(player.getUniqueId());
            }
            plugin.messages().send(player, "staffchat-enabled");
            return true;
        }

        String message = String.join(" ", args);
        String consoleFormatted = plugin.messages().resolve("staffchat-format", Map.of(
            "player", sender.getName(),
            "message", message
        ));
        Bukkit.getConsoleSender().sendMessage(consoleFormatted);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Permissions.has(player, "staffcore.staffchat")) {
                player.sendMessage(plugin.messages().resolve(player, "staffchat-format", Map.of(
                    "player", sender.getName(),
                    "message", message
                )));
            }
        }
        plugin.staffLogManager().log(sender.getName(), "STAFF_CHAT", "staff-channel", message);
        return true;
    }
}
