package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.staff.StaffDutyManager.DutyRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class StaffTopCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffTopCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffcore.staff")) {
            sender.sendMessage("§cYou do not have permission to view staff statistics.");
            return true;
        }

        if (!plugin.featureEnabled("staff-duty")) {
            sender.sendMessage("§cThe Staff Duty module is currently disabled in config.yml.");
            return true;
        }

        List<DutyRecord> topStaff = plugin.staffDutyManager().getTopStaff(10);

        sender.sendMessage("§8§m---------------------------------------------");
        sender.sendMessage("§b§l  STAFFCORE §7— §eTop Active Staff Members (Weekly)");
        sender.sendMessage("§8§m---------------------------------------------");

        if (topStaff.isEmpty()) {
            sender.sendMessage("  §7No duty records found yet for this week.");
        } else {
            int rank = 1;
            for (DutyRecord record : topStaff) {
                long hours = record.weeklyDutySeconds() / 3600;
                long mins = (record.weeklyDutySeconds() % 3600) / 60;
                String color = switch (rank) {
                    case 1 -> "§6§l#1 ";
                    case 2 -> "§f§l#2 ";
                    case 3 -> "§c§l#3 ";
                    default -> "§7#" + rank + " ";
                };
                sender.sendMessage(color + "§e" + record.name() + " §8» §a" + hours + "h " + mins + "m §7(" + record.actionsCount() + " actions)");
                rank++;
            }
        }
        sender.sendMessage("§8§m---------------------------------------------");
        return true;
    }
}
