package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public final class StaffLangCommand implements CommandExecutor {

    private final StaffCore plugin;

    public StaffLangCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(player, "staffcore.lang")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(player, "lang-usage");
            return true;
        }

        String language = args[0].toLowerCase(Locale.ROOT);
        if (!plugin.langManager().setPlayerLanguage(player.getUniqueId(), language)) {
            plugin.messages().send(player, "lang-not-found", Map.of("lang", language));
            return true;
        }

        boolean updateDefault = plugin.getConfig().getBoolean("language.stafflang-updates-default", true);
        if (updateDefault) {
            plugin.getConfig().set("language.default", language);
            plugin.saveConfig();
            plugin.langManager().reload();
            plugin.messages().reload();
            plugin.messages().send(player, "lang-default-updated", Map.of("lang", language));
        }

        plugin.messages().send(player, "lang-updated", Map.of("lang", language));
        return true;
    }
}
