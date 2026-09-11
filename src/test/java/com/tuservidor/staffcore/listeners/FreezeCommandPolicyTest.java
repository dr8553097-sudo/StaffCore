package com.tuservidor.staffcore.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreezeCommandPolicyTest {

    @Test
    void normalizeCommandRemovesSlashNamespaceAndArgs() {
        assertEquals("scban", FreezeCommandPolicy.normalizeCommand("/staffcore:scban player reason"));
        assertEquals("msg", FreezeCommandPolicy.normalizeCommand("/msg user hola"));
        assertEquals("freeze", FreezeCommandPolicy.normalizeCommand("freeze"));
    }

    @Test
    void blockedCommandsAlwaysWinEvenIfAllowedByConfig() {
        boolean allowed = FreezeCommandPolicy.isAllowedWhileFrozen(
            "spawn",
            List.of("/spawn", "/msg"),
            List.of("/spawn")
        );
        assertFalse(allowed);
    }

    @Test
    void coreCommunicationCommandsStayAllowed() {
        assertTrue(FreezeCommandPolicy.isAllowedWhileFrozen("msg", List.of(), List.of()));
        assertTrue(FreezeCommandPolicy.isAllowedWhileFrozen("reply", List.of(), List.of()));
    }

    @Test
    void unknownCommandIsDeniedByDefault() {
        assertFalse(FreezeCommandPolicy.isAllowedWhileFrozen("home", List.of("/msg"), List.of("/spawn")));
    }
}
