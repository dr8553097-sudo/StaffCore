package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffHudManager {

    private final StaffCore plugin;
    private final StaffManager staffManager;
    private final VanishManager vanishManager;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    private BukkitTask task;
    private boolean enabled = true;
    private boolean showWhenStaffMode = true;
    private boolean showWhenVanished = true;
    private long updateIntervalTicks = 8L;

    // Actionbar settings
    private boolean actionbarEnabled = true;
    private String format = "&8TPS:&f{tps} &8| &8PING:&f{ping}ms &8| &8V:{vanish_icon}";
    private String vanishOn = "&aON";
    private String vanishOff = "&cOFF";
    private String vanishOnIcon = "&a&l✔";
    private String vanishOffIcon = "&c&l✖";

    // BossBar settings
    private boolean bossbarEnabled = true;
    private BarColor bossbarColor = BarColor.BLUE;
    private BarStyle bossbarStyle = BarStyle.SOLID;
    private String bossbarFormat = "&b&lSTAFF &8| &7TPS: &a{tps} &8| &7Ping: &e{ping}ms &8| &7Reports: &c{reports} &8| &7Staff: &b{staff_online} &8| &7Vanish: {vanish_icon}";
    private boolean bossbarDynamicProgress = true;

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

        actionbarEnabled = plugin.getConfig().getBoolean("staff-hud.actionbar.enabled", true);
        format = plugin.uiString(
            "staff-hud.format",
            "&8[&bTPS&8] &f{tps}  &8[&aPING&8] &f{ping}ms  &8[&dVANISH&8] {vanish_icon}"
        );
        vanishOn = plugin.uiString("staff-hud.vanish-on-text", "&aON");
        vanishOff = plugin.uiString("staff-hud.vanish-off-text", "&cOFF");
        vanishOnIcon = plugin.uiString("staff-hud.vanish-on-icon", "&a&l✔");
        vanishOffIcon = plugin.uiString("staff-hud.vanish-off-icon", "&c&l✖");

        bossbarEnabled = plugin.getConfig().getBoolean("staff-hud.bossbar.enabled", true);
        bossbarColor = parseBarColor(plugin.getConfig().getString("staff-hud.bossbar.color", "BLUE"));
        bossbarStyle = parseBarStyle(plugin.getConfig().getString("staff-hud.bossbar.style", "SOLID"));
        bossbarFormat = plugin.uiString(
            "staff-hud.bossbar.format",
            "&b&lSTAFF &8| &7TPS: &a{tps} &8| &7Ping: &e{ping}ms &8| &7Reports: &c{reports} &8| &7Staff: &b{staff_online} &8| &7Vanish: {vanish_icon}"
        );
        bossbarDynamicProgress = plugin.getConfig().getBoolean("staff-hud.bossbar.dynamic-progress", true);

        clearAllBossBars();
        restartTask();
    }

    private BarColor parseBarColor(String colorName) {
        if (colorName == null) return BarColor.BLUE;
        String upper = colorName.trim().toUpperCase(Locale.ROOT);
        if (upper.equals("CYAN") || upper.equals("AQUA")) {
            return BarColor.BLUE;
        }
        try {
            return BarColor.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }

    private BarStyle parseBarStyle(String styleName) {
        if (styleName == null) return BarStyle.SOLID;
        try {
            return BarStyle.valueOf(styleName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clearAllBossBars();
    }

    private void clearAllBossBars() {
        for (BossBar bar : activeBossBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        activeBossBars.clear();
    }

    public void removePlayer(UUID uuid) {
        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
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
        double rawTps = rawOneMinuteTps();
        String tps = String.format(Locale.US, "%.2f", rawTps);
        int openReports = plugin.reportManager() != null ? plugin.reportManager().openReportsCount() : 0;
        int onlineStaff = staffManager.staffPlayers().size();
        int onlineTotal = Bukkit.getOnlinePlayers().size();
        int maxSlots = Bukkit.getMaxPlayers();

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean shouldShow = shouldDisplay(player);

            if (!shouldShow) {
                removePlayer(player.getUniqueId());
                continue;
            }

            boolean vanished = vanishManager.isVanished(player);
            int ping = safePing(player);
            String pingText = ping < 0 ? "n/a" : String.valueOf(ping);
            String vanishText = Messages.color(vanished ? vanishOn : vanishOff);
            String vanishIcon = Messages.color(vanished ? vanishOnIcon : vanishOffIcon);
            String worldName = player.getWorld() != null ? player.getWorld().getName() : "world";

            int currentCps = plugin.cpsTracker() != null ? plugin.cpsTracker().getCps(player) : 0;
            String dutyTimeFormatted = formatDutyTime(player.getUniqueId());
            boolean onDuty = plugin.staffDutyManager() != null && plugin.staffDutyManager().isOnDuty(player.getUniqueId());
            String dutyStatus = onDuty ? Messages.color("&aON DUTY") : Messages.color("&cOFF DUTY");

            if (actionbarEnabled) {
                String line = format
                    .replace("{tps}", tps)
                    .replace("{ping}", pingText)
                    .replace("{fps}", pingText)
                    .replace("{reports}", String.valueOf(openReports))
                    .replace("{open_reports}", String.valueOf(openReports))
                    .replace("{staff_online}", String.valueOf(onlineStaff))
                    .replace("{staff_count}", String.valueOf(onlineStaff))
                    .replace("{online}", String.valueOf(onlineTotal))
                    .replace("{online_players}", String.valueOf(onlineTotal))
                    .replace("{max}", String.valueOf(maxSlots))
                    .replace("{max_players}", String.valueOf(maxSlots))
                    .replace("{world}", worldName)
                    .replace("{cps}", String.valueOf(currentCps))
                    .replace("{duty_time}", dutyTimeFormatted)
                    .replace("{duty_status}", dutyStatus)
                    .replace("{vanish}", vanishText)
                    .replace("{vanish_text}", vanishText)
                    .replace("{vanish_icon}", vanishIcon)
                    .replace("{player}", player.getName());
                player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.color(line)));
            }

            if (bossbarEnabled) {
                String bossbarLine = bossbarFormat
                    .replace("{tps}", tps)
                    .replace("{ping}", pingText)
                    .replace("{fps}", pingText)
                    .replace("{reports}", String.valueOf(openReports))
                    .replace("{open_reports}", String.valueOf(openReports))
                    .replace("{staff_online}", String.valueOf(onlineStaff))
                    .replace("{staff_count}", String.valueOf(onlineStaff))
                    .replace("{online}", String.valueOf(onlineTotal))
                    .replace("{online_players}", String.valueOf(onlineTotal))
                    .replace("{max}", String.valueOf(maxSlots))
                    .replace("{max_players}", String.valueOf(maxSlots))
                    .replace("{world}", worldName)
                    .replace("{cps}", String.valueOf(currentCps))
                    .replace("{duty_time}", dutyTimeFormatted)
                    .replace("{duty_status}", dutyStatus)
                    .replace("{vanish}", vanishText)
                    .replace("{vanish_text}", vanishText)
                    .replace("{vanish_icon}", vanishIcon)
                    .replace("{player}", player.getName());

                BossBar bar = activeBossBars.computeIfAbsent(player.getUniqueId(), id -> {
                    BossBar newBar = Bukkit.createBossBar(
                        Messages.color(bossbarLine),
                        bossbarColor,
                        bossbarStyle
                    );
                    newBar.addPlayer(player);
                    newBar.setVisible(true);
                    return newBar;
                });

                bar.setTitle(Messages.color(bossbarLine));
                if (bossbarDynamicProgress) {
                    double progress = Math.min(1.0, Math.max(0.0, rawTps / 20.0));
                    bar.setProgress(progress);
                } else {
                    bar.setProgress(1.0);
                }
                if (!bar.getPlayers().contains(player)) {
                    bar.addPlayer(player);
                }
                bar.setVisible(true);
            } else {
                removePlayer(player.getUniqueId());
            }
        }
    }

    private String formatDutyTime(UUID uuid) {
        if (plugin.staffDutyManager() == null || !plugin.staffDutyManager().isOnDuty(uuid)) {
            return "0s";
        }
        long sec = plugin.staffDutyManager().getActiveSessionDurationSeconds(uuid);
        long hours = sec / 3600;
        long minutes = (sec % 3600) / 60;
        long seconds = sec % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.US, "%dm %ds", minutes, seconds);
        } else {
            return String.format(Locale.US, "%ds", seconds);
        }
    }

    private boolean shouldDisplay(Player player) {
        if (showWhenStaffMode && staffManager.isStaff(player)) {
            return true;
        }
        return showWhenVanished && vanishManager.isVanished(player);
    }

    private double rawOneMinuteTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            if (tps == null || tps.length == 0) {
                return 20.0;
            }
            return Math.min(20.0, Math.max(0.0, tps[0]));
        } catch (Throwable ignored) {
            return 20.0;
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
