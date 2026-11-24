package com.example.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import com.example.model.PrayerType
import com.example.model.SoftAzanTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_AZAN) {
            SoftAzanPlayer.stop()
            val prayerOrdinal = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (prayerOrdinal > 0) {
                NotificationManagerCompat.from(context).cancel(prayerOrdinal)
            }
            return
        }

        if (intent.action != ACTION_PRAYER_ALARM) return

        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: return
        val prayer = PrayerType.entries.find { it.name == prayerName } ?: return
        val toneId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_TONE_ID)
        val tone = SoftAzanTone.fromId(toneId) ?: prayer.defaultAzanTone
        val volume = intent.getFloatExtra(PrayerAlarmScheduler.EXTRA_VOLUME, 0.45f)
        val leadMinutes = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_LEAD_MINUTES, 0)

        val notificationId = 2000 + prayer.ordinal
        postPrayerNotification(context, prayer, tone, leadMinutes, notificationId)

        SoftAzanPlayer.play(
            context = context,
            tone = tone,
            targetVolume = volume,
            label = if (leadMinutes > 0) {
                "${prayer.displayName} in $leadMinutes min"
            } else {
                "${prayer.displayName} Prayer Time"
            },
            isAlarmStream = true
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context.applicationContext).settingsFlow.first()
                PrayerAlarmScheduler.rescheduleAll(context.applicationContext, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postPrayerNotification(
        context: Context,
        prayer: PrayerType,
        tone: SoftAzanTone,
        leadMinutes: Int,
        notificationId: Int
    ) {
        val channelId = "azan_channel_${tone.id}"
        ensureChannel(context, channelId, tone)

        val openAppIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100,
            Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_STOP_AZAN
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (leadMinutes > 0) {
            "${prayer.displayName} Reminder ($leadMinutes min)"
        } else {
            "${prayer.displayName} Time"
        }
        val body = if (leadMinutes > 0) {
            "${prayer.displayName} prayer begins in $leadMinutes minutes • ${tone.label}"
        } else {
            "It is time for ${prayer.displayName} prayer • ${tone.label}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop Azan", stopIntent)
            .setDeleteIntent(stopIntent)
            .build()

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canNotify) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun ensureChannel(context: Context, channelId: String, tone: SoftAzanTone) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(channelId) != null) return

        val soundUri = Uri.parse("android.resource://${context.packageName}/${tone.rawResId}")
        val channel = NotificationChannel(
            channelId,
            "Azan: ${tone.label}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Soft Azan prayer time alerts"
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.action.PRAYER_ALARM"
        const val ACTION_STOP_AZAN = "com.example.action.STOP_AZAN"
        private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
