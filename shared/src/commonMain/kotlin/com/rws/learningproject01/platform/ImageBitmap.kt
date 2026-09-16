package com.rws.learningproject01.platform

expect class ImageBitmap(width: Int, height: Int) {
    val width: Int
    val height: Int
    fun getPixels(): ByteArray
    fun setPixels(rgba: ByteArray)
    fun compressPng(quality: Int = 100): ByteArray
    fun compressJpeg(quality: Int): ByteArray
    fun compressWebp(quality: Int): ByteArray
    fun recycle()
}

expect fun createImageBitmapFromRgba(width: Int, height: Int, rgba: ByteArray): ImageBitmap

expect fun scaleImageBitmap(source: ImageBitmap, width: Int, height: Int): ImageBitmap
