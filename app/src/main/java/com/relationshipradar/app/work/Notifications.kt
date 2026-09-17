package com.relationshipradar.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.relationshipradar.app.MainActivity
import com.relationshipradar.app.R
import com.relationshipradar.app.engine.PersonRadar

/**
 * Three levels, per the alert-design rule: urgent (individual), attention (roundup),
 * informational (stays in the app, never pushed).
 */
object Notifications {
    const val CHANNEL_INDIVIDUAL = "individual"
    const val CHANNEL_ROUNDUP = "roundup"
    private const val ROUNDUP_ID = 1
    private const val INDIVIDUAL_BASE = 1000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INDIVIDUAL, "Priority people", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One alert per high-priority person who is due"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ROUNDUP, "Daily roundup", NotificationManager.IMPORTANCE_LOW).apply {
                description = "One quiet summary of everyone who is due"
            },
        )
    }

    fun canPost(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context, personId: Long? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            personId?.let { putExtra("personId", it) }
        }
        return PendingIntent.getActivity(context, personId?.toInt() ?: 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun postIndividual(context: Context, radar: PersonRadar) {
        if (!canPost(context)) return
        val days = radar.daysSinceEffort
        val text = if (days == null) "You haven't reached out yet" else "Last effort was $days days ago"
        val notifId = INDIVIDUAL_BASE + radar.person.id.toInt()
        val snoozeIntent = NotificationActionReceiver.createSnoozeIntent(context, radar.person.id, notifId)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(context, CHANNEL_INDIVIDUAL)
            .setSmallIcon(R.drawable.ic_radar)
            .setContentTitle("Reach out to ${radar.person.displayName}")
            .setContentText(text)
            .setContentIntent(openApp(context, radar.person.id))
            .addAction(R.drawable.ic_radar, "Snooze 3d", snoozePendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, n)
    }

    fun postRoundup(context: Context, due: List<PersonRadar>) {
        if (!canPost(context) || due.isEmpty()) return
        val names = due.take(5).joinToString(", ") { it.person.displayName }
        val more = if (due.size > 5) " and ${due.size - 5} more" else ""
        val style = NotificationCompat.InboxStyle().also { s ->
            due.take(7).forEach { r -> s.addLine("${r.person.displayName} · ${r.daysSinceEffort?.let { "$it days" } ?: "never"}") }
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ROUNDUP)
            .setSmallIcon(R.drawable.ic_radar)
            .setContentTitle("${due.size} ${if (due.size == 1) "person is" else "people are"} due")
            .setContentText(names + more)
            .setStyle(style)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ROUNDUP_ID, n)
    }
}
