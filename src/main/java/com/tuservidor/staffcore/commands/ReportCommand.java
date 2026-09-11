package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.reports.Report;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ReportCommand implements CommandExecutor {

    private final StaffCore plugin;

    public ReportCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(reporter, "staffcore.report")) {
            plugin.messages().send(reporter, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("reports")) {
            plugin.messages().send(reporter, "module-disabled", Map.of("module", "reports"));
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(reporter, "report-usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(reporter, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (target.equals(reporter)) {
            plugin.messages().send(reporter, "cannot-target-self");
            return true;
        }
        if (!plugin.getConfig().getBoolean("reports.allow-protected-targets", false)
            && !ModerationGuard.canTarget(plugin, reporter, target)) {
            plugin.messages().send(reporter, "target-protected");
            return true;
        }

        long cooldown = plugin.reportManager().remainingCooldownSeconds(reporter);
        if (cooldown > 0) {
            plugin.messages().send(reporter, "report-cooldown", Map.of("seconds", String.valueOf(cooldown)));
            return true;
        }
        if (plugin.reportManager().hasOpenReport(reporter, target)) {
            plugin.messages().send(reporter, "report-duplicate", Map.of("player", target.getName()));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        int maxReasonLength = plugin.getConfig().getInt("reports.max-reason-length", 180);
        if (reason.length() > maxReasonLength) {
            plugin.messages().send(reporter, "report-reason-too-long", Map.of("max", String.valueOf(maxReasonLength)));
            return true;
        }
        Report report = plugin.reportManager().create(reporter, target, reason);
        plugin.messages().send(reporter, "report-created", Map.of("id", String.valueOf(report.id()), "player", target.getName()));
        return true;
    }
}
