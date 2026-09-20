package com.relationshipradar.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.data.db.Category
import com.relationshipradar.app.data.db.Interaction
import com.relationshipradar.app.data.db.InteractionType
import com.relationshipradar.app.data.db.PendingIdentity
import com.relationshipradar.app.data.db.Person
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.shizuku.ShellCommands
import com.relationshipradar.app.shizuku.ShizukuBridge
import com.relationshipradar.app.work.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One view model for the whole app; screens are thin. */
class RadarViewModel(app: Application) : AndroidViewModel(app) {
    private val radarApp = RadarApp.from(app)
    val repo = radarApp.repo
    val settings = radarApp.settings
    val contacts = radarApp.contacts

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val radar: StateFlow<List<PersonRadar>> = repo.observeRadar().map { list ->
        list.sortedWith(compareByDescending<PersonRadar> { statusRank(it.status) }.thenBy { it.person.displayName.lowercase() })
    }.state(emptyList())

    val categories = repo.observeCategories().state(emptyList())
    val uncategorized = repo.observeUncategorized().state(emptyList())
    val archived = repo.observeArchived().state(emptyList())
    val appSettings = settings.flow.state(com.relationshipradar.app.data.repo.AppSettings())
    val cursors = repo.observeCursors().state(emptyList())
    val pendingIdentities = repo.observePendingIdentities().state(emptyList())
    val upcomingCallInsights = repo.observeUpcomingCallInsights().state(emptyList())

    fun person(id: Long) = repo.observePerson(id)
    fun interactions(id: Long) = repo.observeInteractions(id)
    fun identifiers(id: Long) = repo.observeIdentifiers(id)
    fun callInsights(id: Long) = repo.observeAcceptedCallInsights(id)

    private fun statusRank(s: RadarStatus) = when (s) {
        RadarStatus.VERY_OVERDUE -> 6; RadarStatus.OVERDUE -> 5; RadarStatus.DUE_SOON -> 4
        RadarStatus.GOOD -> 3; RadarStatus.SNOOZED -> 2; RadarStatus.PAUSED -> 1; RadarStatus.TRACK_ONLY -> 0
    }

    fun logManual(personId: Long, type: InteractionType, timestamp: Long, approximate: Boolean, note: String) =
        viewModelScope.launch { repo.logManual(personId, type, timestamp, approximate, note); com.relationshipradar.app.widget.RadarWidget.refresh(getApplication()) }

    fun createPerson(name: String, categoryId: Long?, onDone: (Long) -> Unit = {}) =
        viewModelScope.launch { onDone(repo.createPerson(name, categoryId)) }

    fun createPersonWithNotes(name: String, categoryId: Long?, notes: String, onDone: (Long) -> Unit = {}) =
        viewModelScope.launch {
            val id = repo.createPerson(name, categoryId)
            if (notes.isNotBlank()) {
                repo.getPerson(id)?.let { repo.updatePerson(it.copy(notes = notes)) }
            }
            onDone(id)
        }

    fun updatePerson(p: Person) = viewModelScope.launch { repo.updatePerson(p) }
    fun setCategory(personId: Long, categoryId: Long?) = viewModelScope.launch { repo.setCategory(personId, categoryId) }
    fun dismissPrompt(personId: Long) = viewModelScope.launch { repo.bumpPromptCount(personId) }
    fun snooze(personId: Long, until: Long?) = viewModelScope.launch { repo.snooze(personId, until) }
    fun pause(personId: Long, until: Long?) = viewModelScope.launch { repo.pause(personId, until) }
    fun archive(personId: Long) = viewModelScope.launch { repo.archive(personId) }
    fun restore(personId: Long) = viewModelScope.launch { repo.restore(personId) }
    fun deleteInteraction(i: Interaction) = viewModelScope.launch { repo.deleteInteraction(i) }

    fun addCategory(c: Category) = viewModelScope.launch { repo.addCategory(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { repo.updateCategory(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c) }

    fun syncContacts(onDone: (String) -> Unit) = viewModelScope.launch {
        val message = runContactSyncSafely(
            sync = contacts::sync,
            markSynced = { settings.setLastSync(System.currentTimeMillis()) },
        )
        onDone(message)
    }

    fun setRoundupHour(h: Int) = viewModelScope.launch { settings.setRoundupHour(h); ReminderScheduler.ensureScheduled(getApplication(), h) }
    fun setRoundupEnabled(v: Boolean) = viewModelScope.launch { settings.setRoundupEnabled(v) }
    fun setIndividualAlerts(v: Boolean) = viewModelScope.launch { settings.setIndividualAlerts(v) }
    fun hasPermission(perm: String) =
        androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), perm) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun setConnectorEnabled(id: String, enabled: Boolean) = viewModelScope.launch { repo.setConnectorEnabled(id, enabled) }

    fun scanNow(onDone: (String) -> Unit) = viewModelScope.launch {
        val results = radarApp.connectors.runAll()
        onDone(results.joinToString(" · ") { r -> "${r.connectorId}: ${r.skipped ?: "${r.imported} new"}" })
    }

    fun resolvePending(p: PendingIdentity, personId: Long) = viewModelScope.launch { repo.resolvePendingIdentity(p, personId) }
    fun resolvePendingAsNew(p: PendingIdentity, name: String) = viewModelScope.launch { repo.resolvePendingAsNewPerson(p, name) }
    fun ignorePending(p: PendingIdentity) = viewModelScope.launch { repo.ignorePendingIdentity(p) }

    fun health(): com.relationshipradar.app.work.Health.Report {
        val s = appSettings.value
        return com.relationshipradar.app.work.Health.check(getApplication(), s.lastReminderRunAt, s.lastScanRunAt)
    }

    /** Runs allow-listed Shizuku commands; reports one line per command. */
    fun runShizuku(commands: List<ShellCommands.Command>, onDone: (List<String>) -> Unit) = viewModelScope.launch {
        val lines = commands.map { c ->
            when (val r = ShizukuBridge.run(c)) {
                is ShizukuBridge.Outcome.Ok -> "✓ ${c.label}"
                is ShizukuBridge.Outcome.Err -> "✗ ${c.label}: ${r.text.take(80)}"
            }
        }
        onDone(lines)
    }

    fun finishOnboarding() = viewModelScope.launch { settings.setOnboardingDone() }

    fun setAiProvider(provider: String) = viewModelScope.launch { settings.setAiProvider(provider) }
    fun setGeminiApiKey(key: String) = viewModelScope.launch { settings.setGeminiApiKey(key) }
    fun setGroqApiKey(key: String) = viewModelScope.launch { settings.setGroqApiKey(key) }
    fun setOpenRouterApiKey(key: String) = viewModelScope.launch { settings.setOpenRouterApiKey(key) }
    fun setGrokApiKey(key: String) = viewModelScope.launch { settings.setGrokApiKey(key) }
    fun setOpenAiApiKey(key: String) = viewModelScope.launch { settings.setOpenAiApiKey(key) }
    fun setAnthropicApiKey(key: String) = viewModelScope.launch { settings.setAnthropicApiKey(key) }
    fun setCustomApi(baseUrl: String, key: String, model: String) = viewModelScope.launch { settings.setCustomApi(baseUrl, key, model) }

    fun recordManualCatchUp(personId: Long, note: String = "Caught up today") = viewModelScope.launch {
        val interaction = com.relationshipradar.app.data.db.Interaction(
            personId = personId,
            source = com.relationshipradar.app.data.db.InteractionSource.MANUAL,
            type = com.relationshipradar.app.data.db.InteractionType.OTHER,
            timestamp = System.currentTimeMillis(),
            direction = com.relationshipradar.app.data.db.Direction.OUTGOING,
            countsTowardTimer = true,
            approximate = false,
            note = note,
            externalId = java.util.UUID.randomUUID().toString()
        )
        repo.record(interaction)
        settings.recordCatchUpGamification(sparks = 10)
    }

    fun awardSparks(sparks: Int) = viewModelScope.launch {
        settings.addSparks(sparks)
    }
}

private typealias Flow<T> = kotlinx.coroutines.flow.Flow<T>
