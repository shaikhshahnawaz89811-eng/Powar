package com.pawar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Every size in this screen (bubble/text wrapping, badge width, etc.) was
            // measured against the reference design at the system's default text scale.
            // Without this, a phone with a larger system font size setting renders every
            // sp text bigger than the reference, which is what was pushing lines/badges
            // to wrap or clip differently on-device than in the reference image.
            val fixedDensity = Density(
                density = LocalDensity.current.density,
                fontScale = 1f
            )
            CompositionLocalProvider(LocalDensity provides fixedDensity) {
                MaterialTheme {
                    var settingsOpen by rememberSaveable { mutableStateOf(false) }
                    val modelManager = remember { ModelManager.getInstance(this@MainActivity) }
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
}
