package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class UnbanIpCommand implements CommandExecutor {

    private final StaffCore plugin;

    public UnbanIpCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.unbanip")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, "unbanip-usage");
            return true;
        }

        String targetOrIp = args[0].trim();
        boolean unbanned = plugin.punishmentManager().unbanIp(sender, targetOrIp);
        if (!unbanned) {
            plugin.messages().send(sender, "ban-not-found", Map.of("player", targetOrIp));
            return true;
        }
        plugin.messages().send(sender, "punishment-unbanip-sent", Map.of("ip", targetOrIp));
        return true;
    }
}
