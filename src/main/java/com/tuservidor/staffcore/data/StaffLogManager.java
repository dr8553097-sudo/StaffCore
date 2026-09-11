package com.tuservidor.staffcore.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StaffLogManager extends AsyncYamlPersistence {

    private final Plugin plugin;
    private final File file;
    private final List<StaffLogEntry> entries = new ArrayList<>();
    private int nextId = 1;

    public StaffLogManager(Plugin plugin) {
        super(plugin, "staff-logs.yml");
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "staff-logs.yml");
        reload();
    }

    public StaffLogEntry log(String staff, String action, String target, String details) {
        StaffLogEntry entry = new StaffLogEntry(nextId++, System.currentTimeMillis(), safe(staff), safe(action), safe(target), safe(details));
        entries.add(entry);
        pruneByPolicy();
        queueSave();
        return entry;
    }

    public List<StaffLogEntry> recent(int limit) {
        return entries.stream()
            .sorted(Comparator.comparingLong(StaffLogEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public List<StaffLogEntry> searchByPlayer(String playerName, int limit) {
        String search = playerName.toLowerCase(Locale.ROOT);
        return entries.stream()
            .filter(entry -> entry.target().toLowerCase(Locale.ROOT).contains(search) || entry.staff().toLowerCase(Locale.ROOT).contains(search))
            .sorted(Comparator.comparingLong(StaffLogEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public void reload() {
        prepareForReload();
        entries.clear();
        nextId = 1;
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("logs");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(key);
            if (item == null) {
                continue;
            }

            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }

            StaffLogEntry entry = new StaffLogEntry(
                id,
                item.getLong("created-at"),
                item.getString("staff", "Unknown"),
                item.getString("action", "UNKNOWN"),
                item.getString("target", "Unknown"),
                item.getString("details", "")
            );
            entries.add(entry);
            nextId = Math.max(nextId, id + 1);
        }

        if (plugin.getConfig().getBoolean("staff-logs.auto-prune-on-reload", true)) {
            int removed = pruneByPolicy();
            if (removed > 0) {
                save();
                if (plugin.getConfig().getBoolean("staff-logs.print-prune-summary", true)) {
                    plugin.getLogger().info("StaffCore pruned " + removed + " staff log entries by retention policy.");
                }
            }
        }
    }

    public void save() {
        flushNow();
    }

    private String safe(String value) {
        return value == null ? "Unknown" : value;
    }

    public int totalLogs() {
        return entries.size();
    }

    public boolean saveQueued() {
        return super.isSaveQueued();
    }

    public boolean asyncWriteInProgress() {
        return super.isAsyncWriteInProgress();
    }

    public int pruneNow() {
        int removed = pruneByPolicy();
        if (removed > 0) {
            queueSave();
        }
        return removed;
    }

    public int clearAll() {
        int removed = entries.size();
        if (removed <= 0) {
            return 0;
        }
        entries.clear();
        nextId = 1;
        queueSave();
        return removed;
    }

    private int pruneByPolicy() {
        int removed = 0;

        int retentionDays = plugin.getConfig().getInt("staff-logs.retention-days", 30);
        if (retentionDays > 0) {
            long cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L);
            int before = entries.size();
            entries.removeIf(entry -> entry.createdAt() < cutoff);
            removed += Math.max(0, before - entries.size());
        }

        int maxEntries = plugin.getConfig().getInt("staff-logs.max-entries", 10000);
        if (maxEntries > 0 && entries.size() > maxEntries) {
            entries.sort(Comparator.comparingLong(StaffLogEntry::createdAt).reversed());
            int before = entries.size();
            entries.subList(maxEntries, entries.size()).clear();
            removed += Math.max(0, before - entries.size());
            entries.sort(Comparator.comparingLong(StaffLogEntry::createdAt));
        }

        return removed;
    }

    @Override
    protected YamlConfiguration buildSnapshot() {
        YamlConfiguration config = new YamlConfiguration();
        for (StaffLogEntry entry : entries) {
            String path = "logs." + entry.id() + ".";
            config.set(path + "created-at", entry.createdAt());
            config.set(path + "staff", entry.staff());
            config.set(path + "action", entry.action());
            config.set(path + "target", entry.target());
            config.set(path + "details", entry.details());
        }
        return config;
    }
}
