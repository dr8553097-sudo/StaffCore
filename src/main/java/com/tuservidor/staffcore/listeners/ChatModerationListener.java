package com.tuservidor.staffcore.listeners;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Permissions;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Map;

public final class ChatModerationListener implements Listener {

    private final StaffCore plugin;
    private final ListenerSupport support;

    public ChatModerationListener(StaffCore plugin, ListenerSupport support) {
        this.plugin = plugin;
        this.support = support;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffChatOrMute(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.featureEnabled("reports")
            && Permissions.has(player, "staffcore.reports.view")
            && plugin.reportManager().isAwaitingRejectReason(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.reportManager().handleRejectReasonInput(player, input));
            return;
        }

        if (support.shouldBlockByGlobalChatMute(player)) {
            event.setCancelled(true);
            support.sendChatMutedNotice(player);
            return;
        }

        if (plugin.featureEnabled("staff-chat")
            && !support.isFrozenRestricted(player)
            && Permissions.has(player, "staffcore.staffchat")
            && (event.getMessage().startsWith("@") || plugin.staffChatManager().isEnabled(player.getUniqueId()))) {
            event.setCancelled(true);
            String content = event.getMessage();
            if (content.startsWith("@")) {
                content = content.substring(1).trim();
            }
            if (content.isEmpty()) {
                return;
            }
            String consoleFormatted = plugin.messages().resolve("staffchat-format", Map.of(
                "player", player.getName(),
                "message", content
            ));
            Bukkit.getConsoleSender().sendMessage(consoleFormatted);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (Permissions.has(online, "staffcore.staffchat")) {
                    online.sendMessage(plugin.messages().resolve(online, "staffchat-format", Map.of(
                        "player", player.getName(),
                        "message", content
                    )));
                }
            }
            plugin.staffLogManager().log(player.getName(), "STAFF_CHAT", "staff-channel", content);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlobalChatMuteModern(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!support.shouldBlockByGlobalChatMute(player)) {
            return;
        }
        event.setCancelled(true);
        support.sendChatMutedNotice(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnsureStaffCanReadGlobalChatLegacy(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (plugin.staffChatManager().isEnabled(sender.getUniqueId()) || event.getMessage().startsWith("@")) {
            return;
        }

        String rendered;
        try {
            rendered = String.format(event.getFormat(), sender.getDisplayName(), event.getMessage());
        } catch (RuntimeException ignored) {
            rendered = sender.getDisplayName() + ": " + event.getMessage();
        }
        final String renderedMessage = rendered;

        List<? extends Player> mirrors = Bukkit.getOnlinePlayers().stream()
            .filter(viewer -> !viewer.equals(sender))
            .filter(viewer -> plugin.staffManager().isStaff(viewer) || plugin.vanishManager().isVanished(viewer))
            .filter(viewer -> !event.getRecipients().contains(viewer))
            .toList();
        if (mirrors.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player viewer : mirrors) {
                viewer.sendMessage(renderedMessage);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnsureStaffCanReadGlobalChatModern(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (plain.startsWith("@") || plugin.staffChatManager().isEnabled(sender.getUniqueId())) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(sender)) {
                continue;
            }
            if (!(plugin.staffManager().isStaff(viewer) || plugin.vanishManager().isVanished(viewer))) {
                continue;
            }
            event.viewers().add(viewer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrivateMessageMirror(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("staff-chat.spy-private-messages-while-staff-mode", true)) {
            return;
        }
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') {
            return;
        }
        String[] parts = raw.substring(1).trim().split("\\s+", 3);
        if (parts.length < 3) {
            return;
        }
        String command = support.normalizeCommand(parts[0]);
        if (!support.isPrivateMessageCommand(command)) {
            return;
        }
        Player sender = event.getPlayer();
        Player target = Bukkit.getPlayerExact(parts[1]);
        String content = parts[2].trim();
        if (target == null || target.equals(sender) || content.isBlank()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(sender)) {
                continue;
            }
            if (!(plugin.staffManager().isStaff(viewer) || plugin.vanishManager().isVanished(viewer))) {
                continue;
            }
            viewer.sendMessage(plugin.messages().resolve(viewer, "staff-private-message-mirror", Map.of(
                "from", sender.getName(),
                "to", target.getName(),
                "message", content
            )));
        }
    }
}
