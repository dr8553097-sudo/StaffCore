package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.reports.Report;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StaffCoreTabCompleter implements TabCompleter {

    private final StaffCore plugin;

    public StaffCoreTabCompleter(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "staff" -> completeStaff(args);
            case "staffchat" -> completeFirst(args, List.of("on", "off"));
            case "chatmute" -> completeFirst(args, List.of("on", "off", "toggle", "status"));
            case "freeze" -> completeFreeze(args, sender);
            case "helpop" -> List.of();
            case "report" -> completeReport(args, sender);
            case "reports" -> completeReports(args);
            case "notes" -> completeNotes(args, sender);
            case "warn" -> completePlayers(args, sender);
            case "mute" -> completeMute(args, sender);
            case "unmute" -> completePlayers(args, sender);
            case "sckick" -> completePlayers(args, sender);
            case "scban" -> completePlayers(args, sender);
            case "scbanip" -> completePlayers(args, sender);
            case "sctempban" -> completeTempBan(args, sender);
            case "sctempbanip" -> completeTempBan(args, sender);
            case "scunban" -> completePlayers(args, sender);
            case "scunbanip" -> completePlayers(args, sender);
            case "history" -> completePlayers(args, sender);
            case "stafflogs" -> completeStaffLogs(args, sender);
            case "stafflang" -> completeLanguages(args);
            case "xrayalerts" -> completeXrayAlerts(args, sender);
            default -> List.of();
        };
    }

    private List<String> completeStaff(String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("on", "off", "menu", "speed", "flyspeed", "reload", "health", "cleanfreeze", "version", "help"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("help")) {
            return filter(args[1], List.of("1", "2", "3", "4"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("speed") || args[0].equalsIgnoreCase("flyspeed"))) {
            return filter(args[1], List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
        }
        return List.of();
    }

    private List<String> completeFreeze(String[] args, CommandSender sender) {
        if (args.length == 1) {
            Set<String> options = new LinkedHashSet<>();
            options.add("claim");
            options.addAll(playerNames(sender));
            return filter(args[0], options);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            return filterPlayers(args[1], sender);
        }
        if (args.length == 2) {
            return filter(args[1], List.of("suspicious_activity", "hack_check", "autoclicker_check", "xray_check", "investigation"));
        }
        return List.of();
    }

    private List<String> completeReport(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filterPlayers(args[0], sender);
        }
        if (args.length == 2) {
            return filter(args[1], List.of("hacks", "xray", "reach", "spam", "insultos", "toxico"));
        }
        return List.of();
    }

    private List<String> completeReports(String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("close"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("close")) {
            List<String> ids = new ArrayList<>();
            for (Report report : plugin.reportManager().openReports()) {
                ids.add(String.valueOf(report.id()));
            }
            return filter(args[1], ids);
        }
        return List.of();
    }

    private List<String> completeNotes(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filterPlayers(args[0], sender);
        }
        if (args.length == 2) {
            return filter(args[1], List.of("list", "add", "remove"));
        }
        return List.of();
    }

    private List<String> completeMute(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filterPlayers(args[0], sender);
        }
        if (args.length == 2) {
            return filter(args[1], List.of("5m", "15m", "30m", "1h", "3h", "12h", "1d", "3d", "7d", "perm"));
        }
        return List.of();
    }

    private List<String> completeTempBan(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filterPlayers(args[0], sender);
        }
        if (args.length == 2) {
            return filter(args[1], List.of("1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d"));
        }
        return List.of();
    }

    private List<String> completeStaffLogs(String[] args, CommandSender sender) {
        if (args.length == 1) {
            Set<String> options = new LinkedHashSet<>();
            options.add("clear");
            options.addAll(playerNames(sender));
            return filter(args[0], options);
        }
        return List.of();
    }

    private List<String> completeLanguages(String[] args) {
        if (args.length == 1) {
            return filter(args[0], plugin.langManager().availableLanguages());
        }
        return List.of();
    }

    private List<String> completeXrayAlerts(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filter(args[0], List.of("on", "off", "toggle", "module", "stats"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            return filter(args[1], List.of("on", "off"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return filterPlayers(args[1], sender);
        }
        return List.of();
    }

    private List<String> completePlayers(String[] args, CommandSender sender) {
        if (args.length == 1) {
            return filterPlayers(args[0], sender);
        }
        return List.of();
    }

    private List<String> completeFirst(String[] args, List<String> options) {
        if (args.length == 1) {
            return filter(args[0], options);
        }
        return List.of();
    }

    private List<String> filterPlayers(String token, CommandSender sender) {
        return filter(token, playerNames(sender));
    }

    private Collection<String> playerNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        String selfName = sender instanceof Player player ? player.getName() : null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (selfName != null && selfName.equalsIgnoreCase(online.getName())) {
                continue;
            }
            names.add(online.getName());
        }
        return names;
    }

    private List<String> filter(String token, Collection<String> options) {
        String partial = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option == null) {
                continue;
            }
            if (partial.isEmpty() || option.toLowerCase(Locale.ROOT).startsWith(partial)) {
                result.add(option);
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }
}
