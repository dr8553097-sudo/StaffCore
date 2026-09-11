package com.tuservidor.staffcore.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class Messages {

    private final JavaPlugin plugin;
    private final LangManager langManager;
    private String prefix;

    public Messages(JavaPlugin plugin, LangManager langManager) {
        this.plugin = plugin;
        this.langManager = langManager;
        reload();
    }

    public void reload() {
        this.prefix = color(plugin.getConfig().getString("prefix", "&8[&bStaffCore&8] &7"));
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(langManager.translate(sender, key, Map.of()));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(langManager.translate(sender, key, placeholders));
    }

    public String resolve(CommandSender sender, String key) {
        return langManager.translate(sender, key, Map.of());
    }

    public String resolve(CommandSender sender, String key, Map<String, String> placeholders) {
        return langManager.translate(sender, key, placeholders);
    }

    public String resolve(String key) {
        return langManager.translate(key, Map.of());
    }

    public String resolve(String key, Map<String, String> placeholders) {
        return langManager.translate(key, placeholders);
    }

    public void console(String message) {
        Bukkit.getConsoleSender().sendMessage(color(message));
    }

    public String format(String key, Map<String, String> placeholders) {
        return resolve(key, placeholders);
    }

    public String prefix() {
        return prefix;
    }

    private static final java.util.regex.Pattern HEX_PATTERN_AMP = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final java.util.regex.Pattern HEX_PATTERN_TAG = java.util.regex.Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.replace("\\n", "\n");

        java.util.regex.Matcher matcher = HEX_PATTERN_AMP.matcher(normalized);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        normalized = buffer.toString();

        matcher = HEX_PATTERN_TAG.matcher(normalized);
        buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
