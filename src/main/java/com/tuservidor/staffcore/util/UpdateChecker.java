package com.tuservidor.staffcore.util;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateChecker {

    private final StaffCore plugin;
    private final Messages messages;
    private final Set<UUID> notifiedPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean checkRunning = new AtomicBoolean(false);

    private BukkitTask periodicTask;
    private boolean enabled;
    private boolean checkOnStartup;
    private boolean notifyOnJoin;
    private boolean notifyOp;
    private boolean consoleLogUpToDate;
    private boolean consoleLogFailures;
    private String notifyPermission;
    private String apiUrlTemplate;
    private String resourcePageUrl;
    private int resourceId;
    private int connectTimeoutMs;
    private int readTimeoutMs;

    private volatile boolean checked;
    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private volatile long lastCheckAtMillis;

    public UpdateChecker(StaffCore plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.latestVersion = "";
    }

    public void reload() {
        shutdownPeriodicTask();
        resetState();

        enabled = plugin.getConfig().getBoolean("update-checker.enabled", true);
        checkOnStartup = plugin.getConfig().getBoolean("update-checker.check-on-startup", true);
        notifyOnJoin = plugin.getConfig().getBoolean("update-checker.notify-on-join", true);
        notifyOp = plugin.getConfig().getBoolean("update-checker.notify-op", true);
        notifyPermission = plugin.getConfig().getString("update-checker.notify-permission", "staffcore.update");
        apiUrlTemplate = plugin.getConfig().getString(
            "update-checker.api-url",
            "https://api.spigotmc.org/legacy/update.php?resource={resource}"
        );
        resourcePageUrl = plugin.getConfig().getString("update-checker.resource-page-url", "");
        resourceId = Math.max(0, plugin.getConfig().getInt("update-checker.spigot-resource-id", 0));
        connectTimeoutMs = Math.max(1000, plugin.getConfig().getInt("update-checker.connect-timeout-ms", 4000));
        readTimeoutMs = Math.max(1000, plugin.getConfig().getInt("update-checker.read-timeout-ms", 4000));
        consoleLogUpToDate = plugin.getConfig().getBoolean("update-checker.console-log-up-to-date", false);
        consoleLogFailures = plugin.getConfig().getBoolean("update-checker.console-log-failures", true);

        if (!enabled) {
            return;
        }
        if (resourceId <= 0) {
            plugin.getLogger().warning("Update checker enabled but update-checker.spigot-resource-id is invalid.");
            return;
        }

        if (checkOnStartup) {
            runCheckAsync();
        }

        long minutes = Math.max(15L, plugin.getConfig().getLong("update-checker.check-interval-minutes", 180L));
        long periodTicks = minutesToTicks(minutes);
        periodicTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::runCheckNow,
            periodTicks,
            periodTicks
        );
    }

    public void shutdown() {
        shutdownPeriodicTask();
    }

    public void handleJoin(Player player) {
        if (!enabled || !notifyOnJoin) {
            return;
        }
        notifyPlayerIfNeeded(player);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean hasUpdate() {
        return checked && updateAvailable;
    }

    public String latestVersion() {
        return latestVersion;
    }

    public long lastCheckAtMillis() {
        return lastCheckAtMillis;
    }

    private void runCheckAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::runCheckNow);
    }

    private void runCheckNow() {
        if (!enabled || resourceId <= 0) {
            return;
        }
        if (!checkRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            String currentVersion = plugin.getDescription().getVersion();
            String remoteVersion = fetchRemoteVersion();
            boolean foundUpdate = compareVersions(currentVersion, remoteVersion) < 0;
            boolean updateStateChanged = foundUpdate != updateAvailable;
            boolean remoteChanged = !remoteVersion.equalsIgnoreCase(latestVersion);

            checked = true;
            updateAvailable = foundUpdate;
            latestVersion = remoteVersion;
            lastCheckAtMillis = System.currentTimeMillis();
            if (foundUpdate) {
                plugin.getLogger().info("Update available: " + currentVersion + " -> " + remoteVersion);
                if (updateStateChanged || remoteChanged) {
                    notifiedPlayers.clear();
                    Bukkit.getScheduler().runTask(plugin, this::notifyOnlineStaff);
                }
            } else if (consoleLogUpToDate) {
                plugin.getLogger().info("StaffCore is up to date: " + currentVersion);
            }
        } catch (Exception exception) {
            if (consoleLogFailures) {
                plugin.getLogger().warning("Update check failed: " + exception.getMessage());
            }
        } finally {
            checkRunning.set(false);
        }
    }

    private String fetchRemoteVersion() throws IOException {
        String endpoint = apiUrlTemplate.replace("{resource}", String.valueOf(resourceId)).trim();
        if (endpoint.isEmpty()) {
            throw new IOException("update-checker.api-url is empty.");
        }

        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", "StaffCore/" + plugin.getDescription().getVersion());

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " while querying update endpoint.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = reader.readLine();
            if (response == null || response.isBlank()) {
                throw new IOException("Empty response from update endpoint.");
            }
            return response.trim();
        } finally {
            connection.disconnect();
        }
    }

    private void notifyOnlineStaff() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            notifyPlayerIfNeeded(player);
        }
    }

    private void notifyPlayerIfNeeded(Player player) {
        if (!checked || !updateAvailable) {
            return;
        }
        if (!canReceiveUpdateNotice(player)) {
            return;
        }
        if (!notifiedPlayers.add(player.getUniqueId())) {
            return;
        }
        String currentVersion = plugin.getDescription().getVersion();
        messages.send(player, "update-available-line-1", Map.of(
            "current", currentVersion,
            "latest", latestVersion
        ));
        if (resourcePageUrl != null && !resourcePageUrl.isBlank()) {
            messages.send(player, "update-available-line-2", Map.of("url", resourcePageUrl));
        }
    }

    private boolean canReceiveUpdateNotice(Player player) {
        if (notifyOp && player.isOp()) {
            return true;
        }
        if (notifyPermission == null || notifyPermission.isBlank()) {
            return false;
        }
        return Permissions.has(player, notifyPermission);
    }

    private void shutdownPeriodicTask() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    private void resetState() {
        checked = false;
        updateAvailable = false;
        latestVersion = "";
        lastCheckAtMillis = 0L;
        notifiedPlayers.clear();
    }

    private long minutesToTicks(long minutes) {
        long clampedMinutes = Math.max(1L, minutes);
        long seconds;
        try {
            seconds = Duration.ofMinutes(clampedMinutes).getSeconds();
        } catch (ArithmeticException ignored) {
            seconds = Long.MAX_VALUE / 20L;
        }
        long ticks = seconds * 20L;
        return Math.max(20L, ticks);
    }

    static int compareVersions(String current, String latest) {
        ParsedVersion currentParsed = ParsedVersion.parse(current);
        ParsedVersion latestParsed = ParsedVersion.parse(latest);
        return currentParsed.compareTo(latestParsed);
    }

    private static final class ParsedVersion implements Comparable<ParsedVersion> {
        private final List<Integer> core;
        private final List<String> preRelease;

        private ParsedVersion(List<Integer> core, List<String> preRelease) {
            this.core = core;
            this.preRelease = preRelease;
        }

        static ParsedVersion parse(String raw) {
            String cleaned = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (cleaned.startsWith("v")) {
                cleaned = cleaned.substring(1);
            }
            int plusIndex = cleaned.indexOf('+');
            if (plusIndex >= 0) {
                cleaned = cleaned.substring(0, plusIndex);
            }

            String corePart = cleaned;
            String preReleasePart = "";
            int dashIndex = cleaned.indexOf('-');
            if (dashIndex >= 0) {
                corePart = cleaned.substring(0, dashIndex);
                if (dashIndex + 1 < cleaned.length()) {
                    preReleasePart = cleaned.substring(dashIndex + 1);
                }
            }

            List<Integer> coreNumbers = new ArrayList<>();
            String[] coreTokens = corePart.split("[^0-9]+");
            for (String token : coreTokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                try {
                    coreNumbers.add(Integer.parseInt(token));
                } catch (NumberFormatException ignored) {
                    coreNumbers.add(0);
                }
            }
            if (coreNumbers.isEmpty()) {
                coreNumbers.add(0);
            }

            List<String> pre = new ArrayList<>();
            if (!preReleasePart.isBlank()) {
                String[] preTokens = preReleasePart.split("[.-]");
                for (String token : preTokens) {
                    if (token == null || token.isBlank()) {
                        continue;
                    }
                    pre.add(token);
                }
            }
            return new ParsedVersion(coreNumbers, pre);
        }

        @Override
        public int compareTo(ParsedVersion other) {
            int maxCore = Math.max(core.size(), other.core.size());
            for (int i = 0; i < maxCore; i++) {
                int left = i < core.size() ? core.get(i) : 0;
                int right = i < other.core.size() ? other.core.get(i) : 0;
                if (left != right) {
                    return Integer.compare(left, right);
                }
            }

            boolean thisPreEmpty = preRelease.isEmpty();
            boolean otherPreEmpty = other.preRelease.isEmpty();
            if (thisPreEmpty && otherPreEmpty) {
                return 0;
            }
            if (thisPreEmpty) {
                return 1;
            }
            if (otherPreEmpty) {
                return -1;
            }

            int maxPre = Math.max(preRelease.size(), other.preRelease.size());
            for (int i = 0; i < maxPre; i++) {
                if (i >= preRelease.size()) {
                    return -1;
                }
                if (i >= other.preRelease.size()) {
                    return 1;
                }
                String leftToken = preRelease.get(i);
                String rightToken = other.preRelease.get(i);
                boolean leftNumeric = isNumeric(leftToken);
                boolean rightNumeric = isNumeric(rightToken);
                if (leftNumeric && rightNumeric) {
                    long left = Long.parseLong(leftToken);
                    long right = Long.parseLong(rightToken);
                    if (left != right) {
                        return Long.compare(left, right);
                    }
                    continue;
                }
                if (leftNumeric != rightNumeric) {
                    return leftNumeric ? -1 : 1;
                }
                int lexical = leftToken.compareTo(rightToken);
                if (lexical != 0) {
                    return lexical;
                }
            }
            return 0;
        }

        private static boolean isNumeric(String token) {
            for (int index = 0; index < token.length(); index++) {
                if (!Character.isDigit(token.charAt(index))) {
                    return false;
                }
            }
            return !token.isEmpty();
        }
    }
}
