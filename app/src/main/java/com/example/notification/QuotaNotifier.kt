package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class QuotaNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "alqanas_quota_alerts"
        const val ADMIN_CHANNEL_ID = "alqanas_admin_broadcasts"
        private const val NOTIF_ID_30 = 3001
        private const val NOTIF_ID_15 = 1501
        private const val NOTIF_ID_TEST = 9001
    }

    init {
        ensureChannel()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun notifyQuota30(summary: String) {
        sendNotification(
            id = NOTIF_ID_30,
            title = "تنبيه استهلاك (30%) - شبكة القناص",
            body = "وصل استهلاكك إلى 70% وتبقى لديك 30% أو أقل ($summary).",
            highPriority = true
        )
    }

    fun notifyQuota15(summary: String) {
        sendNotification(
            id = NOTIF_ID_15,
            title = "تحذير حرج (15%) - شبكة القناص",
            body = "قاربت باقتك على النفاد تماماً ($summary). جهّز كرتاً جديداً لتفادي انقطاع الاتصال.",
            highPriority = true
        )
    }

    fun notifyAdminBroadcast(notifId: Int, title: String, body: String, highPriority: Boolean) {
        sendNotification(
            id = 50000 + notifId,
            title = title,
            body = body,
            highPriority = highPriority
        )
    }

    fun notifyTestOrEnabled(isActiveSession: Boolean, summary: String) {
        val body = if (isActiveSession && summary.isNotBlank()) {
            "حالة باقتك الحالية: $summary — الإشعارات جاهزة للعمل عند 30% و 15%."
        } else {
            "سيصلك تنبيه تلقائي عند تبقي 30% وتنبيه حرج عند تبقي 15% من رصيدك أو وقتك."
        }
        sendNotification(
            id = NOTIF_ID_TEST,
            title = "شبكة القناص: الإشعارات مفعّلة",
            body = body,
            highPriority = true
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notif_channel_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 400L, 120L, 400L, 120L, 600L)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun sendNotification(
        id: Int,
        title: String,
        body: String,
        highPriority: Boolean
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0L, 400L, 120L, 400L, 120L, 600L))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }
}
