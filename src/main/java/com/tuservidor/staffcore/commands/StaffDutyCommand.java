package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffDutyCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffDutyCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly in-game staff members can use this command.");
            return true;
        }

        if (!player.hasPermission("staffcore.staff")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }

        if (!plugin.featureEnabled("staff-duty")) {
            player.sendMessage("§cThe Staff Duty module is currently disabled in config.yml.");
            return true;
        }

        plugin.staffDutyManager().toggleDuty(player);
        return true;
    }
}
