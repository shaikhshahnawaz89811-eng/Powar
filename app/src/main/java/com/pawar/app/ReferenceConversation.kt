package com.pawar.app

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReferenceConversation() {
    UserBubble(
        text = "MainActivity.kt ka pura UI likh do,\nstudents ka data save ho"
    )

    Spacer(Modifier.height(34.dp))

    AssistantText(
        text = "Theek hai — pehle data model, phir screen.\nFile card ban rahi hai, code live stream\nhoga."
    )

    Spacer(Modifier.height(32.dp))
    ThinDivider()

    ActivityRow(
        leading = RowIcon.CLOCK,
        text = "Starting with the data model, then the\nscreen."
    )

    ThinDivider()

    ActivityRow(
        leading = RowIcon.FOLDER,
        text = "Student.kt",
        trailing = "Created  ·  12 lines",
        showChevron = true
    )

    Spacer(Modifier.height(24.dp))
    CodeCard()
}
