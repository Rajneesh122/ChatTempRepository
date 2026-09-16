package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.rws.learningproject01.engine.FilterParams
import com.rws.learningproject01.engine.LayerFilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onFilterSelected: (LayerFilterType) -> Unit,
    onParamsChange: (FilterParams) -> Unit,
    onApply: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val params = uiState.filterParams

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Adjustments", style = MaterialTheme.typography.titleLarge)
            LayerFilterType.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            when (uiState.selectedFilter) {
                LayerFilterType.HSB -> {
                    FilterSlider("Hue shift", params.hueShift, -0.5f..0.5f) {
                        onParamsChange(params.copy(hueShift = it))
                    }
                    FilterSlider("Saturation", params.saturation, 0f..2f) {
                        onParamsChange(params.copy(saturation = it))
                    }
                    FilterSlider("Brightness", params.brightness, -0.5f..0.5f) {
                        onParamsChange(params.copy(brightness = it))
                    }
                }
                LayerFilterType.BLUR -> {
                    FilterSlider("Radius", params.blurRadius.toFloat(), 1f..8f) {
                        onParamsChange(params.copy(blurRadius = it.toInt()))
                    }
                }
                LayerFilterType.NOISE -> {
                    FilterSlider("Amount", params.noiseAmount, 0.05f..0.5f) {
                        onParamsChange(params.copy(noiseAmount = it))
                    }
                }
                LayerFilterType.VIGNETTE -> {
                    FilterSlider("Strength", params.vignetteStrength, 0.1f..1f) {
                        onParamsChange(params.copy(vignetteStrength = it))
                    }
                }
                else -> {
                    Text(
                        "Tap Apply to run ${uiState.selectedFilter.label} on the active layer.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text("Apply to active layer")
            }
        }
    }
}

@Composable
private fun FilterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
