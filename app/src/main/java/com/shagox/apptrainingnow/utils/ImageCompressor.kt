package com.shagox.apptrainingnow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Comprime imágenes antes de subirlas para que no pesen de más.
 *
 * Estrategia (una foto de 4-8 MB queda en ~60-100 KB):
 * 1. Se decodifica con inSampleSize para no cargar el bitmap completo en memoria.
 * 2. Se reescala al lado máximo indicado (por defecto 800 px).
 * 3. Se corrige la rotación EXIF.
 * 4. Se comprime a JPEG bajando la calidad hasta cumplir el tamaño objetivo.
 * 5. Se devuelve como data URI listo para guardar en la base de datos.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 800
    private const val TARGET_BYTES = 120 * 1024 // ~120 KB

    /** Convierte la imagen de [uri] en un data URI JPEG comprimido, o null si falla. */
    fun compressToDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
        targetBytes: Int = TARGET_BYTES
    ): String? {
        val bytes = compressToBytes(context, uri, maxDimension, targetBytes) ?: return null
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    /** Devuelve los bytes JPEG comprimidos de la imagen. */
    fun compressToBytes(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
        targetBytes: Int = TARGET_BYTES
    ): ByteArray? {
        return try {
            // 1) Medir sin cargar en memoria
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val mayor = maxOf(bounds.outWidth, bounds.outHeight)
            if (mayor <= 0) return null

            // 2) Decodificar reducido
            val opciones = BitmapFactory.Options().apply {
                inSampleSize = calcularSampleSize(mayor, maxDimension)
            }
            var bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opciones)
            } ?: return null

            // 3) Escalar al máximo exacto
            bitmap = escalar(bitmap, maxDimension)

            // 4) Corregir rotación EXIF
            bitmap = corregirRotacion(context, uri, bitmap)

            // 5) Comprimir bajando calidad hasta el objetivo
            var calidad = 85
            var salida = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            while (salida.size() > targetBytes && calidad > 40) {
                calidad -= 10
                salida = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            }
            bitmap.recycle()
            salida.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /** Tamaño legible de un data URI, para mostrar al usuario. */
    fun sizeKb(dataUri: String?): Int {
        if (dataUri.isNullOrBlank()) return 0
        val base64 = dataUri.substringAfter("base64,", "")
        return (base64.length * 3 / 4) / 1024
    }

    private fun calcularSampleSize(ladoMayor: Int, objetivo: Int): Int {
        var sample = 1
        while (ladoMayor / sample > objetivo * 2) sample *= 2
        return sample
    }

    private fun escalar(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val mayor = maxOf(bitmap.width, bitmap.height)
        if (mayor <= maxDimension) return bitmap
        val factor = maxDimension.toFloat() / mayor
        val escalado = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
            true
        )
        if (escalado != bitmap) bitmap.recycle()
        return escalado
    }

    private fun corregirRotacion(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientacion = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: return bitmap
            val grados = when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }
            val matriz = Matrix().apply { postRotate(grados) }
            val rotado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            if (rotado != bitmap) bitmap.recycle()
            rotado
        } catch (_: Exception) {
            bitmap
        }
    }
}
