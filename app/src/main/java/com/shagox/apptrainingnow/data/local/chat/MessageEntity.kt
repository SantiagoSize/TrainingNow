package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

@Entity(
    tableName = "messages",
    foreignKeys = [
        // Si se borra un usuario, se borran sus mensajes (para mantener limpia la BD)
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["senderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["receiverId"], onDelete = ForeignKey.CASCADE)
    ],
    // Índices para que el chat cargue rápido
    indices = [androidx.room.Index("senderId"), androidx.room.Index("receiverId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: Int,   // Quien lo envía
    val receiverId: Int, // Quien lo recibe
    val content: String, // El texto
    val timestamp: Long = System.currentTimeMillis(), // Hora
    val isRead: Boolean = false // ¿Lo leyeron?
)