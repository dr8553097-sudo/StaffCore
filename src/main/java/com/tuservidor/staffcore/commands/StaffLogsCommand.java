package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.StaffLogEntry;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class StaffLogsCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffLogsCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(player, "staffcore.logs")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            if (!Permissions.has(player, "staffcore.logs.clear")) {
                plugin.messages().send(player, "no-permission");
                return true;
            }
            int removed = plugin.staffLogManager().clearAll();
            plugin.messages().send(player, "stafflogs-cleared", java.util.Map.of("count", String.valueOf(removed)));
            return true;
        }

        List<StaffLogEntry> logs = args.length == 0
            ? plugin.staffLogManager().recent(15)
            : plugin.staffLogManager().searchByPlayer(args[0], 15);

        if (logs.isEmpty()) {
            plugin.messages().send(player, "logs-empty");
            return true;
        }

        player.sendMessage(plugin.messages().resolve(player, "stafflogs-header"));
        for (StaffLogEntry entry : logs) {
            player.sendMessage(plugin.messages().resolve(player, "stafflogs-entry", java.util.Map.of(
                "id", String.valueOf(entry.id()),
                "action", entry.action(),
                "staff", entry.staff(),
                "target", entry.target(),
                "details", entry.details()
            )));
        }
        return true;
    }
}
