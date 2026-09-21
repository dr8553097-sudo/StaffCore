package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class XrayHeuristicsManager {

    private static class MiningStats {
        int stoneBlocks = 0;
        int rareOres = 0;
        long windowStartEpochMs = System.currentTimeMillis();
        long lastAlertEpochMs = 0;
    }

    private final StaffCore plugin;
    private final boolean enabled;
    private final double ratioThreshold;
    private final int minOresBeforeAlert;
    private final long windowDurationMs;
    private final Map<UUID, MiningStats> statsMap = new ConcurrentHashMap<>();

    public XrayHeuristicsManager(StaffCore plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("features.xray-heuristics", true);
        this.ratioThreshold = plugin.getConfig().getDouble("xray-heuristics.ratio-threshold", 0.15); // 15%
        this.minOresBeforeAlert = plugin.getConfig().getInt("xray-heuristics.min-ores-to-alert", 5);
        this.windowDurationMs = plugin.getConfig().getLong("xray-heuristics.window-minutes", 10) * 60_000L;
    }

    public void recordBlockBreak(Player player, Material material) {
        if (!enabled || player == null || player.hasPermission("staffcore.bypass.xray")) return;

        UUID uuid = player.getUniqueId();
        MiningStats stats = statsMap.computeIfAbsent(uuid, id -> new MiningStats());

        long now = System.currentTimeMillis();
        if (now - stats.windowStartEpochMs > windowDurationMs) {
            stats.stoneBlocks = 0;
            stats.rareOres = 0;
            stats.windowStartEpochMs = now;
        }

        if (isCommonStone(material)) {
            stats.stoneBlocks++;
        } else if (isRareOre(material)) {
            stats.rareOres++;

            if (stats.rareOres >= minOresBeforeAlert) {
                int totalMined = stats.stoneBlocks + stats.rareOres;
                double ratio = totalMined > 0 ? ((double) stats.rareOres / (double) totalMined) : 0.0;

                if (ratio >= ratioThreshold && (now - stats.lastAlertEpochMs > 60_000L)) {
                    stats.lastAlertEpochMs = now;
                    broadcastHeuristicAlert(player, stats.rareOres, totalMined, ratio);
                }
            }
        }
    }

    private void broadcastHeuristicAlert(Player player, int ores, int total, double ratio) {
        double percentage = ratio * 100.0;

        Component alert = Component.text("[StaffCore] ", NamedTextColor.DARK_GRAY)
            .append(Component.text("🚨 X-RAY RATIO ALERT: ", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(player.getName(), NamedTextColor.YELLOW))
            .append(Component.text(" mined ", NamedTextColor.GRAY))
            .append(Component.text(ores + " rare ores", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text(" out of " + total + " blocks (", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f%%", percentage), NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(") ", NamedTextColor.GRAY))
            .append(
                Component.text("[👁️ SPECTATE]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + player.getName(), NamedTextColor.AQUA)))
                    .clickEvent(ClickEvent.runCommand("/tp " + player.getName()))
            );

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("staffcore.xray.alerts")) {
                staff.sendMessage(alert);
            }
        }
    }

    private boolean isCommonStone(Material mat) {
        return mat == Material.STONE || mat == Material.DEEPSLATE || mat == Material.NETHERRACK
            || mat == Material.ANDESITE || mat == Material.DIORITE || mat == Material.GRANITE
            || mat == Material.TUFF || mat == Material.END_STONE;
    }

    private boolean isRareOre(Material mat) {
        return mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE
            || mat == Material.ANCIENT_DEBRIS || mat == Material.EMERALD_ORE
            || mat == Material.DEEPSLATE_EMERALD_ORE;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
