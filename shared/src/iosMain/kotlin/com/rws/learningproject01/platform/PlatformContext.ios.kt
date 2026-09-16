package com.rws.learningproject01.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class PlatformContext

private fun directoryPath(directory: ULong): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(
        directory,
        NSUserDomainMask,
    )
    val url = urls?.firstOrNull() as? NSURL
    return url?.path ?: error("Directory unavailable")
}

actual fun platformFilesDir(context: PlatformContext): String =
    directoryPath(NSDocumentDirectory)

actual fun platformCacheDir(context: PlatformContext): String =
    directoryPath(NSCachesDirectory)

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

fun createPlatformContext(): PlatformContext = PlatformContext()
