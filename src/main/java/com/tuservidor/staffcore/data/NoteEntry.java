package com.tuservidor.staffcore.data;

import java.util.UUID;

public record NoteEntry(
    int id,
    UUID targetUuid,
    String targetName,
    UUID staffUuid,
    String staffName,
    String content,
    long createdAt
) {
}
