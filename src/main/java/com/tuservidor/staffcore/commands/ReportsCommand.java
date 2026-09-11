package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.reports.Report;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class ReportsCommand implements CommandExecutor {

    private final StaffCore plugin;

    public ReportsCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.reports.view")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("reports")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "reports"));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("close")) {
            try {
                int id = Integer.parseInt(args[1]);
                if (!plugin.reportManager().close(id, sender)) {
                    plugin.messages().send(sender, "report-not-found", Map.of("id", args[1]));
                }
            } catch (NumberFormatException exception) {
                plugin.messages().send(sender, "report-invalid-id", Map.of("id", args[1]));
            }
            return true;
        }

        if (args.length == 0 && sender instanceof Player player) {
            plugin.menuManager().openReports(player);
            return true;
        }

        List<Report> reports = plugin.reportManager().openReports();
        if (reports.isEmpty()) {
            plugin.messages().send(sender, "reports-empty");
            return true;
        }

        sender.sendMessage(plugin.messages().resolve(sender, "reports-list-header"));
        reports.stream().limit(8).forEach(report -> sender.sendMessage(plugin.messages().resolve(sender, "reports-list-entry", Map.of(
            "id", String.valueOf(report.id()),
            "target", report.targetName(),
            "reporter", report.reporterName(),
            "reason", report.reason()
        ))));
        sender.sendMessage(plugin.messages().resolve(sender, "reports-list-footer"));
        return true;
    }
}
