package com.norahc.sleeptimer;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmSchedulerTest {
    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void nextDailyTriggerUsesTodayWhenTargetIsStillAhead() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 21, 10);
        long next = AlarmScheduler.calculateNextDailyTrigger(now, 23, 30);

        Calendar result = calendar(next);
        assertEquals(2026, result.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, result.get(Calendar.MONTH));
        assertEquals(21, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
    }

    @Test
    public void nextDailyTriggerRollsToTomorrowWhenTargetPassed() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 23, 31);
        long next = AlarmScheduler.calculateNextDailyTrigger(now, 23, 30);

        Calendar result = calendar(next);
        assertEquals(2026, result.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, result.get(Calendar.MONTH));
        assertEquals(22, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
    }

    @Test
    public void nextDailyTriggerRollsToTomorrowWhenTargetEqualsNow() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long next = AlarmScheduler.calculateNextDailyTrigger(now, 23, 30);

        Calendar result = calendar(next);
        assertEquals(22, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
    }

    private static long timestamp(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private static Calendar calendar(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar;
    }
}
