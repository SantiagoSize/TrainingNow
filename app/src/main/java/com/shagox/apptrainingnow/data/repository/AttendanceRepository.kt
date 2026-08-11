package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.remote.AttendanceApi
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.dto.AttendanceDayDto
import com.shagox.apptrainingnow.data.remote.dto.MonthlyReportDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Registro de asistencia y reporte mensual contra TrainNow-Rutinas (8083).
 */
class AttendanceRepository(
    private val api: AttendanceApi = RemoteModule.attendanceApi()
) {

    /** Registra el día actual como entrenado. Best-effort: no interrumpe la UI si falla. */
    suspend fun registerTrainedToday(
        userId: Int,
        routineId: Int?,
        exercisesCompleted: Int,
        durationMinutes: Int? = null
    ): Boolean = registerDay(userId, todayKey(), "TRAINED", routineId, exercisesCompleted, durationMinutes)

    /** Marca un día como descanso. */
    suspend fun registerRest(userId: Int, date: String = todayKey()): Boolean =
        registerDay(userId, date, "REST", null, 0, null)

    /** Marca un día como perdido (tenía plan y no entrenó). */
    suspend fun registerMissed(userId: Int, date: String, routineId: Int? = null): Boolean =
        registerDay(userId, date, "MISSED", routineId, 0, null)

    private suspend fun registerDay(
        userId: Int,
        date: String,
        status: String,
        routineId: Int?,
        exercises: Int,
        minutes: Int?
    ): Boolean = try {
        val response = api.registerDay(
            AttendanceDayDto(
                userId = userId,
                date = date,
                status = status,
                routineId = routineId,
                exercisesCompleted = exercises,
                durationMinutes = minutes
            )
        )
        response.isSuccessful
    } catch (_: Exception) {
        false
    }

    /** Reporte del mes indicado. month = yyyy-MM; por defecto el mes actual. */
    suspend fun getMonthlyReport(userId: Int, month: String = currentMonthKey()): Result<MonthlyReportDto> =
        try {
            Result.success(api.getMonthlyReport(userId, month))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar el reporte. Verifica la conexión con el servidor."))
        }

    companion object {
        fun todayKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        fun currentMonthKey(): String =
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        /** Clave yyyy-MM desplazada N meses respecto al actual (negativo = pasado). */
        fun monthKeyOffset(offset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, offset)
            return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        }

        /** Nombre legible del mes: "Agosto 2026". */
        fun monthLabel(monthKey: String): String = try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthKey)!!
            SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES")).format(date)
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            monthKey
        }
    }
}
