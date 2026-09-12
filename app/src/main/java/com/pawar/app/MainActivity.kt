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
import androidx.compose.runtime.rememberCoroutineScope
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

                    // Held here -- one level above the settingsOpen if/else below -- so
                    // opening Settings and coming back doesn't wipe the conversation.
                    // See the comment on ChatSessionState for why this can't just live
                    // inside PawarScreen. The pipeline and its coroutine scope are hoisted
                    // alongside it for the same reason: if a reply is still generating when
                    // the user taps into Settings, this scope (unlike one scoped to
                    // PawarScreen) doesn't get cancelled, so generation keeps running and
                    // the answer is there when they come back.
                    val chatState = remember { ChatSessionState() }
                    val chatScope = rememberCoroutineScope()
                    val pipeline = remember(modelManager) {
                        DynamicAgentPipeline(this@MainActivity, modelManager)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.AppBackground
                    ) {
                        BackHandler(enabled = settingsOpen) { settingsOpen = false }
                        if (settingsOpen) {
                            ModuleSettingsScreen(manager = modelManager, onBack = { settingsOpen = false })
                        } else {
                            PawarScreen(
                                modelManager = modelManager,
                                chatState = chatState,
                                pipeline = pipeline,
                                uiScope = chatScope,
                                onOpenSettings = { settingsOpen = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
