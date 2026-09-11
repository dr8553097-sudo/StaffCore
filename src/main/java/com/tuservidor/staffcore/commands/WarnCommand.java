package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class WarnCommand implements CommandExecutor {

    private final StaffCore plugin;

    public WarnCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.warn")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "warn-usage");
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName() == null ? args[0] : target.getName();
        if (sender instanceof Player staff && onlineTarget != null && !ModerationGuard.canTarget(plugin, staff, onlineTarget)) {
            plugin.messages().send(sender, "target-protected");
            return true;
        }
        String reason = ModerationGuard.sanitizeReason(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
        if (!ModerationGuard.validReason(plugin, reason)) {
            plugin.messages().send(sender, "reason-too-short");
            return true;
        }
        plugin.punishmentManager().warn(sender, target, targetName, reason);
        plugin.messages().send(sender, "punishment-warn-sent", Map.of("player", targetName));
        return true;
    }
}
