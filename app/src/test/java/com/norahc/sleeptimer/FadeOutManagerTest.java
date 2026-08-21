package com.norahc.sleeptimer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FadeOutManagerTest {
    @Test
    public void fullDurationKeepsOriginalVolume() {
        assertEquals(10, FadeOutManager.calculateVolume(10, 30_000L));
    }

    @Test
    public void halfwayPointUsesAboutHalfVolume() {
        assertEquals(5, FadeOutManager.calculateVolume(10, 15_000L));
    }

    @Test
    public void endOfFadeReachesZero() {
        assertEquals(0, FadeOutManager.calculateVolume(10, 0L));
    }

    @Test
    public void remainingTimeAboveFadeWindowDoesNotIncreaseVolume() {
        assertEquals(7, FadeOutManager.calculateVolume(7, 60_000L));
    }
}
