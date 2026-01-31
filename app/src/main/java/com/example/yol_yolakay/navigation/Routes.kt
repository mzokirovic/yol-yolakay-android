package com.example.yol_yolakay.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {

    // Asosiy Graph (Pastki menyudagi 5 ta oyna)
    @Serializable data object Search : Screen      // 1. Qidiruv
    @Serializable data object Publish : Screen     // 2. E'lon berish
    @Serializable data object MyTrips : Screen     // 3. Safarlarim
    @Serializable data object Inbox : Screen       // 4. Xabarlar
    @Serializable data object Profile : Screen     // 5. Profil

    // Boshqa ekranlar (Menyuda ko'rinmaydi, lekin navigatsiya bor)
    @Serializable data object Login : Screen
    @Serializable data class TripDetails(val id: String) : Screen
}