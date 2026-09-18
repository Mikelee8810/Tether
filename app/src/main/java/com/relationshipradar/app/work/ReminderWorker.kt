package com.relationshipradar.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.data.db.ReminderBehavior
import com.relationshipradar.app.data.db.ReminderState
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Runs daily. Decides who gets a push today using the backup schedule (day 0/2/4/6, then silent).
 * Snoozed and paused people are skipped. Everything else stays visible in the app.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = RadarApp.from(applicationContext)
        val settings = app.settings.flow.first()
        val stateDao = app.repo.reminderStateDao()
        val now = System.currentTimeMillis()

        val individual = mutableListOf<com.relationshipradar.app.engine.PersonRadar>()
        val roundup = mutableListOf<com.relationshipradar.app.engine.PersonRadar>()

        for (r in app.repo.radarSnapshot()) {
            val dueAt = r.dueAt ?: continue
            if (r.status == RadarStatus.TRACK_ONLY || r.status == RadarStatus.PAUSED || r.status == RadarStatus.SNOOZED) continue
            if (r.status == RadarStatus.GOOD || r.status == RadarStatus.DUE_SOON) continue

            val prev = stateDao.get(r.person.id)
            val sent = if (prev != null && prev.cycleDueAt == dueAt) prev.notificationsSent else 0
            if (!ReminderEngine.shouldNotify(dueAt, sent, now)) continue

            if (r.behavior == ReminderBehavior.INDIVIDUAL && settings.individualAlertsEnabled) individual += r
            else if (settings.roundupEnabled) roundup += r
            else continue
            stateDao.upsert(ReminderState(r.person.id, dueAt, sent + 1, now))
        }

        individual.forEach { Notifications.postIndividual(applicationContext, it) }
        Notifications.postRoundup(applicationContext, roundup)
        CallInsightReminderDelivery.deliverDue(applicationContext, now)
        app.settings.markReminderRun(now)
        com.relationshipradar.app.widget.RadarWidget.refresh(applicationContext)
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "daily_reminders"

    fun ensureScheduled(context: Context, hour: Int = 9) {
        val now = LocalDateTime.now()
        var next = now.with(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
