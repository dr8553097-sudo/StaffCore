package com.tuservidor.staffcore.util;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionsTest {

    @Test
    void hasAllowsAdminOverride() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("staffcore.admin")).thenReturn(true);

        assertTrue(Permissions.has(sender, "staffcore.some.node"));
    }

    @Test
    void hasRequiresSpecificPermissionWithoutAdmin() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("staffcore.admin")).thenReturn(false);
        when(sender.hasPermission("staffcore.freeze")).thenReturn(true);

        assertTrue(Permissions.has(sender, "staffcore.freeze"));
        assertFalse(Permissions.has(sender, "staffcore.vanish"));
    }

    @Test
    void hasAnyChecksProvidedNodes() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("staffcore.admin")).thenReturn(false);
        when(sender.hasPermission("node.a")).thenReturn(false);
        when(sender.hasPermission("node.b")).thenReturn(true);

        assertTrue(Permissions.hasAny(sender, "node.a", "node.b"));
        assertFalse(Permissions.hasAny(sender, "node.c", "node.d"));
    }
}
