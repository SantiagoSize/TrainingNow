package com.shagox.apptrainingnow.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Uri para fotos (cámara / selección) usando FileProvider, como en UINavegacion.
 * Usa cache-path "images/" definido en res/xml/file_paths.xml.
 */
object ComposeFileProvider {

    /** Crea el archivo temporal en cacheDir/images (mismo path que file_paths.xml). */
    fun createTempImageFile(context: Context): File {
        val directory = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        return File.createTempFile("selected_image_", ".jpg", directory)
    }

    /** Convierte la Uri de la imagen mediante el FileProvider (autoridad = packageName.fileprovider). */
    fun getImageUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /** Crea archivo temporal y devuelve su Uri lista para TakePicture / galería. */
    fun getImageUri(context: Context): Uri {
        val file = createTempImageFile(context)
        return getImageUriForFile(context, file)
    }
}