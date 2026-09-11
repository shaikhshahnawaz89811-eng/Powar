package com.pawar.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun OfflineHeader(onMenu: () -> Unit, moduleLoaded: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(30.dp))
            .background(AppColors.CardBackground).border(1.dp, Color(0xFFECEDE9), RoundedCornerShape(30.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onMenu), contentAlignment = Alignment.Center) {
            MenuIcon()
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Pawar", color = Color(0xFF262626), fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
            Text(if (moduleLoaded) "qwen2.5-coder-7b · loaded · 100% offline" else "qwen2.5-coder-7b · not loaded · 100% offline", color = AppColors.SecondaryText, fontSize = 18.sp, lineHeight = 23.sp, maxLines = 1)
        }
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(if (moduleLoaded) AppColors.LightGreen else Color(0xFFF0F1F4)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(if (moduleLoaded) AppColors.Green else Color(0xFF9A9C98)))
        }
    }
}

@Composable
private fun MenuIcon() {
    Canvas(Modifier.size(25.dp)) {
        val c = Color(0xFF555754)
        drawLine(c, androidx.compose.ui.geometry.Offset(4.dp.toPx(), 6.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), 6.dp.toPx()), 2.2.dp.toPx(), StrokeCap.Round)
        drawLine(c, androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height / 2f), androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height / 2f), 2.2.dp.toPx(), StrokeCap.Round)
        drawLine(c, androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height - 6.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height - 6.dp.toPx()), 2.2.dp.toPx(), StrokeCap.Round)
    }
}
