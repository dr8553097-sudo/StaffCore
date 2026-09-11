package com.tuservidor.staffcore.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])");

    private DurationParser() {
    }

    public static Duration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String text = raw.toLowerCase(Locale.ROOT).trim();
        if (text.equals("perm") || text.equals("permanent") || text.equals("forever")) {
            return null;
        }

        Matcher matcher = TOKEN.matcher(text);
        long seconds = 0;
        int consumed = 0;
        while (matcher.find()) {
            consumed += matcher.group(0).length();
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            seconds += switch (unit) {
                case "s" -> value;
                case "m" -> value * 60L;
                case "h" -> value * 3600L;
                case "d" -> value * 86400L;
                case "w" -> value * 604800L;
                default -> 0L;
            };
        }

        if (consumed != text.length() || seconds <= 0L) {
            return Duration.ZERO;
        }

        return Duration.ofSeconds(seconds);
    }

    public static String format(Duration duration) {
        if (duration == null) {
            return "permanent";
        }

        long seconds = duration.toSeconds();
        long weeks = seconds / 604800L;
        seconds %= 604800L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder builder = new StringBuilder();
        append(builder, weeks, "w");
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "m");
        append(builder, seconds, "s");
        return builder.length() == 0 ? "0s" : builder.toString().trim();
    }

    private static void append(StringBuilder builder, long value, String suffix) {
        if (value > 0L) {
            builder.append(value).append(suffix).append(' ');
        }
    }
}
