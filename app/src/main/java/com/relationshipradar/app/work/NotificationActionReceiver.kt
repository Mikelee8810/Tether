package com.relationshipradar.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.widget.RadarWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Headless BroadcastReceiver handling action buttons directly from the Android notification shade.
 * Enables 1-tap Snooze (3 Days) without needing to open the full app.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val personId = intent.getLongExtra(EXTRA_PERSON_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        when (action) {
            ACTION_SNOOZE_3D -> {
                if (personId != -1L) {
                    val app = context.applicationContext as? RadarApp ?: return
                    val snoozeUntil = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.repo.snooze(personId, snoozeUntil)
                        RadarWidget.refresh(context)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE_3D = "com.relationshipradar.app.action.SNOOZE_3D"
        const val EXTRA_PERSON_ID = "extra_person_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun createSnoozeIntent(context: Context, personId: Long, notificationId: Int): Intent =
            Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = ACTION_SNOOZE_3D
                putExtra(EXTRA_PERSON_ID, personId)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
    }
}
