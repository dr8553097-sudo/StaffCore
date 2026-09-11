package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class UnbanCommand implements CommandExecutor {

    private final StaffCore plugin;

    public UnbanCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.unban")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(sender, "unban-usage");
            return true;
        }
        if (!plugin.punishmentManager().unban(sender, args[0])) {
            plugin.messages().send(sender, "ban-not-found", Map.of("player", args[0]));
            return true;
        }
        plugin.messages().send(sender, "punishment-unban-sent", Map.of("player", args[0]));
        return true;
    }
}
