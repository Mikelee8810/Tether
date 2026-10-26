package com.relationshipradar.app.connectors

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.data.db.IdentifierType
import kotlinx.coroutines.launch

/**
 * Push connector for chat apps. Reads MessagingStyle notifications only.
 *
 * Privacy: the message text is never read, logged, or stored. We only look at
 * who sent each message, when, and whether the sender is the user.
 */
class RadarNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in MessagingApps.known) return
        val extras = sbn.notification.extras ?: return
        if (extras.getString(Notification.EXTRA_TEMPLATE) != Notification.MessagingStyle::class.java.name) return

        val app = RadarApp.from(applicationContext)
        val repo = app.repo
        app.scope.launch {
            if (repo.cursor(CONNECTOR_ID)?.enabled == false) return@launch

            val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            val selfName = extras.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME)?.toString()
                ?: (messagingPerson(extras))?.name?.toString()

            val raw = messageBundles(extras) ?: return@launch
            val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(raw).map { m ->
                val sender = m.senderPerson
                val name = sender?.name?.toString()
                // A null sender means "the user" per the MessagingStyle contract.
                val fromUser = sender == null || (selfName != null && name == selfName)
                Mapping.NotifMessage(name, fromUser, m.timestamp)
            }

            // Dedup window: the same notification is re-posted with the full history every time.
            val since = repo.cursor(CONNECTOR_ID)?.lastScannedAt ?: 0L
            val credits = Mapping.notificationCredits(title, isGroup, messages, since)
            if (credits.isEmpty()) return@launch

            var newest = since
            for (credit in credits) {
                newest = maxOf(newest, credit.timestamp)
                val handle = "$pkg:${credit.senderName}"
                val person = repo.findPersonByIdentifier(IdentifierType.HANDLE, handle)
                if (person == null) {
                    // Uncertain match: ask once, remember the answer. Never silently merge.
                    val suggestion = repo.suggestPersonByName(credit.senderName)?.id
                    repo.notePendingIdentity(IdentifierType.HANDLE, handle, MessagingApps.label(pkg), suggestion, credit.timestamp)
                    continue
                }
                repo.record(Mapping.notificationInteraction(person.id, pkg, credit, isGroup))
            }
            val prev = repo.cursor(CONNECTOR_ID)
            repo.saveCursor(
                com.relationshipradar.app.data.db.ConnectorCursor(
                    CONNECTOR_ID, newest, System.currentTimeMillis(), (prev?.lastRunCount ?: 0) + credits.size, enabled = true,
                ),
            )
        }
    }

    companion object {
        // The typed Bundle parcelable getters are API 33+; fall back to the legacy
        // untyped getters (same extras, same values) on API 31–32.
        private fun messagingPerson(extras: Bundle): android.app.Person? =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, android.app.Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON) as? android.app.Person
            }

        private fun messageBundles(extras: Bundle): Array<Bundle>? =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                    ?.filterIsInstance<Bundle>()?.toTypedArray()
            }

        const val CONNECTOR_ID = "notifications"

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun component(context: Context) = ComponentName(context, RadarNotificationListener::class.java)
    }
}
