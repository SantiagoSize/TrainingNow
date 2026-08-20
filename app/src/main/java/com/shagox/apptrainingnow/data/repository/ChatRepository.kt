package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.chat.ChatDao
import com.shagox.apptrainingnow.data.local.chat.ContactoPreferenciaDao
import com.shagox.apptrainingnow.data.local.chat.ContactoPreferenciaEntity
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.ConversationSummaryDto
import com.shagox.apptrainingnow.data.remote.dto.MessageDto
import com.shagox.apptrainingnow.data.remote.dto.UploadResponseDto
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repositorio de chat híbrido:
 * - Room es la fuente de la UI (Flow reactivo, funciona offline).
 * - Cada envío se publica también en TrainNow-Comunicaciones (best-effort).
 * - syncConversation() baja los mensajes del backend que falten localmente.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val contactoPreferenciaDao: ContactoPreferenciaDao,
    private val userDao: UserDao
) {

    /**
     * Garantiza que [usuario] exista como fila local en Room antes de intercambiar mensajes
     * con él. MessageEntity tiene FK estrictas a la tabla "users" (senderId/receiverId), pero
     * como el login real viene del backend (UserApiRepository), esos usuarios nunca quedaban
     * cacheados en Room: el primer mensaje entre dos cuentas nuevas reventaba con
     * "FOREIGN KEY constraint failed" y crasheaba la app. Se llama con el usuario actual y con
     * el contacto apenas se abre un chat, antes de poder escribir.
     */
    suspend fun asegurarUsuarioLocal(usuario: UserEntity) {
        // OJO: insertUser usa OnConflictStrategy.REPLACE. SQLite implementa REPLACE como
        // DELETE + INSERT de la fila en conflicto, y MessageEntity tiene FK onDelete=CASCADE
        // hacia "users" (senderId/receiverId). Como este método se llama cada 15s (refresco
        // periódico del contacto en ChatScreen), volver a insertar un usuario que YA existe
        // disparaba ese DELETE interno y borraba en cascada TODOS sus mensajes locales cada
        // vez que caía el refresco — se veía como si el chat "se borrara solo" al rato de
        // escribir. Por eso ahora solo se inserta si la fila todavía no existe.
        if (userDao.getUserById(usuario.id) == null) {
            userDao.insertUser(usuario)
        }
    }

    /** Borra del historial local los mensajes con más de 7 días de antigüedad. */
    suspend fun limpiarMensajesAntiguos() {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        chatDao.deleteMessagesOlderThan(cutoff)
    }

    // ==================== BLOQUEAR / SILENCIAR / ELIMINAR (preferencias locales) ====================

    /** Preferencia (bloqueado/silenciado) de un contacto, reactiva. Null = valores por defecto (false/false). */
    fun observarPreferencia(ownerId: Int, contactId: Int): Flow<ContactoPreferenciaEntity?> {
        return contactoPreferenciaDao.observarPreferencia(ownerId, contactId)
    }

    suspend fun bloquearContacto(ownerId: Int, contactId: Int) {
        val actual = contactoPreferenciaDao.getPreferencia(ownerId, contactId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(ownerId, contactId)).copy(bloqueado = true)
        )
    }

    suspend fun desbloquearContacto(ownerId: Int, contactId: Int) {
        val actual = contactoPreferenciaDao.getPreferencia(ownerId, contactId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(ownerId, contactId)).copy(bloqueado = false)
        )
    }

    suspend fun silenciarContacto(ownerId: Int, contactId: Int) {
        val actual = contactoPreferenciaDao.getPreferencia(ownerId, contactId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(ownerId, contactId)).copy(silenciado = true)
        )
    }

    suspend fun desilenciarContacto(ownerId: Int, contactId: Int) {
        val actual = contactoPreferenciaDao.getPreferencia(ownerId, contactId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(ownerId, contactId)).copy(silenciado = false)
        )
    }

    /** IDs de contactos silenciados por este usuario (para no resaltar no leídos en las listas). */
    fun observarSilenciados(ownerId: Int): Flow<List<Int>> = contactoPreferenciaDao.observarSilenciados(ownerId)

    /** IDs de contactos bloqueados por este usuario. */
    fun observarBloqueados(ownerId: Int): Flow<List<Int>> = contactoPreferenciaDao.observarBloqueados(ownerId)

    /**
     * IDs de contactos "guardados" (chat abierto al menos una vez, con o sin mensajes) por
     * este usuario. Vive en Room, no se borra al cerrar sesión.
     */
    fun observarContactosGuardados(ownerId: Int): Flow<List<Int>> =
        contactoPreferenciaDao.observarContactosGuardados(ownerId)

    /**
     * Marca que el usuario abrió el chat con [contactId] (por ejemplo desde el Foro), aunque
     * todavía no le haya escrito ningún mensaje. A partir de ahora ese contacto aparece en
     * "Mis chats". No pisa una preferencia de bloqueo/silencio ya existente; si el contacto
     * había sido quitado con "Eliminar de mis chats" (guardado = false), lo vuelve a marcar
     * como guardado.
     */
    suspend fun marcarChatAbierto(ownerId: Int, contactId: Int) {
        val actual = contactoPreferenciaDao.getPreferencia(ownerId, contactId)
        if (actual == null || !actual.guardado) {
            contactoPreferenciaDao.upsert(
                (actual ?: ContactoPreferenciaEntity(ownerId, contactId)).copy(guardado = true)
            )
        }
    }

    /** IDs de contactos con los que ya existe al menos un mensaje intercambiado (local). */
    suspend fun obtenerContactosConMensajes(userId: Int): List<Int> = chatDao.getContactIds(userId)

    /**
     * Borra todo el historial local de la conversación (no afecta al backend). Además marca
     * "borrado hasta ahora": syncConversation() no debe volver a bajar del backend los mensajes
     * de antes de este momento, o el borrado se revertía solo al reabrir el chat (ver
     * ContactoPreferenciaEntity.historialBorradoHastaMs).
     */
    suspend fun eliminarConversacion(myId: Int, otherId: Int) {
        chatDao.deleteConversation(myId, otherId)
        val actual = contactoPreferenciaDao.getPreferencia(myId, otherId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(myId, otherId))
                .copy(historialBorradoHastaMs = System.currentTimeMillis())
        )
    }

    /**
     * Elimina el contacto de "Mis chats": borra el historial local y marca guardado = false
     * para que deje de aparecer en la lista (a diferencia de [eliminarConversacion], que solo
     * borra los mensajes pero mantiene el chat listado). A propósito NO borra la fila entera
     * (como hacía antes): necesitamos conservar historialBorradoHastaMs, o si el usuario
     * reabre el chat más tarde (marcarChatAbierto), syncConversation() no tendría con qué
     * comparar y volvería a bajar del backend los mensajes ya borrados.
     */
    suspend fun eliminarDeMisChats(myId: Int, otherId: Int) {
        chatDao.deleteConversation(myId, otherId)
        val actual = contactoPreferenciaDao.getPreferencia(myId, otherId)
        contactoPreferenciaDao.upsert(
            (actual ?: ContactoPreferenciaEntity(myId, otherId)).copy(
                guardado = false,
                historialBorradoHastaMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun sendMessage(message: MessageEntity) {
        try {
            chatDao.insertMessage(message)
        } catch (e: Exception) {
            // Defensa adicional: si por algún motivo el usuario aún no está sincronizado
            // localmente (ver asegurarUsuarioLocal), esto ya no debe crashear la app.
            android.util.Log.e("ChatRepository", "No se pudo guardar el mensaje localmente", e)
        }
        // Publicar en el backend; si no hay conexión, el mensaje queda local.
        try {
            RemoteModule.chatApi().sendMessage(
                MessageDto(
                    senderId = message.senderId,
                    receiverId = message.receiverId,
                    content = message.content,
                    timestamp = message.timestamp,
                    isRead = message.isRead,
                    attachmentUrl = message.attachmentUrl,
                    attachmentType = message.attachmentType
                )
            )
        } catch (_: Exception) {
            // Sin conexión: se conserva localmente.
        }
    }

    /**
     * Sube un adjunto de chat (imagen o video) ya comprimido por el llamador y devuelve la
     * URL relativa + tipo asignados por el backend, o null si falla (sin conexión, archivo
     * demasiado pesado, etc.). La compresión la decide la UI según el tipo de archivo
     * (imagen → ImageCompressor, video → límite de tamaño de la captura).
     */
    suspend fun subirAdjunto(bytes: ByteArray, mimeType: String, nombreArchivo: String): UploadResponseDto? {
        return try {
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", nombreArchivo, requestBody)
            val response = RemoteModule.chatApi().uploadAttachment(part)
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) {
            null
        }
    }

    /** URL completa y descargable de un adjunto (la app solo guarda la ruta relativa). */
    fun urlCompletaDeAdjunto(attachmentUrl: String): String {
        val base = RemoteModule.chatBaseUrl().trimEnd('/')
        return base + attachmentUrl
    }

    /** Resumen de conversaciones (último mensaje + no leídos) para la lista de chats. */
    suspend fun obtenerResumenConversaciones(userId: Int): List<ConversationSummaryDto> {
        return try {
            RemoteModule.chatApi().getConversationsSummary(userId)
        } catch (_: Exception) {
            emptyList()
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
            // Respeta el "Eliminar conversación": no revive mensajes de antes de ese borrado.
            val borradoHasta = contactoPreferenciaDao.getPreferencia(myId, otherId)?.historialBorradoHastaMs ?: 0L

            for (m in remote) {
                val key = Triple(m.senderId to m.receiverId, m.timestamp ?: 0L, m.content)
                if (key in existentes) continue
                if ((m.timestamp ?: 0L) <= borradoHasta) continue
                try {
                    chatDao.insertMessage(
                        MessageEntity(
                            senderId = m.senderId,
                            receiverId = m.receiverId,
                            content = m.content,
                            timestamp = m.timestamp ?: System.currentTimeMillis(),
                            isRead = m.isRead,
                            attachmentUrl = m.attachmentUrl,
                            attachmentType = m.attachmentType
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
