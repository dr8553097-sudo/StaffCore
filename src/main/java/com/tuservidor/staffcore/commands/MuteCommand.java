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

public final class MuteCommand implements CommandExecutor {

    private final StaffCore plugin;

    public MuteCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.mute")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "mute-usage");
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName() == null ? args[0] : target.getName();
        if (sender instanceof Player staff && onlineTarget != null && !ModerationGuard.canTarget(plugin, staff, onlineTarget)) {
            plugin.messages().send(sender, "target-protected");
            return true;
        }

        Duration duration = DurationParser.parse(args[1]);
        if (duration != null && duration.isZero()) {
            plugin.messages().send(sender, "invalid-duration", Map.of("duration", args[1]));
            return true;
        }
        String reason = ModerationGuard.sanitizeReason(String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)));
        if (!ModerationGuard.validReason(plugin, reason)) {
            plugin.messages().send(sender, "reason-too-short");
            return true;
        }
        String durationText = duration == null ? "permanent" : DurationParser.format(duration);
        plugin.punishmentManager().mute(sender, target, targetName, reason, duration);
        plugin.messages().send(sender, "punishment-mute-sent", Map.of(
            "player", targetName,
            "duration", durationText
        ));
        return true;
    }
}
