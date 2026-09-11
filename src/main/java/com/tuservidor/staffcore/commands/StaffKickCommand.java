package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class StaffKickCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffKickCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.punish.kick")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("punishments")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "punishments"));
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "kick-usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (sender instanceof Player staff) {
            if (target.equals(staff)) {
                plugin.messages().send(sender, "cannot-target-self");
                return true;
            }
            if (!ModerationGuard.canTarget(plugin, staff, target)) {
                plugin.messages().send(sender, "target-protected");
                return true;
            }
        }
        String reason = ModerationGuard.sanitizeReason(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
        if (!ModerationGuard.validReason(plugin, reason)) {
            plugin.messages().send(sender, "reason-too-short");
            return true;
        }
        plugin.punishmentManager().kick(sender, target, reason);
        plugin.messages().send(sender, "punishment-kick-sent", Map.of("player", target.getName()));
        return true;
    }
}
