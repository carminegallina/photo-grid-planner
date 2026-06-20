package com.photogridplanner.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import com.photogridplanner.MainActivity
import com.photogridplanner.R

class PublicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ActionCopyPublicationText) {
            copyPublishingText(context, intent)
            return
        }
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
        intent.getStringExtra(ExtraPublishingText)
            ?.takeIf { it.isNotBlank() }
            ?.let { publishingText ->
                val copyIntent = PendingIntent.getBroadcast(
                    context,
                    intent.getStringExtra(ExtraDate).orEmpty().hashCode() + 1,
                    Intent(context, PublicationReminderReceiver::class.java).apply {
                        action = ActionCopyPublicationText
                        putExtra(ExtraPublishingText, publishingText)
                        putExtra(ExtraLanguage, intent.getStringExtra(ExtraLanguage).orEmpty())
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_launcher_foreground),
                        if (english) "Copy text" else "Copia testo",
                        copyIntent,
                    ).build(),
                )
            }
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

    private fun copyPublishingText(context: Context, intent: Intent) {
        val text = intent.getStringExtra(ExtraPublishingText).orEmpty()
        if (text.isBlank()) return
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val english = intent.getStringExtra(ExtraLanguage) == "English"
        clipboard.setPrimaryClip(ClipData.newPlainText("publication_text", text))
        android.widget.Toast.makeText(
            context,
            if (english) "Text copied" else "Testo copiato",
            android.widget.Toast.LENGTH_SHORT,
        ).show()
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
        const val ActionCopyPublicationText = "com.photogridplanner.ACTION_COPY_PUBLICATION_TEXT"
        const val ExtraDate = "date"
        const val ExtraPostCount = "post_count"
        const val ExtraTime = "time"
        const val ExtraLanguage = "language"
        const val ExtraPreviewPath = "preview_path"
        const val ExtraPublishingText = "publishing_text"
        private const val ChannelId = "publication_reminders"
    }
}
