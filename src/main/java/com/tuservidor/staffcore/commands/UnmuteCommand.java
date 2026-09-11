package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class UnmuteCommand implements CommandExecutor {

    private final StaffCore plugin;

    public UnmuteCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.unmute")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(sender, "unmute-usage");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName() == null ? args[0] : target.getName();
        if (!plugin.punishmentManager().unmute(sender, target, targetName)) {
            plugin.messages().send(sender, "mute-not-found", Map.of("player", targetName));
            return true;
        }
        plugin.messages().send(sender, "punishment-unmute-sent", Map.of("player", targetName));
        return true;
    }
}
