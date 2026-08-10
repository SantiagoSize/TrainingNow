package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.AttendanceDayDto
import com.shagox.apptrainingnow.data.remote.dto.MonthlyReportDto
import retrofit2.Response
import retrofit2.http.*

/**
 * API de asistencia y reportes — microservicio TrainNow-Rutinas (puerto 8083).
 */
interface AttendanceApi {

    @POST("api/attendance")
    suspend fun registerDay(@Body body: AttendanceDayDto): Response<AttendanceDayDto>

    @GET("api/attendance/user/{userId}")
    suspend fun getUserAttendance(@Path("userId") userId: Int): List<AttendanceDayDto>

    /** month en formato yyyy-MM (ej. 2026-08). */
    @GET("api/attendance/user/{userId}/report/{month}")
    suspend fun getMonthlyReport(
        @Path("userId") userId: Int,
        @Path("month") month: String
    ): MonthlyReportDto
}
