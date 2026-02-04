package com.example.yol_yolakay.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.example.yol_yolakay.AppDeepLink
import com.example.yol_yolakay.feature.inbox.InboxHubScreen
import com.example.yol_yolakay.feature.inbox.ThreadScreen
import com.example.yol_yolakay.feature.notifications.NotificationsViewModel
import com.example.yol_yolakay.feature.notifications.NotificationsVmFactory
import com.example.yol_yolakay.feature.profile.*
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
fun MainScreen(
    deepLink: AppDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val ctx = LocalContext.current

    val notifVm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory(ctx))
    val unread = notifVm.state.unreadCount

    // ✅ Updates tab ochish uchun signal
    var openUpdatesSignal by rememberSaveable { mutableStateOf(0) }

    // ✅ Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Notification bosilganda kerakli joyga olib kiradi + info ko‘rsatadi
    LaunchedEffect(deepLink) {
        val dl = deepLink ?: return@LaunchedEffect

        // 1) title/body ko‘rsatish
        val t = dl.title?.trim().orEmpty()
        val b = dl.body?.trim().orEmpty()
        if (t.isNotBlank() || b.isNotBlank()) {
            val msg = if (t.isNotBlank() && b.isNotBlank()) "$t — $b" else (t.ifBlank { b })
            snackbarHostState.showSnackbar(message = msg)
        }

        // 2) navigation
        when {
            !dl.threadId.isNullOrBlank() -> {
                navController.navigate(Screen.Thread(dl.threadId)) { launchSingleTop = true }
            }
            !dl.tripId.isNullOrBlank() -> {
                navController.navigate(Screen.TripDetails(dl.tripId)) { launchSingleTop = true }
            }
            else -> {
                openUpdatesSignal++
                navController.navigate(Screen.Inbox) { launchSingleTop = true }
            }
        }

        onDeepLinkHandled()
    }

    val bottomNavItems = listOf(
        BottomNavItem("Qidiruv", Screen.Search, Icons.Default.Search),
        BottomNavItem("E'lon", Screen.Publish, Icons.Default.AddCircle),
        BottomNavItem("Safarlar", Screen.MyTrips, Icons.Default.DateRange),
        BottomNavItem("Inbox", Screen.Inbox, Icons.Default.Email),
        BottomNavItem("Profil", Screen.Profile, Icons.Default.Person)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val isSelected = when (item.route) {

                        Screen.Profile -> {
                            currentDest?.hierarchy?.any { dest ->
                                dest.hasRoute(Screen.Profile::class) ||
                                        dest.hasRoute(Screen.ProfileEdit::class) ||
                                        dest.hasRoute(Screen.Vehicle::class) ||
                                        dest.hasRoute(Screen.Language::class) ||
                                        dest.hasRoute(Screen.PaymentMethods::class)
                            } == true
                        }

                        Screen.Inbox -> {
                            currentDest?.hierarchy?.any { dest ->
                                dest.hasRoute(Screen.Inbox::class) ||
                                        dest.hasRoute(Screen.Thread::class)
                            } == true
                        }

                        else -> currentDest?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.name) },
                        icon = {
                            if (item.route == Screen.Inbox && unread > 0) {
                                BadgedBox(badge = { Badge { Text(unread.toString()) } }) {
                                    Icon(item.icon, contentDescription = item.name)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.name)
                            }
                        }
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
                SearchScreen { from, to, date, pass ->
                    navController.navigate(Screen.TripList(from, to, date, pass))
                }
            }

            composable<Screen.TripList> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.TripList>()
                TripListScreen(
                    from = args.from,
                    to = args.to,
                    date = args.date,
                    passengers = args.passengers,
                    onBack = { navController.popBackStack() },
                    onTripClick = { tripId -> navController.navigate(Screen.TripDetails(tripId)) }
                )
            }

            composable<Screen.TripDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.TripDetails>()
                TripDetailsScreen(
                    tripId = args.id,
                    onBack = { navController.popBackStack() },
                    onOpenThread = { threadId -> navController.navigate(Screen.Thread(threadId)) },
                    onOpenInbox = { navController.navigate(Screen.Inbox) }
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
                MyTripsScreen(onTripClick = { tripId -> navController.navigate(Screen.TripDetails(tripId)) })
            }

            // ✅ Inbox hub (Chats + Updates)
            composable<Screen.Inbox> {
                InboxHubScreen(
                    onOpenThread = { threadId -> navController.navigate(Screen.Thread(threadId)) },
                    onOpenTrip = { tripId -> navController.navigate(Screen.TripDetails(tripId)) },
                    notifVm = notifVm,
                    openUpdatesSignal = openUpdatesSignal
                )
            }

            composable<Screen.Thread> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.Thread>()
                ThreadScreen(threadId = args.id, onBack = { navController.popBackStack() })
            }

            composable<Screen.Profile> { ProfileScreen(onNavigate = { route -> navController.navigate(route) }) }
            composable<Screen.ProfileEdit> { ProfileEditScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.Vehicle> { VehicleScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.Language> { LanguageScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.PaymentMethods> { PaymentMethodsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
