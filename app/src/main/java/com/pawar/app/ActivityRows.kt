package com.pawar.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RowIcon { CLOCK, FOLDER }

@Composable
fun ThinDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Line))
}

@Composable
fun ActivityRow(leading: RowIcon, text: String, trailing: String? = null, showChevron: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (leading) {
            RowIcon.CLOCK -> ClockIcon()
            RowIcon.FOLDER -> FolderOutlineIcon()
        }
        Spacer(Modifier.width(32.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = androidx.compose.ui.graphics.Color(0xFF60615F),
            fontSize = 21.sp,
            lineHeight = 29.sp,
            maxLines = if (trailing != null) 1 else 2
        )
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            Text(
                trailing,
                color = androidx.compose.ui.graphics.Color(0xFFA8A9A6),
                fontSize = 20.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
        if (showChevron) ChevronIcon()
    }
}

@Composable
private fun ClockIcon() {
    Box(Modifier.size(28.dp)) {
        Canvas(Modifier.fillMaxWidth()) {
            drawCircle(androidx.compose.ui.graphics.Color(0xFF9A9C98), size.minDimension * 0.39f, center, style = Stroke(2.2.dp.toPx()))
            drawLine(androidx.compose.ui.graphics.Color(0xFF9A9C98), Offset(size.width * .50f, size.height * .50f), Offset(size.width * .50f, size.height * .29f), 2.2.dp.toPx(), StrokeCap.Round)
            drawLine(androidx.compose.ui.graphics.Color(0xFF9A9C98), Offset(size.width * .50f, size.height * .50f), Offset(size.width * .66f, size.height * .57f), 2.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun FolderOutlineIcon() {
    Box(Modifier.size(32.dp)) {
        Canvas(Modifier.fillMaxWidth()) {
            val path = Path().apply {
                moveTo(size.width * .10f, size.height * .29f)
                lineTo(size.width * .40f, size.height * .29f)
                lineTo(size.width * .50f, size.height * .42f)
                lineTo(size.width * .89f, size.height * .42f)
                lineTo(size.width * .89f, size.height * .82f)
                lineTo(size.width * .10f, size.height * .82f)
                close()
            }
            drawPath(path, androidx.compose.ui.graphics.Color(0xFF9A9C98), style = Stroke(2.1.dp.toPx()))
        }
    }
}

@Composable
private fun ChevronIcon() {
    Canvas(Modifier.size(25.dp)) {
        drawLine(androidx.compose.ui.graphics.Color(0xFFB4B5B2), Offset(size.width * .36f, size.height * .22f), Offset(size.width * .65f, size.height * .50f), 2.2.dp.toPx(), StrokeCap.Round)
        drawLine(androidx.compose.ui.graphics.Color(0xFFB4B5B2), Offset(size.width * .65f, size.height * .50f), Offset(size.width * .36f, size.height * .78f), 2.2.dp.toPx(), StrokeCap.Round)
    }
}
