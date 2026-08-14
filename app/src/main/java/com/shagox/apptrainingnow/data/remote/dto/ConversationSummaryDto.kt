package com.shagox.apptrainingnow.data.remote.dto

/**
 * Resumen de una conversación (último mensaje + no leídos), usado en la lista de chats
 * en vez de traer todos los mensajes sueltos de cada contacto.
 */
data class ConversationSummaryDto(
    val contactId: Int = 0,
    val lastMessage: String? = null,
    val lastAttachmentType: String? = null,
    val lastTimestamp: Long? = null,
    val unreadCount: Int = 0
)
