package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public final class ChatMuteCommand implements CommandExecutor {

    private final StaffCore plugin;

    public ChatMuteCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasAccess(sender)) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            applyState(sender, !plugin.isChatMuted());
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on", "enable" -> {
                applyState(sender, true);
                return true;
            }
            case "off", "disable" -> {
                applyState(sender, false);
                return true;
            }
            case "status" -> {
                plugin.messages().send(sender, "chatmute-status", Map.of(
                    "state", plugin.messages().resolve(sender, plugin.isChatMuted() ? "chatmute-state-muted" : "chatmute-state-unmuted")
                ));
                return true;
            }
            default -> {
                plugin.messages().send(sender, "chatmute-usage");
                return true;
            }
        }
    }

    private void applyState(CommandSender sender, boolean muted) {
        if (plugin.isChatMuted() == muted) {
            plugin.messages().send(sender, muted ? "chatmute-already-muted" : "chatmute-already-unmuted");
            return;
        }

        plugin.setChatMuted(muted);
        plugin.messages().send(sender, muted ? "chatmute-enabled" : "chatmute-disabled");

        String actor = sender.getName();
        String broadcastKey = muted ? "chatmute-broadcast-enabled" : "chatmute-broadcast-disabled";
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(plugin.messages().resolve(online, broadcastKey, Map.of("staff", actor)));
        }
        Bukkit.getConsoleSender().sendMessage(plugin.messages().resolve(broadcastKey, Map.of("staff", actor)));

        plugin.staffLogManager().log(actor, muted ? "CHAT_MUTE_ON" : "CHAT_MUTE_OFF", "global-chat", muted ? "Global chat muted" : "Global chat unmuted");
    }

    private boolean hasAccess(CommandSender sender) {
        if (sender.isOp()) {
            return true;
        }
        return Permissions.has(sender, "staffcore.chatmute");
    }
}
