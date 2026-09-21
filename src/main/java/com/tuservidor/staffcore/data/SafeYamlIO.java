package com.tuservidor.staffcore.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class SafeYamlIO {

    private SafeYamlIO() {
    }

    public static boolean recoverTempIfNeeded(Plugin plugin, File target, boolean recoverEnabled, boolean forceFileSync) {
        if (!recoverEnabled) {
            return false;
        }

        Path targetPath = target.toPath();
        Path tempPath = targetPath.resolveSibling(target.getName() + ".tmp");
        if (!Files.exists(tempPath)) {
            return false;
        }

        if (Files.exists(targetPath)) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not cleanup stale temp for " + target.getName() + ": " + exception.getMessage());
            }
            return false;
        }

        try {
            moveTempIntoTarget(tempPath, targetPath, forceFileSync);
            plugin.getLogger().info("Recovered " + target.getName() + " from temp snapshot.");
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not recover temp snapshot for " + target.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    public static void writeSnapshot(Plugin plugin,
                                     File target,
                                     YamlConfiguration snapshot,
                                     boolean crashSafeEnabled,
                                     boolean forceFileSync,
                                     boolean keepBackup) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create data folder: " + parent.getAbsolutePath());
        }

        if (!crashSafeEnabled) {
            snapshot.save(target);
            return;
        }

        Path targetPath = target.toPath();
        Path tempPath = targetPath.resolveSibling(target.getName() + ".tmp");

        writeSnapshotToTemp(snapshot, tempPath, forceFileSync);

        if (keepBackup && Files.exists(targetPath)) {
            Path backupPath = targetPath.resolveSibling(target.getName() + ".bak");
            Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            if (forceFileSync) {
                forceDirectorySync(backupPath.getParent());
            }
        } else {
            Path backupPath = targetPath.resolveSibling(target.getName() + ".bak");
            try {
                Files.deleteIfExists(backupPath);
            } catch (IOException ignored) {
            }
        }

        moveTempIntoTarget(tempPath, targetPath, forceFileSync);
    }

    private static void writeSnapshotToTemp(YamlConfiguration snapshot, Path tempPath, boolean forceFileSync) throws IOException {
        byte[] bytes = snapshot.saveToString().getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(
            tempPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            if (forceFileSync) {
                channel.force(true);
            }
        }
    }

    private static void moveTempIntoTarget(Path tempPath, Path targetPath, boolean forceFileSync) throws IOException {
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (forceFileSync) {
            forceDirectorySync(targetPath.getParent());
        }
    }

    private static void forceDirectorySync(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (FileChannel ignored = FileChannel.open(directory, StandardOpenOption.READ)) {
            ignored.force(true);
        } catch (IOException ignored) {
            // Some filesystems/OS combinations do not allow fsync on directories.
        }
    }
}
