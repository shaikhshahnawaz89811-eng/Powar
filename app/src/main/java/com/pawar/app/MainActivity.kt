package com.pawar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                val modelManager = remember { ModelManager(this@MainActivity) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.AppBackground
                ) {
                    BackHandler(enabled = settingsOpen) { settingsOpen = false }
                    if (settingsOpen) {
                        ModuleSettingsScreen(manager = modelManager, onBack = { settingsOpen = false })
                    } else {
                        PawarScreen(modelManager = modelManager, onOpenSettings = { settingsOpen = true })
                    }
                }
            }
        }
    }
}
