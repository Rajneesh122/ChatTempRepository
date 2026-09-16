package com.rws.learningproject01.platform

import android.content.Context

actual class PlatformContext(val androidContext: Context)

actual fun platformFilesDir(context: PlatformContext): String =
    context.androidContext.filesDir.absolutePath

actual fun platformCacheDir(context: PlatformContext): String =
    context.androidContext.cacheDir.absolutePath

fun createPlatformContext(context: Context): PlatformContext = PlatformContext(context)
