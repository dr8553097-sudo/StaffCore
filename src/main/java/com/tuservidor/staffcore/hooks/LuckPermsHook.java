package com.tuservidor.staffcore.hooks;

import com.tuservidor.staffcore.StaffCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public final class LuckPermsHook {

    private final StaffCore plugin;
    private boolean hooked = false;

    public LuckPermsHook(StaffCore plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            init();
        }
    }

    private void init() {
        try {
            LuckPermsHandler.register(plugin);
            this.hooked = true;
            plugin.getLogger().info("[StaffCore] Successfully hooked into LuckPerms dynamic context system.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[StaffCore] Could not hook into LuckPerms context calculator: " + t.getMessage());
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    private static final class LuckPermsHandler {
        private static void register(StaffCore plugin) {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            api.getContextManager().registerCalculator(new net.luckperms.api.context.ContextCalculator<Player>() {
                @Override
                public void calculate(Player target, net.luckperms.api.context.ContextConsumer consumer) {
                    if (target != null && target.isOnline()) {
                        boolean inStaffMode = plugin.staffManager().isStaff(target);
                        boolean isVanished = plugin.vanishManager().isVanished(target);
                        boolean onDuty = plugin.staffDutyManager().isOnDuty(target);

                        consumer.accept("staffcore:staffmode", String.valueOf(inStaffMode));
                        consumer.accept("staffcore:vanished", String.valueOf(isVanished));
                        consumer.accept("staffcore:duty", String.valueOf(onDuty));
                    }
                }

                @Override
                public net.luckperms.api.context.ContextSet estimatePotentialContexts() {
                    net.luckperms.api.context.ImmutableContextSet.Builder builder = net.luckperms.api.context.ImmutableContextSet.builder();
                    builder.add("staffcore:staffmode", "true");
                    builder.add("staffcore:staffmode", "false");
                    builder.add("staffcore:vanished", "true");
                    builder.add("staffcore:vanished", "false");
                    builder.add("staffcore:duty", "true");
                    builder.add("staffcore:duty", "false");
                    return builder.build();
                }
            });
        }
    }
}

