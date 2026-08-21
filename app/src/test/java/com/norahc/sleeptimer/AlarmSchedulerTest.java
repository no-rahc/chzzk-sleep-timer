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

    @Test
    public void warningTriggerIsTenMinutesBeforeDailyTrigger() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 22, 0);
        long daily = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long warning = AlarmScheduler.calculateWarningTrigger(now, daily);

        Calendar result = calendar(warning);
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, result.get(Calendar.MINUTE));
    }

    @Test
    public void warningTriggerShowsImmediatelyWhenLessThanTenMinutesRemain() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 23, 25);
        long daily = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long warning = AlarmScheduler.calculateWarningTrigger(now, daily);

        assertEquals(now + 1_000L, warning);
    }

    @Test
    public void extensionAddsOnlyRequestedMinutesToCurrentOccurrence() {
        long current = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long extended = AlarmScheduler.calculateExtendedTrigger(current, 20);

        Calendar result = calendar(extended);
        assertEquals(21, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, result.get(Calendar.MINUTE));
    }

    @Test
    public void afterExtendedOccurrenceNextDailyReturnsToBaseTime() {
        long afterExtendedRun = timestamp(2026, Calendar.AUGUST, 21, 23, 50);
        long next = AlarmScheduler.calculateNextDailyTrigger(afterExtendedRun, 23, 30);

        Calendar result = calendar(next);
        assertEquals(22, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
    }

    @Test
    public void effectiveTimerUsesOneShotWhenItEndsFirst() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 22, 0);
        long daily = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long oneShot = timestamp(2026, Calendar.AUGUST, 21, 22, 45);

        assertEquals(
                AlarmScheduler.TIMER_SOURCE_ONE_SHOT,
                AlarmScheduler.selectNextActiveTimerSource(now, daily, oneShot)
        );
        assertEquals(
                oneShot,
                AlarmScheduler.selectNextActiveTimerTrigger(now, daily, oneShot)
        );
    }

    @Test
    public void effectiveTimerUsesDailyWhenItEndsFirst() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 22, 0);
        long daily = timestamp(2026, Calendar.AUGUST, 21, 22, 30);
        long oneShot = timestamp(2026, Calendar.AUGUST, 21, 23, 0);

        assertEquals(
                AlarmScheduler.TIMER_SOURCE_DAILY,
                AlarmScheduler.selectNextActiveTimerSource(now, daily, oneShot)
        );
        assertEquals(
                daily,
                AlarmScheduler.selectNextActiveTimerTrigger(now, daily, oneShot)
        );
    }

    @Test
    public void effectiveTimerIgnoresExpiredOneShot() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 22, 0);
        long daily = timestamp(2026, Calendar.AUGUST, 21, 23, 30);
        long expiredOneShot = timestamp(2026, Calendar.AUGUST, 21, 21, 59);

        assertEquals(
                AlarmScheduler.TIMER_SOURCE_DAILY,
                AlarmScheduler.selectNextActiveTimerSource(now, daily, expiredOneShot)
        );
        assertEquals(
                daily,
                AlarmScheduler.selectNextActiveTimerTrigger(now, daily, expiredOneShot)
        );
    }

    @Test
    public void effectiveTimerReturnsNoneWhenEverythingExpired() {
        long now = timestamp(2026, Calendar.AUGUST, 21, 22, 0);
        long expiredDaily = timestamp(2026, Calendar.AUGUST, 21, 21, 30);
        long expiredOneShot = timestamp(2026, Calendar.AUGUST, 21, 21, 45);

        assertEquals(
                AlarmScheduler.TIMER_SOURCE_NONE,
                AlarmScheduler.selectNextActiveTimerSource(now, expiredDaily, expiredOneShot)
        );
        assertEquals(
                0L,
                AlarmScheduler.selectNextActiveTimerTrigger(now, expiredDaily, expiredOneShot)
        );
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
