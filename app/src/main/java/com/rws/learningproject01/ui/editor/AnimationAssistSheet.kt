package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun AnimationAssistSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSelectFrame: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSaveCelFrame: () -> Unit,
    onOnionBeforeChange: (Float) -> Unit,
    onOnionAfterChange: (Float) -> Unit,
    onOnionOpacityChange: (Float) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Animation Assist", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enabled")
                Switch(checked = uiState.animationEnabled, onCheckedChange = onEnabledChange)
            }
            Text("Frames (${uiState.animationFrameCount})")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed((0 until uiState.animationFrameCount).toList()) { _, index ->
                    FilterChip(
                        selected = uiState.currentAnimationFrame == index,
                        onClick = { onSelectFrame(index) },
                        label = { Text("${index + 1}") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onAddFrame) {
                    Icon(Icons.Default.Add, contentDescription = "Add frame")
                }
                IconButton(onClick = onDuplicateFrame) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate frame")
                }
                IconButton(onClick = onDeleteFrame) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete frame")
                }
                IconButton(onClick = onTogglePlayback) {
                    Icon(
                        if (uiState.animationPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Playback",
                    )
                }
            }
            Button(onClick = onSaveCelFrame, modifier = Modifier.fillMaxWidth()) {
                Text("Save cel frame (duplicate tiles)")
            }
            Text("Onion skin — before: ${uiState.onionSkinBefore.toInt()}")
            Slider(
                value = uiState.onionSkinBefore,
                onValueChange = onOnionBeforeChange,
                valueRange = 0f..4f,
                steps = 3,
            )
            Text("Onion skin — after: ${uiState.onionSkinAfter.toInt()}")
            Slider(
                value = uiState.onionSkinAfter,
                onValueChange = onOnionAfterChange,
                valueRange = 0f..4f,
                steps = 3,
            )
            Text("Onion opacity")
            Slider(
                value = uiState.onionSkinOpacity,
                onValueChange = onOnionOpacityChange,
                valueRange = 0.1f..0.8f,
            )
        }
    }
}
