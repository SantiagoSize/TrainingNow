package com.shagox.apptrainingnow.data.remote.dto

/** Respuesta al subir un adjunto de chat (imagen/video) a TrainNow-Comunicaciones. */
data class UploadResponseDto(
    val url: String = "",
    val attachmentType: String = ""
)
