package com.rws.learningproject01.Test

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ChatScreen(
    chatId: String,
    viewModel: ChatViewModel
) {
    val messages by viewModel.getMessages(chatId).collectAsState(initial = emptyList())
    LazyColumn {
        items(messages) { message -> Text(text = message.text) }
    }
}
