package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.staff.FreezeManager;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;

public final class FreezeCommand implements CommandExecutor {

    private final StaffCore plugin;

    public FreezeCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(staff, "staffcore.freeze")) {
            plugin.messages().send(staff, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("freeze")) {
            plugin.messages().send(staff, "module-disabled", Map.of("module", "freeze"));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(staff, "player-not-found", Map.of("player", args[1]));
                return true;
            }
            if (target.equals(staff)) {
                plugin.messages().send(staff, "cannot-target-self");
                return true;
            }
            if (!ModerationGuard.canTarget(plugin, staff, target)) {
                plugin.messages().send(staff, "target-protected");
                return true;
            }
            FreezeManager.ClaimResult claimResult = plugin.freezeManager().claim(staff, target);
            switch (claimResult) {
                case NOT_FROZEN -> plugin.messages().send(staff, "freeze-claim-not-frozen", Map.of("player", target.getName()));
                case ALREADY_ASSIGNED_TO_YOU -> plugin.messages().send(staff, "freeze-claim-already", Map.of("player", target.getName()));
                case CLAIMED -> plugin.messages().send(staff, "freeze-claim-success", Map.of("player", target.getName()));
            }
            return true;
        }

        if (args.length < 1) {
            plugin.messages().send(staff, "freeze-usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(staff, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (target.equals(staff)) {
            plugin.messages().send(staff, "cannot-target-self");
            return true;
        }
        if (!ModerationGuard.canTarget(plugin, staff, target)) {
            plugin.messages().send(staff, "target-protected");
            return true;
        }

        if (plugin.freezeManager().isFrozen(target)) {
            plugin.freezeManager().unfreeze(staff, target);
            return true;
        }

        if (args.length < 2) {
            plugin.messages().send(staff, "freeze-reason-required");
            plugin.messages().send(staff, "freeze-usage");
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (!ModerationGuard.validReason(plugin, reason)) {
            plugin.messages().send(staff, "reason-too-short");
            return true;
        }

        plugin.freezeManager().freeze(staff, target, reason);
        return true;
    }
}
