package com.rws.learningproject01.platform

import okio.Path

expect object ThumbnailGenerator {
    fun generatePlaceholder(width: Int, height: Int, output: Path)
}
