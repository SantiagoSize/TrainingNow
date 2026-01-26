package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val routineId: Int = 0,
    val ownerId: Int,
    val creatorId: Int,
    val name: String,
    val creationDate: Long
)