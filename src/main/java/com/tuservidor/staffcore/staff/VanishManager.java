package com.tuservidor.staffcore.staff;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VanishManager {

    private final StaffCore plugin;
    private final Messages messages;
    private final Set<UUID> vanished = new HashSet<>();
    private final Map<UUID, BukkitTask> pendingRevealTasks = new HashMap<>();

    public VanishManager(StaffCore plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void toggle(Player player) {
        if (isVanished(player)) {
            unvanish(player);
            return;
        }
        vanish(player);
    }

    public void vanish(Player player) {
        cancelPendingReveal(player.getUniqueId());
        if (!vanished.add(player.getUniqueId())) {
            return;
        }

        player.setCollidable(plugin.getConfig().getBoolean("vanish.disable-collision", true) ? false : player.isCollidable());
        if (plugin.getConfig().getBoolean("vanish.disable-item-pickup", true)) {
            player.setCanPickupItems(false);
        }
        clearMobTargets(player, plugin.getConfig().getDouble("vanish.clear-mob-target-radius", 48.0));
        resyncAllOnlineVisibility();
        // Some clients update world/tab one tick later.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isVanished(player)) {
                resyncAllOnlineVisibility();
            }
        }, 1L);
        plugin.staffManager().refreshToolbar(player);
        plugin.staffLogManager().log(player.getName(), "VANISH", player.getName(), "Enabled");
        messages.send(player, "vanish-enabled");
    }

    public void unvanish(Player player) {
        boolean wasVanished = vanished.remove(player.getUniqueId());
        boolean keepPickupBlockedByStaffMode = plugin.staffManager().isStaff(player)
            && plugin.getConfig().getBoolean("staff-mode.prevent-item-pickup", true);

        if (plugin.getConfig().getBoolean("vanish.disable-collision", true)) {
            player.setCollidable(true);
        }
        if (plugin.getConfig().getBoolean("vanish.disable-item-pickup", true) || keepPickupBlockedByStaffMode) {
            player.setCanPickupItems(!keepPickupBlockedByStaffMode);
        }
        cancelPendingReveal(player.getUniqueId());
        resyncAllOnlineVisibility();
        // A second refresh on next tick improves tab-list recovery on some clients,
        // but must not run if player re-enters vanish immediately.
        if (plugin.isEnabled()) {
            BukkitTask revealTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingRevealTasks.remove(player.getUniqueId());
                if (!player.isOnline()) {
                    return;
                }
                if (isVanished(player)) {
                    resyncAllOnlineVisibility();
                    return;
                }
                resyncAllOnlineVisibility();
            }, 1L);
            pendingRevealTasks.put(player.getUniqueId(), revealTask);
        }
        plugin.staffManager().refreshToolbar(player);
        if (wasVanished) {
            plugin.staffLogManager().log(player.getName(), "VANISH", player.getName(), "Disabled");
            messages.send(player, "vanish-disabled");
        }
    }

    public void resyncAllOnlineVisibility() {
        for (Player target : Bukkit.getOnlinePlayers()) {
            boolean targetVanished = isVanished(target);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) {
                    continue;
                }
                if (targetVanished && !canSeeVanished(viewer)) {
                    viewer.hidePlayer(plugin, target);
                    safeUnlist(viewer, target);
                    continue;
                }
                viewer.showPlayer(plugin, target);
                safeList(viewer, target);
            }
        }
    }

    public void shutdown() {
        pendingRevealTasks.values().forEach(BukkitTask::cancel);
        pendingRevealTasks.clear();
    }

    public void handleJoin(Player joined) {
        if (joined == null || !joined.isOnline()) {
            return;
        }
        resyncAllOnlineVisibility();
    }

    public void refreshVisibility(Player player) {
        if (isVanished(player)) {
            resyncAllOnlineVisibility();
        }
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public Set<UUID> vanishedPlayers() {
        return Collections.unmodifiableSet(vanished);
    }

    private void cancelPendingReveal(UUID uuid) {
        BukkitTask task = pendingRevealTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void clearMobTargets(Player player, double radius) {
        double yRadius = Math.max(16.0, radius * 0.75);
        for (Entity entity : player.getNearbyEntities(radius, yRadius, radius)) {
            if (entity instanceof Mob mob && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
    }

    private void safeUnlist(Player viewer, Player target) {
        try {
            viewer.unlistPlayer(target);
        } catch (Throwable ignored) {
            // Keep vanish robust even if another plugin intercepts tab operations.
        }
    }

    private void safeList(Player viewer, Player target) {
        try {
            viewer.listPlayer(target);
        } catch (Throwable ignored) {
            // Keep unvanish robust even if listing is blocked momentarily.
        }
    }

    private boolean canSeeVanished(Player viewer) {
        if (plugin.getConfig().getBoolean("vanish.strict-hide-for-all", false)) {
            return false;
        }
        return Permissions.has(viewer, "staffcore.vanish.see");
    }

}
