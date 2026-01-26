package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.chat.ChatDao
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    suspend fun sendMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }

    fun getConversation(myId: Int, otherId: Int): Flow<List<MessageEntity>> {
        return chatDao.getConversation(myId, otherId)
    }
}