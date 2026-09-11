package com.pawar.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AttachmentMenu(modifier: Modifier, onCamera: () -> Unit, onPhotos: () -> Unit, onZip: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(26.dp)).background(AppColors.CardBackground).border(1.dp, androidx.compose.ui.graphics.Color(0xFFE3E4E1), RoundedCornerShape(26.dp)).padding(12.dp)
    ) {
        Text("Add to chat", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = androidx.compose.ui.graphics.Color(0xFF60615F), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttachmentOption("Camera", AttachmentIcon.CAMERA, onCamera)
            AttachmentOption("Photos", AttachmentIcon.PHOTO, onPhotos)
            AttachmentOption("ZIP", AttachmentIcon.ZIP, onZip)
        }
    }
}

enum class AttachmentIcon { CAMERA, PHOTO, ZIP }

@Composable
private fun AttachmentOption(label: String, icon: AttachmentIcon, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(78.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFF0F1F4)), contentAlignment = Alignment.Center) {
            when (icon) {
                AttachmentIcon.CAMERA -> CameraIcon()
                AttachmentIcon.PHOTO -> PhotoIcon()
                AttachmentIcon.ZIP -> ZipIcon()
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = androidx.compose.ui.graphics.Color(0xFF4E504D), fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun CameraIcon() {
    Canvas(Modifier.size(25.dp)) {
        drawRoundRect(color = androidx.compose.ui.graphics.Color(0xFF555754), topLeft = Offset(3.dp.toPx(), 7.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 12.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()))
        drawCircle(androidx.compose.ui.graphics.Color(0xFFF0F1F4), 4.dp.toPx(), Offset(size.width / 2f, size.height * .60f))
        drawRoundRect(color = androidx.compose.ui.graphics.Color(0xFF555754), topLeft = Offset(size.width * .38f, 3.dp.toPx()), size = Size(size.width * .24f, 5.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
    }
}

@Composable
private fun PhotoIcon() {
    Canvas(Modifier.size(25.dp)) {
        drawRoundRect(color = androidx.compose.ui.graphics.Color(0xFF555754), topLeft = Offset(2.dp.toPx(), 3.dp.toPx()), size = Size(size.width - 4.dp.toPx(), size.height - 6.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(width = 2.2.dp.toPx()))
        drawCircle(androidx.compose.ui.graphics.Color(0xFF555754), 2.dp.toPx(), Offset(size.width * .70f, size.height * .34f))
        val path = Path().apply {
            moveTo(size.width * .12f, size.height * .78f)
            lineTo(size.width * .42f, size.height * .48f)
            lineTo(size.width * .58f, size.height * .64f)
            lineTo(size.width * .73f, size.height * .49f)
            lineTo(size.width * .88f, size.height * .78f)
        }
        drawPath(path, androidx.compose.ui.graphics.Color(0xFF555754), style = Stroke(width = 2.1.dp.toPx()))
    }
}

@Composable
private fun ZipIcon() {
    Box(Modifier.width(21.dp).height(25.dp).clip(RoundedCornerShape(3.dp)).background(androidx.compose.ui.graphics.Color(0xFF555754)), contentAlignment = Alignment.Center) {
        Text("ZIP", color = androidx.compose.ui.graphics.Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ZipMiniIcon(light: Boolean = true) {
    Text("ZIP", color = androidx.compose.ui.graphics.Color(0xFF555754), fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun PlusIcon() {
    Canvas(Modifier.size(27.dp)) {
        drawLine(androidx.compose.ui.graphics.Color(0xFF595A57), Offset(size.width / 2f, size.height * .22f), Offset(size.width / 2f, size.height * .78f), 2.4.dp.toPx(), StrokeCap.Round)
        drawLine(androidx.compose.ui.graphics.Color(0xFF595A57), Offset(size.width * .22f, size.height / 2f), Offset(size.width * .78f, size.height / 2f), 2.4.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun SendArrowIcon() {
    Canvas(Modifier.size(30.dp)) {
        drawLine(androidx.compose.ui.graphics.Color.White, Offset(size.width / 2f, size.height * .76f), Offset(size.width / 2f, size.height * .24f), 2.8.dp.toPx(), StrokeCap.Round)
        drawLine(androidx.compose.ui.graphics.Color.White, Offset(size.width * .27f, size.height * .47f), Offset(size.width / 2f, size.height * .24f), 2.8.dp.toPx(), StrokeCap.Round)
        drawLine(androidx.compose.ui.graphics.Color.White, Offset(size.width * .73f, size.height * .47f), Offset(size.width / 2f, size.height * .24f), 2.8.dp.toPx(), StrokeCap.Round)
    }
}
