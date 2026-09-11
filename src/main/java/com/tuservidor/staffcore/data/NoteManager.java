package com.tuservidor.staffcore.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class NoteManager extends AsyncYamlPersistence {

    private final File file;
    private final List<NoteEntry> notes = new ArrayList<>();
    private int nextId = 1;

    public NoteManager(Plugin plugin) {
        super(plugin, "notes.yml");
        this.file = new File(plugin.getDataFolder(), "notes.yml");
        reload();
    }

    public NoteEntry add(Player staff, OfflinePlayer target, String targetName, String content) {
        NoteEntry note = new NoteEntry(nextId++, target.getUniqueId(), targetName, staff.getUniqueId(), staff.getName(), content, System.currentTimeMillis());
        notes.add(note);
        queueSave();
        return note;
    }

    public Optional<NoteEntry> remove(int id) {
        Optional<NoteEntry> existing = notes.stream().filter(note -> note.id() == id).findFirst();
        existing.ifPresent(notes::remove);
        if (existing.isPresent()) {
            queueSave();
        }
        return existing;
    }

    public List<NoteEntry> byTarget(UUID targetUuid, int limit) {
        return notes.stream()
            .filter(note -> note.targetUuid().equals(targetUuid))
            .sorted(Comparator.comparingLong(NoteEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public List<NoteEntry> byIdentity(UUID targetUuid, String targetName, int limit) {
        String normalizedName = targetName == null ? "" : targetName.toLowerCase(Locale.ROOT);
        return notes.stream()
            .filter(note -> {
                if (targetUuid != null && note.targetUuid().equals(targetUuid)) {
                    return true;
                }
                return !normalizedName.isBlank() && note.targetName().toLowerCase(Locale.ROOT).equals(normalizedName);
            })
            .sorted(Comparator.comparingLong(NoteEntry::createdAt).reversed())
            .limit(limit)
            .toList();
    }

    public Optional<UUID> latestKnownTargetUuidByName(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return Optional.empty();
        }
        String normalized = targetName.toLowerCase(Locale.ROOT);
        return notes.stream()
            .filter(note -> note.targetName().toLowerCase(Locale.ROOT).equals(normalized))
            .sorted(Comparator.comparingLong(NoteEntry::createdAt).reversed())
            .map(NoteEntry::targetUuid)
            .findFirst();
    }

    public void reload() {
        prepareForReload();
        notes.clear();
        nextId = 1;
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("notes");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection row = section.getConfigurationSection(key);
            if (row == null) {
                continue;
            }
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }

            String targetUuid = row.getString("target-uuid");
            String staffUuid = row.getString("staff-uuid");
            if (targetUuid == null || staffUuid == null) {
                continue;
            }

            NoteEntry note = new NoteEntry(
                id,
                UUID.fromString(targetUuid),
                row.getString("target-name", "Unknown"),
                UUID.fromString(staffUuid),
                row.getString("staff-name", "Unknown"),
                row.getString("content", ""),
                row.getLong("created-at")
            );
            notes.add(note);
            nextId = Math.max(nextId, id + 1);
        }
    }

    public void save() {
        flushNow();
    }

    public int totalNotes() {
        return notes.size();
    }

    public boolean saveQueued() {
        return super.isSaveQueued();
    }

    public boolean asyncWriteInProgress() {
        return super.isAsyncWriteInProgress();
    }

    @Override
    protected YamlConfiguration buildSnapshot() {
        YamlConfiguration config = new YamlConfiguration();
        for (NoteEntry note : notes) {
            String path = "notes." + note.id() + ".";
            config.set(path + "target-uuid", note.targetUuid().toString());
            config.set(path + "target-name", note.targetName());
            config.set(path + "staff-uuid", note.staffUuid().toString());
            config.set(path + "staff-name", note.staffName());
            config.set(path + "content", note.content());
            config.set(path + "created-at", note.createdAt());
        }
        return config;
    }
}
