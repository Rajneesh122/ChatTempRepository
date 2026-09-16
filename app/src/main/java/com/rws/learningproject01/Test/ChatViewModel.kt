package com.rws.learningproject01.Test

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow

class ChatViewModel(val repository: ChatRepositoryImpl): ViewModel {

    fun getMessages(chatId: String): Flow<List<MessageEntity>> {
        return repository.observeMessage(chatId)
    }
}