package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.DurationParser;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public final class TempBanIpCommand implements CommandExecutor {

    private final StaffCore plugin;

    public TempBanIpCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.tempbanip")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "tempbanip-usage");
            return true;
        }

        Duration duration = DurationParser.parse(args[1]);
        if (duration == null || duration.isZero()) {
            plugin.messages().send(sender, "invalid-duration", Map.of("duration", args[1]));
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName() == null ? args[0] : target.getName();
        if (sender instanceof Player staff && onlineTarget != null && !ModerationGuard.canTarget(plugin, staff, onlineTarget)) {
            plugin.messages().send(sender, "target-protected");
            return true;
        }
        String reason = ModerationGuard.sanitizeReason(String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)));
        if (!ModerationGuard.validReason(plugin, reason)) {
            plugin.messages().send(sender, "reason-too-short");
            return true;
        }

        String ipAddress = null;
        if (onlineTarget != null && onlineTarget.getAddress() != null) {
            ipAddress = onlineTarget.getAddress().getAddress().getHostAddress();
        }

        String durationText = DurationParser.format(duration);
        plugin.punishmentManager().banIp(sender, target, targetName, ipAddress, reason, duration);
        plugin.messages().send(sender, "punishment-banip-sent", Map.of(
            "player", targetName,
            "ip", ipAddress == null ? "n/a" : ipAddress,
            "duration", durationText
        ));
        return true;
    }
}
