package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public final class FreezeListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public FreezeListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.featureEnabled("freeze") || !plugin.freezeManager().isFrozen(player)) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getX() == event.getTo().getX()
            && event.getFrom().getY() == event.getTo().getY()
            && event.getFrom().getZ() == event.getTo().getZ()
            && event.getFrom().getYaw() == event.getTo().getYaw()
            && event.getFrom().getPitch() == event.getTo().getPitch()) {
            return;
        }
        event.setCancelled(true);
        event.setTo(event.getFrom());
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0f);
        player.setSprinting(false);
        if (player.getWalkSpeed() > 0.0f) {
            player.setWalkSpeed(0.0f);
        }
        if (player.getFlySpeed() > 0.0f) {
            player.setFlySpeed(0.0f);
        }
        if (player.isFlying()) {
            player.setFlying(false);
        }
        support.sendFreezeReminder(player);
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!support.isFrozenRestricted(player)) {
            return;
        }
        event.setCancelled(true);
        player.setFlying(false);
        player.setVelocity(new Vector(0, 0, 0));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!support.isFrozenRestricted(player)) {
            return;
        }
        if (!support.shouldBlockFrozenTeleport(event.getCause())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.featureEnabled("freeze") || !support.isFrozenRestricted(player)) {
            return;
        }
        String command = support.normalizeCommand(event.getMessage());
        if (support.isAllowedCommandWhileFrozen(command)) {
            return;
        }
        event.setCancelled(true);
        plugin.messages().send(player, "freeze-command-blocked");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFreezePrivateChat(AsyncPlayerChatEvent event) {
        if (support.tryRouteFreezePrivateChat(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleEnter(org.bukkit.event.vehicle.VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player && support.isFrozenRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleExit(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (event.getExited() instanceof Player player && support.isFrozenRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityToggleGlide(org.bukkit.event.entity.EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player && support.isFrozenRestricted(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRiptide(org.bukkit.event.player.PlayerRiptideEvent event) {
        if (support.isFrozenRestricted(event.getPlayer())) {
            event.getPlayer().setVelocity(new Vector(0, 0, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (support.isFrozenRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        if (support.isFrozenRestricted(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player && support.isFrozenRestricted(player)) {
            event.setCancelled(true);
        }
    }
}
