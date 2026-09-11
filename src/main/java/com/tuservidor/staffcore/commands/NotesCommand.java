package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.NoteEntry;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class NotesCommand implements CommandExecutor {

    private final StaffCore plugin;

    public NotesCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(staff, "staffcore.notes")) {
            plugin.messages().send(staff, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.messages().send(staff, "notes-usage");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName() == null ? args[0] : target.getName();
        String action = args[1].toLowerCase();

        switch (action) {
            case "list" -> {
                List<NoteEntry> notes = plugin.noteManager().byTarget(target.getUniqueId(), 12);
                if (notes.isEmpty()) {
                    plugin.messages().send(staff, "notes-empty", Map.of("player", targetName));
                    return true;
                }
                staff.sendMessage(plugin.messages().resolve(staff, "notes-list-header", Map.of("player", targetName)));
                for (NoteEntry note : notes) {
                    staff.sendMessage(plugin.messages().resolve(staff, "notes-list-entry", Map.of(
                        "id", String.valueOf(note.id()),
                        "content", note.content(),
                        "staff", note.staffName()
                    )));
                }
            }
            case "add" -> {
                if (args.length < 3) {
                    plugin.messages().send(staff, "notes-add-usage");
                    return true;
                }
                String content = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                int maxLength = plugin.getConfig().getInt("moderation.max-note-length", 220);
                if (content.length() > maxLength) {
                    plugin.messages().send(staff, "note-too-long", Map.of("max", String.valueOf(maxLength)));
                    return true;
                }
                NoteEntry note = plugin.noteManager().add(staff, target, targetName, content);
                plugin.staffLogManager().log(staff.getName(), "NOTE_ADD", targetName, content);
                plugin.messages().send(staff, "note-created", Map.of("id", String.valueOf(note.id()), "player", targetName));
            }
            case "remove" -> {
                if (args.length < 3) {
                    plugin.messages().send(staff, "notes-remove-usage");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[2]);
                    if (plugin.noteManager().remove(id).isPresent()) {
                        plugin.staffLogManager().log(staff.getName(), "NOTE_REMOVE", targetName, "Removed #" + id);
                        plugin.messages().send(staff, "note-removed", Map.of("id", String.valueOf(id)));
                    } else {
                        plugin.messages().send(staff, "note-not-found", Map.of("id", String.valueOf(id)));
                    }
                } catch (NumberFormatException exception) {
                    plugin.messages().send(staff, "note-invalid-id", Map.of("id", args[2]));
                }
            }
            default -> plugin.messages().send(staff, "notes-usage");
        }
        return true;
    }
}
