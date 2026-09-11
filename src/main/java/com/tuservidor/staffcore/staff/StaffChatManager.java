package com.tuservidor.staffcore.staff;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StaffChatManager {

    private final Set<UUID> enabled = new HashSet<>();

    public boolean toggle(UUID uuid) {
        if (enabled.remove(uuid)) {
            return false;
        }
        enabled.add(uuid);
        return true;
    }

    public void disable(UUID uuid) {
        enabled.remove(uuid);
    }

    public boolean isEnabled(UUID uuid) {
        return enabled.contains(uuid);
    }
}
