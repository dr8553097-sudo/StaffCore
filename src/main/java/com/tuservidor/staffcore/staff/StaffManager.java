package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.ItemBuilder;
import com.tuservidor.staffcore.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StaffManager {

    private final StaffCore plugin;
    private final Messages messages;
    private final VanishManager vanishManager;
    private final Set<UUID> staffMode = new HashSet<>();
    private final Map<UUID, StaffData> savedData = new HashMap<>();
    private final Map<UUID, SpeedProfile> speedProfiles = new HashMap<>();
    private final Map<String, SpeedProfile> speedProfilesByName = new HashMap<>();
    private final File speedFile;

    public StaffManager(StaffCore plugin, Messages messages, VanishManager vanishManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.vanishManager = vanishManager;
        this.speedFile = new File(plugin.getDataFolder(), "staff-speeds.yml");
        reload();
    }

    public void toggle(Player player) {
        if (isStaff(player)) {
            disable(player);
            return;
        }
        enable(player);
    }

    public void enable(Player player) {
        UUID uuid = player.getUniqueId();
        if (!staffMode.add(uuid)) {
            return;
        }

        savedData.put(uuid, new StaffData(player));
        player.setGameMode(staffModeGameMode());
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setHealth(player.getMaxHealth());
        player.setFireTicks(0);
        if (plugin.getConfig().getBoolean("staff-mode.prevent-item-pickup", true)) {
            player.setCanPickupItems(false);
        }
        applyStaffProfileSpeed(player);
        giveTools(player);
        startAura(player);

        if (plugin.getConfig().getBoolean("staff-mode.auto-vanish", true)) {
            vanishManager.vanish(player);
        }

        if (plugin.staffDutyManager() != null && plugin.staffDutyManager().isAutoDutyOnStaffMode()) {
            plugin.staffDutyManager().startDuty(player);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.25f);
        player.sendTitle(messages.resolve(player, "staff-title-enabled"), messages.resolve(player, "staff-subtitle-enabled"), 10, 45, 10);
        messages.send(player, "staff-enabled");
        plugin.staffLogManager().log(player.getName(), "STAFF_MODE", player.getName(), "Enabled staff mode");
    }

    public void disable(Player player) {
        UUID uuid = player.getUniqueId();
        if (!staffMode.remove(uuid)) {
            return;
        }
        stopAura(uuid);
        player.setInvulnerable(false);
        rememberCurrentProfile(player);

        StaffData data = savedData.remove(uuid);
        if (data != null) {
            data.restore(player);
        } else {
            GameMode defaultMode = Bukkit.getDefaultGameMode();
            player.setGameMode(defaultMode != null && defaultMode != GameMode.ADVENTURE && defaultMode != GameMode.SPECTATOR ? defaultMode : GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setCanPickupItems(true);
        }

        if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
            GameMode fallback = Bukkit.getDefaultGameMode();
            player.setGameMode(fallback != null && fallback != GameMode.ADVENTURE && fallback != GameMode.SPECTATOR ? fallback : GameMode.SURVIVAL);
        }

        if (plugin.getConfig().getBoolean("staff-mode.disable-vanish-on-exit", true)) {
            vanishManager.unvanish(player);
        }

        if (plugin.staffDutyManager() != null && plugin.staffDutyManager().isAutoDutyOnStaffMode()) {
            plugin.staffDutyManager().stopDuty(player);
        }

        player.setGlowing(false);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.75f);
        player.sendTitle(messages.resolve(player, "staff-title-disabled"), messages.resolve(player, "staff-subtitle-disabled"), 10, 45, 10);
        messages.send(player, "staff-disabled");
        plugin.staffLogManager().log(player.getName(), "STAFF_MODE", player.getName(), "Disabled staff mode");
        saveProfiles();
    }

    public void disableAll() {
        for (UUID uuid : Set.copyOf(staffMode)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                disable(player);
            }
        }
        saveProfiles();
    }

    private void startAura(Player player) {
        player.setGlowing(true);
    }

    private void stopAura(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.setGlowing(false);
        }
    }

    public boolean isStaff(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    public Set<UUID> staffPlayers() {
        return Collections.unmodifiableSet(staffMode);
    }

    public void reload() {
        speedProfiles.clear();
        speedProfilesByName.clear();
        if (!speedFile.exists()) {
            saveProfiles();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(speedFile);
        for (String rawUuid : config.getKeys(false)) {
            if ("names".equalsIgnoreCase(rawUuid)) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(rawUuid);
                float walk = safeSpeed((float) config.getDouble(rawUuid + ".walk", defaultWalkSpeed()));
                float fly = safeSpeed((float) config.getDouble(rawUuid + ".fly", defaultFlySpeed()));
                speedProfiles.put(uuid, new SpeedProfile(walk, fly));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ConfigurationSection namesSection = config.getConfigurationSection("names");
        if (namesSection != null) {
            for (String name : namesSection.getKeys(false)) {
                String normalized = name.toLowerCase(Locale.ROOT);
                float walk = safeSpeed((float) namesSection.getDouble(name + ".walk", defaultWalkSpeed()));
                float fly = safeSpeed((float) namesSection.getDouble(name + ".fly", defaultFlySpeed()));
                speedProfilesByName.put(normalized, new SpeedProfile(walk, fly));
            }
        }
    }

    public void saveProfiles() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, SpeedProfile> entry : speedProfiles.entrySet()) {
            String path = entry.getKey().toString() + ".";
            config.set(path + "walk", entry.getValue().walk());
            config.set(path + "fly", entry.getValue().fly());
        }
        for (Map.Entry<String, SpeedProfile> entry : speedProfilesByName.entrySet()) {
            String path = "names." + entry.getKey() + ".";
            config.set(path + "walk", entry.getValue().walk());
            config.set(path + "fly", entry.getValue().fly());
        }
        File parent = speedFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for staff-speeds.yml");
            return;
        }
        try {
            config.save(speedFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save staff-speeds.yml: " + exception.getMessage());
        }
    }

    public float setStaffWalkSpeed(Player player, float speed) {
        UUID uuid = player.getUniqueId();
        float value = safeSpeed(speed);
        SpeedProfile current = profileFor(player, defaultProfile());
        SpeedProfile updated = new SpeedProfile(value, current.fly());
        speedProfiles.put(uuid, updated);
        speedProfilesByName.put(player.getName().toLowerCase(Locale.ROOT), updated);
        saveProfiles();
        if (isStaff(player)) {
            player.setWalkSpeed(value);
        }
        return value;
    }

    public float setStaffFlySpeed(Player player, float speed) {
        UUID uuid = player.getUniqueId();
        float value = safeSpeed(speed);
        SpeedProfile current = profileFor(player, defaultProfile());
        SpeedProfile updated = new SpeedProfile(current.walk(), value);
        speedProfiles.put(uuid, updated);
        speedProfilesByName.put(player.getName().toLowerCase(Locale.ROOT), updated);
        saveProfiles();
        if (isStaff(player)) {
            player.setAllowFlight(true);
            player.setFlySpeed(value);
        }
        return value;
    }

    public void refreshToolbar(Player player) {
        if (!isStaff(player)) {
            return;
        }
        boolean vanished = vanishManager.isVanished(player);
        player.getInventory().setItem(7, tool(
            vanished ? Material.LIME_DYE : Material.GRAY_DYE,
            "VANISH",
            vanished
                ? messages.resolve(player, "staff-tool-vanish-on-name")
                : messages.resolve(player, "staff-tool-vanish-off-name"),
            messages.resolve(player, "staff-tool-vanish-lore")
        ));
    }

    private void giveTools(Player player) {
        player.getInventory().clear();
        boolean vanished = vanishManager.isVanished(player);
        player.getInventory().setItem(0, tool(
            Material.COMPASS,
            "TELEPORTER",
            messages.resolve(player, "staff-tool-teleporter-name"),
            messages.resolve(player, "staff-tool-teleporter-lore")
        ));
        player.getInventory().setItem(1, tool(
            Material.PACKED_ICE,
            "FREEZE",
            messages.resolve(player, "staff-tool-freeze-name"),
            messages.resolve(player, "staff-tool-freeze-lore")
        ));
        player.getInventory().setItem(2, tool(
            Material.CHEST,
            "INSPECTOR",
            messages.resolve(player, "staff-tool-inspector-name"),
            messages.resolve(player, "staff-tool-inspector-lore")
        ));
        player.getInventory().setItem(3, tool(
            Material.ENDER_EYE,
            "SPECTATOR",
            messages.resolve(player, "staff-tool-spectator-name"),
            messages.resolve(player, "staff-tool-spectator-lore")
        ));
        player.getInventory().setItem(4, tool(
            Material.NETHER_STAR,
            "PANEL",
            messages.resolve(player, "staff-tool-panel-name"),
            messages.resolve(player, "staff-tool-panel-lore")
        ));
        player.getInventory().setItem(5, tool(
            Material.GOLDEN_CARROT,
            "NIGHT_VISION",
            messages.resolve(player, "staff-tool-nightvision-name"),
            messages.resolve(player, "staff-tool-nightvision-lore")
        ));
        player.getInventory().setItem(7, tool(
            vanished ? Material.LIME_DYE : Material.GRAY_DYE,
            "VANISH",
            vanished
                ? messages.resolve(player, "staff-tool-vanish-on-name")
                : messages.resolve(player, "staff-tool-vanish-off-name"),
            messages.resolve(player, "staff-tool-vanish-lore")
        ));
        player.getInventory().setItem(8, tool(
            Material.REDSTONE,
            "EXIT",
            messages.resolve(player, "staff-tool-exit-name"),
            messages.resolve(player, "staff-tool-exit-lore")
        ));
    }

    private ItemStack tool(Material material, String key, String name, String lore) {
        return new ItemBuilder(material)
            .name(name)
            .lore(lore)
            .tag("staffcore-tool", key)
            .build();
    }

    private void applyStaffProfileSpeed(Player player) {
        SpeedProfile profile = profileFor(player, defaultProfile());
        player.setWalkSpeed(profile.walk());
        player.setFlySpeed(profile.fly());
    }

    private SpeedProfile profileFor(Player player, SpeedProfile fallback) {
        UUID uuid = player.getUniqueId();
        SpeedProfile byUuid = speedProfiles.get(uuid);
        if (byUuid != null) {
            return byUuid;
        }
        String normalizedName = player.getName().toLowerCase(Locale.ROOT);
        SpeedProfile byName = speedProfilesByName.get(normalizedName);
        if (byName != null) {
            speedProfiles.put(uuid, byName);
            saveProfiles();
            return byName;
        }
        return fallback;
    }

    private void rememberCurrentProfile(Player player) {
        SpeedProfile current = new SpeedProfile(safeSpeed(player.getWalkSpeed()), safeSpeed(player.getFlySpeed()));
        speedProfiles.put(player.getUniqueId(), current);
        speedProfilesByName.put(player.getName().toLowerCase(Locale.ROOT), current);
    }

    private SpeedProfile defaultProfile() {
        return new SpeedProfile(defaultWalkSpeed(), defaultFlySpeed());
    }

    private float defaultWalkSpeed() {
        float level = (float) plugin.getConfig().getDouble("staff-mode.default-walk-speed-level", 2.0D);
        return safeSpeed(level / 10.0f);
    }

    private float defaultFlySpeed() {
        float level = (float) plugin.getConfig().getDouble("staff-mode.default-fly-speed-level", 1.0D);
        return safeSpeed(level / 10.0f);
    }

    private float safeSpeed(float speed) {
        return Math.max(0.0f, Math.min(1.0f, speed));
    }

    public GameMode staffModeGameMode() {
        String raw = plugin.getConfig().getString("staff-mode.game-mode-on-enable", "ADVENTURE");
        if (raw == null || raw.isBlank()) {
            return GameMode.ADVENTURE;
        }
        try {
            return GameMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GameMode.ADVENTURE;
        }
    }

    private record SpeedProfile(float walk, float fly) {
    }
}
