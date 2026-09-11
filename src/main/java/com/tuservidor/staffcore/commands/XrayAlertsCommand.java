package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.staff.XrayAlertManager;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class XrayAlertsCommand implements CommandExecutor {

    private final StaffCore plugin;

    public XrayAlertsCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, "staffcore.xray.alerts")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("xray-alerts")) {
            plugin.messages().send(sender, "module-disabled", Map.of("module", "xray-alerts"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "xrayalerts-usage");
                return true;
            }
            boolean enabled = plugin.xrayAlertManager().togglePersonalAlerts(player);
            plugin.messages().send(player, enabled ? "xrayalerts-enabled" : "xrayalerts-disabled");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("on") || sub.equals("off") || sub.equals("toggle")) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "players-only");
                return true;
            }
            boolean enabled;
            if (sub.equals("toggle")) {
                enabled = plugin.xrayAlertManager().togglePersonalAlerts(player);
            } else {
                enabled = sub.equals("on");
                plugin.xrayAlertManager().setPersonalAlerts(player.getUniqueId(), enabled);
            }
            plugin.messages().send(player, enabled ? "xrayalerts-enabled" : "xrayalerts-disabled");
            return true;
        }

        if (sub.equals("module")) {
            if (!Permissions.has(sender, "staffcore.reload")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            if (args.length != 2) {
                plugin.messages().send(sender, "xrayalerts-usage");
                return true;
            }
            String state = args[1].toLowerCase(Locale.ROOT);
            if (!state.equals("on") && !state.equals("off")) {
                plugin.messages().send(sender, "xrayalerts-usage");
                return true;
            }
            boolean enabled = state.equals("on");
            plugin.xrayAlertManager().setRuntimeEnabled(enabled);
            plugin.messages().send(sender, enabled ? "xrayalerts-global-enabled" : "xrayalerts-global-disabled");
            return true;
        }

        if (sub.equals("stats")) {
            if (args.length != 2) {
                plugin.messages().send(sender, "xrayalerts-usage");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found", Map.of("player", args[1]));
                return true;
            }

            Optional<XrayAlertManager.XraySnapshot> snapshot = plugin.xrayAlertManager().snapshot(target);
            if (snapshot.isEmpty()) {
                plugin.messages().send(sender, "xrayalerts-no-data", Map.of("player", target.getName()));
                return true;
            }

            XrayAlertManager.XraySnapshot stats = snapshot.get();
            sender.sendMessage(plugin.messages().resolve(sender, "xrayalerts-stats-header", Map.of("player", target.getName())));
            sender.sendMessage(plugin.messages().resolve(sender, "xrayalerts-stats-line-1", Map.of(
                "total", String.valueOf(stats.totalBreaks()),
                "valuable", String.valueOf(stats.valuableBreaks()),
                "deep", String.valueOf(stats.deepValuableBreaks())
            )));
            sender.sendMessage(plugin.messages().resolve(sender, "xrayalerts-stats-line-2", Map.of(
                "ratio", String.format(Locale.US, "%.2f", stats.oreRatio() * 100.0D),
                "deepRatio", String.format(Locale.US, "%.2f", stats.deepOreRatio() * 100.0D),
                "alerts", String.valueOf(stats.alertsSent()),
                "dominantOre", stats.dominantOre()
            )));
            return true;
        }

        plugin.messages().send(sender, "xrayalerts-usage");
        return true;
    }
}
