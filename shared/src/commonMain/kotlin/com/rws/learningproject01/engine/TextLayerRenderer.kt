package com.rws.learningproject01.engine

import com.rws.learningproject01.platform.argbToHex
import com.rws.learningproject01.core.model.LayerMeta
import com.rws.learningproject01.core.model.TextLayerContent
import com.rws.learningproject01.core.storage.FontStore
import com.rws.learningproject01.platform.parseColorHex
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

expect object TextLayerRenderer {
    fun rasterizeOntoBuffer(
        buffer: ByteArray,
        width: Int,
        height: Int,
        meta: LayerMeta,
        fontId: String = "default",
    )

    fun createTextLayerMeta(
        name: String,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = 48f,
        color: Int = 0xFF000000.toInt(),
        width: Int,
        height: Int,
    ): LayerMeta
}

fun TextLayerRenderer.parseColor(hex: String): Int = parseColorHex(hex)

@OptIn(ExperimentalUuidApi::class)
fun createTextLayerMetaCommon(
    name: String,
    text: String,
    x: Float,
    y: Float,
    fontSize: Float,
    color: Int,
    width: Int,
    height: Int,
): LayerMeta {
    val id = Uuid.random().toString()
    return LayerMeta(
        id = id,
        name = name,
        type = "text",
        transform = com.rws.learningproject01.core.model.LayerTransform(x = x, y = y),
        bounds = com.rws.learningproject01.core.model.LayerBounds(maxX = width, maxY = height),
        textContent = TextLayerContent(
            text = text,
            fontSize = fontSize,
            color = argbToHex(color),
        ),
    )
}
