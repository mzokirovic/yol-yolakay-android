package com.example.yol_yolakay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.yol_yolakay.feature.main.MainScreen // Biz yasagan ekran
import com.example.yol_yolakay.ui.theme.YolYolakayTheme // O'zingizdagi Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Tepasidagi soat joyigacha to'liq ekran qilish

        setContent {
            // Ilovani o'zingizning dizayn temangiz bilan o'raymiz
            YolYolakayTheme {
                // Va nihoyat, biz yaratgan asosiy ekranni chaqiramiz!
                MainScreen()
            }
        }
    }
}