package com.tuservidor.staffcore.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

public abstract class AsyncYamlPersistence {

    private static final long WAIT_TIMEOUT_MS = 2500L;

    private final Plugin plugin;
    private final File file;
    private final Object saveLock = new Object();
    private final Object asyncWriteLock = new Object();

    private boolean asyncBatchEnabled = true;
    private long batchDelayTicks = 100L;
    private boolean crashSafeEnabled = true;
    private boolean crashSafeForceFileSync = false;
    private boolean crashSafeKeepBackup = true;
    private boolean crashSafeRecoverTempOnLoad = true;
    private boolean saveQueued = false;
    private BukkitTask saveTask;
    private YamlConfiguration pendingSnapshot;
    private boolean writingAsync = false;

    protected AsyncYamlPersistence(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        reloadSavePolicy();
    }

    protected abstract YamlConfiguration buildSnapshot();

    protected final void prepareForReload() {
        boolean queuedSnapshotPending = cancelQueuedSave();
        waitForAsyncWrites(WAIT_TIMEOUT_MS);
        if (queuedSnapshotPending) {
            writeSnapshotSync(buildSnapshot());
        }
        reloadSavePolicy();
        recoverTempIfNeeded();
    }

    protected final void flushNow() {
        cancelQueuedSave();
        waitForAsyncWrites(WAIT_TIMEOUT_MS);
        writeSnapshotSync(buildSnapshot());
    }

    protected final void queueSave() {
        if (!asyncBatchEnabled || batchDelayTicks <= 0L) {
            writeSnapshotSync(buildSnapshot());
            return;
        }
        synchronized (saveLock) {
            saveQueued = true;
            if (saveTask != null) {
                return;
            }
            saveTask = Bukkit.getScheduler().runTaskLater(plugin, this::flushQueuedSave, batchDelayTicks);
        }
    }

    protected final boolean isSaveQueued() {
        return saveQueued;
    }

    protected final boolean isAsyncWriteInProgress() {
        return writingAsync;
    }

    private void flushQueuedSave() {
        synchronized (saveLock) {
            saveTask = null;
            if (!saveQueued) {
                return;
            }
            saveQueued = false;
        }
        scheduleAsyncWrite(buildSnapshot());
    }

    private void scheduleAsyncWrite(YamlConfiguration snapshot) {
        synchronized (asyncWriteLock) {
            pendingSnapshot = snapshot;
            if (writingAsync) {
                return;
            }
            writingAsync = true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::drainAsyncWrites);
    }

    private void drainAsyncWrites() {
        while (true) {
            YamlConfiguration snapshot;
            synchronized (asyncWriteLock) {
                snapshot = pendingSnapshot;
                pendingSnapshot = null;
                if (snapshot == null) {
                    writingAsync = false;
                    return;
                }
            }
            writeSnapshotSync(snapshot);
        }
    }

    private void writeSnapshotSync(YamlConfiguration snapshot) {
        try {
            SafeYamlIO.writeSnapshot(
                plugin,
                file,
                snapshot,
                crashSafeEnabled,
                crashSafeForceFileSync,
                crashSafeKeepBackup
            );
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + exception.getMessage());
        }
    }

    private void reloadSavePolicy() {
        asyncBatchEnabled = plugin.getConfig().getBoolean("storage.async-save.enabled", true);
        long intervalSeconds = Math.max(1L, plugin.getConfig().getLong("storage.async-save.flush-interval-seconds", 5L));
        batchDelayTicks = intervalSeconds * 20L;
        crashSafeEnabled = plugin.getConfig().getBoolean("storage.crash-safe.enabled", true);
        crashSafeForceFileSync = plugin.getConfig().getBoolean("storage.crash-safe.force-file-sync", false);
        crashSafeKeepBackup = plugin.getConfig().getBoolean("storage.crash-safe.keep-backup", true);
        crashSafeRecoverTempOnLoad = plugin.getConfig().getBoolean("storage.crash-safe.recover-temp-on-load", true);
    }

    private boolean cancelQueuedSave() {
        synchronized (saveLock) {
            boolean wasQueued = saveQueued;
            saveQueued = false;
            if (saveTask != null) {
                saveTask.cancel();
                saveTask = null;
            }
            return wasQueued;
        }
    }

    private void waitForAsyncWrites(long timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(100L, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            synchronized (asyncWriteLock) {
                if (!writingAsync) {
                    return;
                }
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void recoverTempIfNeeded() {
        SafeYamlIO.recoverTempIfNeeded(plugin, file, crashSafeEnabled && crashSafeRecoverTempOnLoad, crashSafeForceFileSync);
    }
}
