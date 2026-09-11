package com.tuservidor.staffcore.listeners;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandVisibilityPolicyTest {

    @Test
    void detectsStaffCoreNamespaceCalls() {
        assertTrue(CommandVisibilityPolicy.isStaffCoreNamespaceCall("/staffcore:report"));
        assertFalse(CommandVisibilityPolicy.isStaffCoreNamespaceCall("/report"));
    }

    @Test
    void hidesCommandWhenSenderLacksPermissions() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("staffcore.admin")).thenReturn(false);
        when(sender.hasPermission("staffcore.punish.ban")).thenReturn(false);

        Map<String, String[]> rules = Map.of("scban", new String[]{"staffcore.punish.ban"});
        assertTrue(CommandVisibilityPolicy.shouldHideCommandFrom(sender, "/scban target", rules));
    }

    @Test
    void allowsCommandWhenSenderHasRequiredPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("staffcore.admin")).thenReturn(false);
        when(sender.hasPermission("staffcore.punish.ban")).thenReturn(true);

        Map<String, String[]> rules = Map.of("scban", new String[]{"staffcore.punish.ban"});
        assertFalse(CommandVisibilityPolicy.shouldHideCommandFrom(sender, "/scban target", rules));
    }
}
