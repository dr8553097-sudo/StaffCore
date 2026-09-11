package com.tuservidor.staffcore.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FreezeCommandPolicy {

    private static final Set<String> CORE_ALLOWED_COMMANDS = Set.of(
        "msg",
        "m",
        "message",
        "pm",
        "dm",
        "tell",
        "t",
        "w",
        "whisper",
        "msgto",
        "etell",
        "r",
        "reply",
        "helpop"
    );

    private FreezeCommandPolicy() {
    }

    public static String normalizeCommand(String rawCommand) {
        String normalized = rawCommand == null ? "" : rawCommand;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int space = normalized.indexOf(' ');
        if (space >= 0) {
            normalized = normalized.substring(0, space);
        }
        int namespaced = normalized.indexOf(':');
        if (namespaced >= 0 && namespaced + 1 < normalized.length()) {
            normalized = normalized.substring(namespaced + 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedWhileFrozen(String normalizedCommand,
                                               List<String> configuredAllowed,
                                               List<String> configuredBlocked) {
        if (normalizedCommand == null || normalizedCommand.isBlank()) {
            return false;
        }

        Set<String> blocked = normalizeAll(configuredBlocked);
        if (blocked.contains(normalizedCommand)) {
            return false;
        }

        if (CORE_ALLOWED_COMMANDS.contains(normalizedCommand)) {
            return true;
        }
        return normalizeAll(configuredAllowed).contains(normalizedCommand);
    }

    private static Set<String> normalizeAll(List<String> commands) {
        Set<String> normalized = new HashSet<>();
        if (commands == null) {
            return normalized;
        }
        for (String entry : commands) {
            String command = normalizeCommand(entry);
            if (!command.isBlank()) {
                normalized.add(command);
            }
        }
        return normalized;
    }
}
