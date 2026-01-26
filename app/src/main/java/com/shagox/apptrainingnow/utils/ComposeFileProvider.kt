package com.shagox.apptrainingnow.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ComposeFileProvider {
    companion object {
        fun getImageUri(context: Context): Uri {
            // 1. Buscamos la carpeta temporal que definimos en file_paths.xml
            // Si no existe la carpeta "images" en caché, la crea.
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()

            // 2. Creamos un archivo temporal con nombre único.
            // "selected_image_" es el prefijo.
            // ".jpg" es la extensión.
            val file = File.createTempFile(
                "selected_image_",
                ".jpg",
                directory
            )

            // 3. Generamos la URI segura usando la "autoridad" que pusimos en el Manifest.
            // Esto se traduce a: "com.shagox.apptrainingnow.fileprovider"
            val authority = context.packageName + ".fileprovider"

            return FileProvider.getUriForFile(
                context,
                authority,
                file
            )
        }
    }
}