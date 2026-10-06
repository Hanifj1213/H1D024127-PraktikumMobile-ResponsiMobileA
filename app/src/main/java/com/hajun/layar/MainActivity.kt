package com.hajun.layar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hajun.layar.ui.LayarApp
import com.hajun.layar.ui.theme.LayarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LayarApplication).container
        setContent {
            LayarTheme {
                LayarApp(repository = container.repository)
            }
        }
    }
}
