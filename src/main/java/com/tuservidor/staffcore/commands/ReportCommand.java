package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.reports.Report;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

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

        String targetName = args[0];
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        
        if (onlineTarget != null) {
            targetName = onlineTarget.getName();
            targetUuid = onlineTarget.getUniqueId();
            if (onlineTarget.equals(reporter)) {
                plugin.messages().send(reporter, "cannot-target-self");
                return true;
            }
            if (!plugin.getConfig().getBoolean("reports.allow-protected-targets", false)
                && !ModerationGuard.canTarget(plugin, reporter, onlineTarget)) {
                plugin.messages().send(reporter, "target-protected");
                return true;
            }
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null) {
                targetName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
                targetUuid = offlineTarget.getUniqueId();
            } else {
                targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if (reporter.getUniqueId().equals(targetUuid) || reporter.getName().equalsIgnoreCase(targetName)) {
                plugin.messages().send(reporter, "cannot-target-self");
                return true;
            }
        }

        long cooldown = plugin.reportManager().remainingCooldownSeconds(reporter);
        if (cooldown > 0) {
            plugin.messages().send(reporter, "report-cooldown", Map.of("seconds", String.valueOf(cooldown)));
            return true;
        }
        if (plugin.reportManager().hasOpenReport(reporter.getUniqueId(), targetUuid, targetName)) {
            plugin.messages().send(reporter, "report-duplicate", Map.of("player", targetName));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        int maxReasonLength = plugin.getConfig().getInt("reports.max-reason-length", 180);
        if (reason.length() > maxReasonLength) {
            plugin.messages().send(reporter, "report-reason-too-long", Map.of("max", String.valueOf(maxReasonLength)));
            return true;
        }
        Report report = plugin.reportManager().create(reporter, targetUuid, targetName, reason);
        plugin.messages().send(reporter, "report-created", Map.of("id", String.valueOf(report.id()), "player", targetName));
        return true;
    }
}
