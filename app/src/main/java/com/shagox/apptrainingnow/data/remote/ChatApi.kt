package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.ConversationSummaryDto
import com.shagox.apptrainingnow.data.remote.dto.MessageDto
import com.shagox.apptrainingnow.data.remote.dto.UploadResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API del chat — microservicio TrainNow-Comunicaciones (puerto 8084).
 */
interface ChatApi {

    @GET("api/messages/conversation/{userA}/{userB}")
    suspend fun getConversation(@Path("userA") userA: Int, @Path("userB") userB: Int): List<MessageDto>

    @GET("api/messages/user/{userId}")
    suspend fun getMessagesByUser(@Path("userId") userId: Int): List<MessageDto>

    /** Resumen de conversaciones (último mensaje + no leídos) para la lista de chats. */
    @GET("api/messages/conversations/{userId}")
    suspend fun getConversationsSummary(@Path("userId") userId: Int): List<ConversationSummaryDto>

    @POST("api/messages")
    suspend fun sendMessage(@Body message: MessageDto): Response<MessageDto>

    @PATCH("api/messages/{id}/read")
    suspend fun markAsRead(@Path("id") id: Int): Response<MessageDto>

    /** Sube una imagen o video de chat ya comprimido y devuelve su URL relativa. */
    @Multipart
    @POST("api/messages/upload")
    suspend fun uploadAttachment(@Part file: MultipartBody.Part): Response<UploadResponseDto>
}
