package com.samind.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samind.app.data.Prefs
import com.samind.app.ui.BreathingScreen
import com.samind.app.ui.ChatScreen
import com.samind.app.ui.GroundingScreen
import com.samind.app.ui.HomeScreen
import com.samind.app.ui.PracticeSettingsScreen
import com.samind.app.ui.PracticesScreen
import com.samind.app.ui.SettingsScreen
import com.samind.app.ui.CountEightsScreen
import com.samind.app.ui.DesignState
import com.samind.app.ui.SignInScreen
import com.samind.app.ui.VoiceScreen
import com.samind.app.ui.components.PillShape
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    // the overlay can launch us while we are already running; a fresh intent
    // must still navigate, otherwise the mascot tap appears to do nothing
    private val destinationRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let { destinationRequests.tryEmit(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = intent.getStringExtra(EXTRA_DESTINATION)
        val technique = intent.getStringExtra(EXTRA_TECHNIQUE)
        val designState = intent.getStringExtra(DesignState.EXTRA)

        setContent {
            SamindTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                val signedIn = Prefs.displayName(this).isNotBlank()
                val start = requested ?: if (signedIn) HOME else SIGN_IN

                LaunchedEffect(Unit) {
                    destinationRequests.collect { navController.navigate(it) }
                }

                Box(Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = start) {
                        composable(SIGN_IN) {
                            SignInScreen(
                                designState = designState,
                                onDone = { navController.navigate(HOME) },
                            )
                        }
                        composable(HOME) { HomeScreen() }
                        composable(CHAT) { ChatScreen(designState = designState) }
                        composable(PRACTICES) {
                            PracticesScreen(
                                onOpenGrounding = { navController.navigate(GROUND) },
                                onOpenBreathing = { navController.navigate(BREATHE) },
                                onOpenEights = { navController.navigate(EIGHTS) },
                                onOpenSettings = { navController.navigate(PRACTICE_SETTINGS) },
                            )
                        }
                        composable(GROUND) {
                            GroundingScreen(
                                initialTechniqueId = technique,
                                designState = designState,
                                onExit = { navController.popBackStack() },
                            )
                        }
                        composable(BREATHE) {
                            // the eights technique has its own screen
                            if (Prefs.technique(this@MainActivity) == "0-8-16-32") {
                                CountEightsScreen(
                                    sessionMinutes = Prefs.minutes(this@MainActivity),
                                    designState = designState,
                                    onExit = { navController.popBackStack() },
                                )
                            } else {
                                BreathingScreen(
                                    patternId = Prefs.technique(this@MainActivity),
                                    sessionMinutes = Prefs.minutes(this@MainActivity),
                                    designState = designState,
                                    onExit = { navController.popBackStack() },
                                    onOpenSettings = { navController.navigate(PRACTICE_SETTINGS) },
                                )
                            }
                        }
                        composable(EIGHTS) {
                            CountEightsScreen(
                                sessionMinutes = Prefs.minutes(this@MainActivity),
                                designState = designState,
                                onExit = { navController.popBackStack() },
                            )
                        }
                        composable(PRACTICE_SETTINGS) {
                            PracticeSettingsScreen { navController.popBackStack() }
                        }
                        composable(VOICE) {
                            VoiceScreen(designState = designState) { navController.popBackStack() }
                        }
                        composable(SETTINGS) {
                            SettingsScreen(
                                onOpenAccessibility = {
                                    startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                },
                                onOpenVoice = { navController.navigate(VOICE) },
                            )
                        }
                    }

                    // pinned pill nav — hidden on chat (full screen) and sign-in
                    if (current in setOf(HOME, PRACTICES, SETTINGS)) {
                        BottomPillNav(
                            current = current,
                            navController = navController,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_TECHNIQUE = "technique"
        const val SIGN_IN = "signin"
        const val HOME = "home"
        const val CHAT = "chat"
        const val PRACTICES = "practices"
        const val GROUND = "ground"
        const val BREATHE = "breathe"
        const val EIGHTS = "eights"
        const val VOICE = "voice"
        const val PRACTICE_SETTINGS = "practice_settings"
        const val SETTINGS = "settings"
    }
}

@Composable
private fun BottomPillNav(
    current: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        MainActivity.HOME to R.drawable.ic_tab_home,
        MainActivity.CHAT to R.drawable.ic_tab_chat,
        MainActivity.PRACTICES to R.drawable.ic_tab_practices,
        MainActivity.SETTINGS to R.drawable.ic_tab_settings,
    )
    Row(
        modifier
            .padding(horizontal = ScreenMargin, vertical = 16.dp)
            .fillMaxWidth()
            .height(68.dp)
            .shadow(8.dp, PillShape, clip = false)
            .background(SamindGradients.navBar, PillShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), PillShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (route, icon) ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable {
                        if (route != current) {
                            navController.navigate(route) {
                                popUpTo(MainActivity.HOME)
                                launchSingleTop = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = stringResource(R.string.app_name),
                    tint = if (route == current) Primary900 else Primary900.copy(alpha = 0.38f),
                    modifier = Modifier.size(23.dp),
                )
            }
        }
    }
}
