package com.norahc.sleeptimer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppPrefsTest {
    @Test
    public void extraDimClampsBelowZero() {
        assertEquals(0, AppPrefs.clampExtraDimPercent(-20));
    }

    @Test
    public void extraDimKeepsValidValue() {
        assertEquals(42, AppPrefs.clampExtraDimPercent(42));
    }

    @Test
    public void extraDimClampsAboveMaximum() {
        assertEquals(AppPrefs.MAX_EXTRA_DIM_PERCENT, AppPrefs.clampExtraDimPercent(100));
    }
}
