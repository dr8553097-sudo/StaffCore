package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public final class CombatProtectionListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public CombatProtectionListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof Player player) || !(event.getEntity() instanceof Mob)) {
            return;
        }
        if (plugin.vanishManager().isVanished(player)) {
            event.setCancelled(true);
            return;
        }
        if (support.isFrozenRestricted(player)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.staffManager().isStaff(player)
            && plugin.getConfig().getBoolean("staff-mode.prevent-all-damage", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVanishCombat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player target && support.isFrozenRestricted(target)) {
            event.setCancelled(true);
            return;
        }

        Player freezeAttacker = support.damagerAsPlayer(event.getDamager());
        if (freezeAttacker != null && support.isFrozenRestricted(freezeAttacker)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getBoolean("staff-mode.prevent-combat", true)) {
            Player attacker = support.damagerAsPlayer(event.getDamager());
            if (attacker != null && plugin.staffManager().isStaff(attacker)) {
                event.setCancelled(true);
                plugin.messages().send(attacker, "staff-combat-blocked");
                return;
            }
            if (event.getEntity() instanceof Player targetPlayer && plugin.staffManager().isStaff(targetPlayer)) {
                if (support.isStaffDamageBypassedByOp(event)) {
                    return;
                }
                event.setCancelled(true);
                if (attacker != null && !plugin.staffManager().isStaff(attacker)) {
                    plugin.messages().send(attacker, "staff-combat-blocked");
                }
                return;
            }
        }

        if (!plugin.featureEnabled("vanish") || !plugin.getConfig().getBoolean("vanish.block-combat-while-vanish", true)) {
            return;
        }

        Player attacker = support.damagerAsPlayer(event.getDamager());
        Player target = event.getEntity() instanceof Player player ? player : null;

        if (attacker != null && plugin.vanishManager().isVanished(attacker)) {
            event.setCancelled(true);
            plugin.messages().send(attacker, "vanish-combat-blocked");
            return;
        }

        if (target != null && plugin.vanishManager().isVanished(target)) {
            event.setCancelled(true);
            if (attacker != null) {
                plugin.messages().send(attacker, "vanish-combat-blocked");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProtectedPlayerAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (support.isFrozenRestricted(target)) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.staffManager().isStaff(target)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("staff-mode.prevent-all-damage", true)) {
            return;
        }
        if (support.isStaffDamageBypassedByOp(event)) {
            return;
        }
        event.setCancelled(true);
    }
}
