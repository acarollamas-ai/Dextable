package com.example.ui.webos

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebOSScreen — Renderiza a interface completa do desktop virtual (WebOS) estilo macOS
 * com ajuste 100% à tela de tablet (16:9), carregando os componentes do frontend
 * com xterm.js (terminal via WebSocket), gerenciador de arquivos (fetch HTTP) e
 * navegador web com iframe.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebOSScreen(
  onSwitchToNative: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isLoading by remember { mutableStateOf(true) }

  BackHandler(enabled = true) {
    if (webViewRef?.canGoBack() == true) {
      webViewRef?.goBack()
    } else if (onSwitchToNative != null) {
      onSwitchToNative()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D1117))
  ) {
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          }

          webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
              return super.onConsoleMessage(consoleMessage)
            }
          }

          webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isLoading = false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
              return false // Mantém navegação interna
            }
          }

          // Carrega o WebOS macOS com proporção 16:9 direto dos assets locais
          loadUrl("file:///android_asset/webos/index.html")
          webViewRef = this
        }
      },
      update = {
        webViewRef = it
      },
      modifier = Modifier.fillMaxSize()
    )

    // Barra flutuante de utilitários no canto superior direito para alternar entre WebOS e Compose nativo
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 4.dp, end = 120.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        color = Color(0x991E293B),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .clickable { webViewRef?.reload() }
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Recarregar WebOS",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Recarregar",
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      if (onSwitchToNative != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          color = Color(0x991E293B),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .clickable { onSwitchToNative() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DesktopWindows,
              contentDescription = "Modo Compose",
              tint = Color(0xFF34D399),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Modo Compose",
              color = Color(0xFFE2E8F0),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Indicador de Carregamento inicial do WebOS
    AnimatedVisibility(
      visible = isLoading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      Surface(
        color = Color(0xDD0F172A),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.padding(24.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          CircularProgressIndicator(
            color = Color(0xFF38BDF8),
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(16.dp))
          Text(
            text = "Iniciando WebOS macOS (16:9)...",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
