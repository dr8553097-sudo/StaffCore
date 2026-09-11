package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CommandVisibilityPolicy {

    private CommandVisibilityPolicy() {
    }

    public static Map<String, String[]> defaultHiddenRules() {
        Map<String, String[]> rules = new LinkedHashMap<>();
        put(rules, "staff", "staffcore.staff", "staffcore.use");
        put(rules, "mod", "staffcore.staff", "staffcore.use");
        put(rules, "staffmode", "staffcore.staff", "staffcore.use");
        put(rules, "staffpanel", "staffcore.staff", "staffcore.use");
        put(rules, "spanel", "staffcore.staff", "staffcore.use");
        put(rules, "smenu", "staffcore.staff", "staffcore.use");
        put(rules, "vanish", "staffcore.vanish");
        put(rules, "v", "staffcore.vanish");
        put(rules, "freeze", "staffcore.freeze");
        put(rules, "staffchat", "staffcore.staffchat");
        put(rules, "sc", "staffcore.staffchat");
        put(rules, "reports", "staffcore.reports.view");
        put(rules, "notes", "staffcore.notes");
        put(rules, "warn", "staffcore.punish.warn");
        put(rules, "mute", "staffcore.punish.mute");
        put(rules, "unmute", "staffcore.punish.unmute");
        put(rules, "sckick", "staffcore.punish.kick");
        put(rules, "scban", "staffcore.punish.ban");
        put(rules, "sctempban", "staffcore.punish.tempban");
        put(rules, "scunban", "staffcore.punish.unban");
        put(rules, "history", "staffcore.history");
        put(rules, "stafflogs", "staffcore.logs");
        put(rules, "stafflang", "staffcore.lang");
        put(rules, "xrayalerts", "staffcore.xray.alerts");
        put(rules, "xalerts", "staffcore.xray.alerts");
        put(rules, "chatmute", "staffcore.chatmute", "staffcore.chatmute.bypass");
        put(rules, "mutechat", "staffcore.chatmute", "staffcore.chatmute.bypass");
        put(rules, "chatsilence", "staffcore.chatmute", "staffcore.chatmute.bypass");
        return rules;
    }

    public static boolean isStaffCoreNamespaceCall(String rawMessage) {
        if (rawMessage == null) {
            return false;
        }
        return rawMessage.toLowerCase(Locale.ROOT).startsWith("/staffcore:");
    }

    public static boolean shouldHideCommandFrom(CommandSender sender, String rawCommand, Map<String, String[]> rules) {
        if (sender == null) {
            return false;
        }
        String command = FreezeCommandPolicy.normalizeCommand(rawCommand);
        String[] required = rules.get(command);
        if (required == null) {
            return false;
        }
        return !Permissions.hasAny(sender, required);
    }

    private static void put(Map<String, String[]> rules, String command, String... permissions) {
        rules.put(command.toLowerCase(Locale.ROOT), permissions);
    }
}
