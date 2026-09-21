package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class DetectionListener implements Listener {

    private final StaffCore plugin;

    public DetectionListener(StaffCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.xrayHeuristicsManager() != null && plugin.xrayHeuristicsManager().isEnabled()) {
            plugin.xrayHeuristicsManager().recordBlockBreak(event.getPlayer(), event.getBlock().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (plugin.cpsTracker() != null && plugin.cpsTracker().isEnabled()) {
                plugin.cpsTracker().recordClick(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (plugin.cpsTracker() != null && plugin.cpsTracker().isEnabled()) {
                plugin.cpsTracker().recordClick(player);
            }
        }
    }
}
