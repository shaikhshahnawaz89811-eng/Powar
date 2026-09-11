package com.pawar.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ModuleSettingsScreen(manager: ModelManager, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember { mutableStateOf(if (manager.exists) ModuleStatus.LOADING else ModuleStatus.NOT_IMPORTED) }
    var busy by remember { mutableStateOf(manager.exists) }
    var error by remember { mutableStateOf<String?>(null) }
    var hash by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(manager) {
        if (manager.exists) {
            val validation = manager.validateStoredModel()
            busy = false
            validation.onSuccess {
                status = ModuleStatus.READY
                error = null
                hash = manager.sha256()
            }.onFailure {
                status = ModuleStatus.ERROR
                error = it.message ?: "Stored module is invalid."
            }
        }
    }

    fun showResult(result: Result<Unit>, success: ModuleStatus, failureStatus: ModuleStatus) {
        busy = false
        result.onSuccess {
            status = success
            error = null
            if (success == ModuleStatus.READY || success == ModuleStatus.LOADED) {
                scope.launch { hash = manager.sha256() }
            } else {
                hash = ""
            }
        }.onFailure {
            status = failureStatus
            error = it.message ?: "Operation failed."
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        status = ModuleStatus.LOADING
        error = null
        scope.launch { showResult(manager.import(uri), ModuleStatus.READY, if (manager.exists) ModuleStatus.READY else ModuleStatus.NOT_IMPORTED) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.AppBackground)
            .safeDrawingPadding()
    ) {
        SettingsTopBar(onBack)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = AppColors.PrimaryText)
            Text("Local module", fontSize = 15.sp, color = AppColors.SecondaryText)

            ModuleCard(
                status = status,
                busy = busy,
                hash = hash,
                onImport = {
                    if (!busy && status != ModuleStatus.LOADED) importer.launch(arrayOf("application/octet-stream", "application/*", "*/*"))
                },
                onLoad = {
                    if (!busy && status == ModuleStatus.READY) {
                        busy = true
                        status = ModuleStatus.LOADING
                        scope.launch { showResult(manager.load(), ModuleStatus.LOADED, ModuleStatus.READY) }
                    }
                },
                onUnload = {
                    if (!busy && status == ModuleStatus.LOADED) {
                        busy = true
                        scope.launch { showResult(manager.unload(), ModuleStatus.READY, ModuleStatus.LOADED) }
                    }
                },
                onDelete = {
                    if (!busy && status == ModuleStatus.READY) {
                        busy = true
                        scope.launch { showResult(manager.delete(), ModuleStatus.NOT_IMPORTED, ModuleStatus.READY) }
                    }
                }
            )

            error?.let {
                Text(it, color = Color(0xFFB42318), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }

            Text(
                "Safety: the app validates the GGUF header and stores the module inside its private app storage. Delete stays locked while the module is loaded or an operation is running.",
                color = AppColors.SecondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { BackArrowIcon() }
        Spacer(Modifier.width(10.dp))
        Text("Pawar", color = AppColors.PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModuleCard(
    status: ModuleStatus,
    busy: Boolean,
    hash: String,
    onImport: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    val loaded = status == ModuleStatus.LOADED
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppColors.CardBackground)
            .border(1.dp, AppColors.Line, RoundedCornerShape(24.dp)).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEDEFEA)), contentAlignment = Alignment.Center) {
                Text("Q", color = Color(0xFF3D3E3B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Qwen2.5-Coder", color = AppColors.PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ModelManager.EXPECTED_FILE_NAME, color = AppColors.SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("1.04 GB · Q4_K_M · GGUF", color = AppColors.SecondaryText, fontSize = 12.sp)
            }
            StatusPill(status)
        }
        Spacer(Modifier.height(18.dp))
        ThinDivider()
        Spacer(Modifier.height(16.dp))
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Working… changes are locked", color = AppColors.SecondaryText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Import", enabled = !busy && status != ModuleStatus.LOADED, onClick = onImport, modifier = Modifier.weight(1f))
            ActionButton("Load", enabled = !busy && status == ModuleStatus.READY, onClick = onLoad, modifier = Modifier.weight(1f), primary = true)
            ActionButton("Unload", enabled = !busy && status == ModuleStatus.LOADED, onClick = onUnload, modifier = Modifier.weight(1f))
            ActionButton("Delete", enabled = !busy && status == ModuleStatus.READY, onClick = onDelete, modifier = Modifier.weight(1f), danger = true)
        }
        if (hash.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("SHA-256  ${hash.take(16)}…", color = Color(0xFF9A9C98), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StatusPill(status: ModuleStatus) {
    val (label, color, bg) = when (status) {
        ModuleStatus.NOT_IMPORTED -> Triple("Not imported", Color(0xFF777975), Color(0xFFF0F1F4))
        ModuleStatus.READY -> Triple("Ready", Color(0xFF2C7A55), AppColors.LightGreen)
        ModuleStatus.LOADING -> Triple("Loading", Color(0xFF8A6500), Color(0xFFFFF1C7))
        ModuleStatus.LOADED -> Triple("Loaded", Color(0xFF196C47), AppColors.LightGreen)
        ModuleStatus.ERROR -> Triple("Error", Color(0xFFB42318), Color(0xFFFFE4E0))
    }
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier, primary: Boolean = false, danger: Boolean = false) {
    val bg = if (primary && enabled) Color(0xFF171717) else Color(0xFFF1F2EF)
    val fg = if (primary && enabled) Color.White else if (danger && enabled) Color(0xFFB42318) else Color(0xFF555754)
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(bg).border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) fg else Color(0xFFB9BAB7), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BackArrowIcon() {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val c = Color(0xFF555754)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .50f), androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .50f), 2.2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .50f), androidx.compose.ui.geometry.Offset(size.width * .45f, size.height * .26f), 2.2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .50f), androidx.compose.ui.geometry.Offset(size.width * .45f, size.height * .74f), 2.2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
