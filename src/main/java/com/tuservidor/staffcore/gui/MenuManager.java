package com.tuservidor.staffcore.gui;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.NoteEntry;
import com.tuservidor.staffcore.reports.Report;
import com.tuservidor.staffcore.util.ItemBuilder;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MenuManager {

    private static final String ACTION_KEY = "staffcore-menu-action";
    private static final String DATA_KEY = "staffcore-menu-data";

    private final StaffCore plugin;

    public MenuManager(StaffCore plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player viewer) {
        Inventory inv = menu(viewer, 45, "menu-title-main");
        fill(inv, material("menu.style.fillers.main", Material.GRAY_STAINED_GLASS_PANE));

        inv.setItem(10, actionItem(
            material("menu.style.icons.open-players", Material.COMPASS),
            tr(viewer, "menu-item-online-players-name"),
            "OPEN_PLAYERS",
            null,
            tr(viewer, "menu-item-online-players-lore")
        ));
        inv.setItem(12, actionItem(
            material("menu.style.icons.open-reports", Material.BOOK),
            tr(viewer, "menu-item-open-reports-name"),
            "OPEN_REPORTS",
            null,
            tr(viewer, "menu-item-open-reports-lore")
        ));
        inv.setItem(14, actionItem(
            material("menu.style.icons.open-frozen", Material.PACKED_ICE),
            tr(viewer, "menu-item-frozen-players-name"),
            "OPEN_FROZEN",
            null,
            tr(viewer, "menu-item-frozen-players-lore")
        ));
        inv.setItem(16, actionItem(
            material("menu.style.icons.open-staff", Material.PLAYER_HEAD),
            tr(viewer, "menu-item-staff-online-name"),
            "OPEN_STAFF",
            null,
            tr(viewer, "menu-item-staff-online-lore")
        ));

        boolean vanished = plugin.vanishManager().isVanished(viewer);
        inv.setItem(30, actionItem(
            vanished
                ? material("menu.style.icons.vanish-on", Material.LIME_DYE)
                : material("menu.style.icons.vanish-off", Material.GRAY_DYE),
            vanished ? tr(viewer, "menu-item-vanish-enabled-name") : tr(viewer, "menu-item-vanish-disabled-name"),
            "TOGGLE_VANISH",
            null,
            tr(viewer, "menu-item-vanish-lore")
        ));
        inv.setItem(32, actionItem(
            material("menu.style.icons.open-logs", Material.CLOCK),
            tr(viewer, "menu-item-staff-logs-name"),
            "OPEN_LOGS",
            null,
            tr(viewer, "menu-item-staff-logs-lore")
        ));
        inv.setItem(40, actionItem(
            material("menu.style.icons.close", Material.BARRIER),
            tr(viewer, "menu-item-close-name"),
            "CLOSE",
            null,
            tr(viewer, "menu-item-close-lore")
        ));

        viewer.openInventory(inv);
    }

    public void openPlayerList(Player viewer) {
        Inventory inv = menu(viewer, 54, "menu-title-players");
        fill(inv, material("menu.style.fillers.players", Material.BLACK_STAINED_GLASS_PANE));
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, playerCard(viewer, online, "OPEN_INSPECT"));
        }
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_MAIN",
            null,
            tr(viewer, "menu-item-back-main-lore")
        ));
        viewer.openInventory(inv);
    }

    public void openReports(Player viewer) {
        Inventory inv = menu(viewer, 54, "menu-title-reports");
        fill(inv, material("menu.style.fillers.reports", Material.BLUE_STAINED_GLASS_PANE));
        int slot = 0;
        for (Report report : plugin.reportManager().openReports()) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, reportItem(viewer, report));
        }
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_MAIN",
            null,
            tr(viewer, "menu-item-back-main-lore")
        ));
        viewer.openInventory(inv);
    }

    public void openReportReview(Player viewer, int reportId) {
        Report report = plugin.reportManager().byId(reportId).orElse(null);
        if (report == null || !report.isOpen()) {
            plugin.messages().send(viewer, "report-not-found", Map.of("id", String.valueOf(reportId)));
            openReports(viewer);
            return;
        }

        Inventory inv = menu(viewer, 27, "menu-title-report-review", Map.of("id", String.valueOf(report.id())));
        fill(inv, material("menu.style.fillers.reports", Material.BLUE_STAINED_GLASS_PANE));

        inv.setItem(11, actionItem(
            material("menu.style.icons.report-accept", Material.LIME_CONCRETE),
            tr(viewer, "menu-report-review-accept-name"),
            "REPORT_REVIEW_ACCEPT",
            String.valueOf(report.id()),
            tr(viewer, "menu-report-review-accept-lore")
        ));
        inv.setItem(13, actionItem(
            material("menu.style.icons.report-teleport", Material.ENDER_PEARL),
            tr(viewer, "menu-report-review-teleport-name"),
            "REPORT_REVIEW_TP",
            String.valueOf(report.id()),
            tr(viewer, "menu-report-review-teleport-lore")
        ));
        inv.setItem(15, actionItem(
            material("menu.style.icons.report-reject", Material.RED_CONCRETE),
            tr(viewer, "menu-report-review-reject-name"),
            "REPORT_REVIEW_REJECT",
            String.valueOf(report.id()),
            tr(viewer, "menu-report-review-reject-lore")
        ));
        inv.setItem(22, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "REPORT_REVIEW_BACK",
            null,
            tr(viewer, "menu-report-review-back-lore")
        ));

        inv.setItem(4, reportItem(viewer, report));
        viewer.openInventory(inv);
    }

    public void openFrozen(Player viewer) {
        Inventory inv = menu(viewer, 54, "menu-title-frozen");
        fill(inv, material("menu.style.fillers.frozen", Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        int slot = 0;
        for (UUID uuid : plugin.freezeManager().frozenPlayers()) {
            if (slot >= 45) {
                break;
            }
            OfflinePlayer frozen = Bukkit.getOfflinePlayer(uuid);
            if (frozen.isOnline()) {
                inv.setItem(slot++, frozenPlayerCard(viewer, frozen));
            } else {
                inv.setItem(slot++, offlinePlayerCard(viewer, frozen));
            }
        }
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_MAIN",
            null,
            tr(viewer, "menu-item-back-main-lore")
        ));
        viewer.openInventory(inv);
    }

    public void openStaffOnline(Player viewer) {
        Inventory inv = menu(viewer, 54, "menu-title-staff-online");
        fill(inv, material("menu.style.fillers.staff-online", Material.CYAN_STAINED_GLASS_PANE));
        int slot = 0;
        for (UUID uuid : plugin.staffManager().staffPlayers()) {
            Player staff = Bukkit.getPlayer(uuid);
            if (staff == null || slot >= 45) {
                continue;
            }
            inv.setItem(slot++, playerCard(viewer, staff, "OPEN_INSPECT"));
        }
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_MAIN",
            null,
            tr(viewer, "menu-item-back-main-lore")
        ));
        viewer.openInventory(inv);
    }

    public void openInspect(Player viewer, Player target) {
        Inventory inv = menu(viewer, 54, "menu-title-inspect", Map.of("player", target.getName()));
        fill(inv, material("menu.style.fillers.inspect", Material.GRAY_STAINED_GLASS_PANE));

        inv.setItem(13, new ItemBuilder(Material.PLAYER_HEAD)
            .name(tr(viewer, "menu-inspect-player-name", Map.of("player", target.getName())))
            .lore(List.of(
                tr(viewer, "menu-inspect-player-world", Map.of("world", target.getWorld().getName())),
                tr(viewer, "menu-inspect-player-gamemode", Map.of("gamemode", target.getGameMode().name())),
                tr(viewer, "menu-inspect-player-health", Map.of("health", String.valueOf(Math.round(target.getHealth() * 10.0D) / 10.0D))),
                tr(viewer, "menu-inspect-player-food", Map.of("food", String.valueOf(target.getFoodLevel()))),
                tr(viewer, "menu-inspect-player-ping", Map.of("ping", target.getPing() + "ms")),
                tr(viewer, "menu-inspect-player-coords", Map.of(
                    "x", String.valueOf(target.getLocation().getBlockX()),
                    "y", String.valueOf(target.getLocation().getBlockY()),
                    "z", String.valueOf(target.getLocation().getBlockZ())
                ))
            ))
            .build());

        String uuid = target.getUniqueId().toString();
        inv.setItem(19, actionItem(
            material("menu.style.icons.inspect-teleport", Material.ENDER_PEARL),
            tr(viewer, "menu-inspect-teleport-name"),
            "INSPECT_TELEPORT",
            uuid,
            tr(viewer, "menu-inspect-teleport-lore")
        ));
        inv.setItem(21, actionItem(
            material("menu.style.icons.inspect-freeze", Material.PACKED_ICE),
            tr(viewer, "menu-inspect-freeze-name"),
            "INSPECT_FREEZE",
            uuid,
            tr(viewer, "menu-inspect-freeze-lore")
        ));
        inv.setItem(23, actionItem(
            material("menu.style.icons.inspect-open-inv", Material.CHEST),
            tr(viewer, "menu-inspect-open-inv-name"),
            "INSPECT_OPEN_INV",
            uuid,
            tr(viewer, "menu-inspect-open-inv-lore")
        ));
        inv.setItem(25, actionItem(
            material("menu.style.icons.inspect-open-ender", Material.ENDER_CHEST),
            tr(viewer, "menu-inspect-open-ender-name"),
            "INSPECT_OPEN_ENDER",
            uuid,
            tr(viewer, "menu-inspect-open-ender-lore")
        ));
        inv.setItem(31, actionItem(
            material("menu.style.icons.inspect-history", Material.WRITABLE_BOOK),
            tr(viewer, "menu-inspect-history-name"),
            "INSPECT_NOTES",
            uuid,
            tr(viewer, "menu-inspect-history-lore")
        ));
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_PLAYERS",
            null,
            tr(viewer, "menu-item-back-players-lore")
        ));

        viewer.openInventory(inv);
    }

    public void openHistory(Player viewer, Player target) {
        Inventory inv = menu(viewer, 54, "menu-title-history", Map.of("player", target.getName()));
        fill(inv, material("menu.style.fillers.history", Material.BROWN_STAINED_GLASS_PANE));

        int slot = 0;
        for (NoteEntry note : plugin.noteManager().byTarget(target.getUniqueId(), 45)) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, new ItemBuilder(material("menu.style.icons.history-note", Material.WRITABLE_BOOK))
                .name(tr(viewer, "menu-history-note-name", Map.of("id", String.valueOf(note.id()))))
                .lore(List.of(
                    tr(viewer, "menu-history-note-staff", Map.of("staff", note.staffName())),
                    tr(viewer, "menu-history-note-text", Map.of("text", note.content()))
                ))
                .build());
        }

        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_INSPECT",
            target.getUniqueId().toString(),
            tr(viewer, "menu-item-back-inspect-lore")
        ));
        viewer.openInventory(inv);
    }

    public void openLogs(Player viewer) {
        Inventory inv = menu(viewer, 54, "menu-title-logs");
        fill(inv, material("menu.style.fillers.logs", Material.GREEN_STAINED_GLASS_PANE));
        int slot = 0;
        for (var entry : plugin.staffLogManager().recent(45)) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, new ItemBuilder(material("menu.style.icons.log-entry", Material.PAPER))
                .name(tr(viewer, "menu-logs-entry-name", Map.of("action", entry.action(), "id", String.valueOf(entry.id()))))
                .lore(List.of(
                    tr(viewer, "menu-logs-entry-staff", Map.of("staff", entry.staff())),
                    tr(viewer, "menu-logs-entry-target", Map.of("target", entry.target())),
                    tr(viewer, "menu-logs-entry-details", Map.of("details", entry.details()))
                ))
                .build());
        }
        inv.setItem(49, actionItem(
            material("menu.style.icons.back", Material.ARROW),
            tr(viewer, "menu-item-back-name"),
            "BACK_MAIN",
            null,
            tr(viewer, "menu-item-back-main-lore")
        ));
        viewer.openInventory(inv);
    }

    public boolean isMenu(InventoryClickEvent event) {
        return isMenuTitle(event.getView().getTitle());
    }

    public boolean isMenuTitle(String title) {
        return title.startsWith(titlePrefix());
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.get(plugin.key(ACTION_KEY), PersistentDataType.STRING);
        String data = pdc.get(plugin.key(DATA_KEY), PersistentDataType.STRING);
        if (action == null) {
            return;
        }

        switch (action) {
            case "OPEN_PLAYERS" -> openPlayerList(viewer);
            case "OPEN_REPORTS" -> {
                if (!plugin.featureEnabled("reports")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "reports"));
                    return;
                }
                if (Permissions.has(viewer, "staffcore.reports.view")) {
                    openReports(viewer);
                } else {
                    plugin.messages().send(viewer, "no-permission");
                }
            }
            case "OPEN_FROZEN" -> openFrozen(viewer);
            case "OPEN_STAFF" -> openStaffOnline(viewer);
            case "OPEN_LOGS" -> {
                if (Permissions.has(viewer, "staffcore.logs")) {
                    openLogs(viewer);
                } else {
                    plugin.messages().send(viewer, "no-permission");
                }
            }
            case "TOGGLE_VANISH" -> {
                if (!plugin.featureEnabled("vanish")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "vanish"));
                    return;
                }
                if (!Permissions.has(viewer, "staffcore.vanish")) {
                    plugin.messages().send(viewer, "no-permission");
                    return;
                }
                plugin.vanishManager().toggle(viewer);
                openMain(viewer);
            }
            case "CLOSE" -> viewer.closeInventory();
            case "BACK_MAIN" -> openMain(viewer);
            case "BACK_PLAYERS" -> openPlayerList(viewer);
            case "BACK_INSPECT" -> {
                Player target = resolveTarget(data);
                if (target != null) {
                    openInspect(viewer, target);
                } else {
                    openPlayerList(viewer);
                }
            }
            case "OPEN_INSPECT" -> {
                Player target = resolveTarget(data);
                if (target == null) {
                    plugin.messages().send(viewer, "player-not-found", Map.of("player", tr(viewer, "menu-offline-placeholder")));
                    return;
                }
                openInspect(viewer, target);
            }
            case "FROZEN_TOGGLE" -> {
                if (!plugin.featureEnabled("freeze")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "freeze"));
                    return;
                }
                if (!Permissions.has(viewer, "staffcore.freeze")) {
                    plugin.messages().send(viewer, "no-permission");
                    return;
                }
                Player target = resolveTarget(data);
                if (target == null) {
                    plugin.messages().send(viewer, "player-not-found", Map.of("player", tr(viewer, "menu-offline-placeholder")));
                    openFrozen(viewer);
                    return;
                }
                if (!ModerationGuard.canTarget(plugin, viewer, target)) {
                    plugin.messages().send(viewer, "target-protected");
                    return;
                }
                plugin.freezeManager().toggle(viewer, target, plugin.messages().resolve(viewer, "freeze-tool-default-reason"));
                openFrozen(viewer);
            }
            case "FROZEN_REMOVE_OFFLINE" -> {
                if (!plugin.featureEnabled("freeze")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "freeze"));
                    return;
                }
                if (!Permissions.has(viewer, "staffcore.freeze")) {
                    plugin.messages().send(viewer, "no-permission");
                    return;
                }
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(data);
                } catch (IllegalArgumentException exception) {
                    openFrozen(viewer);
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                plugin.freezeManager().remove(targetUuid);
                plugin.messages().send(viewer, "unfreeze-staff", Map.of(
                    "player", target.getName() == null ? targetUuid.toString() : target.getName()
                ));
                openFrozen(viewer);
            }
            case "REPORT_ACTION", "OPEN_REPORT_REVIEW" -> {
                if (!plugin.featureEnabled("reports")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "reports"));
                    return;
                }
                if (Permissions.has(viewer, "staffcore.reports.view")) {
                    Integer reportId = parseReportId(data);
                    if (reportId == null) {
                        return;
                    }
                    openReportReview(viewer, reportId);
                } else {
                    plugin.messages().send(viewer, "no-permission");
                }
            }
            case "REPORT_REVIEW_ACCEPT" -> {
                Integer reportId = parseReportId(data);
                if (reportId == null) {
                    return;
                }
                if (!plugin.reportManager().accept(reportId, viewer)) {
                    plugin.messages().send(viewer, "report-not-found", Map.of("id", String.valueOf(reportId)));
                }
                openReports(viewer);
            }
            case "REPORT_REVIEW_REJECT" -> {
                Integer reportId = parseReportId(data);
                if (reportId == null) {
                    return;
                }
                if (plugin.reportManager().beginRejectFlow(viewer, reportId)) {
                    viewer.closeInventory();
                } else {
                    openReports(viewer);
                }
            }
            case "REPORT_REVIEW_TP" -> {
                Integer reportId = parseReportId(data);
                if (reportId == null) {
                    return;
                }
                Report report = plugin.reportManager().byId(reportId).orElse(null);
                if (report == null || !report.isOpen()) {
                    plugin.messages().send(viewer, "report-not-found", Map.of("id", String.valueOf(reportId)));
                    openReports(viewer);
                    return;
                }
                Player target = Bukkit.getPlayer(report.target());
                if (target != null) {
                    viewer.teleport(target.getLocation());
                    plugin.messages().send(viewer, "teleported", Map.of("player", target.getName()));
                } else {
                    plugin.messages().send(viewer, "player-not-found", Map.of("player", report.targetName()));
                }
            }
            case "REPORT_REVIEW_BACK" -> openReports(viewer);
            case "INSPECT_TELEPORT" -> {
                Player target = resolveTarget(data);
                if (target == null) {
                    return;
                }
                viewer.teleport(target.getLocation());
                plugin.messages().send(viewer, "teleported", Map.of("player", target.getName()));
            }
            case "INSPECT_FREEZE" -> {
                if (!plugin.featureEnabled("freeze")) {
                    plugin.messages().send(viewer, "module-disabled", Map.of("module", "freeze"));
                    return;
                }
                if (!Permissions.has(viewer, "staffcore.freeze")) {
                    plugin.messages().send(viewer, "no-permission");
                    return;
                }
                Player target = resolveTarget(data);
                if (target == null) {
                    return;
                }
                if (!ModerationGuard.canTarget(plugin, viewer, target)) {
                    plugin.messages().send(viewer, "target-protected");
                    return;
                }
                plugin.freezeManager().toggle(viewer, target, plugin.messages().resolve(viewer, "freeze-tool-default-reason"));
                openInspect(viewer, target);
            }
            case "INSPECT_OPEN_INV" -> {
                Player target = resolveTarget(data);
                if (target == null) {
                    return;
                }
                viewer.openInventory(plugin.createInspectionInventory(viewer, target));
            }
            case "INSPECT_OPEN_ENDER" -> {
                Player target = resolveTarget(data);
                if (target == null) {
                    return;
                }
                viewer.openInventory(plugin.createEnderInspectionInventory(viewer, target));
            }
            case "INSPECT_HISTORY", "INSPECT_NOTES" -> {
                Player target = resolveTarget(data);
                if (target == null) {
                    return;
                }
                openHistory(viewer, target);
            }
            default -> {
            }
        }
    }

    private Integer parseReportId(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Integer.parseInt(data);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Player resolveTarget(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(data));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ItemStack reportItem(Player viewer, Report report) {
        List<String> lore = new ArrayList<>();
        lore.add(tr(viewer, "menu-report-reporter", Map.of("reporter", report.reporterName())));
        lore.add(tr(viewer, "menu-report-target", Map.of("target", report.targetName())));
        lore.add(tr(viewer, "menu-report-reason", Map.of("reason", report.reason())));
        lore.add(tr(viewer, "menu-report-divider"));
        lore.add(tr(viewer, "menu-report-action-open-review"));
        return actionItem(
            material("menu.style.icons.report-entry", Material.PAPER),
            tr(viewer, "menu-report-name", Map.of("id", String.valueOf(report.id()))),
            "OPEN_REPORT_REVIEW",
            String.valueOf(report.id()),
            lore.toArray(String[]::new)
        );
    }

    private ItemStack playerCard(Player viewer, OfflinePlayer player, String action) {
        String name = player.getName() == null ? tr(viewer, "menu-offline-placeholder") : player.getName();
        ItemBuilder builder = ItemBuilder.skull(player)
            .name(tr(viewer, "menu-player-card-name", Map.of("player", name)))
            .lore(List.of(
                tr(viewer, "menu-player-card-uuid", Map.of("uuid", shortUuid(player.getUniqueId()))),
                tr(viewer, "menu-player-card-status", Map.of(
                    "status", player.isOnline() ? tr(viewer, "menu-status-online") : tr(viewer, "menu-status-offline")
                )),
                tr(viewer, "menu-player-card-divider"),
                tr(viewer, "menu-player-card-click")
            ))
            .tag(ACTION_KEY, action)
            .tag(DATA_KEY, player.getUniqueId().toString());
        if (player.isOnline()) {
            builder.glow();
        }
        return builder.build();
    }

    private ItemStack offlinePlayerCard(Player viewer, OfflinePlayer player) {
        String name = player.getName() == null ? tr(viewer, "menu-offline-placeholder") : player.getName();
        return ItemBuilder.skull(player)
            .name(tr(viewer, "menu-offline-card-name", Map.of("player", name)))
            .lore(List.of(
                tr(viewer, "menu-player-card-uuid", Map.of("uuid", shortUuid(player.getUniqueId()))),
                tr(viewer, "menu-player-card-status", Map.of("status", tr(viewer, "menu-status-offline"))),
                tr(viewer, "menu-player-card-divider"),
                tr(viewer, "menu-offline-card-frozen")
            ))
            .tag(ACTION_KEY, "FROZEN_REMOVE_OFFLINE")
            .tag(DATA_KEY, player.getUniqueId().toString())
            .glow()
            .build();
    }

    private ItemStack frozenPlayerCard(Player viewer, OfflinePlayer player) {
        String name = player.getName() == null ? tr(viewer, "menu-offline-placeholder") : player.getName();
        ItemBuilder builder = ItemBuilder.skull(player)
            .name(tr(viewer, "menu-player-card-name", Map.of("player", name)))
            .lore(List.of(
                tr(viewer, "menu-player-card-uuid", Map.of("uuid", shortUuid(player.getUniqueId()))),
                tr(viewer, "menu-player-card-status", Map.of("status", tr(viewer, "menu-status-online"))),
                tr(viewer, "menu-player-card-divider"),
                tr(viewer, "menu-frozen-player-click")
            ))
            .tag(ACTION_KEY, "FROZEN_TOGGLE")
            .tag(DATA_KEY, player.getUniqueId().toString())
            .glow();
        return builder.build();
    }

    private String shortUuid(UUID uuid) {
        String raw = uuid.toString();
        return raw.substring(0, Math.min(8, raw.length())) + "...";
    }

    private ItemStack actionItem(Material material, String name, String action, String data, String... lore) {
        ItemBuilder builder = new ItemBuilder(material).name(name).lore(List.of(lore)).tag(ACTION_KEY, action);
        boolean glowActions = plugin.getConfig().getBoolean(stylePath("glow-actions"), true);
        boolean glowClose = plugin.getConfig().getBoolean(stylePath("glow-close"), false);
        boolean shouldGlow = glowActions && (!"CLOSE".equals(action) || glowClose);
        if (shouldGlow) {
            builder.glow();
        }
        if (data != null) {
            builder.tag(DATA_KEY, data);
        }
        return builder.build();
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = new ItemBuilder(material).name(tr("menu-filler-name")).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private Inventory menu(Player viewer, int size, String titleKey) {
        return menu(viewer, size, titleKey, Map.of());
    }

    private Inventory menu(Player viewer, int size, String titleKey, Map<String, String> placeholders) {
        return Bukkit.createInventory(null, size, titlePrefix() + tr(viewer, titleKey, placeholders));
    }

    private String titlePrefix() {
        String value = plugin.getConfig().getString(stylePath("title-prefix"), "&8StaffCore ");
        return Messages.color(value);
    }

    private String tr(Player viewer, String key) {
        return plugin.messages().resolve(viewer, key);
    }

    private String tr(Player viewer, String key, Map<String, String> placeholders) {
        return plugin.messages().resolve(viewer, key, placeholders);
    }

    private String tr(String key) {
        return plugin.messages().resolve(key);
    }

    private Material material(String path, Material fallback) {
        String raw = plugin.getConfig().getString(stylePath(path.replace("menu.style.", "")), fallback.name());
        Material parsed = Material.matchMaterial(raw == null ? fallback.name() : raw);
        return parsed == null ? fallback : parsed;
    }

    private String stylePath(String relative) {
        String template = plugin.getConfig().getString("menu.style.template", "custom");
        if (template == null || template.isBlank() || template.equalsIgnoreCase("custom")) {
            return "menu.style." + relative;
        }
        String presetPath = "menu.templates." + template + "." + relative;
        if (plugin.getConfig().contains(presetPath)) {
            return presetPath;
        }
        return "menu.style." + relative;
    }
}
