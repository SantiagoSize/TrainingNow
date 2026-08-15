package com.shagox.apptrainingnow.data.remote.dto

/**
 * DTO de un registro de actividad administrativa (auditoría).
 * Contrato con TrainNow-Usuarios (`/api/audit-logs`).
 */
data class AuditLogDto(
    val id: Long? = null,
    val actorId: Long,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val targetType: String? = null,
    val targetId: Long? = null,
    val targetName: String? = null,
    val details: String? = null,
    val timestamp: Long? = null
)
