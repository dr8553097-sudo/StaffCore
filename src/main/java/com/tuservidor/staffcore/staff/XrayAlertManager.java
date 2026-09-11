package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class XrayAlertManager {

    private final StaffCore plugin;
    private final Messages messages;
    private final File stateFile;

    private final Map<UUID, MiningSample> samples = new HashMap<>();
    private final Map<UUID, Long> joinedAt = new HashMap<>();
    private final Set<UUID> mutedAlerts = new HashSet<>();

    private boolean runtimeEnabled = true;

    private Set<Material> trackedOres = Set.of();
    private Set<String> ignoredWorlds = Set.of();
    private Set<String> whitelistedWorlds = Set.of();
    private Set<Biome> whitelistedBiomes = Set.of();
    private List<WhitelistedMine> whitelistedMines = List.of();
    private String notifyPermission = "staffcore.xray.alerts";
    private String bypassPermission = "staffcore.xray.bypass";
    private int monitorYMax = 48;
    private int deepYLevelMax = 24;
    private int warmupSecondsAfterJoin = 90;
    private int windowSeconds = 180;
    private int minimumTotalBreaks = 80;
    private int minimumValuableOres = 6;
    private double minimumOreRatio = 0.10D;
    private double minimumDeepOreRatio = 0.55D;
    private int burstWindowSeconds = 30;
    private int burstValuableOres = 5;
    private int alertCooldownSeconds = 45;
    private int escalationAfterAlerts = 3;
    private Sound alertSound = Sound.BLOCK_NOTE_BLOCK_PLING;
    private float alertSoundVolume = 1.1f;
    private float alertSoundPitch = 1.0f;
    private long totalCheckNanos = 0L;
    private long totalChecks = 0L;

    public XrayAlertManager(StaffCore plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.stateFile = new File(plugin.getDataFolder(), "xray-alerts.yml");
        reload();
    }

    public void reload() {
        reloadConfigValues();
        loadState();
        samples.clear();
        joinedAt.clear();
        long now = System.currentTimeMillis();
        long warmupMillis = Math.max(0, warmupSecondsAfterJoin) * 1000L;
        for (Player online : Bukkit.getOnlinePlayers()) {
            joinedAt.put(online.getUniqueId(), now - warmupMillis);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("runtime-enabled", runtimeEnabled);
        config.set("muted-alerts", mutedAlerts.stream().map(UUID::toString).toList());
        try {
            config.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save xray-alerts.yml: " + exception.getMessage());
        }
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled;
    }

    public void setRuntimeEnabled(boolean enabled) {
        this.runtimeEnabled = enabled;
        save();
    }

    public boolean togglePersonalAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        boolean enabled;
        if (mutedAlerts.remove(uuid)) {
            enabled = true;
        } else {
            mutedAlerts.add(uuid);
            enabled = false;
        }
        save();
        return enabled;
    }

    public boolean setPersonalAlerts(UUID staff, boolean enabled) {
        boolean changed;
        if (enabled) {
            changed = mutedAlerts.remove(staff);
        } else {
            changed = mutedAlerts.add(staff);
        }
        if (changed) {
            save();
        }
        return changed;
    }

    public boolean personalAlertsEnabled(Player player) {
        return Permissions.has(player, notifyPermission) && !mutedAlerts.contains(player.getUniqueId());
    }

    public int trackedPlayersCount() {
        return samples.size();
    }

    public int warmupTrackedPlayersCount() {
        return joinedAt.size();
    }

    public int mutedAlertsCount() {
        return mutedAlerts.size();
    }

    public int whitelistedMinesCount() {
        return whitelistedMines.size();
    }

    public int whitelistedBiomesCount() {
        return whitelistedBiomes.size();
    }

    public int whitelistedWorldsCount() {
        return whitelistedWorlds.size();
    }

    public long totalChecks() {
        return totalChecks;
    }

    public double averageCheckMicros() {
        if (totalChecks <= 0L) {
            return 0D;
        }
        return (double) totalCheckNanos / (double) totalChecks / 1000.0D;
    }

    public void trackJoin(UUID playerUuid) {
        joinedAt.put(playerUuid, System.currentTimeMillis());
    }

    public void clearSnapshot(UUID playerUuid) {
        samples.remove(playerUuid);
        joinedAt.remove(playerUuid);
    }

    public Optional<XraySnapshot> snapshot(Player player) {
        MiningSample sample = samples.get(player.getUniqueId());
        if (sample == null) {
            return Optional.empty();
        }
        double oreRatio = sample.totalBreaks == 0 ? 0D : (double) sample.valuableBreaks / (double) sample.totalBreaks;
        double deepRatio = sample.valuableBreaks == 0 ? 0D : (double) sample.deepValuableBreaks / (double) sample.valuableBreaks;
        return Optional.of(new XraySnapshot(
            player.getName(),
            sample.totalBreaks,
            sample.valuableBreaks,
            sample.deepValuableBreaks,
            oreRatio,
            deepRatio,
            sample.alertsSent,
            dominantOre(sample)
        ));
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        long checkStartNanos = System.nanoTime();
        try {
            if (event.isCancelled()) {
                return;
            }
            if (!plugin.featureEnabled("xray-alerts")) {
                return;
            }
            if (!plugin.getConfig().getBoolean("xray-alerts.enabled", true)) {
                return;
            }
            if (!runtimeEnabled) {
                return;
            }

            Player player = event.getPlayer();
            if (Permissions.has(player, bypassPermission)) {
                return;
            }
            if (ignoredWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
                return;
            }
            if (isWhitelisted(event)) {
                return;
            }

            long now = System.currentTimeMillis();
            if (warmupActive(player.getUniqueId(), now)) {
                return;
            }
            if (event.getBlock().getY() > monitorYMax) {
                return;
            }

            long windowMillis = windowSeconds * 1000L;

            MiningSample sample = samples.computeIfAbsent(player.getUniqueId(), uuid -> new MiningSample(now));
            if (now - sample.windowStartMillis > windowMillis) {
                sample.reset(now);
            }

            sample.totalBreaks++;

            Material broken = event.getBlock().getType();
            boolean valuable = trackedOres.contains(broken);
            if (!valuable) {
                return;
            }

            sample.valuableBreaks++;
            if (event.getBlock().getY() <= deepYLevelMax) {
                sample.deepValuableBreaks++;
            }
            sample.oreCounts.merge(broken, 1, Integer::sum);

            long burstWindowMillis = burstWindowSeconds * 1000L;
            sample.valuableBurstTimes.addLast(now);
            while (!sample.valuableBurstTimes.isEmpty() && now - sample.valuableBurstTimes.peekFirst() > burstWindowMillis) {
                sample.valuableBurstTimes.removeFirst();
            }

            if (!cooldownReady(now, sample.lastAlertMillis, alertCooldownSeconds)) {
                return;
            }

            double oreRatio = sample.totalBreaks == 0 ? 0D : (double) sample.valuableBreaks / (double) sample.totalBreaks;
            double deepRatio = sample.valuableBreaks == 0 ? 0D : (double) sample.deepValuableBreaks / (double) sample.valuableBreaks;
            TriggerEvaluation evaluation = evaluateTriggers(
                sample.totalBreaks,
                sample.valuableBreaks,
                oreRatio,
                deepRatio,
                sample.valuableBurstTimes.size(),
                minimumTotalBreaks,
                minimumValuableOres,
                minimumOreRatio,
                minimumDeepOreRatio,
                burstValuableOres
            );

            if (!evaluation.triggered()) {
                return;
            }

            sample.lastAlertMillis = now;
            sample.alertsSent++;

            String trigger = evaluation.triggerType();
            double score = computeScore(oreRatio, deepRatio, sample.alertsSent, sample.valuableBurstTimes.size());
            String dominantOre = dominantOre(sample);

            Map<String, String> placeholders = Map.ofEntries(
                Map.entry("player", player.getName()),
                Map.entry("world", player.getWorld().getName()),
                Map.entry("x", String.valueOf(player.getLocation().getBlockX())),
                Map.entry("y", String.valueOf(player.getLocation().getBlockY())),
                Map.entry("z", String.valueOf(player.getLocation().getBlockZ())),
                Map.entry("trigger", trigger),
                Map.entry("score", String.format(Locale.US, "%.1f", score)),
                Map.entry("total", String.valueOf(sample.totalBreaks)),
                Map.entry("valuable", String.valueOf(sample.valuableBreaks)),
                Map.entry("deep", String.valueOf(sample.deepValuableBreaks)),
                Map.entry("ratio", String.format(Locale.US, "%.2f", oreRatio * 100.0D)),
                Map.entry("deepRatio", String.format(Locale.US, "%.2f", deepRatio * 100.0D)),
                Map.entry("dominantOre", dominantOre)
            );

            String consoleLine1 = messages.resolve("xray-alert-line-1", placeholders);
            String consoleLine2 = messages.resolve("xray-alert-line-2", placeholders);
            Bukkit.getConsoleSender().sendMessage(consoleLine1);
            Bukkit.getConsoleSender().sendMessage(consoleLine2);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (personalAlertsEnabled(online)) {
                    sendInteractiveAlert(online, player, placeholders);
                }
            }

            plugin.staffLogManager().log("System", "XRAY_ALERT", player.getName(),
                "trigger=" + trigger
                    + ", score=" + String.format(Locale.US, "%.1f", score)
                    + ", total=" + sample.totalBreaks
                    + ", valuable=" + sample.valuableBreaks
                    + ", ratio=" + String.format(Locale.US, "%.2f", oreRatio * 100.0D) + "%"
                    + ", deepRatio=" + String.format(Locale.US, "%.2f", deepRatio * 100.0D) + "%"
                    + ", dominantOre=" + dominantOre);

            if (!sample.escalated && sample.alertsSent >= escalationAfterAlerts) {
                sample.escalated = true;
                String escalationConsole = messages.resolve("xray-alert-escalation", placeholders);
                Bukkit.getConsoleSender().sendMessage(escalationConsole);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (personalAlertsEnabled(online)) {
                        online.sendMessage(messages.resolve(online, "xray-alert-escalation", placeholders));
                    }
                }
                plugin.staffLogManager().log("System", "XRAY_ESCALATION", player.getName(), "Escalated after " + sample.alertsSent + " alerts");
            }
        } finally {
            totalChecks++;
            totalCheckNanos += Math.max(0L, System.nanoTime() - checkStartNanos);
        }
    }

    private void reloadConfigValues() {
        notifyPermission = plugin.getConfig().getString("xray-alerts.notify-permission", "staffcore.xray.alerts");
        bypassPermission = plugin.getConfig().getString("xray-alerts.bypass-permission", "staffcore.xray.bypass");

        monitorYMax = plugin.getConfig().getInt("xray-alerts.monitor-y-max", 48);
        deepYLevelMax = plugin.getConfig().getInt("xray-alerts.deep-y-level-max", 24);
        warmupSecondsAfterJoin = Math.max(0, plugin.getConfig().getInt("xray-alerts.warmup-seconds-after-join", 90));
        windowSeconds = Math.max(30, plugin.getConfig().getInt("xray-alerts.window-seconds", 180));
        minimumTotalBreaks = Math.max(15, plugin.getConfig().getInt("xray-alerts.minimum-total-breaks", 80));
        minimumValuableOres = Math.max(3, plugin.getConfig().getInt("xray-alerts.minimum-valuable-ores", 6));
        minimumOreRatio = clamp01(plugin.getConfig().getDouble("xray-alerts.minimum-ore-ratio", 0.10D));
        minimumDeepOreRatio = clamp01(plugin.getConfig().getDouble("xray-alerts.minimum-deep-ore-ratio", 0.55D));
        burstWindowSeconds = Math.max(10, plugin.getConfig().getInt("xray-alerts.burst-window-seconds", 30));
        burstValuableOres = Math.max(3, plugin.getConfig().getInt("xray-alerts.burst-valuable-ores", 5));
        alertCooldownSeconds = Math.max(10, plugin.getConfig().getInt("xray-alerts.alert-cooldown-seconds", 45));
        escalationAfterAlerts = Math.max(2, plugin.getConfig().getInt("xray-alerts.escalation-after-alerts", 3));
        alertSound = parseSound(plugin.getConfig().getString("xray-alerts.alert-sound", "BLOCK_NOTE_BLOCK_PLING"));
        alertSoundVolume = (float) plugin.getConfig().getDouble("xray-alerts.alert-volume", 1.1D);
        alertSoundPitch = (float) plugin.getConfig().getDouble("xray-alerts.alert-pitch", 1.0D);

        Set<Material> ores = new HashSet<>();
        for (String raw : plugin.getConfig().getStringList("xray-alerts.track-ores")) {
            Material material = Material.matchMaterial(raw);
            if (material != null) {
                ores.add(material);
            }
        }
        if (ores.isEmpty()) {
            ores.add(Material.DIAMOND_ORE);
            ores.add(Material.DEEPSLATE_DIAMOND_ORE);
            ores.add(Material.ANCIENT_DEBRIS);
        }
        trackedOres = Set.copyOf(ores);

        Set<String> worlds = new HashSet<>();
        for (String world : plugin.getConfig().getStringList("xray-alerts.ignored-worlds")) {
            worlds.add(world.toLowerCase(Locale.ROOT));
        }
        ignoredWorlds = Set.copyOf(worlds);

        Set<String> whiteWorlds = new HashSet<>();
        for (String world : plugin.getConfig().getStringList("xray-alerts.whitelist-worlds")) {
            if (world != null && !world.isBlank()) {
                whiteWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }
        whitelistedWorlds = Set.copyOf(whiteWorlds);

        Set<Biome> biomes = new HashSet<>();
        for (String raw : plugin.getConfig().getStringList("xray-alerts.whitelist-biomes")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                biomes.add(Biome.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        whitelistedBiomes = Set.copyOf(biomes);

        List<WhitelistedMine> mines = new ArrayList<>();
        int index = 0;
        for (Map<?, ?> row : plugin.getConfig().getMapList("xray-alerts.whitelist-mines")) {
            index++;
            Object worldRaw = row.get("world");
            Object pos1Raw = row.get("pos1");
            Object pos2Raw = row.get("pos2");
            if (!(worldRaw instanceof String world) || !(pos1Raw instanceof String pos1) || !(pos2Raw instanceof String pos2)) {
                continue;
            }
            IntPoint first = parsePoint(pos1);
            IntPoint second = parsePoint(pos2);
            if (first == null || second == null) {
                continue;
            }
            Object rawId = row.containsKey("id") ? row.get("id") : "mine-" + index;
            String id = String.valueOf(rawId);
            mines.add(new WhitelistedMine(
                id,
                world,
                Math.min(first.x(), second.x()),
                Math.min(first.y(), second.y()),
                Math.min(first.z(), second.z()),
                Math.max(first.x(), second.x()),
                Math.max(first.y(), second.y()),
                Math.max(first.z(), second.z())
            ));
        }
        whitelistedMines = List.copyOf(mines);
    }

    private void loadState() {
        mutedAlerts.clear();
        if (!stateFile.exists()) {
            runtimeEnabled = plugin.getConfig().getBoolean("xray-alerts.runtime-enabled", true);
            save();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        runtimeEnabled = config.getBoolean("runtime-enabled", plugin.getConfig().getBoolean("xray-alerts.runtime-enabled", true));
        for (String raw : config.getStringList("muted-alerts")) {
            try {
                mutedAlerts.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    static boolean cooldownReady(long now, long lastAlertMillis, int cooldownSeconds) {
        return now - lastAlertMillis >= Math.max(0, cooldownSeconds) * 1000L;
    }

    static TriggerEvaluation evaluateTriggers(
        int totalBreaks,
        int valuableBreaks,
        double oreRatio,
        double deepRatio,
        int burstCount,
        int minimumTotalBreaks,
        int minimumValuableOres,
        double minimumOreRatio,
        double minimumDeepOreRatio,
        int burstValuableOres
    ) {
        boolean ratioTrigger = totalBreaks >= minimumTotalBreaks
            && valuableBreaks >= minimumValuableOres
            && oreRatio >= minimumOreRatio
            && deepRatio >= minimumDeepOreRatio;
        boolean burstTrigger = burstCount >= burstValuableOres;
        boolean triggered = ratioTrigger || burstTrigger;
        String triggerType = burstTrigger ? "BURST" : "RATIO";
        return new TriggerEvaluation(ratioTrigger, burstTrigger, triggered, triggerType);
    }

    private boolean warmupActive(UUID playerUuid, long now) {
        long joined = joinedAt.computeIfAbsent(playerUuid, key -> now);
        if (warmupSecondsAfterJoin <= 0) {
            return false;
        }
        return now - joined < warmupSecondsAfterJoin * 1000L;
    }

    private boolean isWhitelisted(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName().toLowerCase(Locale.ROOT);
        if (whitelistedWorlds.contains(worldName)) {
            return true;
        }
        if (whitelistedBiomes.contains(event.getBlock().getBiome())) {
            return true;
        }
        Location location = event.getBlock().getLocation();
        for (WhitelistedMine mine : whitelistedMines) {
            if (mine.contains(location)) {
                return true;
            }
        }
        return false;
    }

    private IntPoint parsePoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new IntPoint(
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim())
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double computeScore(double oreRatio, double deepRatio, int alerts, int burstAmount) {
        double score = (oreRatio * 100.0D)
            + (deepRatio * 35.0D)
            + (alerts * 8.0D)
            + (Math.max(0, burstAmount - burstValuableOres) * 6.0D);
        return Math.min(100.0D, Math.max(0.0D, score));
    }

    private void sendInteractiveAlert(Player viewer, Player suspect, Map<String, String> placeholders) {
        String line1 = messages.resolve(viewer, "xray-alert-line-1", placeholders);
        String line2 = messages.resolve(viewer, "xray-alert-line-2", placeholders);

        String tpCommand = "/tp " + suspect.getName();
        String statsCommand = "/xrayalerts stats " + suspect.getName();
        String coordHint = suspect.getWorld().getName()
            + " | "
            + suspect.getLocation().getBlockX() + ","
            + suspect.getLocation().getBlockY() + ","
            + suspect.getLocation().getBlockZ();

        String hoverPrimary = messages.resolve(viewer, "xray-alert-hover-primary", Map.ofEntries(
            Map.entry("player", suspect.getName()),
            Map.entry("command", tpCommand),
            Map.entry("world", suspect.getWorld().getName()),
            Map.entry("x", String.valueOf(suspect.getLocation().getBlockX())),
            Map.entry("y", String.valueOf(suspect.getLocation().getBlockY())),
            Map.entry("z", String.valueOf(suspect.getLocation().getBlockZ())),
            Map.entry("trigger", placeholders.getOrDefault("trigger", "UNKNOWN")),
            Map.entry("score", placeholders.getOrDefault("score", "0.0")),
            Map.entry("total", placeholders.getOrDefault("total", "0")),
            Map.entry("valuable", placeholders.getOrDefault("valuable", "0")),
            Map.entry("deep", placeholders.getOrDefault("deep", "0")),
            Map.entry("ratio", placeholders.getOrDefault("ratio", "0.00")),
            Map.entry("deepRatio", placeholders.getOrDefault("deepRatio", "0.00")),
            Map.entry("dominantOre", placeholders.getOrDefault("dominantOre", "UNKNOWN"))
        ));
        String hoverSecondary = messages.resolve(viewer, "xray-alert-hover-secondary", Map.ofEntries(
            Map.entry("player", suspect.getName()),
            Map.entry("command", statsCommand),
            Map.entry("coords", coordHint),
            Map.entry("trigger", placeholders.getOrDefault("trigger", "UNKNOWN")),
            Map.entry("score", placeholders.getOrDefault("score", "0.0")),
            Map.entry("total", placeholders.getOrDefault("total", "0")),
            Map.entry("valuable", placeholders.getOrDefault("valuable", "0")),
            Map.entry("deep", placeholders.getOrDefault("deep", "0")),
            Map.entry("ratio", placeholders.getOrDefault("ratio", "0.00")),
            Map.entry("deepRatio", placeholders.getOrDefault("deepRatio", "0.00")),
            Map.entry("dominantOre", placeholders.getOrDefault("dominantOre", "UNKNOWN"))
        ));

        TextComponent firstLine = legacy(line1);
        firstLine.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand));
        firstLine.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(hoverPrimary)));

        TextComponent secondLine = legacy(line2);
        secondLine.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, statsCommand));
        secondLine.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(hoverSecondary)));

        viewer.spigot().sendMessage(firstLine);
        viewer.spigot().sendMessage(secondLine);
        if (alertSound != null) {
            viewer.playSound(
                viewer.getLocation(),
                alertSound,
                Math.max(0f, alertSoundVolume),
                Math.max(0f, alertSoundPitch)
            );
        }
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private TextComponent legacy(String text) {
        return new TextComponent(TextComponent.fromLegacyText(Messages.color(text)));
    }

    private BaseComponent[] hover(String text) {
        return new ComponentBuilder(Messages.color(text)).create();
    }

    private String dominantOre(MiningSample sample) {
        return sample.oreCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
            .map(entry -> entry.getKey().name())
            .orElse("UNKNOWN");
    }

    private double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static final class MiningSample {
        long windowStartMillis;
        long lastAlertMillis;
        int totalBreaks;
        int valuableBreaks;
        int deepValuableBreaks;
        int alertsSent;
        boolean escalated;
        final Map<Material, Integer> oreCounts = new HashMap<>();
        final ArrayDeque<Long> valuableBurstTimes = new ArrayDeque<>();

        MiningSample(long now) {
            this.windowStartMillis = now;
            this.lastAlertMillis = 0L;
        }

        void reset(long now) {
            windowStartMillis = now;
            totalBreaks = 0;
            valuableBreaks = 0;
            deepValuableBreaks = 0;
            alertsSent = 0;
            escalated = false;
            oreCounts.clear();
            valuableBurstTimes.clear();
        }
    }

    public record XraySnapshot(
        String player,
        int totalBreaks,
        int valuableBreaks,
        int deepValuableBreaks,
        double oreRatio,
        double deepOreRatio,
        int alertsSent,
        String dominantOre
    ) {
    }

    record TriggerEvaluation(
        boolean ratioTrigger,
        boolean burstTrigger,
        boolean triggered,
        String triggerType
    ) {
    }

    private record IntPoint(int x, int y, int z) {
    }

    private record WhitelistedMine(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
    ) {
        boolean contains(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            if (!location.getWorld().getName().equalsIgnoreCase(world)) {
                return false;
            }
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
        }
    }
}
