package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Entity

/**
 * Preferencias LOCALES (por dispositivo/cuenta) de un usuario sobre un contacto de chat:
 * - bloqueado: no se pueden enviar ni recibir mensajes nuevos en esa conversación.
 * - silenciado: la conversación no resalta como no leída en las listas de chats.
 * - historialBorradoHastaMs: marca de tiempo de la última vez que este usuario borró el
 *   historial local ("Eliminar conversación" o "Eliminar de mis chats"). syncConversation()
 *   no debe re-traer del backend ningún mensaje con timestamp <= este valor, o el borrado
 *   local se "revertía solo" cada vez que se volvía a entrar al chat (el sync los volvía a
 *   bajar todos).
 * - guardado: si el contacto debe listarse en "Mis chats". true cuando el usuario abre el
 *   chat (marcarChatAbierto) o interactúa con el contacto; false tras "Eliminar de mis
 *   chats", para sacarlo de la lista SIN perder el corte de historialBorradoHastaMs (antes
 *   se borraba la fila entera, lo que hacía perder ese corte: si el usuario reabría el chat
 *   más tarde, syncConversation() no tenía con qué comparar y volvía a bajar los mensajes
 *   viejos del backend).
 *
 * No se sincroniza al backend: es una preferencia puramente del cliente, igual que el
 * tema claro/oscuro o las unidades de medida.
 */
@Entity(tableName = "contacto_preferencias", primaryKeys = ["ownerId", "contactId"])
data class ContactoPreferenciaEntity(
    val ownerId: Int,
    val contactId: Int,
    val bloqueado: Boolean = false,
    val silenciado: Boolean = false,
    val historialBorradoHastaMs: Long = 0L,
    val guardado: Boolean = true
)
