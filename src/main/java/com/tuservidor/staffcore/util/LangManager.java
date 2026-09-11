package com.tuservidor.staffcore.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class LangManager {

    private final JavaPlugin plugin;
    private final File langFolder;
    private final File playerFile;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage;
    private List<String> available;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        this.playerFile = new File(plugin.getDataFolder(), "player-languages.yml");
        reload();
    }

    public void reload() {
        this.defaultLanguage = plugin.getConfig().getString("language.default", "en").toLowerCase(Locale.ROOT);
        this.available = plugin.getConfig().getStringList("language.available");
        if (available.isEmpty()) {
            available = List.of("en", "es", "fr");
        }

        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create lang folder.");
        }

        languages.clear();
        for (String code : available) {
            String normalized = code.toLowerCase(Locale.ROOT);
            String resource = "lang/" + normalized + ".yml";
            File target = new File(langFolder, normalized + ".yml");
            ensureLanguageFile(resource, target);
            languages.put(normalized, YamlConfiguration.loadConfiguration(target));
        }

        loadPlayerLanguages();
    }

    public String translate(CommandSender sender, String key, Map<String, String> placeholders) {
        String language = sender instanceof Player player ? playerLanguage(player.getUniqueId()) : defaultLanguage;
        String line = value(language, key);
        line = line.replace("{prefix}", Messages.color(plugin.getConfig().getString("prefix", "&8[&bStaffCore&8] &7")));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            line = line.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return Messages.color(line);
    }

    public String translate(String key, Map<String, String> placeholders) {
        String line = value(defaultLanguage, key);
        line = line.replace("{prefix}", Messages.color(plugin.getConfig().getString("prefix", "&8[&bStaffCore&8] &7")));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            line = line.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return Messages.color(line);
    }

    public List<String> availableLanguages() {
        return available;
    }

    public String playerLanguage(UUID uuid) {
        return playerLanguages.getOrDefault(uuid, defaultLanguage);
    }

    public boolean setPlayerLanguage(UUID uuid, String code) {
        String normalized = code.toLowerCase(Locale.ROOT);
        if (!languages.containsKey(normalized)) {
            return false;
        }
        playerLanguages.put(uuid, normalized);
        savePlayerLanguages();
        return true;
    }

    private String value(String language, String key) {
        YamlConfiguration lang = languages.get(language);
        if (lang != null && lang.contains("messages." + key)) {
            return lang.getString("messages." + key, "&cMissing message: " + key);
        }
        boolean allowFallback = plugin.getConfig().getBoolean("language.allow-default-fallback", false);
        if (allowFallback) {
            YamlConfiguration fallback = languages.get(defaultLanguage);
            if (fallback != null && fallback.contains("messages." + key)) {
                return fallback.getString("messages." + key, "&cMissing message: " + key);
            }
        }
        return "&cMissing message: " + key;
    }

    private void loadPlayerLanguages() {
        playerLanguages.clear();
        if (!playerFile.exists()) {
            savePlayerLanguages();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        if (!config.isConfigurationSection("players")) {
            return;
        }
        for (String rawUuid : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                String lang = config.getString("players." + rawUuid, defaultLanguage).toLowerCase(Locale.ROOT);
                if (languages.containsKey(lang)) {
                    playerLanguages.put(uuid, lang);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void savePlayerLanguages() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : playerLanguages.entrySet()) {
            config.set("players." + entry.getKey(), entry.getValue());
        }
        try {
            config.save(playerFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player-languages.yml: " + exception.getMessage());
        }
    }

    private void ensureLanguageFile(String resourcePath, File targetFile) {
        if (!targetFile.exists()) {
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Missing bundled language file: " + resourcePath);
            }
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(targetFile);
        YamlConfiguration bundled = loadBundled(resourcePath);
        if (bundled == null) {
            return;
        }

        boolean changed = mergeMissing(current, bundled, "");
        if (!changed) {
            return;
        }
        try {
            current.save(targetFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not update language file " + targetFile.getName() + ": " + exception.getMessage());
        }
    }

    private YamlConfiguration loadBundled(String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read bundled language " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }

    private boolean mergeMissing(YamlConfiguration current, YamlConfiguration bundled, String path) {
        boolean changed = false;
        ConfigurationSection section = path.isEmpty() ? bundled : bundled.getConfigurationSection(path);
        if (section == null) {
            return false;
        }
        for (String key : section.getKeys(false)) {
            String childPath = path.isEmpty() ? key : path + "." + key;
            Object bundledValue = bundled.get(childPath);
            if (bundledValue instanceof ConfigurationSection) {
                changed |= mergeMissing(current, bundled, childPath);
                continue;
            }
            if (!current.contains(childPath)) {
                current.set(childPath, bundledValue);
                changed = true;
                continue;
            }
            if (bundledValue instanceof String bundledText) {
                String currentText = current.getString(childPath);
                if (currentText != null && shouldReplaceCorruptedText(currentText, bundledText)) {
                    current.set(childPath, bundledText);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean shouldReplaceCorruptedText(String current, String bundled) {
        if (current.equals(bundled)) {
            return false;
        }
        final String mojibakeBlock = "\u00E2\u20AC\u201C";
        final String mojibakeATilde = "\u00C3".toLowerCase(Locale.ROOT);
        final String replacementChar = "\uFFFD";
        String currentLower = current.toLowerCase(Locale.ROOT);
        String bundledLower = bundled.toLowerCase(Locale.ROOT);
        boolean hasMojibake = currentLower.contains(mojibakeBlock)
            || currentLower.contains(mojibakeATilde)
            || currentLower.contains(replacementChar);
        if (!hasMojibake) {
            return shouldReplaceLegacyQuestionBadges(current, bundled);
        }
        return !bundledLower.contains(mojibakeBlock)
            && !bundledLower.contains(mojibakeATilde)
            && !bundledLower.contains(replacementChar);
    }

    private boolean shouldReplaceLegacyQuestionBadges(String current, String bundled) {
        if (current == null || bundled == null) {
            return false;
        }
        String currentLower = current.toLowerCase(Locale.ROOT);
        String bundledLower = bundled.toLowerCase(Locale.ROOT);
        boolean bundledLooksLikeBadge = bundledLower.contains("[alert")
            || bundledLower.contains("[freeze")
            || bundledLower.contains("[helpop")
            || bundledLower.contains("[staff log")
            || bundledLower.contains("[registro staff")
            || bundledLower.contains("[journal staff")
            || bundledLower.contains("[log staff");
        if (!bundledLooksLikeBadge) {
            return false;
        }
        boolean currentHasLegacyQuestionBadge = currentLower.contains("? ")
            || currentLower.startsWith("&c&l?")
            || currentLower.startsWith("&e&l?")
            || currentLower.startsWith("&6&l?")
            || currentLower.startsWith("&b&l?");
        return currentHasLegacyQuestionBadge && !current.contains("[");
    }
}
