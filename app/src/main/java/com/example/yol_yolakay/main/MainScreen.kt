package com.example.yol_yolakay.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.example.yol_yolakay.feature.inbox.InboxScreen
import com.example.yol_yolakay.feature.profile.ProfileScreen
import com.example.yol_yolakay.feature.publish.PublishScreen
import com.example.yol_yolakay.feature.search.SearchScreen
import com.example.yol_yolakay.feature.search.TripListScreen
import com.example.yol_yolakay.feature.tripdetails.TripDetailsScreen
import com.example.yol_yolakay.feature.trips.MyTripsScreen
import com.example.yol_yolakay.navigation.Screen

data class BottomNavItem(
    val name: String,
    val route: Any,
    val icon: ImageVector
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Qidiruv", Screen.Search, Icons.Default.Search),
        BottomNavItem("E'lon", Screen.Publish, Icons.Default.AddCircle),
        BottomNavItem("Safarlar", Screen.MyTrips, Icons.Default.DateRange),
        BottomNavItem("Inbox", Screen.Inbox, Icons.Default.Email),
        BottomNavItem("Profil", Screen.Profile, Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val isSelected = currentDest?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.name) },
                        icon = { Icon(item.icon, contentDescription = item.name) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable<Screen.Search> {
                SearchScreen(
                    onSearchClick = { from: String, to: String, date: String, pass: Int ->
                        navController.navigate(Screen.TripList(from, to, date, pass))
                    }
                )
            }

            composable<Screen.TripList> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.TripList>()
                TripListScreen(
                    from = args.from,
                    to = args.to,
                    date = args.date,
                    passengers = args.passengers,
                    onBack = { navController.popBackStack() },
                    onTripClick = { tripId ->
                        navController.navigate(Screen.TripDetails(tripId))
                    }
                )
            }

            // ✅ Trip Details
            composable<Screen.TripDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.TripDetails>()
                TripDetailsScreen(
                    tripId = args.id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Publish> {
                PublishScreen(
                    onPublished = {
                        navController.navigate(Screen.MyTrips) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable<Screen.MyTrips> {
                MyTripsScreen(
                    onTripClick = { tripId ->
                        navController.navigate(Screen.TripDetails(tripId))
                    }
                )
            }
            composable<Screen.Inbox> { InboxScreen() }
            composable<Screen.Profile> { ProfileScreen() }
        }
    }
}
