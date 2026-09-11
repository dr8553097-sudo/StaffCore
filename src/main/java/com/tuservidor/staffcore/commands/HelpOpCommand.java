package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HelpOpCommand implements CommandExecutor {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final StaffCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public HelpOpCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(player, "staffcore.helpop")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        if (!plugin.getConfig().getBoolean("helpop.enabled", true)) {
            plugin.messages().send(player, "module-disabled", Map.of("module", "helpop"));
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(player, "helpop-usage");
            return true;
        }

        long remaining = remainingCooldownSeconds(player.getUniqueId());
        if (remaining > 0) {
            plugin.messages().send(player, "helpop-cooldown", Map.of("seconds", String.valueOf(remaining)));
            return true;
        }

        String content = String.join(" ", args).trim();
        int maxLength = Math.max(20, plugin.getConfig().getInt("helpop.max-message-length", 220));
        if (content.length() > maxLength) {
            plugin.messages().send(player, "helpop-too-long", Map.of("max", String.valueOf(maxLength)));
            return true;
        }
        int wrapWidth = plugin.getConfig().getInt("helpop.message-wrap-width", 44);
        String wrappedMessage = wrapMessageForTemplate(content, wrapWidth);
        int compactMax = Math.max(40, plugin.getConfig().getInt("helpop.template-compact-max-chars", 110));
        String compactMessage = compactMessageForTemplate(content, compactMax);

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.messages().send(player, "helpop-sent");

        String serverName = plugin.getServer().getName();
        String now = LocalTime.now().format(TIME_FORMAT);
        Map<String, String> placeholders = Map.of(
            "player", player.getName(),
            "message", wrappedMessage,
            "message_raw", content,
            "message_wrapped", wrappedMessage,
            "message_compact", compactMessage,
            "time", now,
            "server", serverName
        );

        for (String line : templateLines()) {
            String rendered = applyPlaceholders(line, placeholders);
            Bukkit.getConsoleSender().sendMessage(rendered);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (Permissions.has(online, "staffcore.helpop.receive")) {
                    online.sendMessage(rendered);
                }
            }
        }
        alertStaffVisuals(placeholders);

        plugin.staffLogManager().log(player.getName(), "HELPOP", "staff-helpop", content);
        return true;
    }

    private long remainingCooldownSeconds(UUID playerId) {
        long cooldownMillis = Math.max(0L, plugin.getConfig().getLong("helpop.cooldown-seconds", 45L)) * 1000L;
        if (cooldownMillis == 0L) {
            return 0L;
        }
        long last = cooldowns.getOrDefault(playerId, 0L);
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= cooldownMillis) {
            return 0L;
        }
        return Math.max(1L, (cooldownMillis - elapsed + 999L) / 1000L);
    }

    private List<String> templateLines() {
        List<String> lines = plugin.uiStringList("helpop.template-lines");
        if (!lines.isEmpty()) {
            return lines;
        }
        return List.of(
            "&8&m------------------------------------------------",
            "&c&lSOLICITUD DE AYUDA",
            "&7Nueva solicitud recibida:",
            "&f{message_compact}",
            "&7Jugador: &f{player} &8| &7Hora: &f{time}",
            "&7Servidor: &f{server}",
            "&8&m------------------------------------------------"
        );
    }

    private void alertStaffVisuals(Map<String, String> placeholders) {
        String rawTitle = plugin.uiString("helpop.alert-title", "&c&l[HELPOP]");
        String rawSubtitle = plugin.uiString("helpop.alert-subtitle", "&f{player} &7needs staff assistance");
        Sound sound = parseSound(plugin.getConfig().getString("helpop.alert-sound", "BLOCK_NOTE_BLOCK_PLING"));
        float volume = (float) plugin.getConfig().getDouble("helpop.alert-volume", 1.4D);
        float pitch = (float) plugin.getConfig().getDouble("helpop.alert-pitch", 1.05D);
        int fadeIn = plugin.getConfig().getInt("helpop.alert-title-fade-in", 5);
        int stay = plugin.getConfig().getInt("helpop.alert-title-stay", 45);
        int fadeOut = plugin.getConfig().getInt("helpop.alert-title-fade-out", 12);
        boolean sendTitle = plugin.getConfig().getBoolean("helpop.alert-title-enabled", true);

        String title = applyPlaceholders(rawTitle, placeholders);
        String subtitle = applyPlaceholders(rawSubtitle, placeholders);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!Permissions.has(online, "staffcore.helpop.receive")) {
                continue;
            }
            if (sendTitle) {
                online.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
            if (sound != null) {
                online.playSound(online.getLocation(), sound, Math.max(0f, volume), Math.max(0f, pitch));
            }
        }
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String applyPlaceholders(String line, Map<String, String> placeholders) {
        String rendered = line;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey().toLowerCase(Locale.ROOT) + "}", entry.getValue());
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return Messages.color(rendered);
    }

    private String wrapMessageForTemplate(String message, int configuredWidth) {
        int width = Math.max(24, Math.min(80, configuredWidth));
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                for (int i = 0; i < word.length(); i += width) {
                    lines.add(word.substring(i, Math.min(word.length(), i + width)));
                }
                continue;
            }
            int projected = current.length() == 0 ? word.length() : current.length() + 1 + word.length();
            if (projected > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            return "-";
        }
        return String.join("\n&f", lines);
    }

    private String compactMessageForTemplate(String message, int maxChars) {
        String normalized = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "-";
        }
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxChars - 3)) + "...";
    }
}
