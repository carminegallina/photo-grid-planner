package com.niwlayr.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niwlayr.app.data.PlannerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Restores future publication reminders after a reboot or app package update. */
class PublicationReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supportedAction = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!supportedAction) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val data = PlannerRepository(appContext).data.first()
                PublicationReminderScheduler.sync(appContext, data)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
