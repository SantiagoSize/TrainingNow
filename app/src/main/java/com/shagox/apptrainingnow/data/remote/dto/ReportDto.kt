package com.shagox.apptrainingnow.data.remote.dto

/**
 * DTO de un reporte de usuario (ej: acoso en el chat, sospecha de bot).
 * Contrato con TrainNow-Usuarios (`/api/reports`).
 */
data class ReportDto(
    val id: Long? = null,
    val reporterId: Long,
    val reporterName: String,
    val reportedId: Long,
    val reportedName: String,
    val reason: String,
    val details: String? = null,
    val status: String = "PENDING",
    val timestamp: Long? = null
)
