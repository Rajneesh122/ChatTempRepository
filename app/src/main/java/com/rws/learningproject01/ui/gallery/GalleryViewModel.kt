package com.rws.learningproject01.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rws.learningproject01.ArtApplication
import com.rws.learningproject01.viewmodel.GalleryViewModel as SharedGalleryViewModel
import kotlinx.coroutines.flow.StateFlow

typealias GalleryUiState = com.rws.learningproject01.viewmodel.GalleryUiState

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val shared = SharedGalleryViewModel(
        repository = (application as ArtApplication).projectRepository,
        scope = viewModelScope,
    )

    val uiState: StateFlow<GalleryUiState> = shared.uiState

    fun refresh() = shared.refresh()
    fun createProject(onCreated: (String) -> Unit) = shared.createProject(onCreated)
    fun clearError() = shared.clearError()
    fun importPsd(fileName: String, bytes: ByteArray, onCreated: (String) -> Unit) =
        shared.importPsd(fileName, bytes, onCreated)
    fun deleteProject(projectId: String) = shared.deleteProject(projectId)
}
