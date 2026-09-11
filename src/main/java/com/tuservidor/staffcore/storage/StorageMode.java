package com.tuservidor.staffcore.storage;

import java.util.Locale;

public enum StorageMode {
    YAML("yaml"),
    SQLITE("sqlite"),
    MYSQL("mysql");

    private final String configName;

    StorageMode(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return configName;
    }

    public static StorageMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return YAML;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (StorageMode mode : values()) {
            if (mode.configName.equals(normalized)) {
                return mode;
            }
        }
        return YAML;
    }
}
