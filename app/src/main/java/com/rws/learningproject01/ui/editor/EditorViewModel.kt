package com.rws.learningproject01.ui.editor

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rws.learningproject01.ArtApplication
import com.rws.learningproject01.engine.ArtEngine
import com.rws.learningproject01.engine.DrawingTool
import com.rws.learningproject01.engine.FilterParams
import com.rws.learningproject01.engine.HarmonyType
import com.rws.learningproject01.engine.LayerFilterType
import com.rws.learningproject01.engine.SymmetryMode
import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.core.model.ExportFormat
import com.rws.learningproject01.platform.createPlatformContext
import com.rws.learningproject01.viewmodel.EditorViewModel as SharedEditorViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.File

typealias EditorUiState = com.rws.learningproject01.viewmodel.EditorUiState
typealias LayerUiModel = com.rws.learningproject01.viewmodel.LayerUiModel

class EditorViewModel(
    application: Application,
    projectId: String,
) : AndroidViewModel(application) {

    private val shared = SharedEditorViewModel(
        repository = (application as ArtApplication).projectRepository,
        platformContext = createPlatformContext(application),
        scope = viewModelScope,
        projectId = projectId,
    )

    val uiState: StateFlow<EditorUiState> = shared.uiState

    fun engineOrNull(): ArtEngine? = shared.engineOrNull()
    fun onStrokeFinished() = shared.onStrokeFinished()
    fun undo() = shared.undo()
    fun redo() = shared.redo()
    fun setActiveTool(tool: DrawingTool) = shared.setActiveTool(tool)
    fun setBrushSize(size: Float) = shared.setBrushSize(size)
    fun setBrushOpacity(opacity: Float) = shared.setBrushOpacity(opacity)
    fun setBrushHardness(hardness: Float) = shared.setBrushHardness(hardness)
    fun setBrushSpacing(spacing: Float) = shared.setBrushSpacing(spacing)
    fun setStreamLine(value: Float) = shared.setStreamLine(value)
    fun setSmudgeStrength(value: Float) = shared.setSmudgeStrength(value)
    fun setBrushColor(color: Int) = shared.setBrushColor(color)
    fun setSymmetryMode(mode: SymmetryMode) = shared.setSymmetryMode(mode)
    fun toggleQuickShape() = shared.toggleQuickShape()
    fun toggleBrushStudio() = shared.toggleBrushStudio()
    fun toggleLayerPanel() = shared.toggleLayerPanel()
    fun toggleColorPanel() = shared.toggleColorPanel()
    fun toggleFiltersPanel() = shared.toggleFiltersPanel()
    fun setColorFromHsv(hue: Float? = null, saturation: Float? = null, value: Float? = null) =
        shared.setColorFromHsv(hue, saturation, value)
    fun setHarmonyType(type: HarmonyType) = shared.setHarmonyType(type)
    fun selectColor(color: Int) = shared.selectColor(color)
    fun onColorPicked(color: Int) = shared.onColorPicked(color)
    fun addColorToPalette() = shared.addColorToPalette()
    fun setColorDropTolerance(value: Float) = shared.setColorDropTolerance(value)
    fun setSelectedFilter(filter: LayerFilterType) = shared.setSelectedFilter(filter)
    fun setFilterParams(params: FilterParams) = shared.setFilterParams(params)
    fun applySelectedFilter() = shared.applySelectedFilter()
    fun addLayer() = shared.addLayer()
    fun selectLayer(index: Int) = shared.selectLayer(index)
    fun deleteLayer(index: Int) = shared.deleteLayer(index)
    fun moveLayerUp(index: Int) = shared.moveLayerUp(index)
    fun moveLayerDown(index: Int) = shared.moveLayerDown(index)
    fun setLayerOpacity(index: Int, opacity: Float) = shared.setLayerOpacity(index, opacity)
    fun setLayerBlendMode(index: Int, blendMode: BlendMode) = shared.setLayerBlendMode(index, blendMode)
    fun toggleLayerVisibility(index: Int) = shared.toggleLayerVisibility(index)
    fun toggleLayerLock(index: Int) = shared.toggleLayerLock(index)
    fun toggleLayerClipMask(index: Int) = shared.toggleLayerClipMask(index)
    fun toggleLayerAlphaLock(index: Int) = shared.toggleLayerAlphaLock(index)
    fun saveNow() = shared.saveNow()
    fun startExport(onComplete: (Uri?) -> Unit) {
        shared.startExport { path ->
            val uri = path?.let {
                FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    File(it),
                )
            }
            onComplete(uri)
        }
    }
    suspend fun exportPngAndGetUri(): Uri? {
        val path = shared.exportPngAndGetPath()
        return path?.let {
            FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                File(it),
            )
        }
    }
    fun onPause() = shared.onPause()
    fun toggleExportPanel() = shared.toggleExportPanel()
    fun toggleAnimationPanel() = shared.toggleAnimationPanel()
    fun toggleTextEditor() = shared.toggleTextEditor()
    fun setExportFormat(format: ExportFormat) = shared.setExportFormat(format)
    fun setExportTransparentBackground(enabled: Boolean) = shared.setExportTransparentBackground(enabled)
    fun setExportAnimation(enabled: Boolean) = shared.setExportAnimation(enabled)
    fun setExportTimelapse4K(enabled: Boolean) = shared.setExportTimelapse4K(enabled)
    fun setAnimationEnabled(enabled: Boolean) = shared.setAnimationEnabled(enabled)
    fun selectAnimationFrame(index: Int) = shared.selectAnimationFrame(index)
    fun addAnimationFrame() = shared.addAnimationFrame()
    fun duplicateAnimationFrame() = shared.duplicateAnimationFrame()
    fun deleteAnimationFrame() = shared.deleteAnimationFrame()
    fun toggleAnimationPlayback() = shared.toggleAnimationPlayback()
    fun saveCelFrame() = shared.saveCelFrame()
    fun setOnionSkinBefore(value: Float) = shared.setOnionSkinBefore(value)
    fun setOnionSkinAfter(value: Float) = shared.setOnionSkinAfter(value)
    fun setOnionSkinOpacity(value: Float) = shared.setOnionSkinOpacity(value)
    fun onTextPlaced(x: Float, y: Float) = shared.onTextPlaced(x, y)
    fun setTextLayerContent(text: String) = shared.setTextLayerContent(text)
    fun setTextLayerFontSize(size: Float) = shared.setTextLayerFontSize(size)
    fun addTextLayerAtPending() = shared.addTextLayerAtPending()
    fun applyTextLayerEdits() = shared.applyTextLayerEdits()
    fun importPalette(fileName: String, bytes: ByteArray) = shared.importPalette(fileName, bytes)
}
