package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rws.learningproject01.core.model.BlendMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPanelSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onSelectLayer: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onOpacityChange: (Int, Float) -> Unit,
    onBlendModeChange: (Int, BlendMode) -> Unit,
    onToggleClip: (Int) -> Unit,
    onToggleAlphaLock: (Int) -> Unit,
    onAddLayer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Layers", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAddLayer) { Text("Add layer") }
            }
            LazyColumn {
                itemsIndexed(uiState.layers.reversed(), key = { _, layer -> layer.id }) { reversedIndex, layer ->
                    val index = uiState.layers.lastIndex - reversedIndex
                    val selected = index == uiState.activeLayerIndex
                    LayerRow(
                        layer = layer,
                        selected = selected,
                        onSelect = { onSelectLayer(index) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onDelete = { onDelete(index) },
                        onToggleVisibility = { onToggleVisibility(index) },
                        onOpacityChange = { onOpacityChange(index, it) },
                        onBlendModeChange = { onBlendModeChange(index, it) },
                        onToggleClip = { onToggleClip(index) },
                        onToggleAlphaLock = { onToggleAlphaLock(index) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerRow(
    layer: LayerUiModel,
    selected: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (BlendMode) -> Unit,
    onToggleClip: () -> Unit,
    onToggleAlphaLock: () -> Unit,
) {
    var blendExpanded by remember { mutableStateOf(false) }
    val blendMode = BlendMode.fromId(layer.blendMode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Visibility",
                )
            }
            Text(layer.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Opacity", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
            Slider(
                value = layer.opacity,
                onValueChange = onOpacityChange,
                modifier = Modifier.weight(1f),
            )
        }
        ExposedDropdownMenuBox(
            expanded = blendExpanded,
            onExpandedChange = { blendExpanded = it },
        ) {
            TextButton(
                onClick = { blendExpanded = true },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            ) {
                Text("Blend: ${blendMode.id}")
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = blendExpanded)
            }
            ExposedDropdownMenu(expanded = blendExpanded, onDismissRequest = { blendExpanded = false }) {
                BlendMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.id) },
                        onClick = {
                            onBlendModeChange(mode)
                            blendExpanded = false
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onToggleClip) {
                Text(if (layer.clipToBelow) "Clip ✓" else "Clip")
            }
            TextButton(onClick = onToggleAlphaLock) {
                Text(if (layer.alphaLock) "Alpha lock ✓" else "Alpha lock")
            }
        }
    }
}
