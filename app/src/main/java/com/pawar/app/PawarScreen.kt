package com.pawar.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun PawarScreen(modelManager: ModelManager, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    val composerAttachments = remember { mutableStateListOf<Attachment>() }
    val sentMessages = remember { mutableStateListOf<SentMessage>() }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val messageListState = rememberLazyListState()
    val uiScope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            composerAttachments.add(Attachment(uri, "Photo", AttachmentKind.IMAGE))
        }
    }

    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = displayName(context, uri) ?: "Archive.zip"
            if (name.lowercase().endsWith(".zip")) {
                composerAttachments.add(Attachment(uri, name, AttachmentKind.ZIP))
            }
        }
    }

    val cameraCapture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            composerAttachments.add(Attachment(uri, "Photo", AttachmentKind.IMAGE))
        } else if (!success && uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        cameraUri = null
    }

    fun openCamera() {
        val file = File.createTempFile("pawar_camera_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraUri = uri
        attachmentMenuOpen = false
        cameraCapture.launch(uri)
    }

    fun openPhotos() {
        attachmentMenuOpen = false
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun openZip() {
        attachmentMenuOpen = false
        zipPicker.launch(
            arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream"
            )
        )
    }

    fun send() {
        val trimmed = input.trim()
        if (trimmed.isNotEmpty() || composerAttachments.isNotEmpty()) {
            sentMessages.add(SentMessage(trimmed, composerAttachments.toList()))
            input = ""
            composerAttachments.clear()
            attachmentMenuOpen = false
        }
    }

    // Keep the newest message visible after sending. This is the same bottom-anchored
    // behavior expected from a chat thread rather than leaving the user in the old viewport.
    LaunchedEffect(sentMessages.size) {
        if (sentMessages.isNotEmpty()) {
            messageListState.animateScrollToItem(messageListState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.AppBackground)
            .safeDrawingPadding()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            OfflineHeader(onMenu = onOpenSettings, moduleLoaded = modelManager.loaded)
            Spacer(Modifier.height(54.dp))

            LazyColumn(
                state = messageListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
            ) {
                item(key = "reference") {
                    ReferenceConversation()
                }
                itemsIndexed(
                    items = sentMessages,
                    key = { index, _ -> "sent-$index" }
                ) { _, message ->
                    Spacer(Modifier.height(24.dp))
                    UserMessageWithAttachments(message)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                if (attachmentMenuOpen) {
                    Popup(
                        alignment = Alignment.BottomStart,
                        offset = with(LocalDensity.current) {
                            IntOffset(0, (-104).dp.roundToPx())
                        },
                        onDismissRequest = { attachmentMenuOpen = false },
                        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                    ) {
                        AttachmentMenu(
                            modifier = Modifier,
                            onCamera = ::openCamera,
                            onPhotos = ::openPhotos,
                            onZip = ::openZip
                        )
                    }
                }

                Composer(
                    value = input,
                    attachments = composerAttachments,
                    onValueChange = { input = it },
                    onPlus = { attachmentMenuOpen = !attachmentMenuOpen },
                    onRemoveAttachment = { composerAttachments.remove(it) },
                    onInputFocused = {
                        if (messageListState.layoutInfo.totalItemsCount > 0) {
                            // Post until the list has the reduced IME viewport, then reveal the last item.
                            uiScope.launch {
                                kotlinx.coroutines.yield()
                                val last = messageListState.layoutInfo.totalItemsCount - 1
                                if (last >= 0) messageListState.animateScrollToItem(last)
                            }
                        }
                    },
                    onSend = ::send
                )
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(
        uri,
        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else uri.lastPathSegment
    }
