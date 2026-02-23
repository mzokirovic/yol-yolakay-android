package com.example.yol_yolakay.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.yol_yolakay.AppDeepLink
import com.example.yol_yolakay.R
import com.example.yol_yolakay.core.session.SessionStore
import com.example.yol_yolakay.feature.inbox.InboxHubScreen
import com.example.yol_yolakay.feature.inbox.ThreadScreen
import com.example.yol_yolakay.feature.notifications.NotificationsViewModel
import com.example.yol_yolakay.feature.notifications.NotificationsVmFactory
import com.example.yol_yolakay.feature.profile.edit.ProfileEditScreen
import com.example.yol_yolakay.feature.profile.home.ProfileScreen
import com.example.yol_yolakay.feature.profile.language.LanguageScreen
import com.example.yol_yolakay.feature.profile.payment.PaymentMethodsScreen
import com.example.yol_yolakay.feature.profile.settings.SettingsScreen
import com.example.yol_yolakay.feature.profile.vehicle.VehicleScreen
import com.example.yol_yolakay.feature.publish.PublishScreen
import com.example.yol_yolakay.feature.search.SearchScreen
import com.example.yol_yolakay.feature.search.TripListScreen
import com.example.yol_yolakay.feature.tripdetails.TripDetailsScreen
import com.example.yol_yolakay.feature.trips.MyTripsScreen
import com.example.yol_yolakay.navigation.Screen
import kotlinx.coroutines.launch

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

    val notifVm: NotificationsViewModel = viewModel(factory = NotificationsVmFactory(ctx))

    val sessionStore = remember { SessionStore(ctx.applicationContext) }
    val isLoggedIn by sessionStore.isLoggedIn.collectAsState(initial = false)
    LaunchedEffect(isLoggedIn) { notifVm.onLoginState(isLoggedIn) }

    val unreadCount = notifVm.state.unreadCount
    var openUpdatesSignal by rememberSaveable { mutableStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ✅ FAQAT shu 4 ta asosiy ekranda menyu ko'rinadi (Settings/Publish/... da yashirinadi)
    val showBottomBar = currentDestination?.let { dest ->
        dest.hasRoute(Screen.Search::class) ||
                dest.hasRoute(Screen.MyTrips::class) ||
                dest.hasRoute(Screen.Inbox::class) ||
                dest.hasRoute(Screen.Profile::class)
    } ?: true

    LaunchedEffect(deepLink) {
        val dl = deepLink ?: return@LaunchedEffect

        val t = dl.title?.trim().orEmpty()
        val b = dl.body?.trim().orEmpty()
        if (t.isNotBlank() || b.isNotBlank()) {
            val msg = if (t.isNotBlank() && b.isNotBlank()) "$t — $b" else (t.ifBlank { b })
            scope.launch { snackbarHostState.showSnackbar(message = msg) }
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

    // ✅ Til o'zgarsa bottom nav label ham real o'zgaradi
    val navSearch = stringResource(R.string.nav_search)
    val navPublish = stringResource(R.string.nav_publish)
    val navTrips = stringResource(R.string.nav_trips)
    val navInbox = stringResource(R.string.nav_inbox)
    val navProfile = stringResource(R.string.nav_profile)

    val bottomNavItems = remember(navSearch, navPublish, navTrips, navInbox, navProfile) {
        listOf(
            BottomNavItem(navSearch, Screen.Search, Icons.Rounded.Search, Icons.Outlined.Search),
            BottomNavItem(navPublish, Screen.Publish, Icons.Rounded.Add, Icons.Outlined.Add),
            BottomNavItem(navTrips, Screen.MyTrips, Icons.Rounded.DirectionsCar, Icons.Outlined.DirectionsCar),
            BottomNavItem(navInbox, Screen.Inbox, Icons.Rounded.Inbox, Icons.Outlined.Inbox),
            BottomNavItem(navProfile, Screen.Profile, Icons.Rounded.Person, Icons.Outlined.Person),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                MainBottomBar(
                    navController = navController,
                    items = bottomNavItems,
                    unreadCount = unreadCount,
                    currentDestination = currentDestination
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
            )
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

            // ✅ Settings
            composable<Screen.Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIZAYN O'ZGARISHSZ SAQLANDI (Sizning asil kodingiz)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MainBottomBar(
    navController: NavHostController,
    items: List<BottomNavItem>,
    unreadCount: Int,
    currentDestination: NavDestination?
) {
    val cs = MaterialTheme.colorScheme

    // ✅ hammasi 28
    val iconSize = 28.dp
    val publishIconSize = 28.dp

    Surface(color = cs.surface, shadowElevation = 10.dp) {
        NavigationBar(
            containerColor = cs.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = when (item.route) {
                    Screen.Profile -> currentDestination.isProfileSection()
                    Screen.Inbox -> currentDestination.isInboxSection()
                    else -> currentDestination.isInHierarchy(item.route)
                }

                val iconVector = if (isSelected) item.selectedIcon else item.unselectedIcon
                val currentIconSize = if (item.route == Screen.Publish) publishIconSize else iconSize

                val badgeText = when {
                    unreadCount <= 0 -> null
                    unreadCount > 99 -> "99+"
                    else -> unreadCount.toString()
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
                    alwaysShowLabel = false,
                    icon = {
                        if (item.route == Screen.Inbox && badgeText != null) {
                            BadgedBox(badge = { Badge { Text(badgeText) } }) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(currentIconSize)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = item.name,
                                modifier = Modifier.size(currentIconSize)
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
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
                dest.hasRoute(Screen.PaymentMethods::class) ||
                dest.hasRoute(Screen.Settings::class) // ✅ Settings ham Profile tabga tegishli
    } == true