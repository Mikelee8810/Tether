package com.relationshipradar.app.ui.callinsights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.screens.PersonScreen
import com.relationshipradar.app.ui.theme.RadarTheme
import kotlinx.coroutines.launch

class PersonCallMemoriesDemoActivity : ComponentActivity() {
    private val viewModel by viewModels<RadarViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val repo = RadarApp.from(this@PersonCallMemoriesDemoActivity).repo
            val personId = repo.suggestPersonByName("Marcus")?.id ?: repo.createPerson("Marcus")
            setContent {
                RadarTheme {
                    PersonScreen(viewModel, personId, onLog = {}, onOpenSettings = {}, onBack = {})
                }
            }
        }
    }
}
