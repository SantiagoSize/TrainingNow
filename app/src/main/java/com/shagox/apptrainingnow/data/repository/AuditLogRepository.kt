package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.AuditLogDto

/**
 * Registro de actividad administrativa: quién hizo qué y cuándo (creación/edición de
 * ejercicios, renombrado de categorías, sanciones a usuarios, rutinas globales).
 * Best-effort: si el backend no responde, la acción original igual se completa; solo
 * se pierde su rastro en el log (no debe tumbar el flujo principal del admin).
 */
class AuditLogRepository {

    suspend fun log(
        actorId: Int,
        actorName: String,
        actorRole: String,
        action: String,
        targetType: String? = null,
        targetId: Int? = null,
        targetName: String? = null,
        details: String? = null
    ) {
        try {
            RemoteModule.userApi().recordAuditLog(
                AuditLogDto(
                    actorId = actorId.toLong(),
                    actorName = actorName,
                    actorRole = actorRole,
                    action = action,
                    targetType = targetType,
                    targetId = targetId?.toLong(),
                    targetName = targetName,
                    details = details
                )
            )
        } catch (_: Exception) {
            // Sin conexión o backend caído: no interrumpe la acción que se estaba registrando.
        }
    }

    suspend fun getAll(targetType: String? = null): List<AuditLogDto> {
        return try {
            RemoteModule.userApi().getAuditLogs(targetType)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
