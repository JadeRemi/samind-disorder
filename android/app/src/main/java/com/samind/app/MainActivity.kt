package com.samind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = intent.getStringExtra(EXTRA_DESTINATION) ?: "home"

        setContent {
            SamindTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route

                val tabs = listOf(
                    "home" to stringResource(R.string.tab_home),
                    "chat" to stringResource(R.string.tab_chat),
                    "ground" to stringResource(R.string.tab_ground),
                    "stats" to stringResource(R.string.tab_stats),
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            tabs.forEach { (route, label) ->
                                NavigationBarItem(
                                    selected = current == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo("home")
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painterResource(R.drawable.ic_mascot),
                                            contentDescription = label,
                                        )
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
