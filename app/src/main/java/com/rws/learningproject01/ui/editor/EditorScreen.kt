package com.rws.learningproject01.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rws.learningproject01.engine.ArtEngine
import com.rws.learningproject01.engine.DrawingTool
import com.rws.learningproject01.engine.DrawingSurface
import com.rws.learningproject01.engine.SymmetryMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory(
            projectId,
            LocalContext.current.applicationContext as android.app.Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.error, uiState.saveMessage) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        uiState.saveMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val paletteImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment ?: "palette.ase"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.importPalette(name, bytes)
    }

    if (uiState.showExportPanel) {
        ExportSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleExportPanel() },
            onFormatSelected = viewModel::setExportFormat,
            onTransparentBackgroundChange = viewModel::setExportTransparentBackground,
            onExportAnimationChange = viewModel::setExportAnimation,
            onTimelapse4KChange = viewModel::setExportTimelapse4K,
            onExport = {
                viewModel.startExport { uri ->
                    if (uri != null) {
                        val mime = uiState.exportFormat.mimeType
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = mime
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share export"))
                    }
                }
            },
        )
    }

    if (uiState.showAnimationPanel) {
        AnimationAssistSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleAnimationPanel() },
            onEnabledChange = viewModel::setAnimationEnabled,
            onSelectFrame = viewModel::selectAnimationFrame,
            onAddFrame = viewModel::addAnimationFrame,
            onDuplicateFrame = viewModel::duplicateAnimationFrame,
            onDeleteFrame = viewModel::deleteAnimationFrame,
            onTogglePlayback = viewModel::toggleAnimationPlayback,
            onSaveCelFrame = viewModel::saveCelFrame,
            onOnionBeforeChange = viewModel::setOnionSkinBefore,
            onOnionAfterChange = viewModel::setOnionSkinAfter,
            onOnionOpacityChange = viewModel::setOnionSkinOpacity,
        )
    }

    if (uiState.showTextEditor) {
        TextEditorSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleTextEditor() },
            onTextChange = viewModel::setTextLayerContent,
            onFontSizeChange = viewModel::setTextLayerFontSize,
            onApply = viewModel::applyTextLayerEdits,
            onAddLayer = viewModel::addTextLayerAtPending,
        )
    }

    if (uiState.showBrushStudio) {
        BrushStudioSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleBrushStudio() },
            onSizeChange = viewModel::setBrushSize,
            onOpacityChange = viewModel::setBrushOpacity,
            onHardnessChange = viewModel::setBrushHardness,
            onSpacingChange = viewModel::setBrushSpacing,
            onStreamLineChange = viewModel::setStreamLine,
            onSmudgeStrengthChange = viewModel::setSmudgeStrength,
            onQuickShapeToggle = { viewModel.toggleQuickShape() },
        )
    }

    if (uiState.showLayerPanel) {
        LayerPanelSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleLayerPanel() },
            onSelectLayer = viewModel::selectLayer,
            onMoveUp = viewModel::moveLayerUp,
            onMoveDown = viewModel::moveLayerDown,
            onDelete = viewModel::deleteLayer,
            onToggleVisibility = viewModel::toggleLayerVisibility,
            onOpacityChange = viewModel::setLayerOpacity,
            onBlendModeChange = viewModel::setLayerBlendMode,
            onToggleClip = viewModel::toggleLayerClipMask,
            onToggleAlphaLock = viewModel::toggleLayerAlphaLock,
            onAddLayer = viewModel::addLayer,
        )
    }

    if (uiState.showColorPanel) {
        ColorPanelSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleColorPanel() },
            onHueChange = { viewModel.setColorFromHsv(hue = it) },
            onSaturationChange = { viewModel.setColorFromHsv(saturation = it) },
            onValueChange = { viewModel.setColorFromHsv(value = it) },
            onHarmonyTypeChange = viewModel::setHarmonyType,
            onColorSelected = viewModel::selectColor,
            onAddToPalette = viewModel::addColorToPalette,
            onToleranceChange = viewModel::setColorDropTolerance,
            onImportPalette = { paletteImportLauncher.launch(arrayOf("*/*")) },
        )
    }

    if (uiState.showFiltersPanel) {
        FiltersSheet(
            uiState = uiState,
            onDismiss = { viewModel.toggleFiltersPanel() },
            onFilterSelected = viewModel::setSelectedFilter,
            onParamsChange = viewModel::setFilterParams,
            onApply = viewModel::applySelectedFilter,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onPause()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = uiState.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = uiState.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { viewModel.toggleColorPanel() }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color")
                    }
                    IconButton(onClick = { viewModel.toggleFiltersPanel() }) {
                        Icon(Icons.Default.Tune, contentDescription = "Filters")
                    }
                    IconButton(onClick = { viewModel.toggleBrushStudio() }) {
                        Icon(Icons.Default.Brush, contentDescription = "Brush Studio")
                    }
                    IconButton(onClick = { viewModel.toggleLayerPanel() }) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers")
                    }
                    IconButton(onClick = { viewModel.toggleAnimationPanel() }) {
                        Icon(Icons.Default.Animation, contentDescription = "Animation")
                    }
                    IconButton(onClick = { viewModel.toggleTextEditor() }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Text")
                    }
                    IconButton(onClick = { viewModel.saveNow() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { viewModel.toggleExportPanel() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !uiState.isEngineReady -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Could not open project",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Button(
                            onClick = onBack,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text("Back to gallery")
                        }
                    }
                }
            }
            else -> {
                val artEngine = viewModel.engineOrNull()
                if (artEngine == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    EditorCanvasContent(
                        padding = padding,
                        uiState = uiState,
                        artEngine = artEngine,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorCanvasContent(
    padding: PaddingValues,
    uiState: EditorUiState,
    artEngine: ArtEngine,
    viewModel: EditorViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF121212)),
        ) {
            key(uiState.projectId) {
                AndroidView(
                    factory = { ctx ->
                        DrawingSurface(ctx, artEngine).also { surface ->
                            surface.renderer.canvasWidth = uiState.canvasWidth
                            surface.renderer.canvasHeight = uiState.canvasHeight
                            surface.inputEnabled = uiState.isEngineReady && !uiState.isExporting && !uiState.isLoading
                            surface.onStrokeFinished = { viewModel.onStrokeFinished() }
                            surface.onColorPicked = { viewModel.onColorPicked(it) }
                            surface.onTextPlaced = { x, y -> viewModel.onTextPlaced(x, y) }
                        }
                    },
                    update = { surface ->
                        surface.inputEnabled = uiState.isEngineReady && !uiState.isExporting && !uiState.isLoading
                        surface.onStrokeFinished = { viewModel.onStrokeFinished() }
                        surface.onColorPicked = { viewModel.onColorPicked(it) }
                        surface.onTextPlaced = { x, y -> viewModel.onTextPlaced(x, y) }
                        surface.requestCanvasRender()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ToolSelectorRow(
            activeTool = uiState.activeTool,
            onToolSelected = viewModel::setActiveTool,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SymmetryMode.entries.size) { index ->
                val mode = SymmetryMode.entries[index]
                FilterChip(
                    selected = uiState.symmetryMode == mode,
                    onClick = { viewModel.setSymmetryMode(mode) },
                    label = { Text(mode.label) },
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.savedPalette.size) { index ->
                val color = uiState.savedPalette[index]
                val selected = uiState.brushColor == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape,
                        )
                        .clickable { viewModel.selectColor(color) },
                )
            }
        }
    }
}

@Composable
private fun ToolSelectorRow(
    activeTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ToolChip("Brush", Icons.Default.Brush, activeTool == DrawingTool.BRUSH) {
            onToolSelected(DrawingTool.BRUSH)
        }
        ToolChip("Eraser", Icons.Default.Brush, activeTool == DrawingTool.ERASER) {
            onToolSelected(DrawingTool.ERASER)
        }
        ToolChip("Smudge", Icons.Default.WaterDrop, activeTool == DrawingTool.SMUDGE) {
            onToolSelected(DrawingTool.SMUDGE)
        }
        ToolChip("Fill", Icons.Default.FormatColorFill, activeTool == DrawingTool.COLOR_DROP) {
            onToolSelected(DrawingTool.COLOR_DROP)
        }
        ToolChip("Pick", Icons.Default.Colorize, activeTool == DrawingTool.COLOR_PICKER) {
            onToolSelected(DrawingTool.COLOR_PICKER)
        }
        ToolChip("Text", Icons.Default.TextFields, activeTool == DrawingTool.TEXT) {
            onToolSelected(DrawingTool.TEXT)
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) },
    )
}
