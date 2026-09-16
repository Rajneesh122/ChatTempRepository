package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rws.learningproject01.engine.HarmonyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPanelSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onHarmonyTypeChange: (HarmonyType) -> Unit,
    onColorSelected: (Int) -> Unit,
    onAddToPalette: () -> Unit,
    onToleranceChange: (Float) -> Unit,
    onImportPalette: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Color", style = MaterialTheme.typography.titleLarge)
            Text("Active color", style = MaterialTheme.typography.labelMedium)
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(uiState.brushColor))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            HsvSlider("Hue", uiState.colorHue, 0f..360f, onHueChange)
            HsvSlider("Saturation", uiState.colorSaturation, 0f..1f, onSaturationChange)
            HsvSlider("Brightness", uiState.colorValue, 0f..1f, onValueChange)

            Text("Harmony", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HarmonyType.entries) { type ->
                    FilterChip(
                        selected = uiState.harmonyType == type,
                        onClick = { onHarmonyTypeChange(type) },
                        label = { Text(type.label) },
                    )
                }
            }
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.harmonyColors) { color ->
                    ColorSwatch(color = color, selected = color == uiState.brushColor, onClick = { onColorSelected(color) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Palette", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImportPalette) { Text("Import ASE/ACO") }
                    Button(onClick = onAddToPalette) { Text("Add swatch") }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.savedPalette) { color ->
                    ColorSwatch(color = color, selected = color == uiState.brushColor, onClick = { onColorSelected(color) })
                }
            }

            Text(
                "ColorDrop tolerance",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Slider(
                value = uiState.colorDropTolerance.toFloat(),
                onValueChange = { onToleranceChange(it) },
                valueRange = 0f..128f,
            )
        }
    }
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
