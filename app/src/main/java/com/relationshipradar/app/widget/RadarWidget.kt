package com.relationshipradar.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.relationshipradar.app.MainActivity
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine.needsAttention
import com.relationshipradar.app.ui.Format

/**
 * Home-screen glance: who needs a nudge, worst first. Tap a row → that person.
 * Tap the header → "I saw someone". Same colours as the app so meaning carries over.
 */
class RadarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val radar = RadarApp.from(context).repo.radarSnapshot()
            .filter { it.status.needsAttention }
            .sortedByDescending { rank(it.status) }
        provideContent { GlanceTheme { Content(radar) } }
    }

    private fun rank(s: RadarStatus) = when (s) { RadarStatus.VERY_OVERDUE -> 3; RadarStatus.OVERDUE -> 2; RadarStatus.DUE_SOON -> 1; else -> 0 }

    @Composable
    private fun Content(radar: List<PersonRadar>) {
        val context = androidx.glance.LocalContext.current
        Column(
            GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.widgetBackground).cornerRadius(18.dp).padding(14.dp),
        ) {
            Row(
                GlanceModifier.fillMaxWidth().clickable(actionStartActivity(Intent(context, MainActivity::class.java).putExtra("openLog", true))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tether", style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface))
                Spacer(GlanceModifier.defaultWeight())
                Text("+ Quick Log", style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (radar.isEmpty()) {
                Text("In touch with everyone", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            } else {
                radar.take(5).forEach { r ->
                    Row(
                        GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable(actionStartActivity(Intent(context, MainActivity::class.java).putExtra("personId", r.person.id))),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            GlanceModifier.size(width = 4.dp, height = 14.dp)
                                .background(when (r.status) {
                                    RadarStatus.DUE_SOON -> GlanceTheme.colors.tertiary
                                    RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> GlanceTheme.colors.error
                                    else -> GlanceTheme.colors.primary
                                })
                                .cornerRadius(2.dp)
                        ) {}
                        Spacer(GlanceModifier.width(8.dp))
                        Text(r.person.displayName, style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text(Format.ago(r.lastEffortAt), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    }
                }
                if (radar.size > 5) Text("+${radar.size - 5} more", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            }
        }
    }

    companion object {
        /** Glance composables have no LocalContext for intents; the receiver sets this. */
        internal lateinit var LocalCtx: Context

        suspend fun refresh(context: Context) {
            LocalCtx = context.applicationContext
            RadarWidget().updateAll(context)
        }
    }
}

class RadarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget get() = RadarWidget()
    override fun onReceive(context: Context, intent: Intent) {
        RadarWidget.LocalCtx = context.applicationContext
        super.onReceive(context, intent)
    }
}

