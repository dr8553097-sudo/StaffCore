package com.tuservidor.staffcore.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FirstJoinManager extends AsyncYamlPersistence {

    private final File file;
    private final Set<UUID> seenPlayers = new HashSet<>();

    public FirstJoinManager(Plugin plugin) {
        super(plugin, "first-joins.yml");
        this.file = new File(plugin.getDataFolder(), "first-joins.yml");
        reload();
    }

    public boolean markIfFirstJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (seenPlayers.contains(uuid)) {
            return false;
        }
        seenPlayers.add(uuid);
        queueSave();
        return true;
    }

    public void reload() {
        prepareForReload();
        seenPlayers.clear();
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String rawUuid : config.getStringList("seen")) {
            try {
                seenPlayers.add(UUID.fromString(rawUuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        flushNow();
    }

    public int seenPlayersCount() {
        return seenPlayers.size();
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
        config.set("seen", seenPlayers.stream().map(UUID::toString).toList());
        return config;
    }
}
