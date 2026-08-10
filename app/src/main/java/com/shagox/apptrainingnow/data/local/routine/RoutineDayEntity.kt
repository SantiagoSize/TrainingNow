package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Un día dentro de una rutina.
 *
 * Una rutina (ej. "Hipertrofia") contiene varios días; cada día tiene su
 * nombre de sesión (ej. "Pecho y Tríceps") y su propia lista de ejercicios.
 */
@Entity(
    tableName = "routine_days",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("routineId")]
)
data class RoutineDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineId: Int,
    /** Lunes, Martes, ... */
    val dayLabel: String,
    /** Nombre de la sesión: "Pecho y Tríceps", "Descanso", etc. */
    val activityName: String = "",
    /** Orden en la semana: 0 = lunes. */
    val dayOrder: Int = 0,
    /** Hora del recordatorio de este día (null = usa la hora general de la rutina). */
    val reminderHour: Int? = null,
    /** Minuto del recordatorio de este día. */
    val reminderMinute: Int? = null
)
