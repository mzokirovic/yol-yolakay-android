package com.example.yol_yolakay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.yol_yolakay.main.MainScreen // Yangilangan import
import com.example.yol_yolakay.ui.theme.YolYolakayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YolYolakayTheme {
                MainScreen()
            }
        }
    }
}