package com.pawar.app

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AttachmentKind { IMAGE, ZIP }

data class Attachment(
    val uri: Uri,
    val name: String,
    val kind: AttachmentKind
)

data class SentMessage(
    val text: String,
    val attachments: List<Attachment>
)

data class ConversationTurn(
    val message: SentMessage,
    val pipeline: PipelineRun? = null
)

// Everything that makes up the in-progress chat session, held in ONE object so
// it can be `remember`-ed once, above the Settings/Chat if-else in MainActivity,
// instead of inside PawarScreen itself. Compose throws away `remember` state
// the moment a composable leaves composition -- and PawarScreen fully leaves
// composition every time settingsOpen flips to true (MainActivity swaps it out
// for ModuleSettingsScreen). That was the whole bug: open Settings, come back,
// conversationTurns/input/attachments were all freshly re-created as empty.
// Keeping this object one level up means it's never disposed just because the
// user glanced at Settings.
class ChatSessionState {
    var input by mutableStateOf("")
    var attachmentMenuOpen by mutableStateOf(false)
    var cameraUri by mutableStateOf<Uri?>(null)
    val composerAttachments = mutableStateListOf<Attachment>()
    val conversationTurns = mutableStateListOf<ConversationTurn>()
}
