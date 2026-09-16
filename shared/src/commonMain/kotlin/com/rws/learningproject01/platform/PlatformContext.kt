package com.rws.learningproject01.platform

expect class PlatformContext

expect fun platformFilesDir(context: PlatformContext): String

expect fun platformCacheDir(context: PlatformContext): String
