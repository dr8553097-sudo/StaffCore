package com.tuservidor.staffcore.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageModeResolverTest {

    @Test
    void yamlModeRemainsActive() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.mode", "yaml");

        StorageModeSelection selection = StorageModeResolver.resolve(config);
        assertEquals(StorageMode.YAML, selection.requested());
        assertEquals(StorageMode.YAML, selection.active());
        assertFalse(selection.fallbackInUse());
    }

    @Test
    void sqliteFallsBackToYamlForNow() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.mode", "sqlite");

        StorageModeSelection selection = StorageModeResolver.resolve(config);
        assertEquals(StorageMode.SQLITE, selection.requested());
        assertEquals(StorageMode.YAML, selection.active());
        assertTrue(selection.fallbackInUse());
    }

    @Test
    void unknownModeDefaultsToYaml() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.mode", "mongo");

        StorageModeSelection selection = StorageModeResolver.resolve(config);
        assertEquals(StorageMode.YAML, selection.requested());
        assertEquals(StorageMode.YAML, selection.active());
    }
}
