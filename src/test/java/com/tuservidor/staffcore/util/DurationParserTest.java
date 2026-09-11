package com.tuservidor.staffcore.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DurationParserTest {

    @Test
    void parseSupportsCompoundDurations() {
        Duration duration = DurationParser.parse("1h30m15s");
        assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(15), duration);
    }

    @Test
    void parseReturnsZeroForInvalidInput() {
        assertEquals(Duration.ZERO, DurationParser.parse("abc"));
        assertEquals(Duration.ZERO, DurationParser.parse("0m"));
    }

    @Test
    void parseReturnsNullForPermanentKeywords() {
        assertNull(DurationParser.parse("perm"));
        assertNull(DurationParser.parse("permanent"));
        assertNull(DurationParser.parse("forever"));
    }

    @Test
    void formatBuildsReadableTokens() {
        Duration duration = Duration.ofDays(8).plusHours(2).plusMinutes(5);
        assertEquals("1w 1d 2h 5m", DurationParser.format(duration));
    }
}
