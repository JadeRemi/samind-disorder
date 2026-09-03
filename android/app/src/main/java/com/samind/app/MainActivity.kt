package com.samind.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samind.app.ui.ChatScreen
import com.samind.app.ui.GroundingScreen
import com.samind.app.ui.HomeScreen
import com.samind.app.ui.StatsScreen
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
        val start = intent.getStringExtra(EXTRA_DESTINATION) ?: "home"

        setContent {
            SamindTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route

                val tabs = listOf(
                    Triple("home", stringResource(R.string.tab_home), R.drawable.ic_tab_home),
                    Triple("chat", stringResource(R.string.tab_chat), R.drawable.ic_tab_chat),
                    Triple("ground", stringResource(R.string.tab_ground), R.drawable.ic_tab_ground),
                    Triple("stats", stringResource(R.string.tab_stats), R.drawable.ic_tab_stats),
                )

                // navigate when the overlay launches us while already running
                LaunchedEffect(Unit) {
                    destinationRequests.collect { route -> navController.navigate(route) }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEach { (route, label, iconRes) ->
                                NavigationBarItem(
                                    selected = current == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo("home")
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(painterResource(iconRes), contentDescription = label)
                                    },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = start,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable("home") { HomeScreen() }
                        composable("chat") { ChatScreen() }
                        composable("ground") { GroundingScreen() }
                        composable("stats") { StatsScreen() }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
    }
}
