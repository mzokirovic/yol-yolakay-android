package com.example.yol_yolakay.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Search : Screen
    @Serializable data object Publish : Screen
    @Serializable data object MyTrips : Screen
    @Serializable data object Inbox : Screen
    @Serializable data object Profile : Screen

    @Serializable
    data class TripList(
        val from: String,
        val to: String,
        val date: String,
        val passengers: Int
    ) : Screen

    @Serializable data object Login : Screen

    @Serializable data class TripDetails(val id: String) : Screen

    // Profile sub screens
    @Serializable data object ProfileEdit : Screen
    @Serializable data object Vehicle : Screen
    @Serializable data object Language : Screen
    @Serializable data object PaymentMethods : Screen

    // ✅ YANGI: Sozlamalar
    @Serializable data object Settings : Screen

    // Inbox / Chat
    @Serializable data class Thread(val id: String) : Screen
}