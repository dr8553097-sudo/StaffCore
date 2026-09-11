package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffPanelCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffPanelCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.hasAny(player, "staffcore.staff", "staffcore.use")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        plugin.menuManager().openMain(player);
        return true;
    }
}
