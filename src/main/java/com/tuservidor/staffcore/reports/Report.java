package com.tuservidor.staffcore.reports;

import java.time.Instant;
import java.util.UUID;

public record Report(
    int id,
    UUID reporter,
    String reporterName,
    UUID target,
    String targetName,
    String reason,
    Instant createdAt,
    boolean open,
    String status,
    String handledBy,
    Instant handledAt,
    String resolutionReason
) {
    public boolean isOpen() {
        return open;
    }

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public String normalizedStatus() {
        return status == null || status.isBlank() ? (open ? "OPEN" : "CLOSED") : status.toUpperCase();
    }
}
