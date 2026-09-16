package com.rws.learningproject01.navigation

import android.net.Uri

object Routes {
    const val GALLERY = "gallery"
    const val EDITOR = "editor?projectId={projectId}"

    fun editor(projectId: String): String =
        "editor?projectId=${Uri.encode(projectId)}"
}
