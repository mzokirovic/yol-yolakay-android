package com.example.yol_yolakay.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Search : Screen      // 1. Qidiruv formasi
    @Serializable data object Publish : Screen     // 2. E'lon berish
    @Serializable data object MyTrips : Screen     // 3. Safarlarim
    @Serializable data object Inbox : Screen       // 4. Xabarlar
    @Serializable data object Profile : Screen     // 5. Profil

    // 🔍 Qidiruv natijalari (Vertical Slice: Search ichida qoladi)
    @Serializable
    data class TripList(
        val from: String,
        val to: String,
        val date: String,
        val passengers: Int
    ) : Screen

    @Serializable data object Login : Screen
    @Serializable data class TripDetails(val id: String) : Screen
}