package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.chat.ChatDao
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.MessageDto
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de chat híbrido:
 * - Room es la fuente de la UI (Flow reactivo, funciona offline).
 * - Cada envío se publica también en TrainNow-Comunicaciones (best-effort).
 * - syncConversation() baja los mensajes del backend que falten localmente.
 */
class ChatRepository(private val chatDao: ChatDao) {

    suspend fun sendMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
        // Publicar en el backend; si no hay conexión, el mensaje queda local.
        try {
            RemoteModule.chatApi().sendMessage(
                MessageDto(
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    content = message.content,
                    timestamp = message.timestamp,
                    isRead = message.isRead
                )
            )
        } catch (_: Exception) {
            // Sin conexión: se conserva localmente.
        }
    }

    fun getConversation(myId: Int, otherId: Int): Flow<List<MessageEntity>> {
        return chatDao.getConversation(myId, otherId)
    }

    /**
     * Sincroniza la conversación desde el backend hacia Room.
     * Identidad de un mensaje: (senderId, receiverId, timestamp, content).
     * Best-effort: silencioso si no hay conexión.
     */
    suspend fun syncConversation(myId: Int, otherId: Int) {
        try {
            val remote = RemoteModule.chatApi().getConversation(myId, otherId)
            if (remote.isEmpty()) return
            val locales = chatDao.getConversationSync(myId, otherId)
            val existentes = locales.map {
                Triple(it.senderId to it.receiverId, it.timestamp, it.content)
            }.toHashSet()

            for (m in remote) {
                val key = Triple(m.senderId to m.receiverId, m.timestamp ?: 0L, m.content)
                if (key in existentes) continue
                try {
                    chatDao.insertMessage(
                        MessageEntity(
                            senderId = m.senderId,
                            receiverId = m.receiverId,
                            content = m.content,
                            timestamp = m.timestamp ?: System.currentTimeMillis(),
                            isRead = m.isRead
                        )
                    )
                } catch (_: Exception) {
                    // FK a usuario inexistente en Room local: se omite ese mensaje.
                }
            }
        } catch (_: Exception) {
            // Offline o backend caído: se reintenta al reabrir el chat.
        }
    }
}
