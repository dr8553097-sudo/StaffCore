package com.tuservidor.staffcore.util;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class ModerationGuard {

    private static final List<String> REASON_MARKERS = List.of("razon:", "reason:", "motivo:", "raison:", "razao:");
    private static final List<String> CUT_MARKERS = List.of("|", " staff:", " staff ", " duracion:", " duration:", " tiempo:", " tempo:", " target:", " jugador:", " player:");

    private ModerationGuard() {
    }

    public static boolean canTarget(StaffCore plugin, Player actor, Player target) {
        if (actor.equals(target)) {
            return false;
        }

        String overridePermission = plugin.getConfig().getString("moderation.override-permission", "staffcore.override");
        String staffOverridePermission = plugin.getConfig().getString("moderation.staff-target-override-permission", "staffcore.stafftarget.override");
        if (actor.isOp() || actor.hasPermission(overridePermission) || actor.hasPermission(staffOverridePermission)) {
            return true;
        }

        if (isStaffMember(actor) && isStaffMember(target)) {
            plugin.staffLogManager().log(actor.getName(), "ANTIABUSE_BLOCK", target.getName(), "Blocked staff-to-staff moderation attempt");
            return false;
        }

        boolean protectEnabled = plugin.getConfig().getBoolean("moderation.protect-enabled", true);
        if (!protectEnabled) {
            return true;
        }

        String protectPermission = plugin.getConfig().getString("moderation.protect-permission", "staffcore.protect");

        boolean blocked = target.hasPermission(protectPermission) || target.hasPermission("staffcore.admin");
        if (blocked) {
            plugin.staffLogManager().log(actor.getName(), "ANTIABUSE_BLOCK", target.getName(), "Blocked target protected by permissions");
            return false;
        }
        return true;
    }

    public static boolean validReason(StaffCore plugin, String reason) {
        int minLength = plugin.getConfig().getInt("moderation.min-reason-length", 4);
        return reason != null && reason.trim().length() >= minLength;
    }

    public static String sanitizeReason(String input) {
        if (input == null) {
            return "";
        }
        String stripped = stripDecoration(input);
        if (stripped.isBlank()) {
            return "";
        }

        String lower = normalizeForMatch(stripped);
        int markerIndex = -1;
        int markerLength = 0;
        for (String marker : REASON_MARKERS) {
            int idx = lower.lastIndexOf(marker);
            if (idx > markerIndex) {
                markerIndex = idx;
                markerLength = marker.length();
            }
        }
        String candidate = markerIndex >= 0
            ? stripped.substring(Math.min(stripped.length(), markerIndex + markerLength)).trim()
            : stripped;

        String lowerCandidate = normalizeForMatch(candidate);
        int cutAt = candidate.length();
        for (String marker : CUT_MARKERS) {
            int idx = lowerCandidate.indexOf(marker);
            if (idx >= 0 && idx < cutAt) {
                cutAt = idx;
            }
        }
        candidate = candidate.substring(0, Math.max(0, cutAt)).trim();

        if (candidate.isEmpty()) {
            return stripped;
        }
        return candidate;
    }

    private static String stripDecoration(String input) {
        String line = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input));
        if (line == null) {
            return "";
        }
        line = line.replace('\n', ' ')
            .replace('\r', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        return line;
    }

    private static String normalizeForMatch(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static boolean isStaffMember(Player player) {
        return player.hasPermission("staffcore.staff")
            || player.hasPermission("staffcore.admin")
            || player.hasPermission("staffcore.freeze")
            || player.hasPermission("staffcore.vanish")
            || player.hasPermission("staffcore.punish.warn")
            || player.hasPermission("staffcore.punish.mute")
            || player.hasPermission("staffcore.punish.kick")
            || player.hasPermission("staffcore.punish.ban")
            || player.hasPermission("staffcore.punish.tempban");
    }
}
