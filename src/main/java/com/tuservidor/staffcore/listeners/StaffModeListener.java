package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.gui.InventoryTagHolder;
import com.tuservidor.staffcore.util.ModerationGuard;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public final class StaffModeListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public StaffModeListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler
    public void onToolUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        String tool = support.toolName(event.getItem());
        if (support.isVanishInteractionRestricted(player)
            && support.shouldBlockWorldInteraction(event.getAction())
            && tool == null) {
            event.setCancelled(true);
            support.sendVanishWorldBlocked(player);
            return;
        }
        if (!plugin.staffManager().isStaff(player)) {
            return;
        }
        if (plugin.getConfig().getBoolean("staff-mode.prevent-world-interactions", true)
            && support.shouldBlockWorldInteraction(event.getAction())
            && tool == null) {
            event.setCancelled(true);
            support.sendStaffWorldBlocked(player);
            return;
        }

        if (tool == null) {
            return;
        }

        if (tool.equals("TELEPORTER")
            && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (!support.canRunToolAction(player.getUniqueId() + ":TELEPORTER")) {
                return;
            }
            event.setCancelled(true);
            Player target = support.teleportTarget(player);
            if (target == null) {
                plugin.messages().send(player, "teleporter-no-target");
                return;
            }
            player.teleport(target.getLocation());
            plugin.messages().send(player, "teleported", Map.of("player", target.getName()));
            plugin.staffLogManager().log(player.getName(), "TELEPORT", target.getName(), "Teleported via teleporter tool");
            return;
        }

        if (tool.equals("VANISH")) {
            if (!support.canRunToolAction(player.getUniqueId() + ":VANISH")) {
                return;
            }
            event.setCancelled(true);
            plugin.vanishManager().toggle(player);
            return;
        }
        if (tool.equals("SPECTATOR")) {
            if (!support.canRunToolAction(player.getUniqueId() + ":SPECTATOR")) {
                return;
            }
            event.setCancelled(true);
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(plugin.staffManager().staffModeGameMode());
                player.setAllowFlight(true);
                player.setFlying(true);
                plugin.messages().send(player, "staff-spectator-disabled");
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                plugin.messages().send(player, "staff-spectator-enabled");
            }
            return;
        }
        if (tool.equals("NIGHT_VISION")) {
            if (!support.canRunToolAction(player.getUniqueId() + ":NIGHT_VISION")) {
                return;
            }
            event.setCancelled(true);
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                plugin.messages().send(player, "staff-nightvision-disabled");
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, true));
                plugin.messages().send(player, "staff-nightvision-enabled");
            }
            return;
        }
        if (tool.equals("EXIT")) {
            if (!support.canRunToolAction(player.getUniqueId() + ":EXIT")) {
                return;
            }
            event.setCancelled(true);
            plugin.staffManager().disable(player);
            return;
        }
        if (tool.equals("PANEL")) {
            if (!support.canRunToolAction(player.getUniqueId() + ":PANEL")) {
                return;
            }
            event.setCancelled(true);
            plugin.menuManager().openMain(player);
        }
    }

    @EventHandler
    public void onToolUseEntity(PlayerInteractEntityEvent event) {
        if (support.isFrozenRestricted(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        Player staff = event.getPlayer();
        if (!plugin.staffManager().isStaff(staff)) {
            return;
        }

        String tool = support.toolName(staff.getInventory().getItemInMainHand());
        if (tool == null) {
            return;
        }
        if (!support.canRunToolAction(staff.getUniqueId() + ":" + tool + ":" + target.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        if (tool.equals("TELEPORTER")) {
            staff.teleport(target.getLocation());
            plugin.messages().send(staff, "teleported", Map.of("player", target.getName()));
            plugin.staffLogManager().log(staff.getName(), "TELEPORT", target.getName(), "Teleported via staff tool");
            return;
        }
        if (tool.equals("FREEZE")) {
            if (!ModerationGuard.canTarget(plugin, staff, target)) {
                plugin.messages().send(staff, "target-protected");
                return;
            }
            plugin.freezeManager().toggle(staff, target, plugin.messages().resolve(staff, "freeze-tool-default-reason"));
            return;
        }
        if (tool.equals("INSPECTOR")) {
            staff.openInventory(plugin.createInspectionInventory(staff, target));
            plugin.messages().send(staff, "inspecting", Map.of("player", target.getName()));
            plugin.staffLogManager().log(staff.getName(), "INSPECT_INV", target.getName(), "Opened inventory inspection");
        }
    }

    @EventHandler
    public void onToolDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.staffManager().isStaff(player)) {
            return;
        }
        if (support.toolName(event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
            plugin.messages().send(player, "staff-tool-drop-blocked");
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.staffManager().isStaff(player)) {
            return;
        }
        if (support.toolName(event.getMainHandItem()) != null || support.toolName(event.getOffHandItem()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.menuManager().isMenu(event)) {
            plugin.menuManager().handleClick(event);
            return;
        }

        var holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof InventoryTagHolder tagHolder
            && (InventoryTagHolder.INSPECT.equals(tagHolder.tag()) || InventoryTagHolder.ENDER.equals(tagHolder.tag()))) {
            event.setCancelled(true);
            return;
        }

        if (plugin.staffManager().isStaff(player) && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            String tool = support.toolName(event.getCurrentItem());
            if (tool != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        String title = event.getView().getTitle();
        var holder = event.getView().getTopInventory().getHolder();
        if (plugin.menuManager().isMenuTitle(title)
            || (holder instanceof InventoryTagHolder tagHolder
            && (InventoryTagHolder.INSPECT.equals(tagHolder.tag()) || InventoryTagHolder.ENDER.equals(tagHolder.tag())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFrozenPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.staffManager().isStaff(player)
            && plugin.getConfig().getBoolean("staff-mode.prevent-item-pickup", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStaffBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (support.isVanishWorldModificationRestricted(player)) {
            event.setCancelled(true);
            support.sendVanishWorldBlocked(player);
            return;
        }
        if (!plugin.staffManager().isStaff(player)) {
            plugin.xrayAlertManager().handleBlockBreak(event);
            return;
        }
        if (!plugin.getConfig().getBoolean("staff-mode.prevent-world-modification", true)) {
            return;
        }
        event.setCancelled(true);
        support.sendStaffWorldBlocked(player);
    }

    @EventHandler
    public void onStaffBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (support.isVanishWorldModificationRestricted(player)) {
            event.setCancelled(true);
            support.sendVanishWorldBlocked(player);
            return;
        }
        if (!plugin.staffManager().isStaff(player)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("staff-mode.prevent-world-modification", true)) {
            return;
        }
        event.setCancelled(true);
        support.sendStaffWorldBlocked(player);
    }
}
