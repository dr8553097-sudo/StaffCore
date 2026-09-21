package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class StaffDutyManager {

    public static record DutyRecord(
        UUID uuid,
        String name,
        long totalDutySeconds,
        long weeklyDutySeconds,
        long monthlyDutySeconds,
        int actionsCount,
        long lastSeenEpochMs
    ) {}

    private final StaffCore plugin;
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMovementTimes = new ConcurrentHashMap<>();
    private final Set<UUID> afkStaff = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, DutyRecord> dutyCache = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final long afkThresholdMs;
    private final boolean autoDutyOnStaffMode;

    public StaffDutyManager(StaffCore plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("features.staff-duty", true);
        this.afkThresholdMs = plugin.getConfig().getLong("staff-duty.afk-threshold-minutes", 3) * 60_000L;
        this.autoDutyOnStaffMode = plugin.getConfig().getBoolean("staff-duty.auto-duty-on-staffmode", true);

        if (enabled) {
            startAfkChecker();
        }
    }

    private void startAfkChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (UUID uuid : sessionStartTimes.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                Location last = lastLocations.get(uuid);
                Location current = player.getLocation();

                if (last == null || last.getWorld() != current.getWorld() || last.distanceSquared(current) > 0.05) {
                    lastLocations.put(uuid, current.clone());
                    lastMovementTimes.put(uuid, now);
                    if (afkStaff.remove(uuid)) {
                        player.sendMessage("§a[StaffCore] You are no longer marked as AFK. Duty timer resumed.");
                    }
                } else {
                    long lastMove = lastMovementTimes.getOrDefault(uuid, now);
                    if (now - lastMove >= afkThresholdMs && !afkStaff.contains(uuid)) {
                        afkStaff.add(uuid);
                        player.sendMessage("§e[StaffCore] You are now marked as AFK. Duty timer paused.");
                    }
                }
            }
        }, 100L, 100L); // check every 5 seconds
    }

    public boolean isOnDuty(Player player) {
        if (player == null) return false;
        return isOnDuty(player.getUniqueId());
    }

    public boolean isOnDuty(UUID uuid) {
        return uuid != null && sessionStartTimes.containsKey(uuid);
    }

    public long getActiveSessionDurationSeconds(UUID uuid) {
        if (uuid == null) return 0L;
        Long start = sessionStartTimes.get(uuid);
        if (start == null) return 0L;
        return Math.max(0L, (System.currentTimeMillis() - start) / 1000L);
    }

    public boolean isAfk(Player player) {
        return player != null && afkStaff.contains(player.getUniqueId());
    }

    public void startDuty(Player player) {
        if (!enabled) return;
        UUID uuid = player.getUniqueId();
        if (sessionStartTimes.containsKey(uuid)) return;

        sessionStartTimes.put(uuid, System.currentTimeMillis());
        lastLocations.put(uuid, player.getLocation().clone());
        lastMovementTimes.put(uuid, System.currentTimeMillis());
        afkStaff.remove(uuid);

        plugin.messages().send(player, "staff-duty-start");
        
        if (plugin.discordWebhookService() != null) {
            plugin.discordWebhookService().sendDutyEmbed(player.getName(), true, 0, getWeeklyHours(uuid));
        }
    }

    public void stopDuty(Player player) {
        if (!enabled) return;
        UUID uuid = player.getUniqueId();
        Long start = sessionStartTimes.remove(uuid);
        lastLocations.remove(uuid);
        lastMovementTimes.remove(uuid);
        afkStaff.remove(uuid);

        if (start != null) {
            long durationSeconds = (System.currentTimeMillis() - start) / 1000L;
            addDutySeconds(uuid, player.getName(), durationSeconds);
            
            String formatted = com.tuservidor.staffcore.util.DurationParser.format(java.time.Duration.ofSeconds(durationSeconds));
            long mins = Math.max(1, durationSeconds / 60);
            plugin.messages().send(player, "staff-duty-stop", Map.of(
                "time", formatted,
                "duration", formatted,
                "minutes", String.valueOf(mins)
            ));
            
            if (plugin.discordWebhookService() != null) {
                plugin.discordWebhookService().sendDutyEmbed(player.getName(), false, mins, getWeeklyHours(uuid));
            }
        }
    }

    public void toggleDuty(Player player) {
        if (isOnDuty(player)) {
            stopDuty(player);
        } else {
            startDuty(player);
        }
    }

    public void recordAction(Player player) {
        if (!enabled || player == null) return;
        UUID uuid = player.getUniqueId();
        DutyRecord existing = dutyCache.get(uuid);
        if (existing != null) {
            dutyCache.put(uuid, new DutyRecord(
                uuid,
                player.getName(),
                existing.totalDutySeconds(),
                existing.weeklyDutySeconds(),
                existing.monthlyDutySeconds(),
                existing.actionsCount() + 1,
                System.currentTimeMillis()
            ));
        }
    }

    private void addDutySeconds(UUID uuid, String name, long seconds) {
        DutyRecord existing = dutyCache.computeIfAbsent(uuid, id -> loadRecord(id, name));
        DutyRecord updated = new DutyRecord(
            uuid,
            name,
            existing.totalDutySeconds() + seconds,
            existing.weeklyDutySeconds() + seconds,
            existing.monthlyDutySeconds() + seconds,
            existing.actionsCount(),
            System.currentTimeMillis()
        );
        dutyCache.put(uuid, updated);
        saveRecordAsync(updated);
    }

    public long getWeeklyHours(UUID uuid) {
        DutyRecord record = dutyCache.get(uuid);
        return record != null ? (record.weeklyDutySeconds() / 3600) : 0;
    }

    public List<DutyRecord> getTopStaff(int limit) {
        List<DutyRecord> list = new ArrayList<>(dutyCache.values());
        list.sort((a, b) -> Long.compare(b.weeklyDutySeconds(), a.weeklyDutySeconds()));
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    private DutyRecord loadRecord(UUID uuid, String name) {
        if (plugin.databaseManager() != null && plugin.databaseManager().isAvailable()) {
            try (Connection conn = plugin.databaseManager().getConnection()) {
                if (conn != null) {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM staffcore_duty WHERE uuid = ?")) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                return new DutyRecord(
                                    uuid,
                                    rs.getString("name"),
                                    rs.getLong("total_duty_seconds"),
                                    rs.getLong("weekly_duty_seconds"),
                                    rs.getLong("monthly_duty_seconds"),
                                    rs.getInt("actions_count"),
                                    rs.getLong("last_seen_epoch_ms")
                                );
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading duty record for " + name, e);
            }
        }
        return new DutyRecord(uuid, name, 0, 0, 0, 0, System.currentTimeMillis());
    }

    private void saveRecordAsync(DutyRecord record) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.databaseManager() != null && plugin.databaseManager().isAvailable()) {
                try (Connection conn = plugin.databaseManager().getConnection()) {
                    if (conn != null) {
                        String sql = (plugin.databaseManager().getMode() == com.tuservidor.staffcore.storage.StorageMode.SQLITE)
                            ? "INSERT OR REPLACE INTO staffcore_duty (uuid, name, total_duty_seconds, weekly_duty_seconds, monthly_duty_seconds, actions_count, last_seen_epoch_ms) VALUES (?, ?, ?, ?, ?, ?, ?)"
                            : "INSERT INTO staffcore_duty (uuid, name, total_duty_seconds, weekly_duty_seconds, monthly_duty_seconds, actions_count, last_seen_epoch_ms) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name), total_duty_seconds=VALUES(total_duty_seconds), weekly_duty_seconds=VALUES(weekly_duty_seconds), monthly_duty_seconds=VALUES(monthly_duty_seconds), actions_count=VALUES(actions_count), last_seen_epoch_ms=VALUES(last_seen_epoch_ms)";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, record.uuid().toString());
                            ps.setString(2, record.name());
                            ps.setLong(3, record.totalDutySeconds());
                            ps.setLong(4, record.weeklyDutySeconds());
                            ps.setLong(5, record.monthlyDutySeconds());
                            ps.setInt(6, record.actionsCount());
                            ps.setLong(7, record.lastSeenEpochMs());
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error saving duty record for " + record.name(), e);
                }
            }
        });
    }

    public boolean isAutoDutyOnStaffMode() {
        return autoDutyOnStaffMode;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
