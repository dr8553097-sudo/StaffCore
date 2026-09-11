package com.tuservidor.staffcore.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerVersionTest {

    @Test
    void compareDetectsWhenLatestIsHigher() {
        assertTrue(UpdateChecker.compareVersions("1.2.0", "1.2.1") < 0);
        assertTrue(UpdateChecker.compareVersions("1.2.0", "1.10.0") < 0);
    }

    @Test
    void compareTreatsReleaseAsHigherThanPrerelease() {
        assertTrue(UpdateChecker.compareVersions("1.2.0-beta.2", "1.2.0") < 0);
        assertTrue(UpdateChecker.compareVersions("1.2.0", "1.2.0-beta.2") > 0);
    }

    @Test
    void compareHandlesEqualVersions() {
        assertEquals(0, UpdateChecker.compareVersions("v1.2.0", "1.2.0"));
    }
}
