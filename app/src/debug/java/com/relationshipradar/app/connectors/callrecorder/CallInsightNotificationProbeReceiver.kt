package com.relationshipradar.app.connectors.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.work.CallInsightReminderDelivery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class CallInsightNotificationProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = RadarApp.from(context).repo
                val personId = repo.suggestPersonByName("Marcus")?.id ?: repo.createPerson("Marcus")
                val runId = intent?.getStringExtra("runId") ?: "001"
                val candidateId = "due-follow-up-$runId"
                repo.acceptCallInsight(
                    personId, "notification-probe-$runId", candidateId, InsightType.FOLLOW_UP.name,
                    "Ask how the interview went", "Remember to ask how it went next week.", 742_000, LocalDate.now(),
                )
                val first = CallInsightReminderDelivery.deliverDue(context)
                val second = CallInsightReminderDelivery.deliverDue(context)
                val row = repo.callInsightsForPerson(personId).first { it.candidateId == candidateId }
                Log.i("InsightNotifyProbe", "success first=$first second=$second notified=${row.notifiedAt != null}")
            } catch (error: Throwable) {
                Log.e("InsightNotifyProbe", "failure", error)
            } finally {
                pending.finish()
            }
        }
    }
}
