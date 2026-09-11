package com.tuservidor.staffcore.staff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XrayAlertManagerHeuristicTest {

    @Test
    void ratioTriggerActivatesWhenThresholdsAreMet() {
        XrayAlertManager.TriggerEvaluation evaluation = XrayAlertManager.evaluateTriggers(
            120,
            16,
            0.1333D,
            0.62D,
            3,
            80,
            6,
            0.10D,
            0.55D,
            5
        );

        assertTrue(evaluation.ratioTrigger());
        assertFalse(evaluation.burstTrigger());
        assertTrue(evaluation.triggered());
        assertEquals("RATIO", evaluation.triggerType());
    }

    @Test
    void burstTriggerActivatesIndependently() {
        XrayAlertManager.TriggerEvaluation evaluation = XrayAlertManager.evaluateTriggers(
            25,
            6,
            0.06D,
            0.20D,
            5,
            80,
            6,
            0.10D,
            0.55D,
            5
        );

        assertFalse(evaluation.ratioTrigger());
        assertTrue(evaluation.burstTrigger());
        assertTrue(evaluation.triggered());
        assertEquals("BURST", evaluation.triggerType());
    }

    @Test
    void cooldownPreventsRapidRetrigger() {
        long now = 1_000_000L;
        assertFalse(XrayAlertManager.cooldownReady(now, now - 10_000L, 15));
        assertTrue(XrayAlertManager.cooldownReady(now, now - 20_000L, 15));
    }
}
