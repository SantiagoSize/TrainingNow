package com.shagox.apptrainingnow.utils

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Fetcher para Coil (librería que carga las imágenes en AsyncImage) que entiende
 * "data URI" en base64, ej: "data:image/jpeg;base64,AAAA...".
 *
 * Por qué existe: [ImageCompressor] guarda las fotos de perfil y de ejercicios como
 * data URI directamente en la base de datos (para no depender de un servidor de
 * archivos aparte). El problema es que Coil, tal cual viene, NO sabe leer ese
 * formato: no encuentra ningún Fetcher para el esquema "data" y la imagen queda
 * en blanco/gris sin ningún error visible (justo lo que se veía en el círculo de
 * foto del carrusel de bienvenida). Este Fetcher decodifica el base64 a Bitmap
 * manualmente para que Coil pueda mostrarlo con normalidad.
 */
class DataUriFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // schemeSpecificPart = todo lo que viene después de "data:", ej: "image/jpeg;base64,AAAA"
        val ssp = uri.schemeSpecificPart
        val base64 = ssp.substringAfter("base64,", ssp)
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("No se pudo decodificar la imagen: data URI inválida")
        return DrawableResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "data") return null
            return DataUriFetcher(data, options)
        }
    }
}
