package com.lemomation.lucidlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.lemomation.lucidlogger.ui.LucidRoot
import com.lemomation.lucidlogger.ui.theme.LucidCanvas
import com.lemomation.lucidlogger.ui.theme.LucidLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LucidLoggerApp).container
        setContent {
            LucidLoggerTheme {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(LucidCanvas),
                ) {
                    LucidRoot(container)
                }
            }
        }
    }
}
