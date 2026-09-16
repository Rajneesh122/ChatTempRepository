package com.rws.learningproject01.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object IosCoroutineScope {
    fun mainScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}
