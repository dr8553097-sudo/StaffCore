package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

public final class StaffHudManager {

    private final StaffCore plugin;
    private final StaffManager staffManager;
    private final VanishManager vanishManager;

    private BukkitTask task;
    private boolean enabled = true;
    private boolean showWhenStaffMode = true;
    private boolean showWhenVanished = true;
    private long updateIntervalTicks = 8L;
    private String format = "&8TPS:&f{tps} &8| &8PING:&f{ping}ms &8| &8V:{vanish_icon}";
    private String vanishOn = "&aON";
    private String vanishOff = "&cOFF";
    private String vanishOnIcon = "&a✓";
    private String vanishOffIcon = "&c✗";

    public StaffHudManager(StaffCore plugin, StaffManager staffManager, VanishManager vanishManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.vanishManager = vanishManager;
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("staff-hud.enabled", true);
        showWhenStaffMode = plugin.getConfig().getBoolean("staff-hud.show-when-staff-mode", true);
        showWhenVanished = plugin.getConfig().getBoolean("staff-hud.show-when-vanished", true);
        updateIntervalTicks = Math.max(4L, plugin.getConfig().getLong("staff-hud.update-interval-ticks", 8L));
        format = plugin.uiString(
            "staff-hud.format",
            "&8TPS:&f{tps} &8| &8PING:&f{ping}ms &8| &8V:{vanish_icon}"
        );
        vanishOn = plugin.uiString("staff-hud.vanish-on-text", "&aON");
        vanishOff = plugin.uiString("staff-hud.vanish-off-text", "&cOFF");
        vanishOnIcon = plugin.uiString("staff-hud.vanish-on-icon", "&a✓");
        vanishOffIcon = plugin.uiString("staff-hud.vanish-off-icon", "&c✗");
        restartTask();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void restartTask() {
        shutdown();
        if (!enabled) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, updateIntervalTicks);
    }

    private void tick() {
        String tps = oneMinuteTps();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!shouldDisplay(player)) {
                continue;
            }
            boolean vanished = vanishManager.isVanished(player);
            int ping = safePing(player);
            String pingText = ping < 0 ? "n/a" : String.valueOf(ping);
            String vanishText = Messages.color(vanished ? vanishOn : vanishOff);
            String vanishIcon = Messages.color(vanished ? vanishOnIcon : vanishOffIcon);
            String line = format
                .replace("{tps}", tps)
                .replace("{ping}", pingText)
                .replace("{fps}", pingText)
                .replace("{vanish}", vanishText)
                .replace("{vanish_text}", vanishText)
                .replace("{vanish_icon}", vanishIcon);
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.color(line)));
        }
    }

    private boolean shouldDisplay(Player player) {
        if (showWhenStaffMode && staffManager.isStaff(player)) {
            return true;
        }
        return showWhenVanished && vanishManager.isVanished(player);
    }

    private String oneMinuteTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            if (tps == null || tps.length == 0) {
                return "n/a";
            }
            return String.format(Locale.US, "%.2f", tps[0]);
        } catch (Throwable ignored) {
            return "n/a";
        }
    }

    private int safePing(Player player) {
        try {
            return Math.max(0, player.getPing());
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
