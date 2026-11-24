package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

data class HijriDateInfo(
    val day: Int,
    val monthName: String,
    val year: Int
) {
    fun formatted(): String = "$day $monthName, $year AH"
}

object HijriCalendarHelper {

    private val HIJRI_MONTHS = listOf(
        "Muharram",
        "Safar",
        "Rabi' I",
        "Rabi' II",
        "Jumada I",
        "Jumada II",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    fun formatCombinedDateBanner(
        epochMillis: Long,
        timeZoneId: String,
        hijriOffsetDays: Int
    ): String {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val gregorianFmt = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).apply {
            timeZone = tz
        }
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = epochMillis
            add(Calendar.DAY_OF_YEAR, hijriOffsetDays)
        }
        val hijri = gregorianToHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        return "${gregorianFmt.format(Date(epochMillis))}  |  ${hijri.formatted()}"
    }

    /**
     * Kuwaiti tabular Islamic calendar algorithm (works deterministically across all API levels).
     */
    fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDateInfo {
        var m = month
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0).toInt()
        var b = 2 - a + floor(a / 4.0).toInt()
        if (y < 1583) b = 0
        if (y == 1582) {
            if (m > 10) b = -10
            if (m == 10) {
                b = 0
                if (day > 4) b = -10
            }
        }
        val jd = floor(365.25 * (y + 4716)).toInt() +
            floor(30.6001 * (m + 1)).toInt() + day + b - 1524

        val iYear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0

        var z = jd - epochAstro
        val cyc = floor(z / 10631.0).toInt()
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iYear).toInt()
        val iy = 30 * cyc + j
        z -= floor(j * iYear + shift1)
        var im = floor((z + 28.5001) / 29.5).toInt()
        if (im == 13) im = 12
        val id = (z - floor(29.5001 * im - 29.0)).toInt()

        val monthName = HIJRI_MONTHS[(im - 1).coerceIn(0, 11)]
        return HijriDateInfo(day = id, monthName = monthName, year = iy)
    }
}
