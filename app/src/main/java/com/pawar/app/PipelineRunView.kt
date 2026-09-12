package com.pawar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Activity UI is driven only by actions that the orchestrator actually emitted. */
@Composable
fun PipelineRunView(
    run: PipelineRun,
    onOptionSelected: ((AgentOption) -> Unit)? = null,
    onShareArtifact: ((String) -> Unit)? = null
) {
    var expanded by remember(run.steps.size, run.complete) { mutableStateOf(!run.complete) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        if (run.steps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(8.dp).background(if (run.status == AgentStatus.BLOCKED) Color(0xFFB42318) else AppColors.Green, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (run.complete) "${run.steps.size} steps" else "Working · ${run.steps.size} step(s)",
                    color = AppColors.PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(if (expanded) "⌃" else "⌄", color = AppColors.SecondaryText, fontSize = 16.sp)
            }

            if (expanded) {
                run.steps.forEach { step -> PipelineStepRow(step) }
            }
        }

        run.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onOptionSelected != null) { onOptionSelected?.invoke(option) }
                    .padding(horizontal = 30.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("${option.id}. ", color = AppColors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f)) {
                    Text(option.title, color = AppColors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(option.description, color = AppColors.SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        run.toolResults.forEach { result -> ToolResultRow(result) }

        run.verification?.let { verification ->
            Text(
                text = "Verification: ${verification.detail}",
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 5.dp),
                color = if (verification.success) AppColors.Green else Color(0xFFB42318),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        run.artifactPath?.let { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onShareArtifact != null) { onShareArtifact?.invoke(path) }
                    .padding(horizontal = 30.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ZIP artifact ready", color = AppColors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Share", color = AppColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (run.response.isNotBlank()) {
            Spacer(Modifier.size(12.dp))
            AssistantText(run.response)
        }
    }
}

@Composable
private fun ToolResultRow(result: ToolResult) {
    val color = if (result.success) AppColors.Green else Color(0xFFB42318)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp, vertical = 5.dp)
    ) {
        Text(result.summary, color = AppColors.PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(result.detail, color = color, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PipelineStepRow(step: PipelineStep) {
    val (marker, label) = when (step.state) {
        PipelineStepState.ACTIVE -> Color(0xFFD59A00) to "Working…"
        PipelineStepState.COMPLETED -> AppColors.Green to "Done"
        PipelineStepState.FAILED -> Color(0xFFB42318) to "Failed"
        PipelineStepState.BLOCKED -> Color(0xFFB42318) to "Blocked"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(Modifier.padding(top = 4.dp).size(8.dp).background(marker, CircleShape))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(step.title, color = AppColors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(step.detail, color = AppColors.SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text(label, color = marker, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
