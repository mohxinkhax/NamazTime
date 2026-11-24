package com.example

import com.example.domain.HijriCalendarHelper
import com.example.domain.PrayerTimeCalculator
import com.example.model.AppSettings
import com.example.model.PrayerAdjustment
import com.example.model.PrayerType
import com.example.model.SoftAzanTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {

    @Test
    fun hijriConversion_matchesExpectedDate() {
        val hijri = HijriCalendarHelper.gregorianToHijri(2026, 11, 24)
        assertEquals(1448, hijri.year)
        assertEquals("Jumada II", hijri.monthName)
        assertTrue(hijri.day in 13..15)
    }

    @Test
    fun prayerSchedule_ordersPrayersAndComputesNextCountdown() {
        val tz = TimeZone.getTimeZone("Europe/London")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 24, 6, 23, 42)
            set(Calendar.MILLISECOND, 0)
        }
        val settings = AppSettings()
        val snapshot = PrayerTimeCalculator.buildSchedule(cal.timeInMillis, settings)

        assertEquals(5, snapshot.entries.size)
        for (i in 0 until snapshot.entries.size - 1) {
            assertTrue(snapshot.entries[i].epochMillis < snapshot.entries[i + 1].epochMillis)
        }
        assertEquals(PrayerType.FAJR, snapshot.currentPrayer)
        assertEquals(PrayerType.DHUHR, snapshot.nextPrayer)
    }

    @Test
    fun customPrayerTimeAndLeadAlarm_applyProperly() {
        val tz = TimeZone.getTimeZone("Europe/London")
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 24, 5, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val base = AppSettings()
        val customAdjustments = base.adjustments.toMutableMap().apply {
            put(
                PrayerType.FAJR,
                PrayerAdjustment(
                    alarmEnabled = true,
                    customHour = 6,
                    customMinute = 15,
                    azanTone = SoftAzanTone.DAWN_WHISPER,
                    reminderBeforeMinutes = 10
                )
            )
        }
        val settings = base.copy(adjustments = customAdjustments)
        val alarmMillis = PrayerTimeCalculator.computeUpcomingAlarmMillis(
            PrayerType.FAJR,
            cal.timeInMillis,
            settings
        )
        val alarmCal = Calendar.getInstance(tz).apply { timeInMillis = alarmMillis }
        assertEquals(6, alarmCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(5, alarmCal.get(Calendar.MINUTE))
    }
}
