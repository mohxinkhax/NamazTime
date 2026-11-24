package com.example.domain

import com.example.model.AppSettings
import com.example.model.AsrJuristicMethod
import com.example.model.CalculationMethod
import com.example.model.CityLocation
import com.example.model.CountdownParts
import com.example.model.PrayerTimeEntry
import com.example.model.PrayerType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class PrayerScheduleSnapshot(
    val entries: List<PrayerTimeEntry>,
    val currentPrayer: PrayerType?,
    val nextPrayer: PrayerType,
    val nextPrayerEpochMillis: Long,
    val countdown: CountdownParts,
    val sunriseFormatted: String
)

object PrayerTimeCalculator {

    fun buildSchedule(
        nowMillis: Long,
        settings: AppSettings
    ): PrayerScheduleSnapshot {
        val tz = TimeZone.getTimeZone(settings.location.timeZoneId)
        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }

        val todayTimes = computeDayTimes(
            year = todayCal.get(Calendar.YEAR),
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH),
            location = settings.location,
            method = settings.calculationMethod,
            asrMethod = settings.asrMethod,
            tz = tz,
            settings = settings
        )

        val tomorrowCal = (todayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowTimes = computeDayTimes(
            year = tomorrowCal.get(Calendar.YEAR),
            month = tomorrowCal.get(Calendar.MONTH) + 1,
            day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
            location = settings.location,
            method = settings.calculationMethod,
            asrMethod = settings.asrMethod,
            tz = tz,
            settings = settings
        )

        val orderedTypes = PrayerType.entries
        val nextToday = orderedTypes.firstOrNull { (todayTimes.prayers[it] ?: 0L) > nowMillis }

        val nextPrayerType: PrayerType
        val nextPrayerMillis: Long
        val currentPrayerType: PrayerType?

        if (nextToday != null) {
            nextPrayerType = nextToday
            nextPrayerMillis = todayTimes.prayers.getValue(nextToday)
            val nextIdx = orderedTypes.indexOf(nextToday)
            currentPrayerType = if (nextIdx > 0) orderedTypes[nextIdx - 1] else PrayerType.ISHA
        } else {
            // After Isha -> next prayer is tomorrow's Fajr
            nextPrayerType = PrayerType.FAJR
            nextPrayerMillis = tomorrowTimes.prayers.getValue(PrayerType.FAJR)
            currentPrayerType = PrayerType.ISHA
        }

        val timePattern = if (settings.use24HourFormat) "HH:mm" else "hh:mm a"
        val formatter = SimpleDateFormat(timePattern, Locale.getDefault()).apply {
            timeZone = tz
        }
        val compactFormatter = SimpleDateFormat(
            if (settings.use24HourFormat) "HH:mm" else "hh:mm",
            Locale.getDefault()
        ).apply {
            timeZone = tz
        }

        val entries = orderedTypes.map { type ->
            val millis = todayTimes.prayers.getValue(type)
            val adj = settings.adjustments[type]
            val effectiveTone = adj?.azanTone ?: settings.defaultAzanTone
            val isCurrent = type == currentPrayerType
            val isNext = type == nextPrayerType
            val buttonColor = if (settings.dynamicPrayerColors) {
                type.defaultColor
            } else {
                androidx.compose.ui.graphics.Color(settings.themePreset.accentHex)
            }

            PrayerTimeEntry(
                type = type,
                epochMillis = millis,
                formattedTime = compactFormatter.format(Date(millis)),
                isCurrent = isCurrent,
                isNext = isNext,
                isCustomTime = adj?.isCustomFixedTime == true,
                offsetMinutes = adj?.offsetMinutes ?: 0,
                alarmEnabled = adj?.alarmEnabled ?: true,
                effectiveTone = effectiveTone,
                reminderBeforeMinutes = adj?.reminderBeforeMinutes ?: 0,
                buttonColor = buttonColor
            )
        }

        val remainingSec = ((nextPrayerMillis - nowMillis).coerceAtLeast(0L) / 1000L)
        val hours = (remainingSec / 3600L).toInt()
        val minutes = ((remainingSec % 3600L) / 60L).toInt()
        val seconds = (remainingSec % 60L).toInt()

        return PrayerScheduleSnapshot(
            entries = entries,
            currentPrayer = currentPrayerType,
            nextPrayer = nextPrayerType,
            nextPrayerEpochMillis = nextPrayerMillis,
            countdown = CountdownParts(
                hours = hours.toString().padStart(2, '0'),
                minutes = minutes.toString().padStart(2, '0'),
                seconds = seconds.toString().padStart(2, '0')
            ),
            sunriseFormatted = formatter.format(Date(todayTimes.sunriseMillis))
        )
    }

    fun computeUpcomingAlarmMillis(
        prayerType: PrayerType,
        nowMillis: Long,
        settings: AppSettings
    ): Long {
        val tz = TimeZone.getTimeZone(settings.location.timeZoneId)
        val todayCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val leadMinutes = settings.adjustments[prayerType]?.reminderBeforeMinutes ?: 0
        val leadMillis = leadMinutes * 60_000L

        val todayTimes = computeDayTimes(
            year = todayCal.get(Calendar.YEAR),
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH),
            location = settings.location,
            method = settings.calculationMethod,
            asrMethod = settings.asrMethod,
            tz = tz,
            settings = settings
        )
        val todayTarget = todayTimes.prayers.getValue(prayerType) - leadMillis
        if (todayTarget > nowMillis + 1_000L) {
            return todayTarget
        }

        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowTimes = computeDayTimes(
            year = tomorrowCal.get(Calendar.YEAR),
            month = tomorrowCal.get(Calendar.MONTH) + 1,
            day = tomorrowCal.get(Calendar.DAY_OF_MONTH),
            location = settings.location,
            method = settings.calculationMethod,
            asrMethod = settings.asrMethod,
            tz = tz,
            settings = settings
        )
        return tomorrowTimes.prayers.getValue(prayerType) - leadMillis
    }

    fun getCalculatedAutoTime(
        prayerType: PrayerType,
        nowMillis: Long,
        settings: AppSettings
    ): Pair<Int, Int> {
        val tz = TimeZone.getTimeZone(settings.location.timeZoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val autoSettings = settings.copy(
            adjustments = PrayerType.entries.associateWith {
                com.example.model.PrayerAdjustment()
            }
        )
        val dayTimes = computeDayTimes(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            location = settings.location,
            method = settings.calculationMethod,
            asrMethod = settings.asrMethod,
            tz = tz,
            settings = autoSettings
        )
        val targetCal = Calendar.getInstance(tz).apply {
            timeInMillis = dayTimes.prayers.getValue(prayerType)
        }
        return targetCal.get(Calendar.HOUR_OF_DAY) to targetCal.get(Calendar.MINUTE)
    }

    private data class DayPrayerTimes(
        val prayers: Map<PrayerType, Long>,
        val sunriseMillis: Long
    )

    private fun computeDayTimes(
        year: Int,
        month: Int,
        day: Int,
        location: CityLocation,
        method: CalculationMethod,
        asrMethod: AsrJuristicMethod,
        tz: TimeZone,
        settings: AppSettings
    ): DayPrayerTimes {
        val dayCal = Calendar.getInstance(tz).apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tzOffsetHours = tz.getOffset(dayCal.timeInMillis) / 3_600_000.0
        val jd = julianDate(year, month, day) - location.longitude / (15.0 * 24.0)

        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = asinDeg(sinDeg(e) * sinDeg(l))

        val midDay = fixHour(12.0 - eqt)

        fun sunAngleTime(angleDeg: Double, ccw: Boolean): Double {
            val cosHa = (-sinDeg(angleDeg) - sinDeg(location.latitude) * sinDeg(decl)) /
                (cosDeg(location.latitude) * cosDeg(decl))
            val clamped = cosHa.coerceIn(-0.9999, 0.9999)
            val ha = acosDeg(clamped) / 15.0
            return midDay + if (ccw) -ha else ha
        }

        fun asrTime(factor: Int): Double {
            val cotVal = factor + tanDeg(abs(location.latitude - decl))
            val angle = -atanDeg(1.0 / cotVal)
            return sunAngleTime(angle, ccw = false)
        }

        val fajrSolar = sunAngleTime(method.fajrAngle, ccw = true)
        val sunriseSolar = sunAngleTime(0.833, ccw = true)
        val dhuhrSolar = midDay
        val asrSolar = asrTime(asrMethod.shadowFactor)
        val maghribSolar = sunAngleTime(0.833, ccw = false)
        val ishaSolar = if (method.ishaMinutesAfterMaghrib != null) {
            maghribSolar + method.ishaMinutesAfterMaghrib / 60.0
        } else {
            sunAngleTime(method.ishaAngle, ccw = false)
        }

        fun solarToEpoch(solarHour: Double, adjustment: com.example.model.PrayerAdjustment?): Long {
            val cal = Calendar.getInstance(tz).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (adjustment != null && adjustment.customHour != null && adjustment.customMinute != null) {
                cal.set(Calendar.HOUR_OF_DAY, adjustment.customHour)
                cal.set(Calendar.MINUTE, adjustment.customMinute)
                return cal.timeInMillis
            }
            val localHours = fixHour(solarHour + tzOffsetHours - location.longitude / 15.0)
            val totalMinutes = (localHours * 60.0).roundToInt() + (adjustment?.offsetMinutes ?: 0)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.add(Calendar.MINUTE, totalMinutes)
            return cal.timeInMillis
        }

        val prayerMap = mapOf(
            PrayerType.FAJR to solarToEpoch(fajrSolar, settings.adjustments[PrayerType.FAJR]),
            PrayerType.DHUHR to solarToEpoch(dhuhrSolar, settings.adjustments[PrayerType.DHUHR]),
            PrayerType.ASR to solarToEpoch(asrSolar, settings.adjustments[PrayerType.ASR]),
            PrayerType.MAGHRIB to solarToEpoch(maghribSolar, settings.adjustments[PrayerType.MAGHRIB]),
            PrayerType.ISHA to solarToEpoch(ishaSolar, settings.adjustments[PrayerType.ISHA])
        )

        return DayPrayerTimes(
            prayers = prayerMap,
            sunriseMillis = solarToEpoch(sunriseSolar, null)
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        val a = angle - 360.0 * floor(angle / 360.0)
        return if (a < 0) a + 360.0 else a
    }

    private fun fixHour(hour: Double): Double {
        val h = hour - 24.0 * floor(hour / 24.0)
        return if (h < 0) h + 24.0 else h
    }

    private fun sinDeg(deg: Double) = sin(Math.toRadians(deg))
    private fun cosDeg(deg: Double) = cos(Math.toRadians(deg))
    private fun tanDeg(deg: Double) = tan(Math.toRadians(deg))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x))
    private fun acosDeg(x: Double) = Math.toDegrees(acos(x))
    private fun atanDeg(x: Double) = Math.toDegrees(atan(x))
    private fun atan2Deg(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
}
