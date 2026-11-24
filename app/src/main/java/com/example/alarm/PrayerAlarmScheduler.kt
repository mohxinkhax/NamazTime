package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.PrayerTimeCalculator
import com.example.model.AppSettings
import com.example.model.PrayerType

object PrayerAlarmScheduler {

    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_TONE_ID = "extra_tone_id"
    const val EXTRA_VOLUME = "extra_volume"
    const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
    private const val BASE_REQUEST_CODE = 4100

    fun rescheduleAll(context: Context, settings: AppSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        PrayerType.entries.forEach { prayer ->
            val adj = settings.adjustments[prayer]
            val requestCode = BASE_REQUEST_CODE + prayer.ordinal

            val cancelIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            }
            val existing = PendingIntent.getBroadcast(
                context,
                requestCode,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(existing)

            if (adj?.alarmEnabled != false) {
                val triggerAtMillis = PrayerTimeCalculator.computeUpcomingAlarmMillis(
                    prayerType = prayer,
                    nowMillis = now,
                    settings = settings
                )
                val tone = adj?.azanTone ?: settings.defaultAzanTone
                val volume = adj?.customVolume ?: settings.softAzanVolume
                val leadMinutes = adj?.reminderBeforeMinutes ?: 0

                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                    putExtra(EXTRA_PRAYER_NAME, prayer.name)
                    putExtra(EXTRA_TONE_ID, tone.id)
                    putExtra(EXTRA_VOLUME, volume)
                    putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
