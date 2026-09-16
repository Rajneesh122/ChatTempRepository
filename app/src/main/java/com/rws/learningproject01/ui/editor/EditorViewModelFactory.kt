package com.rws.learningproject01.ui.editor

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditorViewModelFactory(
    private val projectId: String,
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            return EditorViewModel(application, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
