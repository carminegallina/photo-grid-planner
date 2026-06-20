package com.photogridplanner.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import com.photogridplanner.MainActivity
import com.photogridplanner.R

class PublicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val count = intent.getIntExtra(ExtraPostCount, 0).coerceAtLeast(1)
        val time = intent.getStringExtra(ExtraTime).orEmpty()
        val english = intent.getStringExtra(ExtraLanguage) == "English"
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager, english)

        val title = if (english) "Publishing reminder" else "Promemoria pubblicazione"
        val body = if (english) {
            "$count ${if (count == 1) "post is" else "posts are"} scheduled for $time."
        } else {
            "$count ${if (count == 1) "post pianificato" else "post pianificati"} per le $time."
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            intent.getStringExtra(ExtraDate).orEmpty().hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        val preview = intent.getStringExtra(ExtraPreviewPath)
            ?.takeIf { it.isNotBlank() }
            ?.let(BitmapFactory::decodeFile)
        if (preview != null) {
            // The renderer adds padding around the square grid, so BigPicture can show the full mosaic.
            builder.setLargeIcon(preview)
            builder.setStyle(
                Notification.BigPictureStyle()
                    .bigPicture(preview)
                    .bigLargeIcon(null as android.graphics.Bitmap?),
            )
        } else {
            builder.setStyle(Notification.BigTextStyle().bigText(body))
        }
        val notification = builder.build()
        manager.notify(intent.getStringExtra(ExtraDate).orEmpty().hashCode(), notification)
    }

    private fun ensureChannel(manager: NotificationManager, english: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            if (english) "Publishing reminders" else "Promemoria pubblicazione",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = if (english) {
                "Reminders for scheduled posts"
            } else {
                "Promemoria per i post pianificati"
            }
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ActionReminder = "com.photogridplanner.ACTION_PUBLICATION_REMINDER"
        const val ExtraDate = "date"
        const val ExtraPostCount = "post_count"
        const val ExtraTime = "time"
        const val ExtraLanguage = "language"
        const val ExtraPreviewPath = "preview_path"
        private const val ChannelId = "publication_reminders"
    }
}
