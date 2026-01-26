package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Enviar mensaje
    @Insert
    suspend fun insertMessage(message: MessageEntity)

    // Obtener conversación completa entre Dos Personas (Yo y el Coach)
    // La lógica es: "Dame los mensajes donde (Yo soy emisor Y El es receptor) O (El es emisor Y Yo soy receptor)"
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId) 
        ORDER BY timestamp ASC
    """)
    fun getConversation(myId: Int, otherId: Int): Flow<List<MessageEntity>>

    // Ver el último mensaje (para la lista de chats)
    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) ORDER BY timestamp DESC LIMIT 1")
    fun getLastMessageForUser(userId: Int): Flow<MessageEntity?>
}