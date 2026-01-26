package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sendMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE (senderId = :myId AND receiverId = :otherId) OR (senderId = :otherId AND receiverId = :myId) ORDER BY timestamp ASC")
    fun getChatHistory(myId: Int, otherId: Int): Flow<List<MessageEntity>>
}