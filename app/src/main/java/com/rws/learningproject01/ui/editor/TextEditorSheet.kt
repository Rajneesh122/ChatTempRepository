package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorSheet(
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onApply: () -> Unit,
    onAddLayer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Text Layer", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = uiState.textLayerContent,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Text") },
                minLines = 2,
            )
            Text("Font size: ${uiState.textLayerFontSize.toInt()}sp")
            Slider(
                value = uiState.textLayerFontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..200f,
            )
            if (uiState.editingTextLayer) {
                Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                    Text("Update text layer")
                }
            } else {
                Button(onClick = onAddLayer, modifier = Modifier.fillMaxWidth()) {
                    Text("Add text layer at tap point")
                }
            }
        }
    }
}
