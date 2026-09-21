package com.tuservidor.staffcore.reports;

import com.tuservidor.staffcore.StaffCore;
import com.tuservidor.staffcore.data.AsyncYamlPersistence;
import com.tuservidor.staffcore.util.Messages;
import com.tuservidor.staffcore.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ReportManager extends AsyncYamlPersistence {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CLOSED = "CLOSED";

    private final StaffCore plugin;
    private final Messages messages;
    private final File file;
    private final List<Report> reports = new ArrayList<>();
    private final Map<UUID, Long> reportCooldowns = new HashMap<>();
    private final Map<UUID, PendingReject> pendingRejects = new HashMap<>();
    private int nextId = 1;

    public ReportManager(StaffCore plugin, Messages messages) {
        super(plugin, "reports.yml");
        this.plugin = plugin;
        this.messages = messages;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
        reload();
    }

    public Report create(Player reporter, UUID targetUuid, String targetName, String reason) {
        String cleanReason = reason == null ? "" : reason.trim();
        Report report = new Report(
            nextId++,
            reporter.getUniqueId(),
            reporter.getName(),
            targetUuid,
            targetName,
            cleanReason,
            Instant.now(),
            true,
            STATUS_OPEN,
            "",
            null,
            ""
        );
        reports.add(report);
        reportCooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
        queueSave();

        plugin.staffLogManager().log(reporter.getName(), "REPORT", targetName, cleanReason);
        messages.send(reporter, "report-created-reporter", Map.of(
            "id", String.valueOf(report.id()),
            "target", targetName,
            "reason", cleanReason
        ));
        notifyStaff(report);

        if (plugin.discordWebhookService() != null) {
            plugin.discordWebhookService().sendReportEmbed(reporter.getName(), targetName, cleanReason);
        }
        if (plugin.redisManager() != null && plugin.redisManager().isEnabled()) {
            plugin.redisManager().publish("reports", reporter.getName() + " reported " + targetName + ": " + cleanReason);
        }

        return report;
    }

    public Report create(Player reporter, Player target, String reason) {
        return create(reporter, target.getUniqueId(), target.getName(), reason);
    }

    public boolean hasOpenReport(Player reporter, Player target) {
        return hasOpenReport(reporter.getUniqueId(), target.getUniqueId(), target.getName());
    }

    public boolean hasOpenReport(UUID reporter, UUID target) {
        return hasOpenReport(reporter, target, null);
    }

    public boolean hasOpenReport(UUID reporter, UUID targetUuid, String targetName) {
        return reports.stream()
            .filter(Report::isOpen)
            .anyMatch(r -> r.reporter().equals(reporter) && (
                (targetUuid != null && r.target().equals(targetUuid))
                || (targetName != null && r.targetName().equalsIgnoreCase(targetName))
            ));
    }

    public long remainingCooldownSeconds(Player reporter) {
        return remainingCooldownSeconds(reporter.getUniqueId());
    }

    public long remainingCooldownSeconds(UUID reporter) {
        Long last = reportCooldowns.get(reporter);
        if (last == null) {
            return 0L;
        }
        long diff = System.currentTimeMillis() - last;
        long cd = cooldownMillis();
        if (diff >= cd) {
            reportCooldowns.remove(reporter);
            return 0L;
        }
        return (cd - diff + 999L) / 1000L;
    }

    public List<Report> openReports() {
        return reports.stream()
            .filter(Report::isOpen)
            .sorted(Comparator.comparingInt(Report::id).reversed())
            .toList();
    }

    public List<Report> reports() {
        return reports.stream()
            .sorted(Comparator.comparingInt(Report::id).reversed())
            .toList();
    }

    public Optional<Report> byId(int id) {
        return reports.stream().filter(report -> report.id() == id).findFirst();
    }

    public boolean close(int id, CommandSender sender) {
        Optional<Report> existing = byId(id);
        if (existing.isEmpty() || !existing.get().isOpen()) {
            return false;
        }

        Report report = existing.get();
        String reason = plugin.getConfig().getString("reports.default-close-reason", "Handled by staff.");
        Report resolved = resolve(report, STATUS_CLOSED, sender.getName(), reason);
        if (resolved == null) {
            return false;
        }

        queueSave();
        plugin.staffLogManager().log(sender.getName(), "REPORT_CLOSE", report.targetName(), "Closed report #" + id);
        messages.send(sender, "report-closed", Map.of("id", String.valueOf(id)));
        notifyReporterResolution(resolved);
        notifyStaffResolution(resolved);
        return true;
    }

    public boolean accept(int id, CommandSender sender) {
        Optional<Report> existing = byId(id);
        if (existing.isEmpty() || !existing.get().isOpen()) {
            return false;
        }

        Report report = existing.get();
        String reason = plugin.getConfig().getString("reports.default-accept-reason", "Your report is now under review.");
        Report resolved = resolve(report, STATUS_ACCEPTED, sender.getName(), reason);
        if (resolved == null) {
            return false;
        }

        queueSave();
        plugin.staffLogManager().log(sender.getName(), "REPORT_ACCEPT", report.targetName(), "Accepted report #" + id);
        messages.send(sender, "report-accepted-staff", Map.of("id", String.valueOf(id), "target", report.targetName()));
        notifyReporterResolution(resolved);
        notifyStaffResolution(resolved);
        return true;
    }

    public boolean reject(int id, Player sender, String reason) {
        Optional<Report> existing = byId(id);
        if (existing.isEmpty() || !existing.get().isOpen()) {
            return false;
        }

        Report report = existing.get();
        Report resolved = resolve(report, STATUS_REJECTED, sender.getName(), reason);
        if (resolved == null) {
            return false;
        }

        queueSave();
        plugin.staffLogManager().log(sender.getName(), "REPORT_REJECT", report.targetName(), "Rejected report #" + id + ": " + reason);
        messages.send(sender, "report-rejected-staff", Map.of("id", String.valueOf(id), "target", report.targetName()));
        notifyReporterResolution(resolved);
        notifyStaffResolution(resolved);
        return true;
    }

    public boolean beginRejectFlow(Player staff, int reportId) {
        Optional<Report> existing = byId(reportId);
        if (existing.isEmpty() || !existing.get().isOpen()) {
            messages.send(staff, "report-not-found", Map.of("id", String.valueOf(reportId)));
            return false;
        }
        purgeExpiredRejectPrompts();
        pendingRejects.put(staff.getUniqueId(), new PendingReject(reportId, System.currentTimeMillis()));
        messages.send(staff, "report-reject-prompt", Map.of("id", String.valueOf(reportId)));
        return true;
    }

    public boolean isAwaitingRejectReason(UUID staffUuid) {
        purgeExpiredRejectPrompts();
        return pendingRejects.containsKey(staffUuid);
    }

    public void clearRejectFlow(UUID staffUuid) {
        pendingRejects.remove(staffUuid);
    }

    public void clearCooldownIfExpired(UUID playerUuid) {
        long cooldownMillis = cooldownMillis();
        if (cooldownMillis <= 0L) {
            reportCooldowns.remove(playerUuid);
            return;
        }

        Long lastReportAt = reportCooldowns.get(playerUuid);
        if (lastReportAt == null) {
            return;
        }
        if (System.currentTimeMillis() - lastReportAt >= cooldownMillis) {
            reportCooldowns.remove(playerUuid);
        }
    }

    public void clearCooldown(UUID playerUuid) {
        reportCooldowns.remove(playerUuid);
    }

    public void handleRejectReasonInput(Player staff, String inputRaw) {
        PendingReject pending = pendingRejects.get(staff.getUniqueId());
        if (pending == null) {
            return;
        }

        String input = inputRaw == null ? "" : inputRaw.trim();
        if (input.equalsIgnoreCase("cancel")) {
            pendingRejects.remove(staff.getUniqueId());
            messages.send(staff, "report-reject-cancelled");
            return;
        }

        int minReason = Math.max(2, plugin.getConfig().getInt("reports.reject-min-reason-length", 4));
        if (input.length() < minReason) {
            messages.send(staff, "report-reject-reason-too-short", Map.of("min", String.valueOf(minReason)));
            return;
        }

        Optional<Report> existing = byId(pending.reportId());
        if (existing.isEmpty() || !existing.get().isOpen()) {
            pendingRejects.remove(staff.getUniqueId());
            messages.send(staff, "report-not-found", Map.of("id", String.valueOf(pending.reportId())));
            return;
        }

        if (reject(pending.reportId(), staff, input)) {
            pendingRejects.remove(staff.getUniqueId());
        }
    }

    public void reload() {
        prepareForReload();
        reports.clear();
        reportCooldowns.clear();
        pendingRejects.clear();
        nextId = 1;
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("reports");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection reportSection = section.getConfigurationSection(key);
            if (reportSection == null) {
                continue;
            }
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Invalid report id in reports.yml: " + key);
                continue;
            }
            String reporter = reportSection.getString("reporter");
            String target = reportSection.getString("target");
            if (reporter == null || target == null) {
                plugin.getLogger().warning("Skipping broken report #" + id);
                continue;
            }
            boolean open = reportSection.getBoolean("open", true);
            String status = reportSection.getString("status", open ? STATUS_OPEN : STATUS_CLOSED);
            long handledAtRaw = reportSection.getLong("handled-at", 0L);
            reports.add(new Report(
                id,
                UUID.fromString(reporter),
                reportSection.getString("reporter-name", "Unknown"),
                UUID.fromString(target),
                reportSection.getString("target-name", "Unknown"),
                reportSection.getString("reason", "No reason"),
                Instant.ofEpochMilli(reportSection.getLong("created-at")),
                open,
                status,
                reportSection.getString("handled-by", ""),
                handledAtRaw <= 0L ? null : Instant.ofEpochMilli(handledAtRaw),
                reportSection.getString("resolution-reason", "")
            ));
            nextId = Math.max(nextId, id + 1);
        }
    }

    public void save() {
        flushNow();
    }

    public int totalReports() {
        return reports.size();
    }

    public int openReportsCount() {
        int count = 0;
        for (Report report : reports) {
            if (report.isOpen()) {
                count++;
            }
        }
        return count;
    }

    public int cooldownTrackedPlayers() {
        purgeExpiredReportCooldowns();
        return reportCooldowns.size();
    }

    public int pendingRejectFlows() {
        return pendingRejects.size();
    }

    public boolean saveQueued() {
        return super.isSaveQueued();
    }

    public boolean asyncWriteInProgress() {
        return super.isAsyncWriteInProgress();
    }

    @Override
    protected YamlConfiguration buildSnapshot() {
        YamlConfiguration config = new YamlConfiguration();
        for (Report report : reports) {
            String path = "reports." + report.id() + ".";
            config.set(path + "reporter", report.reporter().toString());
            config.set(path + "reporter-name", report.reporterName());
            config.set(path + "target", report.target().toString());
            config.set(path + "target-name", report.targetName());
            config.set(path + "reason", report.reason());
            config.set(path + "created-at", report.createdAt().toEpochMilli());
            config.set(path + "open", report.open());
            config.set(path + "status", report.normalizedStatus());
            config.set(path + "handled-by", report.handledBy());
            config.set(path + "handled-at", report.handledAt() == null ? 0L : report.handledAt().toEpochMilli());
            config.set(path + "resolution-reason", report.resolutionReason());
        }
        return config;
    }

    private Report resolve(Report openReport, String status, String staffName, String resolutionReason) {
        if (!openReport.isOpen()) {
            return null;
        }
        reports.remove(openReport);
        Report resolved = new Report(
            openReport.id(),
            openReport.reporter(),
            openReport.reporterName(),
            openReport.target(),
            openReport.targetName(),
            openReport.reason(),
            openReport.createdAt(),
            false,
            status,
            staffName == null ? "Unknown" : staffName,
            Instant.now(),
            resolutionReason == null ? "" : resolutionReason
        );
        reports.add(resolved);
        return resolved;
    }

    private void notifyStaff(Report report) {
        Map<String, String> placeholders = Map.of(
            "id", String.valueOf(report.id()),
            "reporter", report.reporterName(),
            "target", report.targetName(),
            "reason", report.reason()
        );
        String consoleLine = messages.resolve("report-alert", placeholders);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Permissions.has(player, "staffcore.reports.view")) {
                player.sendMessage(messages.resolve(player, "report-alert", placeholders));
            }
        }
        Bukkit.getConsoleSender().sendMessage(consoleLine);
    }

    private void notifyReporterResolution(Report report) {
        Player reporter = Bukkit.getPlayer(report.reporter());
        if (reporter == null) {
            return;
        }
        String staff = report.handledBy() == null || report.handledBy().isBlank() ? "Unknown" : report.handledBy();
        String reason = report.resolutionReason() == null || report.resolutionReason().isBlank()
            ? plugin.getConfig().getString("reports.default-close-reason", "Handled by staff.")
            : report.resolutionReason();

        Map<String, String> placeholders = Map.of(
            "id", String.valueOf(report.id()),
            "staff", staff,
            "reason", reason,
            "target", report.targetName()
        );

        if (report.isAccepted()) {
            messages.send(reporter, "report-accepted-reporter", placeholders);
            return;
        }
        if (report.isRejected()) {
            messages.send(reporter, "report-rejected-reporter", placeholders);
            return;
        }
        messages.send(reporter, "report-closed-reporter", placeholders);
    }

    private void notifyStaffResolution(Report report) {
        Map<String, String> placeholders = Map.of(
            "id", String.valueOf(report.id()),
            "status", report.normalizedStatus(),
            "staff", report.handledBy() == null || report.handledBy().isBlank() ? "Unknown" : report.handledBy(),
            "target", report.targetName(),
            "reporter", report.reporterName(),
            "reason", report.resolutionReason() == null || report.resolutionReason().isBlank() ? "-" : report.resolutionReason()
        );
        String consoleLine = messages.resolve("report-resolved-alert", placeholders);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Permissions.has(player, "staffcore.reports.view")) {
                player.sendMessage(messages.resolve(player, "report-resolved-alert", placeholders));
            }
        }
        Bukkit.getConsoleSender().sendMessage(consoleLine);
    }

    private void purgeExpiredRejectPrompts() {
        long timeoutSeconds = Math.max(15L, plugin.getConfig().getLong("reports.reject-prompt-timeout-seconds", 120L));
        long now = System.currentTimeMillis();
        pendingRejects.entrySet().removeIf(entry -> now - entry.getValue().startedAtMillis() > timeoutSeconds * 1000L);
    }

    private void purgeExpiredReportCooldowns() {
        long cooldownMillis = cooldownMillis();
        if (cooldownMillis <= 0L) {
            reportCooldowns.clear();
            return;
        }
        long now = System.currentTimeMillis();
        reportCooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= cooldownMillis);
    }

    private long cooldownMillis() {
        return Math.max(0L, plugin.getConfig().getLong("reports.cooldown-seconds", 45L)) * 1000L;
    }

    private record PendingReject(int reportId, long startedAtMillis) {
    }
}
