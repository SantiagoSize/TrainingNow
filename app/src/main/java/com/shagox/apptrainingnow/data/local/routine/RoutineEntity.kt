package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * Rutina de entrenamiento (ej. "Hipertrofia").
 * Sus días viven en [RoutineDayEntity]; ownerId null = rutina pública.
 */
@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["ownerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["creatorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [androidx.room.Index("ownerId"), androidx.room.Index("creatorId")]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int? = null,
    val creatorId: Int,
    val name: String,
    /** Resumen visible en la lista: "Lunes, Miércoles, Viernes". */
    val dayInfo: String = "",
    val creationDate: Long = System.currentTimeMillis(),
    val scheduledTime: Long = System.currentTimeMillis(),
    /** true = un entrenador la compartió (ownerId ya puesto) pero el usuario todavía no la
     *  acepta. No debe listarse en "Mis rutinas" hasta que pase a false. */
    val pendingShare: Boolean = false,
    /** true = plantilla reutilizable de un entrenador (ownerId null, no es rutina global). */
    val isTemplate: Boolean = false
)
