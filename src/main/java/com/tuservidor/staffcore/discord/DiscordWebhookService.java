package com.tuservidor.staffcore.discord;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscordWebhookService {

    private final Logger logger;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String reportsWebhook;
    private final String dutyWebhook;
    private final String serverName;

    public DiscordWebhookService(FileConfiguration config, Logger logger) {
        this.logger = logger;
        this.enabled = config.getBoolean("discord.enabled", false);
        this.reportsWebhook = config.getString("discord.webhooks.reports", "");
        this.dutyWebhook = config.getString("discord.webhooks.duty", "");
        this.serverName = config.getString("discord.server-name", "Survival");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public void sendReportEmbed(String reporter, String reported, String reason) {
        if (!enabled || reportsWebhook.isBlank()) return;

        String avatarUrl = "https://mc-heads.net/avatar/" + reported + "/100";

        String json = "{"
            + "\"username\": \"StaffCore Reports\","
            + "\"embeds\": [{"
            + "\"title\": \"🚨 New Player Report\","
            + "\"color\": 0xE11D48,"
            + "\"thumbnail\": {\"url\": \"" + avatarUrl + "\"},"
            + "\"fields\": ["
            + "{\"name\": \"⚠️ Reported Player\", \"value\": \"" + escapeJson(reported) + "\", \"inline\": true},"
            + "{\"name\": \"📢 Reporter\", \"value\": \"" + escapeJson(reporter) + "\", \"inline\": true},"
            + "{\"name\": \"📝 Reason\", \"value\": \"" + escapeJson(reason) + "\", \"inline\": false}"
            + "],"
            + "\"footer\": {\"text\": \"Server: " + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + Instant.now().toString() + "\""
            + "}]"
            + "}";

        sendAsync(reportsWebhook, json);
    }

    public void sendDutyEmbed(String staffName, boolean onDuty, long sessionMinutes, long weeklyHours) {
        if (!enabled || dutyWebhook.isBlank()) return;

        int color = onDuty ? 0x10B981 : 0x64748B; // Green or Slate
        String title = onDuty ? "🟢 Staff Member Clocked IN" : "🔴 Staff Member Clocked OUT";

        String json = "{"
            + "\"username\": \"StaffCore Duty Log\","
            + "\"embeds\": [{"
            + "\"title\": \"" + title + "\","
            + "\"color\": " + color + ","
            + "\"fields\": ["
            + "{\"name\": \"🛡️ Staff\", \"value\": \"" + escapeJson(staffName) + "\", \"inline\": true},"
            + "{\"name\": \"⏱️ Session\", \"value\": \"" + sessionMinutes + " mins\", \"inline\": true},"
            + "{\"name\": \"📊 Total Weekly Hours\", \"value\": \"" + weeklyHours + " hrs\", \"inline\": true}"
            + "],"
            + "\"footer\": {\"text\": \"Server: " + escapeJson(serverName) + "\"},"
            + "\"timestamp\": \"" + Instant.now().toString() + "\""
            + "}]"
            + "}";

        sendAsync(dutyWebhook, json);
    }

    private void sendAsync(String webhookUrl, String jsonPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", "StaffCore-Discord-Webhook")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(5))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    logger.log(Level.FINE, "[StaffCore] Discord webhook failed: " + ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            logger.log(Level.FINE, "[StaffCore] Invalid Discord Webhook URL: " + e.getMessage());
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public boolean isEnabled() {
        return enabled;
    }
}
