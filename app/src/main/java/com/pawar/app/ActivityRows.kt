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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RowIcon { CLOCK, FILE_CREATED }

@Composable
fun ThinDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Line))
}

@Composable
fun ActivityRow(leading: RowIcon, text: String, trailing: String? = null, showChevron: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (leading) {
            RowIcon.CLOCK -> ClockIcon()
            RowIcon.FILE_CREATED -> FileCreatedIcon()
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = androidx.compose.ui.graphics.Color(0xFF60615F),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            maxLines = if (trailing != null) 1 else 2
        )
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                trailing,
                color = androidx.compose.ui.graphics.Color(0xFFA8A9A6),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (showChevron) ChevronIcon()
    }
}

@Composable
private fun ClockIcon() {
    Box(Modifier.size(28.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(androidx.compose.ui.graphics.Color(0xFF9A9C98), size.minDimension * 0.39f, center, style = Stroke(2.2.dp.toPx()))
            drawLine(androidx.compose.ui.graphics.Color(0xFF9A9C98), Offset(size.width * .50f, size.height * .50f), Offset(size.width * .50f, size.height * .29f), 2.2.dp.toPx(), StrokeCap.Round)
            drawLine(androidx.compose.ui.graphics.Color(0xFF9A9C98), Offset(size.width * .50f, size.height * .50f), Offset(size.width * .66f, size.height * .57f), 2.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

// Small file-with-a-plus icon shown whenever a row represents a file being
// created. Sized to sit level with the row's text, not a full-size icon.
@Composable
private fun FileCreatedIcon() {
    Box(Modifier.size(18.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val fileColor = androidx.compose.ui.graphics.Color(0xFF9A9C98)
            val path = Path().apply {
                moveTo(size.width * .18f, size.height * .06f)
                lineTo(size.width * .60f, size.height * .06f)
                lineTo(size.width * .82f, size.height * .28f)
                lineTo(size.width * .82f, size.height * .94f)
                lineTo(size.width * .18f, size.height * .94f)
                close()
            }
            drawPath(path, fileColor, style = Stroke(1.6.dp.toPx()))
            // fold corner
            drawLine(fileColor, Offset(size.width * .60f, size.height * .06f), Offset(size.width * .60f, size.height * .28f), 1.6.dp.toPx(), StrokeCap.Round)
            drawLine(fileColor, Offset(size.width * .60f, size.height * .28f), Offset(size.width * .82f, size.height * .28f), 1.6.dp.toPx(), StrokeCap.Round)
            // plus sign, badge-style, bottom-right of the file
            val plusColor = AppColors.Green
            val cx = size.width * .78f
            val cy = size.height * .78f
            val arm = size.width * .16f
            drawLine(plusColor, Offset(cx - arm, cy), Offset(cx + arm, cy), 1.8.dp.toPx(), StrokeCap.Round)
            drawLine(plusColor, Offset(cx, cy - arm), Offset(cx, cy + arm), 1.8.dp.toPx(), StrokeCap.Round)
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
