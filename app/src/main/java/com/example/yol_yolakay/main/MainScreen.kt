package com.example.yol_yolakay.main

import android.net.http.SslCertificate.restoreState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.example.yol_yolakay.AppDeepLink
import com.example.yol_yolakay.core.session.SessionStore
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

private data class BottomNavItem(
    val name: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    deepLink: AppDeepLink? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val ctx = LocalContext.current

    // ✅ Notifications VM
    val notifVm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory(ctx))

    // ✅ Login state -> notifVm
    val sessionStore = remember { SessionStore(ctx.applicationContext) }
    val isLoggedIn by sessionStore.isLoggedIn.collectAsState(initial = false)
    LaunchedEffect(isLoggedIn) { notifVm.onLoginState(isLoggedIn) }

    // ✅ unreadCount
    val unreadCount = notifVm.state.unreadCount

    // Updates tab ochish uchun signal
    var openUpdatesSignal by rememberSaveable { mutableIntStateOf(0) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Deep link navigation
    LaunchedEffect(deepLink) {
        val dl = deepLink ?: return@LaunchedEffect

        val t = dl.title?.trim().orEmpty()
        val b = dl.body?.trim().orEmpty()
        if (t.isNotBlank() || b.isNotBlank()) {
            val msg = if (t.isNotBlank() && b.isNotBlank()) "$t — $b" else (t.ifBlank { b })
            snackbarHostState.showSnackbar(message = msg)
        }

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

    // ✅ Clean bottom tabs (Filled vs Outlined)
    val bottomNavItems = remember {
        listOf(
            BottomNavItem("Qidiruv", Screen.Search, Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem("E'lon", Screen.Publish, Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
            BottomNavItem("Safarlar", Screen.MyTrips, Icons.Filled.DateRange, Icons.Outlined.DateRange),
            BottomNavItem("Inbox", Screen.Inbox, Icons.Filled.Email, Icons.Outlined.Email),
            BottomNavItem("Profil", Screen.Profile, Icons.Filled.Person, Icons.Outlined.Person),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MainBottomBar(
                navController = navController,
                items = bottomNavItems,
                unreadCount = unreadCount
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Search,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable<Screen.Search> {
                SearchScreen(
                    onSearchClick = { from, to, date, pass ->
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
                            popUpTo(Screen.Search) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<Screen.MyTrips> {
                MyTripsScreen(
                    onTripClick = { tripId -> navController.navigate(Screen.TripDetails(tripId)) }
                )
            }

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
                ThreadScreen(
                    threadId = args.id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Profile> {
                ProfileScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable<Screen.ProfileEdit> { ProfileEditScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.Vehicle> { VehicleScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.Language> { LanguageScreen(onBack = { navController.popBackStack() }) }
            composable<Screen.PaymentMethods> { PaymentMethodsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun MainBottomBar(
    navController: NavHostController,
    items: List<BottomNavItem>,
    unreadCount: Int
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val cs = MaterialTheme.colorScheme

    Surface(color = cs.surface, shadowElevation = 6.dp) {
        NavigationBar(
            containerColor = cs.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = when (item.route) {
                    Screen.Profile -> currentDest.isProfileSection()
                    Screen.Inbox -> currentDest.isInboxSection()
                    else -> currentDest.isInHierarchy(item.route)
                }

                val iconVector = if (isSelected) item.selectedIcon else item.unselectedIcon

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    // ✅ MUHIM: label bermaymiz => icon tepaga ko‘chmaydi
                    alwaysShowLabel = false,
                    icon = {
                        if (item.route == Screen.Inbox && unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                                Icon(iconVector, contentDescription = item.name)
                            }
                        } else {
                            Icon(iconVector, contentDescription = item.name)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent, // pill yo‘q
                        selectedIconColor = cs.onSurface,
                        unselectedIconColor = cs.onSurfaceVariant
                    )
                )
            }
        }
    }
}


private fun NavDestination?.isInHierarchy(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

private fun NavDestination?.isInboxSection(): Boolean =
    this?.hierarchy?.any { dest ->
        dest.hasRoute(Screen.Inbox::class) || dest.hasRoute(Screen.Thread::class)
    } == true

private fun NavDestination?.isProfileSection(): Boolean =
    this?.hierarchy?.any { dest ->
        dest.hasRoute(Screen.Profile::class) ||
                dest.hasRoute(Screen.ProfileEdit::class) ||
                dest.hasRoute(Screen.Vehicle::class) ||
                dest.hasRoute(Screen.Language::class) ||
                dest.hasRoute(Screen.PaymentMethods::class)
    } == true
