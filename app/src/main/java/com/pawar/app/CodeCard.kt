package com.pawar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(androidx.compose.ui.graphics.Color(0xFFFBFCFE), RoundedCornerShape(30.dp))
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE8E9E8), RoundedCornerShape(30.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("StudentActivity.kt", color = androidx.compose.ui.graphics.Color(0xFF4B4C4A), fontSize = 20.sp, letterSpacing = 0.2.sp)
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(56.dp).height(6.dp).background(AppColors.Green, RoundedCornerShape(50)))
            Spacer(Modifier.width(4.dp))
            Text("Writing", color = AppColors.Green, fontSize = 19.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        ThinDivider()
        Spacer(Modifier.height(304.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(55.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0", color = androidx.compose.ui.graphics.Color(0xFF2A2A2A), fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(" tokens", color = androidx.compose.ui.graphics.Color(0xFF7D7F7C), fontSize = 17.sp)
            Spacer(Modifier.width(38.dp))
            Text("0", color = androidx.compose.ui.graphics.Color(0xFF2A2A2A), fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(" tok/s", color = androidx.compose.ui.graphics.Color(0xFF7D7F7C), fontSize = 17.sp)
            Spacer(Modifier.width(30.dp))
            Text("ctx", color = androidx.compose.ui.graphics.Color(0xFF7D7F7C), fontSize = 17.sp)
            Spacer(Modifier.width(18.dp))
            Box(Modifier.weight(1f).height(8.dp).background(AppColors.ProgressTrack, RoundedCornerShape(50)))
            Spacer(Modifier.width(12.dp))
            Text("0%", color = androidx.compose.ui.graphics.Color(0xFF7D7F7C), fontSize = 17.sp)
        }
    }
}
