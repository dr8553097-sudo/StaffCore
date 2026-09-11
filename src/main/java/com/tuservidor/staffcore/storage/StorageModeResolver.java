package com.tuservidor.staffcore.storage;

import org.bukkit.configuration.file.FileConfiguration;

public final class StorageModeResolver {

    private StorageModeResolver() {
    }

    public static StorageModeSelection resolve(FileConfiguration config) {
        StorageMode requested = StorageMode.fromConfig(config.getString("storage.mode", "yaml"));
        if (requested == StorageMode.YAML) {
            return new StorageModeSelection(StorageMode.YAML, StorageMode.YAML, "yaml backend enabled");
        }

        // Placeholder for future database adapters.
        String note = switch (requested) {
            case SQLITE -> "sqlite backend planned; using yaml fallback";
            case MYSQL -> "mysql backend planned; using yaml fallback";
            default -> "using yaml fallback";
        };
        return new StorageModeSelection(requested, StorageMode.YAML, note);
    }
}
