package com.photogridplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PostKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object PublicationReminderScheduler {
    private const val PreferencesName = "publication_reminders"
    private const val ScheduledDatesKey = "scheduled_dates"

    suspend fun sync(context: Context, data: PlannerData) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        preferences.getStringSet(ScheduledDatesKey, emptySet()).orEmpty().forEach { date ->
            alarmManager.cancel(reminderIntent(context, date, 0, "", ""))
        }

        if (!data.notificationsEnabled) {
            preferences.edit().remove(ScheduledDatesKey).apply()
            return
        }

        val now = System.currentTimeMillis()
        val scheduledPostsByDate = data.posts
            .asSequence()
            .filter { post -> post.kind == PostKind.Image && !post.scheduledDate.isNullOrBlank() }
            .groupBy { post -> post.scheduledDate.orEmpty() }
        val scheduledDates = mutableSetOf<String>()
        scheduledPostsByDate.forEach { (dateKey, scheduledPosts) ->
            val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return@forEach
            val timeText = data.calendarPlanFor(dateKey)
                ?.recommendedTime
                ?.takeIf { it.isNotBlank() }
                ?: defaultReminderTime(date)
            val time = runCatching { LocalTime.parse(timeText) }.getOrElse {
                LocalTime.parse(defaultReminderTime(date))
            }
            val postCount = scheduledPosts.size

            val triggerAtMillis = date.atTime(time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            if (triggerAtMillis <= now) return@forEach

            val previewPath = ScheduledFeedPreviewRenderer
                .render(context = context, posts = data.posts, date = dateKey)
                ?.absolutePath
                .orEmpty()
            val publishingText = scheduledPosts
                .map { it.publishingText }
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n\n---\n\n")
                .take(MaxNotificationCopyLength)
            val pendingIntent = reminderIntent(
                context = context,
                date = dateKey,
                postCount = postCount,
                time = timeText,
                language = data.language.name,
                previewPath = previewPath,
                publishingText = publishingText,
            )
            schedule(alarmManager, triggerAtMillis, pendingIntent)
            scheduledDates += dateKey
        }
        preferences.edit().putStringSet(ScheduledDatesKey, scheduledDates).apply()
    }

    private fun defaultReminderTime(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "18:30"
            2 -> "19:00"
            3 -> "12:30"
            4 -> "19:00"
            5 -> "18:00"
            6 -> "11:00"
            else -> "10:30"
        }
    }

    private fun schedule(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            // No special exact-alarm permission is requested; Android delivers this close to the chosen time.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun reminderIntent(
        context: Context,
        date: String,
        postCount: Int,
        time: String,
        language: String,
        previewPath: String = "",
        publishingText: String = "",
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            date.hashCode(),
            Intent(context, PublicationReminderReceiver::class.java).apply {
                action = PublicationReminderReceiver.ActionReminder
                putExtra(PublicationReminderReceiver.ExtraDate, date)
                putExtra(PublicationReminderReceiver.ExtraPostCount, postCount)
                putExtra(PublicationReminderReceiver.ExtraTime, time)
                putExtra(PublicationReminderReceiver.ExtraLanguage, language)
                putExtra(PublicationReminderReceiver.ExtraPreviewPath, previewPath)
                putExtra(PublicationReminderReceiver.ExtraPublishingText, publishingText)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val MaxNotificationCopyLength = 8_000
}
