package com.relationshipradar.app.ui.callinsights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.screens.DashboardScreen
import com.relationshipradar.app.ui.theme.RadarTheme

class DashboardCallFollowUpsDemoActivity : ComponentActivity() {
    private val viewModel by viewModels<RadarViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadarTheme {
                AmbientGlassCanvas {
                    DashboardScreen(viewModel, onOpenPerson = {}, onOpenNewPeople = {}, onOpenWho = {}, onOpenSettings = {})
                }
            }
        }
    }
}
