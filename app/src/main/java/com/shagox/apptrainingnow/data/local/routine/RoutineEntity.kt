package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.shagox.apptrainingnow.data.local.user.UserEntity

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
    val dayInfo: String,
    val creationDate: Long = System.currentTimeMillis(),
    val scheduledTime: Long = System.currentTimeMillis()
)