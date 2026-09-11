package com.pawar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 570.dp)
                .background(AppColors.DarkBubble, RoundedCornerShape(36.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(text, color = androidx.compose.ui.graphics.Color.White, fontSize = 18.sp, lineHeight = 27.sp)
        }
    }
}

@Composable
fun AssistantText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 14.dp),
        text = text,
        color = AppColors.PrimaryText,
        fontSize = 18.sp,
        lineHeight = 28.sp
    )
}

@Composable
fun UserMessageWithAttachments(message: SentMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
        Column(modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 570.dp), horizontalAlignment = Alignment.End) {
            message.attachments.forEach { attachment ->
                SentAttachmentCard(attachment)
                androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            }
            if (message.text.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(AppColors.DarkBubble, RoundedCornerShape(36.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(message.text, color = androidx.compose.ui.graphics.Color.White, fontSize = 18.sp, lineHeight = 27.sp)
                }
            }
        }
    }
}
