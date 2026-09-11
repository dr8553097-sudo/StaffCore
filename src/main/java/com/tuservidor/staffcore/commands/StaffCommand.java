package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StaffCommand implements CommandExecutor {

    private static final List<List<String>> HELP_PAGES = List.of(
        List.of(
            "staff-help-line-1",
            "staff-help-line-2",
            "staff-help-line-3",
            "staff-help-line-4",
            "staff-help-line-5",
            "staff-help-line-6"
        ),
        List.of(
            "staff-help-line-7",
            "staff-help-line-8",
            "staff-help-line-9",
            "staff-help-line-10",
            "staff-help-line-11",
            "staff-help-line-12"
        ),
        List.of(
            "staff-help-line-13",
            "staff-help-line-14",
            "staff-help-line-15",
            "staff-help-line-16",
            "staff-help-line-17",
            "staff-help-line-18"
        ),
        List.of(
            "staff-help-line-19",
            "staff-help-line-20",
            "staff-help-line-21",
            "staff-help-line-22",
            "staff-help-line-23",
            "staff-help-line-24",
            "staff-help-line-25",
            "staff-help-line-26"
        )
    );

    private final StaffCore plugin;

    public StaffCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("about"))) {
            sender.sendMessage(Messages.color("&8&m----------------------------------------"));
            sender.sendMessage(Messages.color("&bStaffCore &7| &fModern Moderation Suite"));
            sender.sendMessage(Messages.color("&7Version: &f" + plugin.getDescription().getVersion()));
            sender.sendMessage(Messages.color("&7Author: &f" + String.join(", ", plugin.getDescription().getAuthors())));
            sender.sendMessage(Messages.color("&7Paper API: &f" + org.bukkit.Bukkit.getBukkitVersion()));
            sender.sendMessage(Messages.color("&7Supported Version: &aYes (Native 1.21.x / 26.x)"));
            sender.sendMessage(Messages.color("&8&m----------------------------------------"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!Permissions.has(sender, "staffcore.reload")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            plugin.reloadPlugin();
            plugin.messages().send(sender, "reloaded");
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("health") || args[0].equalsIgnoreCase("debug"))) {
            if (!Permissions.has(sender, "staffcore.reload")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            sender.sendMessage(plugin.messages().resolve(sender, "health-header"));
            for (String line : plugin.healthSummary().split("\\n")) {
                sender.sendMessage(plugin.messages().resolve(sender, "health-line", Map.of("line", line)));
            }
            sender.sendMessage(plugin.messages().resolve(sender, "health-footer"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("cleanfreeze")) {
            if (!Permissions.has(sender, "staffcore.reload")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            int removed = plugin.freezeManager().cleanupOfflineFrozen();
            sender.sendMessage(plugin.messages().resolve(sender, "cleanfreeze-result", Map.of("count", String.valueOf(removed))));
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
            int page = 1;
            if (args.length >= 2) {
                page = parseHelpPage(args[1]);
            }
            int totalPages = HELP_PAGES.size();
            if (page < 1 || page > totalPages) {
                plugin.messages().send(sender, "staff-help-invalid-page", Map.of(
                    "page", args.length >= 2 ? args[1] : String.valueOf(page),
                    "total", String.valueOf(totalPages)
                ));
                return true;
            }
            sendHelpPage(sender, page, totalPages);
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }

        if (!Permissions.hasAny(player, "staffcore.staff", "staffcore.use")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("menu")) {
            plugin.menuManager().openMain(player);
            return true;
        }

        if (args.length == 0) {
            plugin.staffManager().toggle(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            plugin.staffManager().enable(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            plugin.staffManager().disable(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("speed")) {
            if (!Permissions.has(player, "staffcore.speed")) {
                plugin.messages().send(player, "no-permission");
                return true;
            }
            if (args.length != 2) {
                plugin.messages().send(player, "staff-speed-usage");
                return true;
            }
            Float level = parseSpeedLevel(args[1]);
            if (level == null) {
                plugin.messages().send(player, "staff-speed-invalid", Map.of("value", args[1]));
                return true;
            }
            float speed = normalizedSpeed(level);
            float applied = plugin.staffManager().setStaffWalkSpeed(player, speed);
            plugin.messages().send(player, "staff-speed-set", Map.of(
                "level", trim(level),
                "value", String.format(Locale.US, "%.2f", applied)
            ));
            plugin.staffLogManager().log(player.getName(), "STAFF_SPEED", player.getName(), "walk=" + applied + " level=" + trim(level));
            return true;
        }

        if (args[0].equalsIgnoreCase("flyspeed")) {
            if (!Permissions.has(player, "staffcore.speed")) {
                plugin.messages().send(player, "no-permission");
                return true;
            }
            if (args.length != 2) {
                plugin.messages().send(player, "staff-flyspeed-usage");
                return true;
            }
            Float level = parseSpeedLevel(args[1]);
            if (level == null) {
                plugin.messages().send(player, "staff-speed-invalid", Map.of("value", args[1]));
                return true;
            }
            float speed = normalizedSpeed(level);
            float applied = plugin.staffManager().setStaffFlySpeed(player, speed);
            plugin.messages().send(player, "staff-flyspeed-set", Map.of(
                "level", trim(level),
                "value", String.format(Locale.US, "%.2f", applied)
            ));
            plugin.staffLogManager().log(player.getName(), "STAFF_FLYSPEED", player.getName(), "fly=" + applied + " level=" + trim(level));
            return true;
        }

        plugin.messages().send(player, "staff-usage");
        return true;
    }

    private Float parseSpeedLevel(String raw) {
        try {
            float value = Float.parseFloat(raw.replace(',', '.'));
            if (value < 0.0f || value > 10.0f) {
                return null;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private float normalizedSpeed(float level) {
        return Math.max(0.0f, Math.min(1.0f, level / 10.0f));
    }

    private String trim(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private int parseHelpPage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendHelpPage(CommandSender sender, int page, int totalPages) {
        sender.sendMessage(plugin.messages().resolve(sender, "staff-help-header"));
        sender.sendMessage(plugin.messages().resolve(sender, "staff-help-page-indicator", Map.of(
            "page", String.valueOf(page),
            "total", String.valueOf(totalPages)
        )));

        List<String> keys = HELP_PAGES.get(page - 1);

        for (String key : keys) {
            sender.sendMessage(plugin.messages().resolve(sender, key));
        }

        sendHelpNavigation(sender, page, totalPages);
        sender.sendMessage(plugin.messages().resolve(sender, "staff-help-footer"));
    }

    private void sendHelpNavigation(CommandSender sender, int page, int totalPages) {
        boolean hasPrev = page > 1;
        boolean hasNext = page < totalPages;
        if (!hasPrev && !hasNext) {
            return;
        }

        if (!(sender instanceof Player player)) {
            if (hasPrev) {
                sender.sendMessage(plugin.messages().resolve(sender, "staff-help-page-hint-prev", Map.of(
                    "prev", String.valueOf(page - 1)
                )));
            }
            if (hasNext) {
                sender.sendMessage(plugin.messages().resolve(sender, "staff-help-page-hint-next", Map.of(
                    "next", String.valueOf(page + 1)
                )));
            }
            return;
        }

        TextComponent navigationLine = new TextComponent();
        if (hasPrev) {
            int prevPage = page - 1;
            TextComponent prev = legacy(plugin.messages().resolve(player, "staff-help-nav-prev", Map.of(
                "prev", String.valueOf(prevPage)
            )));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/staff help " + prevPage));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(plugin.messages().resolve(player, "staff-help-nav-hover-prev", Map.of(
                "prev", String.valueOf(prevPage)
            )))));
            navigationLine.addExtra(prev);
        }
        if (hasPrev && hasNext) {
            navigationLine.addExtra(legacy(plugin.messages().resolve(player, "staff-help-nav-separator")));
        }
        if (hasNext) {
            int nextPage = page + 1;
            TextComponent next = legacy(plugin.messages().resolve(player, "staff-help-nav-next", Map.of(
                "next", String.valueOf(nextPage)
            )));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/staff help " + nextPage));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover(plugin.messages().resolve(player, "staff-help-nav-hover-next", Map.of(
                "next", String.valueOf(nextPage)
            )))));
            navigationLine.addExtra(next);
        }
        player.spigot().sendMessage(navigationLine);
    }

    private TextComponent legacy(String text) {
        return new TextComponent(TextComponent.fromLegacyText(Messages.color(text)));
    }

    private BaseComponent[] hover(String text) {
        return new ComponentBuilder(Messages.color(text)).create();
    }
}
