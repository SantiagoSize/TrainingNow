package com.shagox.apptrainingnow.data.local.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para las preferencias locales de contacto (bloqueado/silenciado) del chat.
 */
@Dao
interface ContactoPreferenciaDao {

    @Query("SELECT * FROM contacto_preferencias WHERE ownerId = :ownerId AND contactId = :contactId")
    fun observarPreferencia(ownerId: Int, contactId: Int): Flow<ContactoPreferenciaEntity?>

    @Query("SELECT * FROM contacto_preferencias WHERE ownerId = :ownerId AND contactId = :contactId")
    suspend fun getPreferencia(ownerId: Int, contactId: Int): ContactoPreferenciaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preferencia: ContactoPreferenciaEntity)

    /** IDs de contactos bloqueados por este usuario (para filtrar/ocultar en las listas). */
    @Query("SELECT contactId FROM contacto_preferencias WHERE ownerId = :ownerId AND bloqueado = 1")
    fun observarBloqueados(ownerId: Int): Flow<List<Int>>

    /** IDs de contactos silenciados por este usuario (para no resaltar no leídos). */
    @Query("SELECT contactId FROM contacto_preferencias WHERE ownerId = :ownerId AND silenciado = 1")
    fun observarSilenciados(ownerId: Int): Flow<List<Int>>
}
