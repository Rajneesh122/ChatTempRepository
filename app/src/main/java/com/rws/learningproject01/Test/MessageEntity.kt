package com.rws.learningproject01.Test

data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val text: String
)