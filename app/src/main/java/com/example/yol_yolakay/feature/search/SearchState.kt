package com.example.yol_yolakay.feature.search

import java.time.LocalDate

data class SearchUiState(
    val fromLocation: String = "",
    val toLocation: String = "",
    val date: LocalDate = LocalDate.now(), // Bugungi sana (Default)
    val passengers: Int = 1,               // Kamida 1 yo'lovchi
    val isSearching: Boolean = false
)