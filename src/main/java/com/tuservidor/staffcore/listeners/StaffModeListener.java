package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.gui.InventoryTagHolder;
import com.tuservidor.staffcore.util.ModerationGuard;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.inventory.ItemStack;
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
            && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (!support.canRunToolAction(player.getUniqueId() + ":TELEPORTER")) {
                return;
            }
            event.setCancelled(true);
            Player target = support.teleportTarget(player);
            if (target != null) {
                player.teleport(target.getLocation());
                plugin.messages().send(player, "teleported", Map.of("player", target.getName()));
                plugin.staffLogManager().log(player.getName(), "TELEPORT", target.getName(), "Teleported via teleporter tool");
                return;
            }
            int maxDistance = Math.max(10, plugin.getConfig().getInt("staff-tools.teleporter.max-distance", 120));
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(maxDistance, org.bukkit.FluidCollisionMode.NEVER);
            if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                Location loc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(player.getLocation().getPitch());
                player.teleport(loc);
                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                return;
            }
            org.bukkit.util.Vector dir = player.getLocation().getDirection().normalize().multiply(Math.min(maxDistance, 25));
            Location forwardLoc = player.getLocation().add(dir);
            forwardLoc.setYaw(player.getLocation().getYaw());
            forwardLoc.setPitch(player.getLocation().getPitch());
            player.teleport(forwardLoc);
            player.playSound(forwardLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
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
            toggleSpectatorMode(player);
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
        String mainTool = support.toolName(event.getMainHandItem());
        String offTool = support.toolName(event.getOffHandItem());
        if (mainTool != null || offTool != null) {
            event.setCancelled(true);
            if ("SPECTATOR".equals(mainTool) || "SPECTATOR".equals(offTool)) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    toggleSpectatorMode(player);
                }
            }
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

        if (plugin.staffManager().isStaff(player)) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                clickedItem = event.getCursor();
            }
            String tool = support.toolName(clickedItem);
            if (tool != null) {
                event.setCancelled(true);
                handleToolAction(player, tool);
            }
        }
    }

    private void handleToolAction(Player player, String tool) {
        if (!support.canRunToolAction(player.getUniqueId() + ":" + tool)) {
            return;
        }
        switch (tool) {
            case "TELEPORTER" -> {
                player.closeInventory();
                plugin.menuManager().openPlayerList(player);
            }
            case "SPECTATOR" -> {
                toggleSpectatorMode(player);
                player.closeInventory();
            }
            case "VANISH" -> plugin.vanishManager().toggle(player);
            case "EXIT" -> {
                player.closeInventory();
                plugin.staffManager().disable(player);
            }
            case "NIGHT_VISION" -> {
                if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    plugin.messages().send(player, "staff-nightvision-disabled");
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, true));
                    plugin.messages().send(player, "staff-nightvision-enabled");
                }
            }
            case "PANEL" -> {
                player.closeInventory();
                plugin.menuManager().openMain(player);
            }
            default -> {
            }
        }
    }

    private void toggleSpectatorMode(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(plugin.staffManager().staffModeGameMode());
            player.setAllowFlight(true);
            player.setFlying(true);
            plugin.messages().send(player, "staff-spectator-disabled");
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            plugin.messages().send(player, "staff-spectator-enabled");
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
