package com.rws.learningproject01.engine

import com.rws.learningproject01.core.model.BlendMode
import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.platform.ImageBitmap

expect object CanvasCompositor {
    fun compositeToBitmap(
        layers: List<LayerData>,
        layerOrder: List<String>,
        width: Int,
        height: Int,
        includeBackground: Boolean = true,
        backgroundColor: Int = 0xFFFFFFFF.toInt(),
        frameOverrides: Map<String, Pair<Boolean, Float>> = emptyMap(),
    ): ImageBitmap
}
