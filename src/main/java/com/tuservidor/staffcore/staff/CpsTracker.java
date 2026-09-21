package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CpsTracker {

    private final StaffCore plugin;
    private final boolean enabled;
    private final int alertThreshold;
    private final long alertCooldownMs;
    private final Map<UUID, Deque<Long>> leftClicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAlertTimes = new ConcurrentHashMap<>();

    public CpsTracker(StaffCore plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("features.cps-tracker", true);
        this.alertThreshold = plugin.getConfig().getInt("cps-tracker.alert-threshold", 18);
        this.alertCooldownMs = plugin.getConfig().getLong("cps-tracker.cooldown-seconds", 30) * 1000L;
    }

    public void recordClick(Player player) {
        if (!enabled || player == null || player.hasPermission("staffcore.bypass.cps")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Deque<Long> clicks = leftClicks.computeIfAbsent(uuid, id -> new ArrayDeque<>());
        synchronized (clicks) {
            clicks.addLast(now);
            // remove clicks older than 1000ms
            while (!clicks.isEmpty() && (now - clicks.peekFirst() > 1000L)) {
                clicks.removeFirst();
            }

            int currentCps = clicks.size();
            if (currentCps >= alertThreshold) {
                long lastAlert = lastAlertTimes.getOrDefault(uuid, 0L);
                if (now - lastAlert >= alertCooldownMs) {
                    lastAlertTimes.put(uuid, now);
                    broadcastCpsAlert(player, currentCps);
                }
            }
        }
    }

    public int getCps(Player player) {
        if (player == null) return 0;
        Deque<Long> clicks = leftClicks.get(player.getUniqueId());
        if (clicks == null) return 0;
        long now = System.currentTimeMillis();
        synchronized (clicks) {
            while (!clicks.isEmpty() && (now - clicks.peekFirst() > 1000L)) {
                clicks.removeFirst();
            }
            return clicks.size();
        }
    }

    private void broadcastCpsAlert(Player player, int cps) {
        Component alert = Component.text("[StaffCore] ", NamedTextColor.DARK_GRAY)
            .append(Component.text("⚡ HIGH CPS ALERT: ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(player.getName(), NamedTextColor.YELLOW))
            .append(Component.text(" reached ", NamedTextColor.GRAY))
            .append(Component.text(cps + " CPS", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(" (Possible AutoClicker) ", NamedTextColor.GRAY))
            .append(
                Component.text("[👁️ SPECTATE]", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to " + player.getName(), NamedTextColor.AQUA)))
                    .clickEvent(ClickEvent.runCommand("/tp " + player.getName()))
            );

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("staffcore.alerts.cps")) {
                staff.sendMessage(alert);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
