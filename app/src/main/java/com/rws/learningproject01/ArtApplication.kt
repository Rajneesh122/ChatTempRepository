package com.rws.learningproject01

import android.app.Application
import com.rws.learningproject01.core.storage.ProjectRepository
import com.rws.learningproject01.platform.createPlatformContext

class ArtApplication : Application() {
    lateinit var projectRepository: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        projectRepository = ProjectRepository(createPlatformContext(this))
    }
}
