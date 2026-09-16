package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushStudioSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onHardnessChange: (Float) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onStreamLineChange: (Float) -> Unit,
    onSmudgeStrengthChange: (Float) -> Unit,
    onQuickShapeToggle: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Brush Studio", style = MaterialTheme.typography.titleLarge)
            BrushSlider("Size", uiState.brushSize, 4f..128f, onSizeChange)
            BrushSlider("Opacity", uiState.brushOpacity, 0.05f..1f, onOpacityChange)
            BrushSlider("Hardness", uiState.brushHardness, 0f..1f, onHardnessChange)
            BrushSlider("Spacing", uiState.brushSpacing, 0.05f..0.5f, onSpacingChange)
            BrushSlider("StreamLine", uiState.streamLine, 0f..1f, onStreamLineChange)
            if (uiState.activeTool == com.rws.learningproject01.engine.DrawingTool.SMUDGE) {
                BrushSlider("Smudge", uiState.smudgeStrength, 0.1f..1f, onSmudgeStrengthChange)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("QuickShape", modifier = Modifier.weight(1f))
                Switch(checked = uiState.quickShapeEnabled, onCheckedChange = { onQuickShapeToggle() })
            }
        }
    }
}

@Composable
private fun BrushSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = "%.0f%%".format(value * if (label == "Size") 1f else 100f),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
