package com.tuservidor.staffcore;

import com.tuservidor.staffcore.commands.FreezeCommand;
import com.tuservidor.staffcore.commands.ChatMuteCommand;
import com.tuservidor.staffcore.commands.HelpOpCommand;
import com.tuservidor.staffcore.commands.HistoryCommand;
import com.tuservidor.staffcore.commands.MuteCommand;
import com.tuservidor.staffcore.commands.NotesCommand;
import com.tuservidor.staffcore.commands.ReportCommand;
import com.tuservidor.staffcore.commands.ReportsCommand;
import com.tuservidor.staffcore.commands.StaffBanCommand;
import com.tuservidor.staffcore.commands.StaffBanIpCommand;
import com.tuservidor.staffcore.commands.StaffChatCommand;
import com.tuservidor.staffcore.commands.StaffCommand;
import com.tuservidor.staffcore.commands.StaffKickCommand;
import com.tuservidor.staffcore.commands.StaffLangCommand;
import com.tuservidor.staffcore.commands.StaffLogsCommand;
import com.tuservidor.staffcore.commands.StaffPanelCommand;
import com.tuservidor.staffcore.commands.StaffCoreTabCompleter;
import com.tuservidor.staffcore.commands.TempBanCommand;
import com.tuservidor.staffcore.commands.TempBanIpCommand;
import com.tuservidor.staffcore.commands.UnbanCommand;
import com.tuservidor.staffcore.commands.UnbanIpCommand;
import com.tuservidor.staffcore.commands.UnmuteCommand;
import com.tuservidor.staffcore.commands.VanishCommand;
import com.tuservidor.staffcore.commands.WarnCommand;
import com.tuservidor.staffcore.commands.XrayAlertsCommand;
import com.tuservidor.staffcore.data.FirstJoinManager;
import com.tuservidor.staffcore.data.NoteManager;
import com.tuservidor.staffcore.data.PunishmentManager;
import com.tuservidor.staffcore.data.StaffLogManager;
import com.tuservidor.staffcore.gui.InventoryTagHolder;
import com.tuservidor.staffcore.gui.MenuManager;
import com.tuservidor.staffcore.listeners.ChatModerationListener;
import com.tuservidor.staffcore.listeners.CommandVisibilityListener;
import com.tuservidor.staffcore.listeners.CombatProtectionListener;
import com.tuservidor.staffcore.listeners.FreezeListener;
import com.tuservidor.staffcore.listeners.ListenerSupport;
import com.tuservidor.staffcore.listeners.PlayerSessionListener;
import com.tuservidor.staffcore.listeners.StaffModeListener;
import com.tuservidor.staffcore.reports.ReportManager;
import com.tuservidor.staffcore.staff.FreezeManager;
import com.tuservidor.staffcore.staff.StaffChatManager;
import com.tuservidor.staffcore.staff.StaffManager;
import com.tuservidor.staffcore.staff.StaffHudManager;
import com.tuservidor.staffcore.staff.VanishManager;
import com.tuservidor.staffcore.staff.XrayAlertManager;
import com.tuservidor.staffcore.storage.StorageModeResolver;
import com.tuservidor.staffcore.storage.StorageModeSelection;
import com.tuservidor.staffcore.util.ItemBuilder;
import com.tuservidor.staffcore.util.LangManager;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class StaffCore extends JavaPlugin {

    private Messages messages;
    private LangManager langManager;
    private VanishManager vanishManager;
    private FreezeManager freezeManager;
    private StaffChatManager staffChatManager;
    private StaffManager staffManager;
    private ReportManager reportManager;
    private XrayAlertManager xrayAlertManager;
    private StaffLogManager staffLogManager;
    private NoteManager noteManager;
    private PunishmentManager punishmentManager;
    private FirstJoinManager firstJoinManager;
    private MenuManager menuManager;
    private StaffHudManager staffHudManager;
    private UpdateChecker updateChecker;
    private TabCompleter tabCompleter;
    private File uiFile;
    private YamlConfiguration uiConfig;
    private volatile boolean chatMuted;
    private StorageModeSelection storageModeSelection;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        syncConfigDefaults();
        resolveStorageMode();
        reloadUiConfig();
        loadChatMuteStateFromConfig();
        ensureWikiFiles();
        ItemBuilder.init(this);
        this.langManager = new LangManager(this);
        this.messages = new Messages(this, langManager);
        this.vanishManager = new VanishManager(this, messages);
        this.freezeManager = new FreezeManager(this, messages);
        this.staffChatManager = new StaffChatManager();
        this.staffLogManager = new StaffLogManager(this);
        this.noteManager = new NoteManager(this);
        this.firstJoinManager = new FirstJoinManager(this);
        this.punishmentManager = new PunishmentManager(this, messages, staffLogManager);
        this.reportManager = new ReportManager(this, messages);
        this.xrayAlertManager = new XrayAlertManager(this, messages);
        this.staffManager = new StaffManager(this, messages, vanishManager);
        this.menuManager = new MenuManager(this);
        this.staffHudManager = new StaffHudManager(this, staffManager, vanishManager);
        this.updateChecker = new UpdateChecker(this, messages);
        this.updateChecker.reload();
        this.tabCompleter = new StaffCoreTabCompleter(this);

        registerCommands();
        registerDomainListeners();
        Bukkit.getScheduler().runTask(this, () -> vanishManager.resyncAllOnlineVisibility());

        messages.console("&8&m----------------------------------------");
        messages.console("&bStaffCore &7| &fModern Moderation Suite");
        messages.console("&7Version: &f" + getDescription().getVersion());
        messages.console("&7Paper API: &f" + Bukkit.getBukkitVersion());
        messages.console("&7Supported Version: " + (isSupportedServerVersion() ? "&aYes (Native 1.21.x / 26.x)" : "&eYes (Compatible)"));
        messages.console("&aReady in " + (System.currentTimeMillis() - start) + "ms");
        messages.console("&8&m----------------------------------------");
    }

    private boolean isSupportedServerVersion() {
        String ver = Bukkit.getBukkitVersion().toLowerCase(Locale.ROOT);
        return ver.contains("1.21") || ver.contains("1.20") || ver.contains("26.");
    }

    @Override
    public void onDisable() {
        if (staffManager != null) {
            staffManager.disableAll();
        }
        if (reportManager != null) {
            reportManager.save();
        }
        if (noteManager != null) {
            noteManager.save();
        }
        if (punishmentManager != null) {
            punishmentManager.save();
        }
        if (staffLogManager != null) {
            staffLogManager.save();
        }
        if (firstJoinManager != null) {
            firstJoinManager.save();
        }
        if (freezeManager != null) {
            freezeManager.save();
        }
        if (vanishManager != null) {
            vanishManager.shutdown();
        }
        if (xrayAlertManager != null) {
            xrayAlertManager.save();
        }
        if (staffHudManager != null) {
            staffHudManager.shutdown();
        }
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
        if (messages != null) {
            messages.console("&cStaffCore disabled safely.");
        } else {
            getLogger().info("StaffCore disabled safely.");
        }
    }

    public void reloadPlugin() {
        createSafetyBackup();
        reloadConfig();
        syncConfigDefaults();
        resolveStorageMode();
        reloadUiConfig();
        loadChatMuteStateFromConfig();
        messages.reload();
        langManager.reload();
        reportManager.reload();
        noteManager.reload();
        punishmentManager.reload();
        staffLogManager.reload();
        firstJoinManager.reload();
        freezeManager.reload();
        xrayAlertManager.reload();
        staffManager.reload();
        staffHudManager.reload();
        updateChecker.reload();
        vanishManager.resyncAllOnlineVisibility();
    }

    private void registerCommands() {
        register("staff", new StaffCommand(this));
        register("staffpanel", new StaffPanelCommand(this));
        register("vanish", new VanishCommand(this));
        register("freeze", new FreezeCommand(this));
        register("staffchat", new StaffChatCommand(this));
        register("chatmute", new ChatMuteCommand(this));
        register("helpop", new HelpOpCommand(this));
        register("report", new ReportCommand(this));
        register("reports", new ReportsCommand(this));
        register("notes", new NotesCommand(this));
        register("warn", new WarnCommand(this));
        register("mute", new MuteCommand(this));
        register("unmute", new UnmuteCommand(this));
        register("sckick", new StaffKickCommand(this));
        register("scban", new StaffBanCommand(this));
        register("scbanip", new StaffBanIpCommand(this));
        register("sctempban", new TempBanCommand(this));
        register("sctempbanip", new TempBanIpCommand(this));
        register("scunban", new UnbanCommand(this));
        register("scunbanip", new UnbanIpCommand(this));
        register("history", new HistoryCommand(this));
        register("stafflogs", new StaffLogsCommand(this));
        register("stafflang", new StaffLangCommand(this));
        register("xrayalerts", new XrayAlertsCommand(this));
    }

    private void registerDomainListeners() {
        ListenerSupport listenerSupport = new ListenerSupport(this);
        Bukkit.getPluginManager().registerEvents(new PlayerSessionListener(this, listenerSupport), this);
        Bukkit.getPluginManager().registerEvents(new CommandVisibilityListener(this, listenerSupport), this);
        Bukkit.getPluginManager().registerEvents(new FreezeListener(this, listenerSupport), this);
        Bukkit.getPluginManager().registerEvents(new ChatModerationListener(this, listenerSupport), this);
        Bukkit.getPluginManager().registerEvents(new StaffModeListener(this, listenerSupport), this);
        Bukkit.getPluginManager().registerEvents(new CombatProtectionListener(this, listenerSupport), this);
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }

    public Messages messages() {
        return messages;
    }

    public LangManager langManager() {
        return langManager;
    }

    public VanishManager vanishManager() {
        return vanishManager;
    }

    public FreezeManager freezeManager() {
        return freezeManager;
    }

    public StaffChatManager staffChatManager() {
        return staffChatManager;
    }

    public StaffManager staffManager() {
        return staffManager;
    }

    public ReportManager reportManager() {
        return reportManager;
    }

    public XrayAlertManager xrayAlertManager() {
        return xrayAlertManager;
    }

    public StaffLogManager staffLogManager() {
        return staffLogManager;
    }

    public NoteManager noteManager() {
        return noteManager;
    }

    public PunishmentManager punishmentManager() {
        return punishmentManager;
    }

    public FirstJoinManager firstJoinManager() {
        return firstJoinManager;
    }

    public MenuManager menuManager() {
        return menuManager;
    }

    public StaffHudManager staffHudManager() {
        return staffHudManager;
    }

    public UpdateChecker updateChecker() {
        return updateChecker;
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    public void setChatMuted(boolean muted) {
        this.chatMuted = muted;
        getConfig().set("chat-moderation.chat-muted", muted);
        saveConfig();
    }

    public String uiString(String path, String fallback) {
        if (uiConfig != null && uiConfig.contains(path)) {
            return uiConfig.getString(path, fallback);
        }
        return getConfig().getString(path, fallback);
    }

    public List<String> uiStringList(String path) {
        if (uiConfig != null && uiConfig.contains(path)) {
            return uiConfig.getStringList(path);
        }
        return getConfig().getStringList(path);
    }

    public NamespacedKey key(String value) {
        return new NamespacedKey(this, value);
    }

    public Inventory createInspectionInventory(Player viewer, Player target) {
        String title = messages.resolve(viewer, "menu-title-inspect-inventory", java.util.Map.of("player", target.getName()));
        return createInspectionInventoryInternal(target, title);
    }

    public Inventory createInspectionInventory(Player target) {
        return createInspectionInventoryInternal(target, Messages.color("&8Inspecting " + target.getName()));
    }

    public Inventory createEnderInspectionInventory(Player viewer, Player target) {
        String title = messages.resolve(viewer, "menu-title-inspect-ender", java.util.Map.of("player", target.getName()));
        return createEnderInspectionInventoryInternal(target, title);
    }

    public Inventory createEnderInspectionInventory(Player target) {
        return createEnderInspectionInventoryInternal(target, Messages.color("&8EnderChest " + target.getName()));
    }

    public String healthSummary() {
        if (storageModeSelection == null) {
            storageModeSelection = StorageModeResolver.resolve(getConfig());
        }
        File data = getDataFolder();
        File notes = new File(data, "notes.yml");
        File firstJoins = new File(data, "first-joins.yml");
        File reports = new File(data, "reports.yml");
        File punishments = new File(data, "punishments.yml");
        File logs = new File(data, "staff-logs.yml");
        File frozen = new File(data, "frozen.yml");
        String asyncSaveMode = getConfig().getBoolean("storage.async-save.enabled", true) ? "enabled" : "disabled";
        long asyncSaveFlushSeconds = Math.max(1L, getConfig().getLong("storage.async-save.flush-interval-seconds", 5L));
        boolean crashSafeEnabled = getConfig().getBoolean("storage.crash-safe.enabled", true);
        boolean crashSafeForceSync = getConfig().getBoolean("storage.crash-safe.force-file-sync", false);
        boolean crashSafeBackup = getConfig().getBoolean("storage.crash-safe.keep-backup", true);
        boolean crashSafeRecover = getConfig().getBoolean("storage.crash-safe.recover-temp-on-load", true);
        boolean hudEnabled = getConfig().getBoolean("staff-hud.enabled", true);
        long hudInterval = Math.max(10L, getConfig().getLong("staff-hud.update-interval-ticks", 20L));
        return String.join("\n",
            "Data folder: " + data.getAbsolutePath(),
            "notes.yml: " + existsState(notes),
            "first-joins.yml: " + existsState(firstJoins),
            "reports.yml: " + existsState(reports),
            "punishments.yml: " + existsState(punishments),
            "staff-logs.yml: " + existsState(logs),
            "frozen.yml: " + existsState(frozen),
            "server tps (1m/5m/15m): " + formatTps(),
            "staff hud: " + (hudEnabled ? "enabled" : "disabled") + " (" + hudInterval + " ticks)",
            "chat muted: " + (chatMuted ? "yes" : "no"),
            "storage mode: requested=" + storageModeSelection.requested().configName()
                + ", active=" + storageModeSelection.active().configName()
                + ", note=" + storageModeSelection.note(),
            "async save: " + asyncSaveMode + " (flush every " + asyncSaveFlushSeconds + "s)",
            "crash-safe save: " + (crashSafeEnabled ? "enabled" : "disabled")
                + " (force-sync=" + crashSafeForceSync
                + ", backup=" + crashSafeBackup
                + ", recover-temp=" + crashSafeRecover + ")",
            "notes state: total=" + noteManager.totalNotes()
                + ", queued-save=" + noteManager.saveQueued()
                + ", async-write=" + noteManager.asyncWriteInProgress(),
            "first-join state: seen=" + firstJoinManager.seenPlayersCount()
                + ", queued-save=" + firstJoinManager.saveQueued()
                + ", async-write=" + firstJoinManager.asyncWriteInProgress(),
            "report state: total=" + reportManager.totalReports()
                + ", open=" + reportManager.openReportsCount()
                + ", cooldown-map=" + reportManager.cooldownTrackedPlayers()
                + ", reject-prompts=" + reportManager.pendingRejectFlows()
                + ", queued-save=" + reportManager.saveQueued()
                + ", async-write=" + reportManager.asyncWriteInProgress(),
            "punishment state: total=" + punishmentManager.totalPunishments()
                + ", active=" + punishmentManager.activePunishmentsCount()
                + ", queued-save=" + punishmentManager.saveQueued()
                + ", async-write=" + punishmentManager.asyncWriteInProgress(),
            "staff logs state: total=" + staffLogManager.totalLogs()
                + ", queued-save=" + staffLogManager.saveQueued()
                + ", async-write=" + staffLogManager.asyncWriteInProgress(),
            "loaded languages: " + langManager.availableLanguages().size(),
            "online staff mode: " + staffManager.staffPlayers().size(),
            "vanished online: " + vanishManager.vanishedPlayers().stream().filter(uuid -> Bukkit.getPlayer(uuid) != null).count(),
            "frozen tracked: " + freezeManager.frozenPlayers().size(),
            "freeze chat links: " + freezeManager.activeSessionCount(),
            "xray runtime enabled: " + xrayAlertManager.isRuntimeEnabled(),
            "xray tracked samples: " + xrayAlertManager.trackedPlayersCount(),
            "xray warmup tracked: " + xrayAlertManager.warmupTrackedPlayersCount(),
            "xray muted alert-staff: " + xrayAlertManager.mutedAlertsCount(),
            "xray whitelists: worlds=" + xrayAlertManager.whitelistedWorldsCount()
                + ", biomes=" + xrayAlertManager.whitelistedBiomesCount()
                + ", mines=" + xrayAlertManager.whitelistedMinesCount(),
            "xray checks: total=" + xrayAlertManager.totalChecks()
                + ", avg-check=" + String.format(Locale.US, "%.2f", xrayAlertManager.averageCheckMicros()) + "us",
            "update checker: " + (updateChecker.isEnabled() ? "enabled" : "disabled")
                + ", checked=" + updateChecker.isChecked()
                + ", update-available=" + updateChecker.hasUpdate()
                + ", latest=" + (updateChecker.latestVersion().isBlank() ? "n/a" : updateChecker.latestVersion())
        );
    }

    public boolean featureEnabled(String key) {
        return getConfig().getBoolean("features." + key, true);
    }

    private String existsState(File file) {
        return file.exists() ? "OK (" + file.length() + " bytes)" : "MISSING";
    }

    private String formatTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            if (tps == null || tps.length < 3) {
                return "n/a";
            }
            return String.format(Locale.US, "%.2f / %.2f / %.2f", tps[0], tps[1], tps[2]);
        } catch (Throwable ignored) {
            return "n/a";
        }
    }

    private void createSafetyBackup() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return;
        }
        File backupDir = new File(dataFolder, "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return;
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        copyIfExists(new File(dataFolder, "notes.yml"), new File(backupDir, "notes-" + stamp + ".yml"));
        copyIfExists(new File(dataFolder, "first-joins.yml"), new File(backupDir, "first-joins-" + stamp + ".yml"));
        copyIfExists(new File(dataFolder, "reports.yml"), new File(backupDir, "reports-" + stamp + ".yml"));
        copyIfExists(new File(dataFolder, "punishments.yml"), new File(backupDir, "punishments-" + stamp + ".yml"));
        copyIfExists(new File(dataFolder, "staff-logs.yml"), new File(backupDir, "staff-logs-" + stamp + ".yml"));
        copyIfExists(new File(dataFolder, "frozen.yml"), new File(backupDir, "frozen-" + stamp + ".yml"));
    }

    private void copyIfExists(File source, File target) {
        if (!source.exists()) {
            return;
        }
        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            getLogger().warning("Backup failed for " + source.getName() + ": " + exception.getMessage());
        }
    }

    private void ensureWikiFiles() {
        syncBundledResource("WIKI_ES.md", "wiki/WIKI_ES.md");
        syncBundledResource("WIKI_EN.md", "wiki/WIKI_EN.md");
        removeLegacyWikiAtRoot("WIKI_ES.md", "wiki/WIKI_ES.md");
        removeLegacyWikiAtRoot("WIKI_EN.md", "wiki/WIKI_EN.md");
    }

    private void syncBundledResource(String resourcePath, String targetRelative) {
        File target = new File(getDataFolder(), targetRelative);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            getLogger().warning("Could not create wiki directory: " + parent.getAbsolutePath());
            return;
        }
        try (InputStream stream = getResource(resourcePath)) {
            if (stream == null) {
                getLogger().warning("Optional resource missing in jar: " + resourcePath);
                return;
            }
            Files.copy(stream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            getLogger().warning("Could not save wiki file " + targetRelative + ": " + exception.getMessage());
        }
    }

    private void removeLegacyWikiAtRoot(String legacyName, String canonicalRelative) {
        File legacy = new File(getDataFolder(), legacyName);
        File canonical = new File(getDataFolder(), canonicalRelative);
        if (!legacy.exists() || !canonical.exists()) {
            return;
        }
        try {
            Files.deleteIfExists(legacy.toPath());
        } catch (IOException exception) {
            getLogger().warning("Could not delete legacy wiki file " + legacyName + ": " + exception.getMessage());
        }
    }

    private void reloadUiConfig() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder for ui.yml");
            return;
        }
        if (uiFile == null) {
            uiFile = new File(getDataFolder(), "ui.yml");
        }
        if (!uiFile.exists()) {
            saveResource("ui.yml", false);
        }

        uiConfig = YamlConfiguration.loadConfiguration(uiFile);
        YamlConfiguration bundled = loadBundledYaml("ui.yml");
        if (bundled == null) {
            return;
        }
        boolean normalized = normalizeLegacyUi(uiConfig, bundled);
        boolean merged = mergeMissing(uiConfig, bundled, "");
        if (!normalized && !merged) {
            return;
        }
        try {
            uiConfig.save(uiFile);
        } catch (IOException exception) {
            getLogger().warning("Could not update ui.yml with new defaults: " + exception.getMessage());
        }
    }

    private YamlConfiguration loadBundledYaml(String resourcePath) {
        try (InputStream stream = getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            getLogger().warning("Could not read bundled file " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }

    private void syncConfigDefaults() {
        YamlConfiguration bundled = loadBundledYaml("config.yml");
        if (bundled == null) {
            return;
        }
        boolean changed = mergeMissing(getConfig(), bundled, "");
        if (!changed) {
            return;
        }
        saveConfig();
    }

    private boolean mergeMissing(ConfigurationSection current, YamlConfiguration bundled, String path) {
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
            }
        }
        return changed;
    }

    private boolean normalizeLegacyUi(YamlConfiguration current, YamlConfiguration bundled) {
        boolean changed = false;
        List<String> template = current.getStringList("helpop.template-lines");
        boolean hasLegacyHelpop = template.stream()
            .map(line -> line.toLowerCase(Locale.ROOT))
            .anyMatch(line -> line.contains("(helpop)") || line.contains("solicitud de ayuda (helpop)"));
        boolean hasOldCompactHelpop = template.size() == 6
            && template.get(1).toLowerCase(Locale.ROOT).contains("solicitud de ayuda")
            && template.get(4).toLowerCase(Locale.ROOT).contains("mensaje:")
            && template.get(4).contains("{message_compact}");
        if (hasLegacyHelpop || hasOldCompactHelpop) {
            current.set("helpop.template-lines", bundled.getStringList("helpop.template-lines"));
            changed = true;
        }

        String title = current.getString("helpop.alert-title", "");
        if (title != null && title.toLowerCase(Locale.ROOT).contains("(helpop)")) {
            current.set("helpop.alert-title", bundled.getString("helpop.alert-title", "&c&lHELPOP"));
            changed = true;
        }
        return changed;
    }

    private void loadChatMuteStateFromConfig() {
        chatMuted = getConfig().getBoolean("chat-moderation.chat-muted", false);
    }

    private void resolveStorageMode() {
        storageModeSelection = StorageModeResolver.resolve(getConfig());
        if (storageModeSelection.fallbackInUse()) {
            getLogger().warning("Storage mode '" + storageModeSelection.requested().configName()
                + "' is not active yet. Falling back to '" + storageModeSelection.active().configName() + "'.");
        }
    }

    private Inventory createInspectionInventoryInternal(Player target, String title) {
        InventoryTagHolder holder = new InventoryTagHolder(InventoryTagHolder.INSPECT);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.bind(inventory);
        ItemStack[] contents = target.getInventory().getContents();
        for (int slot = 0; slot < Math.min(36, contents.length); slot++) {
            inventory.setItem(slot, contents[slot]);
        }

        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int slot = 0; slot < armor.length; slot++) {
            inventory.setItem(45 + slot, armor[slot]);
        }
        inventory.setItem(53, target.getInventory().getItemInOffHand());
        return inventory;
    }

    private Inventory createEnderInspectionInventoryInternal(Player target, String title) {
        InventoryTagHolder holder = new InventoryTagHolder(InventoryTagHolder.ENDER);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.bind(inventory);
        ItemStack[] contents = target.getEnderChest().getContents();
        for (int slot = 0; slot < Math.min(inventory.getSize(), contents.length); slot++) {
            inventory.setItem(slot, contents[slot]);
        }
        return inventory;
    }

}
