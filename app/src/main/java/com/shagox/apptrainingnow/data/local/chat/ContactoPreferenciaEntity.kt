package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Entity

/**
 * Preferencias LOCALES (por dispositivo/cuenta) de un usuario sobre un contacto de chat:
 * - bloqueado: no se pueden enviar ni recibir mensajes nuevos en esa conversación.
 * - silenciado: la conversación no resalta como no leída en las listas de chats.
 *
 * No se sincroniza al backend: es una preferencia puramente del cliente, igual que el
 * tema claro/oscuro o las unidades de medida.
 */
@Entity(tableName = "contacto_preferencias", primaryKeys = ["ownerId", "contactId"])
data class ContactoPreferenciaEntity(
    val ownerId: Int,
    val contactId: Int,
    val bloqueado: Boolean = false,
    val silenciado: Boolean = false
)
