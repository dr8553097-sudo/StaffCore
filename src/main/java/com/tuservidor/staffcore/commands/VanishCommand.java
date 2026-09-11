package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class VanishCommand implements CommandExecutor {

    private final StaffCore plugin;

    public VanishCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(player, "staffcore.vanish")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        if (!plugin.featureEnabled("vanish")) {
            plugin.messages().send(player, "module-disabled", java.util.Map.of("module", "vanish"));
            return true;
        }
        plugin.vanishManager().toggle(player);
        // Extra safeguard: keep staff toolbar state synchronized when using command/alias.
        plugin.staffManager().refreshToolbar(player);
        return true;
    }
}
