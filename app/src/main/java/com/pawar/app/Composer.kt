package com.pawar.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Composer(
    value: String,
    attachments: List<Attachment>,
    onValueChange: (String) -> Unit,
    onPlus: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onInputFocused: () -> Unit,
    onSend: () -> Unit
) {
    val composerHeight = if (attachments.isNotEmpty()) 152.dp else 86.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(composerHeight)
            .clip(RoundedCornerShape(44.dp))
            .background(AppColors.CardBackground)
            .border(1.dp, Color(0xFFE9EAE7), RoundedCornerShape(44.dp))
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (attachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 12.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachments) { attachment ->
                        ComposerAttachment(attachment) { onRemoveAttachment(attachment) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFF0F1F4)).clickable(onClick = onPlus),
                    contentAlignment = Alignment.Center
                ) { PlusIcon() }

                Spacer(Modifier.width(22.dp))
                val textState = rememberTextFieldState(value)
                LaunchedEffect(textState) {
                    snapshotFlow { textState.text.toString() }.collect { text ->
                        if (text != value) onValueChange(text)
                    }
                }
                LaunchedEffect(value) {
                    if (textState.text.toString() != value) {
                        textState.setTextAndPlaceCursorAtEnd(value)
                    }
                }
                BasicTextField(
                    state = textState,
                    modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onInputFocused() },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    onKeyboardAction = { onSend() },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = TextStyle(color = Color(0xFF5D5E5B), fontSize = 20.sp),
                    decorator = { inner ->
                        if (textState.text.isEmpty()) Text("Reply", color = Color(0xFFB6B7B4), fontSize = 22.sp)
                        inner()
                    }
                )

                Box(
                    modifier = Modifier
                        .height(54.dp)
                        .widthIn(min = 0.dp, max = 150.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0xFFE0E1DE), RoundedCornerShape(28.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Qwen2.5-Coder · Q4_K_M", color = Color(0xFF777975), fontSize = 16.sp, maxLines = 1)
                }
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF151515)).clickable(onClick = onSend),
                    contentAlignment = Alignment.Center
                ) { SendArrowIcon() }
            }
        }
    }
}

@Composable
fun ComposerAttachment(attachment: Attachment, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (attachment.kind == AttachmentKind.IMAGE) 62.dp else 160.dp, 62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF0F1F4))
            .border(1.dp, Color(0xFFE0E1DE), RoundedCornerShape(14.dp))
    ) {
        if (attachment.kind == AttachmentKind.IMAGE) {
            AttachmentImage(attachment.uri)
        } else {
            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                ZipMiniIcon()
                Spacer(Modifier.width(8.dp))
                Text(attachment.name, color = Color(0xFF50524F), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clip(CircleShape).background(Color(0xCC181818)).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) { Text("×", color = Color.White, fontSize = 15.sp, lineHeight = 15.sp) }
    }
}

@Composable
fun SentAttachmentCard(attachment: Attachment) {
    if (attachment.kind == AttachmentKind.IMAGE) {
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFFE9EAE7))) {
            AttachmentImage(attachment.uri)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppColors.DarkBubble).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                ZipMiniIcon(light = false)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(attachment.name, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("ZIP file", color = Color(0xFFB9B9B9), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AttachmentImage(uri: Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, bounds)
                    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_IMAGE_PX)
                    context.contentResolver.openInputStream(uri)?.use { secondInput ->
                        BitmapFactory.decodeStream(
                            secondInput,
                            null,
                            BitmapFactory.Options().apply { inSampleSize = sample }
                        )
                    }
                }
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

private fun calculateInSampleSize(width: Int, height: Int, targetPx: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > targetPx * 2 || height / sample > targetPx * 2) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private const val TARGET_IMAGE_PX = 1200

