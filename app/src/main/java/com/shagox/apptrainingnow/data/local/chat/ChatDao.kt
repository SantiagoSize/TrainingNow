package com.shagox.apptrainingnow.data.local.chat

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar mensajes y conversaciones del chat.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Envío y recepción de mensajes
 * - Gestión de conversaciones
 * - Estado de lectura de mensajes
 * - Lista de chats activos
 */
@Dao
interface ChatDao {

    // ==================== MENSAJES - CRUD ====================

    /**
     * Inserta un nuevo mensaje.
     * @return ID del mensaje insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Inserta múltiples mensajes.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Actualiza un mensaje.
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Elimina un mensaje.
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    /**
     * Elimina un mensaje por ID.
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Int)

    // ==================== MENSAJES - QUERIES GENERALES ====================

    /**
     * Obtiene un mensaje por ID.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Int): MessageEntity?

    /**
     * Obtiene la conversación completa entre dos usuarios (bidireccional).
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId) 
        ORDER BY timestamp ASC
    """)
    fun getConversation(myId: Int, otherId: Int): Flow<List<MessageEntity>>

    /**
     * Obtiene la conversación de forma síncrona.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId) 
        ORDER BY timestamp ASC
    """)
    suspend fun getConversationSync(myId: Int, otherId: Int): List<MessageEntity>

    /**
     * Obtiene los últimos N mensajes de una conversación.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId) 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessages(myId: Int, otherId: Int, limit: Int = 50): List<MessageEntity>

    /**
     * Obtiene mensajes paginados de una conversación.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId) 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getMessagesPaginated(myId: Int, otherId: Int, limit: Int, offset: Int): List<MessageEntity>

    // ==================== LISTA DE CHATS ====================

    /**
     * Obtiene el último mensaje para mostrar en la lista de chats.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :userId OR receiverId = :userId) 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    fun getLastMessageForUser(userId: Int): Flow<MessageEntity?>

    /**
     * Obtiene el último mensaje con cada contacto (para lista de chats).
     */
    @Query("""
        SELECT m.* FROM messages m
        INNER JOIN (
            SELECT 
                CASE WHEN senderId = :userId THEN receiverId ELSE senderId END as contactId,
                MAX(timestamp) as maxTime
            FROM messages
            WHERE senderId = :userId OR receiverId = :userId
            GROUP BY contactId
        ) latest ON (
            (m.senderId = :userId AND m.receiverId = latest.contactId) OR
            (m.receiverId = :userId AND m.senderId = latest.contactId)
        ) AND m.timestamp = latest.maxTime
        ORDER BY m.timestamp DESC
    """)
    fun getConversationPreviews(userId: Int): Flow<List<MessageEntity>>

    /**
     * Obtiene IDs de contactos con conversaciones activas.
     */
    @Query("""
        SELECT DISTINCT 
            CASE WHEN senderId = :userId THEN receiverId ELSE senderId END as contactId
        FROM messages
        WHERE senderId = :userId OR receiverId = :userId
    """)
    suspend fun getContactIds(userId: Int): List<Int>

    /**
     * Cuenta conversaciones activas (contactos únicos).
     */
    @Query("""
        SELECT COUNT(DISTINCT 
            CASE WHEN senderId = :userId THEN receiverId ELSE senderId END
        ) FROM messages
        WHERE senderId = :userId OR receiverId = :userId
    """)
    suspend fun countActiveConversations(userId: Int): Int

    // ==================== ESTADO DE LECTURA ====================

    /**
     * Marca un mensaje como leído.
     */
    @Query("UPDATE messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: Int)

    /**
     * Marca todos los mensajes de una conversación como leídos.
     * Solo marca los mensajes que el usuario recibió (no los que envió).
     */
    @Query("""
        UPDATE messages 
        SET isRead = 1 
        WHERE senderId = :senderId AND receiverId = :myId AND isRead = 0
    """)
    suspend fun markConversationAsRead(myId: Int, senderId: Int)

    /**
     * Cuenta mensajes no leídos de un remitente específico.
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE senderId = :senderId AND receiverId = :myId AND isRead = 0
    """)
    suspend fun getUnreadCountFrom(myId: Int, senderId: Int): Int

    /**
     * Cuenta mensajes no leídos de un remitente como Flow.
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE senderId = :senderId AND receiverId = :myId AND isRead = 0
    """)
    fun observeUnreadCountFrom(myId: Int, senderId: Int): Flow<Int>

    /**
     * Cuenta el total de mensajes no leídos del usuario.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    fun getTotalUnreadCount(userId: Int): Flow<Int>

    /**
     * Cuenta el total de mensajes no leídos de forma síncrona.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    suspend fun getTotalUnreadCountSync(userId: Int): Int

    /**
     * Obtiene mensajes no leídos.
     */
    @Query("SELECT * FROM messages WHERE receiverId = :userId AND isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadMessages(userId: Int): Flow<List<MessageEntity>>

    // ==================== BÚSQUEDA ====================

    /**
     * Busca mensajes por contenido en una conversación.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE ((senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId))
        AND LOWER(content) LIKE '%' || LOWER(:query) || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun searchInConversation(myId: Int, otherId: Int, query: String): List<MessageEntity>

    /**
     * Busca mensajes por contenido en todos los chats.
     */
    @Query("""
        SELECT * FROM messages 
        WHERE (senderId = :userId OR receiverId = :userId)
        AND LOWER(content) LIKE '%' || LOWER(:query) || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchAllMessages(userId: Int, query: String, limit: Int = 50): List<MessageEntity>

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta mensajes en una conversación.
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId)
    """)
    suspend fun countMessagesInConversation(myId: Int, otherId: Int): Int

    /**
     * Cuenta mensajes enviados por el usuario.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE senderId = :userId")
    suspend fun countSentMessages(userId: Int): Int

    /**
     * Cuenta mensajes recibidos por el usuario.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId")
    suspend fun countReceivedMessages(userId: Int): Int

    /**
     * Obtiene la fecha del primer mensaje con un contacto.
     */
    @Query("""
        SELECT MIN(timestamp) FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId)
    """)
    suspend fun getConversationStartDate(myId: Int, otherId: Int): Long?

    // ==================== LIMPIEZA ====================

    /**
     * Elimina toda la conversación con un contacto.
     */
    @Query("""
        DELETE FROM messages 
        WHERE (senderId = :myId AND receiverId = :otherId) 
           OR (senderId = :otherId AND receiverId = :myId)
    """)
    suspend fun deleteConversation(myId: Int, otherId: Int)

    /**
     * Elimina mensajes antiguos (anteriores a una fecha).
     */
    @Query("DELETE FROM messages WHERE timestamp < :thresholdTime")
    suspend fun deleteOldMessages(thresholdTime: Long)

    /**
     * Elimina todos los mensajes de un usuario.
     */
    @Query("DELETE FROM messages WHERE senderId = :userId OR receiverId = :userId")
    suspend fun deleteAllMessagesForUser(userId: Int)
}