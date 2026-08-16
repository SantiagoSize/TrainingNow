package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.ReportDto

/**
 * Reportes de usuarios (ej: acoso en el chat, sospecha de bot). A diferencia de
 * AuditLogRepository, crear un reporte SÍ debe avisar al usuario si falla (es una acción
 * que espera confirmación), por eso [crear] devuelve Boolean en vez de ser best-effort.
 */
class ReportRepository {

    suspend fun crear(
        reporterId: Int,
        reporterName: String,
        reportedId: Int,
        reportedName: String,
        reason: String,
        details: String? = null
    ): Boolean {
        return try {
            val response = RemoteModule.userApi().createReport(
                ReportDto(
                    reporterId = reporterId.toLong(),
                    reporterName = reporterName,
                    reportedId = reportedId.toLong(),
                    reportedName = reportedName,
                    reason = reason,
                    details = details
                )
            )
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getAll(status: String? = null): List<ReportDto> {
        return try {
            RemoteModule.userApi().getReports(status)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun resolver(id: Long, status: String): Boolean {
        return try {
            RemoteModule.userApi().resolveReport(id, mapOf("status" to status)).isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
