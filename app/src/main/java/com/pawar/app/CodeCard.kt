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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeCard(codeLines: List<String> = DEFAULT_CODE_LINES) {
    val codeScroll = rememberScrollState()

    // During a real streamed response, new code lines/tokens will update this list.
    // Keep the code viewport pinned to the newest content automatically.
    LaunchedEffect(codeLines.joinToString("\n")) {
        withFrameNanos { }
        codeScroll.scrollTo(codeScroll.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFFFBFCFE), RoundedCornerShape(26.dp))
            .border(1.dp, Color(0xFFE8E9E8), RoundedCornerShape(26.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "StudentActivity.kt",
                color = Color(0xFF4B4C4A),
                fontSize = 15.sp,
                letterSpacing = 0.1.sp,
                maxLines = 1
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .background(AppColors.Green, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "Writing",
                color = AppColors.Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        ThinDivider()

        // Exactly five compact code lines are visible at once; the rest can be scrolled.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .verticalScroll(codeScroll)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            codeLines.forEach { line ->
                Text(
                    text = if (line.isEmpty()) " " else line,
                    color = Color(0xFF4B4C4A),
                    fontSize = 11.sp,
                    lineHeight = 22.sp,
                    maxLines = 1
                )
            }
        }

        ThinDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0", color = Color(0xFF2A2A2A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(" tokens", color = Color(0xFF7D7F7C), fontSize = 12.sp)
            Spacer(Modifier.width(22.dp))
            Text("0", color = Color(0xFF2A2A2A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(" tok/s", color = Color(0xFF7D7F7C), fontSize = 12.sp)
            Spacer(Modifier.width(20.dp))
            Text("ctx", color = Color(0xFF7D7F7C), fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(AppColors.ProgressTrack, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(8.dp))
            Text("0%", color = Color(0xFF7D7F7C), fontSize = 12.sp)
        }
    }
}

private val DEFAULT_CODE_LINES = listOf(
    "data class Student(val name: String, val age: Int)",
    "",
    "private val students = mutableListOf<Student>()",
    "",
    "fun addStudent(name: String, age: Int) {",
    "    students.add(Student(name, age))",
    "}",
    "",
    "fun removeStudent(name: String) {",
    "    students.removeAll { it.name == name }",
    "}",
    "",
    "fun getStudents(): List<Student> = students.toList()"
)
