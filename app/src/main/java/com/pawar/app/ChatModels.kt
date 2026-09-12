package com.pawar.app

import android.net.Uri

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
