package com.tuservidor.staffcore.data;

public record StaffLogEntry(
    int id,
    long createdAt,
    String staff,
    String action,
    String target,
    String details
) {
}
