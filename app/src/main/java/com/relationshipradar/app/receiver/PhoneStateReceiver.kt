package com.relationshipradar.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.service.CallOverlayService
import com.relationshipradar.app.ui.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Android phone calls.
 * Automatically checks if the caller/callee is a tracked friend in Tether,
 * and launches the floating pre-call HUD overlay if found!
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RadarApp ?: return

        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                when (stateStr) {
                    TelephonyManager.EXTRA_STATE_RINGING, TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (!incomingNumber.isNullOrBlank()) {
                            checkAndShowOverlay(context, app, incomingNumber)
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        CallOverlayService.stop(context)
                    }
                }
            }
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (!outgoingNumber.isNullOrBlank()) {
                    checkAndShowOverlay(context, app, outgoingNumber)
                }
            }
        }
    }

    private fun checkAndShowOverlay(context: Context, app: RadarApp, phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val person = app.repo.findPersonByIdentifier(IdentifierType.PHONE, phoneNumber) ?: return@launch
            val interactions = app.db.interactionDao().forPerson(person.id)
            val lastEffort = interactions.firstOrNull { it.countsTowardTimer }?.timestamp
            val lastEffortStr = lastEffort?.let { "Last connected " + Format.ago(it).lowercase() } ?: "No previous calls"

            CallOverlayService.show(
                context = context,
                name = person.displayName,
                lastEffort = lastEffortStr,
                talkingPoints = person.talkingPoints,
                notes = person.notes
            )
        }
    }
}
