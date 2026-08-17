package com.shagox.apptrainingnow.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.SuperficieElevada
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeTN

private val REGEX_ID_YOUTUBE = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([\\w-]{6,20})")

private fun idYoutubeDe(url: String): String? = REGEX_ID_YOUTUBE.find(url)?.groupValues?.get(1)

/**
 * Reproductor de video DENTRO de la app: nunca abre el navegador ni saca al usuario a otra
 * app. Si la URL es de YouTube se embebe con un WebView (player embebido de YouTube); si es
 * un archivo de video propio (adjuntos del chat, subidos a nuestro backend) se reproduce
 * nativamente con VideoView + controles de reproducción.
 *
 * Misma estética que los demás diálogos a pantalla completa de la app (fondo negro,
 * botón "✕" circular arriba a la derecha).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerDialog(videoUrl: String, onDismiss: () -> Unit) {
    val idYoutube = remember(videoUrl) { idYoutubeDe(videoUrl) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NegroFondo),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (idYoutube != null && !error) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        factory = { ctx ->
                            val webView = WebView(ctx)
                            webView.apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                // El error 153 casi siempre es por cookies de terceros bloqueadas:
                                // el WebView de Android las bloquea por defecto desde API 21, y el
                                // player embebido de YouTube las necesita para validar la sesión.
                                // Antes navegábamos directo a youtube.com/embed/... como documento
                                // top-level (sin nunca activar cookies de terceros); ahora se carga
                                // un <iframe> desde un wrapper local, que es el caso real para el
                                // que existen esas cookies.
                                settings.domStorageEnabled = true
                                CookieManager.getInstance().apply {
                                    setAcceptCookie(true)
                                    setAcceptThirdPartyCookies(webView, true)
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        cargando = false
                                    }
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        // WebView propio (no HTTP): YouTube puede mostrar su propia
                                        // tarjeta de error nativa (ej. error 153), la cual bloquea el
                                        // touch del botón "✕" superpuesto en Compose. Por eso sacamos
                                        // el WebView del árbol y mostramos un error 100% nativo, para
                                        // que el diálogo SIEMPRE se pueda cerrar.
                                        cargando = false
                                        error = true
                                    }
                                }
                                val html = """
                                    <html><body style="margin:0;padding:0;background:#000;">
                                    <iframe width="100%" height="100%"
                                        src="https://www.youtube.com/embed/$idYoutube?autoplay=1&playsinline=1"
                                        frameborder="0" allow="autoplay; encrypted-media" allowfullscreen></iframe>
                                    </body></html>
                                """.trimIndent()
                                loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    html,
                                    "text/html",
                                    "utf-8",
                                    null
                                )
                            }
                        }
                    )
                } else if (idYoutube == null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(videoUrl))
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener {
                                    cargando = false
                                    start()
                                }
                                setOnErrorListener { _, _, _ ->
                                    cargando = false
                                    error = true
                                    true
                                }
                            }
                        }
                    )
                }
                if (cargando && !error) {
                    CircularProgressIndicator(color = VerdeTN)
                }
                if (error) {
                    Text("No se pudo reproducir el video.", color = GrisTexto, fontSize = 14.sp)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(SuperficieElevada)
                    .clickable { onDismiss() }
                    .padding(10.dp)
            ) {
                Text("✕", color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
