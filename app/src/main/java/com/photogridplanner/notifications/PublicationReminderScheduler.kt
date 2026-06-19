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

    fun sync(context: Context, data: PlannerData) {
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
        val scheduledDates = buildSet {
            data.calendarPlans.forEach { plan ->
                val date = runCatching { LocalDate.parse(plan.date) }.getOrNull() ?: return@forEach
                val time = runCatching { LocalTime.parse(plan.recommendedTime) }.getOrNull() ?: return@forEach
                val postCount = data.posts.count { post ->
                    post.kind == PostKind.Image && post.scheduledDate == plan.date
                }
                if (postCount == 0) return@forEach

                val triggerAtMillis = date.atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (triggerAtMillis <= now) return@forEach

                val pendingIntent = reminderIntent(
                    context = context,
                    date = plan.date,
                    postCount = postCount,
                    time = plan.recommendedTime,
                    language = data.language.name,
                )
                schedule(alarmManager, triggerAtMillis, pendingIntent)
                add(plan.date)
            }
        }
        preferences.edit().putStringSet(ScheduledDatesKey, scheduledDates).apply()
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
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
