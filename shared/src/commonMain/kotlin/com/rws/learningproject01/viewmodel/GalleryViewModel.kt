package com.rws.learningproject01.viewmodel

import com.rws.learningproject01.core.model.GalleryEntry
import com.rws.learningproject01.core.storage.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val projects: List<GalleryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null,
)

class GalleryViewModel(
    private val repository: ProjectRepository,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.listGallery() }
                .onSuccess { projects ->
                    _uiState.update { it.copy(projects = projects, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Failed to load gallery")
                    }
                }
        }
    }

    fun createProject(onCreated: (String) -> Unit) {
        if (_uiState.value.isCreating) return
        scope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            runCatching { repository.createProject() }
                .onSuccess { openProject ->
                    refresh()
                    onCreated(openProject.manifest.projectId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to create project")
                    }
                }
            _uiState.update { it.copy(isCreating = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun importPsd(fileName: String, bytes: ByteArray, onCreated: (String) -> Unit) {
        scope.launch {
            runCatching { repository.createProjectFromPsd(fileName, bytes) }
                .onSuccess { project ->
                    refresh()
                    onCreated(project.manifest.projectId)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "PSD import failed") }
                }
        }
    }

    fun deleteProject(projectId: String) {
        scope.launch {
            runCatching { repository.deleteProject(projectId) }
                .onSuccess { refresh() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to delete project") }
                }
        }
    }
}
