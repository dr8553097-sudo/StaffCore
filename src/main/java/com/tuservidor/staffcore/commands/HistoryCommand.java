package com.tuservidor.staffcore.commands;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.NoteEntry;
import com.tuservidor.staffcore.data.PunishmentEntry;
import com.tuservidor.staffcore.util.DurationParser;
import com.tuservidor.staffcore.util.ModerationGuard;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HistoryCommand implements CommandExecutor {

    private final StaffCore plugin;

    public HistoryCommand(StaffCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        if (!Permissions.has(staff, "staffcore.history")) {
            plugin.messages().send(staff, "no-permission");
            return true;
        }
        if (args.length != 1) {
            plugin.messages().send(staff, "history-usage");
            return true;
        }

        String inputName = args[0];
        Player onlineTarget = Bukkit.getPlayerExact(inputName);
        UUID targetUuid = resolveTargetUuid(inputName, onlineTarget);
        String targetName = onlineTarget != null
            ? onlineTarget.getName()
            : Bukkit.getOfflinePlayer(targetUuid).getName();
        if (targetName == null || targetName.isBlank()) {
            targetName = inputName;
        }

        List<PunishmentEntry> punishments = plugin.punishmentManager().historyByIdentity(targetUuid, inputName, 10);
        List<NoteEntry> notes = plugin.noteManager().byIdentity(targetUuid, inputName, 10);

        staff.sendMessage(plugin.messages().resolve(staff, "history-header", Map.of("player", targetName)));
        if (punishments.isEmpty()) {
            staff.sendMessage(plugin.messages().resolve(staff, "history-punishments-empty"));
        } else {
            staff.sendMessage(plugin.messages().resolve(staff, "history-punishments-title"));
            for (PunishmentEntry entry : punishments) {
                String duration = entry.expiresAt() == null
                    ? plugin.messages().resolve(staff, "history-duration-permanent")
                    : DurationParser.format(Duration.ofMillis(Math.max(0L, entry.expiresAt() - entry.createdAt())));
                String activeText = entry.active()
                    ? plugin.messages().resolve(staff, "menu-state-yes")
                    : plugin.messages().resolve(staff, "menu-state-no");
                String typeText = plugin.messages().resolve(staff, entry.type().translationKey());
                staff.sendMessage(plugin.messages().resolve(staff, "history-punishments-entry", Map.of(
                    "id", String.valueOf(entry.id()),
                    "type", typeText,
                    "duration", duration,
                    "reason", ModerationGuard.sanitizeReason(entry.reason())
                )));
                staff.sendMessage(plugin.messages().resolve(staff, "history-punishments-entry-active", Map.of(
                    "active", activeText
                )));
            }
        }

        if (notes.isEmpty()) {
            staff.sendMessage(plugin.messages().resolve(staff, "history-notes-empty"));
        } else {
            staff.sendMessage(plugin.messages().resolve(staff, "history-notes-title"));
            for (NoteEntry note : notes) {
                staff.sendMessage(plugin.messages().resolve(staff, "history-notes-entry", Map.of(
                    "id", String.valueOf(note.id()),
                    "content", note.content(),
                    "staff", note.staffName()
                )));
            }
        }

        long openReports = plugin.reportManager().openReports().stream()
            .filter(report -> report.target().equals(targetUuid))
            .count();
        staff.sendMessage(plugin.messages().resolve(staff, "history-open-reports", Map.of("count", String.valueOf(openReports))));
        return true;
    }

    private UUID resolveTargetUuid(String inputName, Player onlineTarget) {
        if (onlineTarget != null) {
            return onlineTarget.getUniqueId();
        }
        return plugin.punishmentManager().latestKnownTargetUuidByName(inputName)
            .or(() -> plugin.noteManager().latestKnownTargetUuidByName(inputName))
            .orElseGet(() -> Bukkit.getOfflinePlayer(inputName).getUniqueId());
    }
}
