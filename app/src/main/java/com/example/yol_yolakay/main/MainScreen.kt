package com.example.yol_yolakay.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.yol_yolakay.navigation.Screen

// Boshqa feature ekranlarini import qilamiz
import com.example.yol_yolakay.feature.search.SearchScreen
import com.example.yol_yolakay.feature.publish.PublishScreen
import com.example.yol_yolakay.feature.trips.MyTripsScreen
import com.example.yol_yolakay.feature.inbox.InboxScreen
import com.example.yol_yolakay.feature.profile.ProfileScreen

// Menyu elementlari uchun model
data class BottomNavItem<T : Any>(
    val name: String,
    val route: T,
    val icon: ImageVector
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // 5 ta asosiy menyu
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
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    // Hozir qaysi menyu tanlanganini tekshirish
                    val isSelected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Bosilganda navigation tarixini tozalash (Back button muammosini oldini olish)
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.name) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Bu yerda ekranlar almashinadi
        NavHost(
            navController = navController,
            startDestination = Screen.Search,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Har bir menyu uchun qaysi Screen ochilishini ko'rsatamiz
            composable<Screen.Search> {
                SearchScreen() // Biz yasagan Search kartasi shu yerda ochiladi
            }
            composable<Screen.Publish> {
                PublishScreen()
            }
            composable<Screen.MyTrips> {
                MyTripsScreen()
            }
            composable<Screen.Inbox> {
                InboxScreen()
            }
            composable<Screen.Profile> {
                ProfileScreen()
            }
        }
    }
}