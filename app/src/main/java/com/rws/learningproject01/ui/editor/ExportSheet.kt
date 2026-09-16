package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rws.learningproject01.core.model.ExportFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit,
    onTransparentBackgroundChange: (Boolean) -> Unit,
    onExportAnimationChange: (Boolean) -> Unit,
    onTimelapse4KChange: (Boolean) -> Unit,
    onExport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Export", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text("Format")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExportFormat.entries.take(5).forEach { format ->
                    FilterChip(
                        selected = uiState.exportFormat == format,
                        onClick = { onFormatSelected(format) },
                        label = { Text(format.name) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ExportFormat.entries.drop(5).forEach { format ->
                    FilterChip(
                        selected = uiState.exportFormat == format,
                        onClick = { onFormatSelected(format) },
                        label = { Text(format.name) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Transparent background")
                Switch(
                    checked = uiState.exportTransparentBackground,
                    onCheckedChange = onTransparentBackgroundChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Export animation frames")
                Switch(
                    checked = uiState.exportAnimation,
                    onCheckedChange = onExportAnimationChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Time-lapse 4K (HEVC)")
                Switch(
                    checked = uiState.exportTimelapse4K,
                    onCheckedChange = onTimelapse4KChange,
                )
            }
            Button(
                onClick = onExport,
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isExporting) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Exporting…")
                    }
                } else {
                    Text("Export & Share")
                }
            }
        }
    }
}
