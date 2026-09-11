package com.tuservidor.staffcore.data;

import java.util.UUID;

public record PunishmentEntry(
    int id,
    PunishmentType type,
    UUID targetUuid,
    String targetName,
    UUID staffUuid,
    String staffName,
    String reason,
    long createdAt,
    Long expiresAt,
    boolean active
) {
}
