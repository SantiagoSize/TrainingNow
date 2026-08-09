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

    /** Archivo de foto de perfil del usuario (persistente en filesDir). */
    fun getProfilePhotoFile(context: Context, userId: Int): File {
        val dir = File(context.filesDir, "profile_photos").apply { if (!exists()) mkdirs() }
        return File(dir, "user_$userId.jpg")
    }

    /** Uri de la foto de perfil para TakePicture (FileProvider). */
    fun getProfilePhotoUri(context: Context, userId: Int): Uri {
        return getImageUriForFile(context, getProfilePhotoFile(context, userId))
    }
}