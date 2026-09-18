package com.relationshipradar.app.work

import android.content.Context
import com.relationshipradar.app.RadarApp

object CallInsightReminderDelivery {
    suspend fun deliverDue(context: Context, now: Long = System.currentTimeMillis()): Int {
        val repo = RadarApp.from(context).repo
        var delivered = 0
        for (insight in repo.dueCallFollowUps(now)) {
            val person = repo.getPerson(insight.personId) ?: continue
            if (Notifications.postCallFollowUp(context, insight, person.displayName)) {
                repo.markCallFollowUpNotified(insight.id, now)
                delivered++
            }
        }
        return delivered
    }
}
