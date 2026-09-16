package com.rws.learningproject01.Test

import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(val dao: MessageDao)
{
     fun observeMessage(chatId: String): Flow<List<MessageEntity>> {
        return dao.observeMessage(chatId)
    }
}