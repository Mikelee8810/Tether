package com.relationshipradar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.ui.theme.StatusColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.relationshipradar.app.ui.screens.CategoriesScreen
import com.relationshipradar.app.ui.screens.ConnectorsScreen
import com.relationshipradar.app.ui.screens.DashboardScreen
import com.relationshipradar.app.ui.screens.HealthScreen
import com.relationshipradar.app.ui.screens.NewPeopleScreen
import com.relationshipradar.app.ui.screens.OnboardingScreen
import com.relationshipradar.app.ui.screens.PersonScreen
import com.relationshipradar.app.ui.screens.PersonSettingsScreen
import com.relationshipradar.app.ui.screens.QuickLogScreen
import com.relationshipradar.app.ui.screens.SettingsScreen
import com.relationshipradar.app.ui.screens.WhoIsThisScreen
import com.relationshipradar.app.ui.callinsights.CallRecordingInsightsScreen

object Routes {
    const val RADAR = "radar"
    const val NEW_PEOPLE = "new_people"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val CONNECTORS = "connectors"
    const val WHO = "who"
    const val HEALTH = "health"
    const val CALL_INSIGHTS = "call_insights"
    const val PERSON = "person/{id}"
    const val PERSON_SETTINGS = "person/{id}/settings"
    const val LOG = "log?personId={personId}"
    fun person(id: Long) = "person/$id"
    fun personSettings(id: Long) = "person/$id/settings"
    fun log(personId: Long? = null) = if (personId == null) "log" else "log?personId=$personId"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadarNavHost(vm: RadarViewModel, openPersonId: Long?, openLog: Boolean = false) {
    val nav = rememberNavController()
    val settings by vm.appSettings.collectAsStateWithLifecycle()
    var onboardingSeen by remember { mutableStateOf(false) }
    if (!settings.onboardingDone && !onboardingSeen) {
        OnboardingScreen(vm) { onboardingSeen = true }
        return
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.RADAR
    var showAddPerson by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }

    LaunchedEffect(openPersonId, openLog) {
        openPersonId?.let { nav.navigate(Routes.person(it)) }
        if (openLog) nav.navigate(Routes.log())
    }

    val uncategorized by vm.uncategorized.collectAsStateWithLifecycle()
    val isTop = route in listOf(Routes.RADAR, Routes.NEW_PEOPLE, Routes.CONNECTORS)
    val isPerson = route == Routes.PERSON || route == Routes.PERSON_SETTINGS
    val title = when {
        route == Routes.RADAR -> "Tether"
        route == Routes.NEW_PEOPLE -> "Orbit"
        route == Routes.SETTINGS -> "Settings"
        route == Routes.CATEGORIES -> "Categories"
        route == Routes.CONNECTORS -> "Auto-Sync"
        route == Routes.WHO -> "Who is this?"
        route == Routes.HEALTH -> "Health"
        route == Routes.CALL_INSIGHTS -> "Call insights"
        route.startsWith("log") -> "Log a moment"
        else -> ""
    }
    AmbientGlassCanvas {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF0F172A),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (!isTop && !isPerson) {
                    TopAppBar(
                        title = {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        navigationIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .liquidGlass(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        elevation = 3.dp,
                                        surfaceAlphaTop = 0.85f,
                                        surfaceAlphaBottom = 0.55f,
                                    )
                                    .clickable { nav.popBackStack() }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (isTop) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 4.dp)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                                elevation = 18.dp,
                                surfaceAlphaTop = 0.94f,
                                surfaceAlphaBottom = 0.88f,
                                specularAlphaTop = 0.98f,
                                specularAlphaBottom = 0.30f,
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Radar Tab
                            val isRadar = route == Routes.RADAR
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        nav.navigate(Routes.RADAR) {
                                            popUpTo(Routes.RADAR)
                                            launchSingleTop = true
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    if (isRadar) Icons.Rounded.Radar else Icons.Outlined.Radar,
                                    contentDescription = "Tether",
                                    tint = if (isRadar) StatusColors.Cobalt else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tether",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isRadar) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (isRadar) StatusColors.Cobalt else Color(0xFF64748B),
                                )
                            }

                            // New People Tab
                            val isNew = route == Routes.NEW_PEOPLE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        nav.navigate(Routes.NEW_PEOPLE) {
                                            popUpTo(Routes.RADAR)
                                            launchSingleTop = true
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Box {
                                    Icon(
                                        if (isNew) Icons.Rounded.Groups else Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = if (isNew) StatusColors.Cobalt else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    if (uncategorized.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(7.dp)
                                                .background(
                                                    StatusColors.Cobalt,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Orbit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isNew) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (isNew) StatusColors.Cobalt else Color(0xFF64748B),
                                )
                            }

                            // Sources Tab (Connectors / Live Feeds)
                            val isSources = route == Routes.CONNECTORS
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        nav.navigate(Routes.CONNECTORS) {
                                            popUpTo(Routes.RADAR)
                                            launchSingleTop = true
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    if (isSources) Icons.Rounded.Sync else Icons.Outlined.Sync,
                                    contentDescription = null,
                                    tint = if (isSources) StatusColors.Cobalt else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Auto-Sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSources) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (isSources) StatusColors.Cobalt else Color(0xFF64748B),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (!isTop && !isPerson) Modifier.padding(top = padding.calculateTopPadding())
                        else Modifier
                    )
            ) {
                NavHost(nav, startDestination = Routes.RADAR) {
                    composable(Routes.RADAR) {
                        DashboardScreen(
                            vm = vm,
                            onOpenPerson = { nav.navigate(Routes.person(it)) },
                            onOpenNewPeople = { nav.navigate(Routes.NEW_PEOPLE) },
                            onOpenWho = { nav.navigate(Routes.WHO) },
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.NEW_PEOPLE) {
                        NewPeopleScreen(
                            vm = vm,
                            onOpenPerson = { nav.navigate(Routes.person(it)) }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                            onOpenConnectors = { nav.navigate(Routes.CONNECTORS) },
                            onOpenWho = { nav.navigate(Routes.WHO) },
                            onOpenHealth = { nav.navigate(Routes.HEALTH) }
                        )
                    }
                    composable(Routes.HEALTH) {
                        HealthScreen(vm)
                    }
                    composable(Routes.CONNECTORS) {
                        ConnectorsScreen(vm) { nav.navigate(Routes.CALL_INSIGHTS) }
                    }
                    composable(Routes.CALL_INSIGHTS) {
                        CallRecordingInsightsScreen(vm)
                    }
                    composable(Routes.WHO) {
                        WhoIsThisScreen(vm)
                    }
                    composable(Routes.CATEGORIES) {
                        CategoriesScreen(vm, showAddCategory) { showAddCategory = false }
                    }
                    composable(
                        Routes.PERSON,
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { e ->
                        val id = e.arguments!!.getLong("id")
                        PersonScreen(
                            vm = vm,
                            personId = id,
                            onLog = { nav.navigate(Routes.log(id)) },
                            onOpenSettings = { nav.navigate(Routes.personSettings(id)) },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable(
                        Routes.PERSON_SETTINGS,
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { e ->
                        val id = e.arguments!!.getLong("id")
                        PersonSettingsScreen(
                            vm = vm,
                            personId = id,
                            onBack = { nav.popBackStack() },
                            onArchived = {
                                nav.popBackStack(Routes.RADAR, inclusive = false)
                            }
                        )
                    }
                    composable(
                        Routes.LOG,
                        arguments = listOf(navArgument("personId") { type = NavType.LongType; defaultValue = -1L })
                    ) { e ->
                        val pid = e.arguments!!.getLong("personId").takeIf { it > 0 }
                        QuickLogScreen(vm, pid) { nav.popBackStack() }
                    }
                }

                if (isTop) {
                    // Soft atmospheric bottom gradient scrim to gracefully fade cards behind the floating dock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFE2E8F0).copy(alpha = 0.40f),
                                        Color(0xFFF1F5F9).copy(alpha = 0.85f),
                                        Color(0xFFF8FAFC),
                                    )
                                )
                            )
                    )
                }
            }
        }
    }

    if (showAddPerson) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPerson = false },
            title = { Text("Add a person") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    vm.createPerson(name, null) { id -> nav.navigate(Routes.person(id)) }
                    showAddPerson = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddPerson = false }) { Text("Cancel") } },
        )
    }
}
