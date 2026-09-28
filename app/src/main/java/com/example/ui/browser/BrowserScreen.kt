package com.example.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.browser.Bookmark
import com.example.core.browser.BrowserEngine

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
  engine: BrowserEngine,
  onOpenBrowserStore: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val tabs by engine.tabs.collectAsState()
  val activeTabId by engine.activeTabId.collectAsState()
  val bookmarks by engine.bookmarks.collectAsState()
  val history by engine.history.collectAsState()
  val currentBrowserType by engine.browserTypeFlow.collectAsState()

  val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

  var inputUrl by remember(activeTab?.url) {
    mutableStateOf(if (activeTab?.url == "about:home") "" else activeTab?.url ?: "")
  }
  var showBookmarksMenu by remember { mutableStateOf(false) }
  var showHistoryMenu by remember { mutableStateOf(false) }
  var showSwitchBrowserMenu by remember { mutableStateOf(false) }
  var showShieldDialog by remember { mutableStateOf(false) }

  val tabWebViews = remember { mutableMapOf<String, WebView>() }

  DisposableEffect(tabs) {
    val currentTabIds = tabs.map { it.id }.toSet()
    val iterator = tabWebViews.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (!currentTabIds.contains(entry.key)) {
        try {
          entry.value.stopLoading()
          entry.value.destroy()
        } catch (_: Exception) {}
        iterator.remove()
      }
    }
    onDispose { }
  }

  DisposableEffect(Unit) {
    onDispose {
      tabWebViews.values.forEach { wv ->
        try {
          wv.stopLoading()
          wv.destroy()
        } catch (_: Exception) {}
      }
      tabWebViews.clear()
    }
  }

  val activeWebView = activeTab?.let { tabWebViews[it.id] }

  // Theme accents depending on browser
  val (brandIcon, brandColor, brandName) = when (currentBrowserType) {
    "chrome" -> Triple("🌐", Color(0xFF4285F4), "Google Chrome")
    "firefox" -> Triple("🦊", Color(0xFFFF7139), "Mozilla Firefox")
    "brave" -> Triple("🦁", Color(0xFFFF5500), "Brave Browser")
    "edge" -> Triple("🌀", Color(0xFF0078D4), "Microsoft Edge")
    "chromium" -> Triple("⚡", Color(0xFF1973E8), "Chromium Open Source")
    else -> Triple("🧭", Color(0xFF009688), "Desktable Browser")
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    // 1. Browser Tab Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(38.dp)
        .background(
          when (currentBrowserType) {
            "chrome" -> Color(0xFF1A1A1A).copy(alpha = 0.85f)
            "firefox" -> Color(0xFF201235).copy(alpha = 0.9f)
            "brave" -> Color(0xFF1F120C).copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
          }
        )
        .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Browser Brand Badge
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = brandColor.copy(alpha = 0.2f),
        modifier = Modifier.padding(end = 6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(brandIcon, fontSize = 13.sp)
          Text(brandName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
        }
      }

      // Tabs
      LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(tabs, key = { it.id }) { tab ->
          val isSelected = tab.id == activeTabId
          Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
            modifier = Modifier
              .width(160.dp)
              .height(34.dp)
              .clickable { engine.selectTab(tab.id) }
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${if (tab.url == "about:home") brandIcon else "📄"} " + tab.title,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )

              IconButton(
                onClick = { engine.closeTab(tab.id) },
                modifier = Modifier.size(18.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar Aba", modifier = Modifier.size(12.dp))
              }
            }
          }
        }
      }

      IconButton(
        onClick = { engine.createNewTab() },
        modifier = Modifier.size(30.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = "Nova Guia", modifier = Modifier.size(18.dp))
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Switch Browser Button
      Box {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = brandColor.copy(alpha = 0.25f),
          modifier = Modifier
            .clickable { showSwitchBrowserMenu = true }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = brandColor, modifier = Modifier.size(14.dp))
            Text("Trocar Navegador", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandColor)
          }
        }

        DropdownMenu(
          expanded = showSwitchBrowserMenu,
          onDismissRequest = { showSwitchBrowserMenu = false }
        ) {
          Text(
            text = "  Alternar Navegador Desktop:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
          )
          HorizontalDivider()

          DropdownMenuItem(
            text = { Text("🌐 Google Chrome Desktop (V8)") },
            onClick = {
              engine.switchBrowserType("chrome")
              showSwitchBrowserMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("🦊 Mozilla Firefox Quantum (Gecko)") },
            onClick = {
              engine.switchBrowserType("firefox")
              showSwitchBrowserMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("🦁 Brave Browser (Shields AdBlock)") },
            onClick = {
              engine.switchBrowserType("brave")
              showSwitchBrowserMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("⚡ Chromium Open Source (Debian)") },
            onClick = {
              engine.switchBrowserType("chromium")
              showSwitchBrowserMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("🌀 Microsoft Edge Linux") },
            onClick = {
              engine.switchBrowserType("edge")
              showSwitchBrowserMenu = false
            }
          )
          DropdownMenuItem(
            text = { Text("🧭 Desktable Browser Nativo") },
            onClick = {
              engine.switchBrowserType("desktable_browser")
              showSwitchBrowserMenu = false
            }
          )

          HorizontalDivider()
          DropdownMenuItem(
            text = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFFD70A53), modifier = Modifier.size(16.dp))
                Text("🛍️ Abrir Loja de Navegadores...", fontWeight = FontWeight.Bold, color = Color(0xFFD70A53))
              }
            },
            onClick = {
              showSwitchBrowserMenu = false
              onOpenBrowserStore()
            }
          )
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Browser Store Icon Shortcut
      IconButton(
        onClick = onOpenBrowserStore,
        modifier = Modifier.size(30.dp)
      ) {
        Icon(Icons.Default.ShoppingBag, contentDescription = "Loja de Navegadores", tint = Color(0xFFD70A53), modifier = Modifier.size(16.dp))
      }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // 2. Navigation Bar & Omnibar / Address Input
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      IconButton(
        onClick = { activeWebView?.goBack() },
        enabled = activeTab?.canGoBack == true,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", modifier = Modifier.size(18.dp))
      }

      IconButton(
        onClick = { activeWebView?.goForward() },
        enabled = activeTab?.canGoForward == true,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avançar", modifier = Modifier.size(18.dp))
      }

      IconButton(
        onClick = { activeWebView?.reload() },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Recarregar", modifier = Modifier.size(18.dp))
      }

      IconButton(
        onClick = { activeTab?.let { engine.navigate(it.id, "about:home") } },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.Home, contentDescription = "Início", modifier = Modifier.size(18.dp))
      }

      // Address Bar (Chrome Omnibar / Firefox Address Bar)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (currentBrowserType == "chrome") androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.3f)) else null,
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Security / Shield indicator
          if (currentBrowserType == "firefox" || currentBrowserType == "brave") {
            IconButton(
              onClick = { showShieldDialog = true },
              modifier = Modifier.size(22.dp)
            ) {
              Icon(
                Icons.Default.Security,
                contentDescription = "Proteção contra Rastreamento",
                tint = if (currentBrowserType == "firefox") Color(0xFFFF7139) else Color(0xFFFF5500),
                modifier = Modifier.size(15.dp)
              )
            }
          } else {
            Icon(
              Icons.Default.Lock,
              contentDescription = "Seguro",
              tint = Color(0xFF4CAF50),
              modifier = Modifier.size(13.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          BasicTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface,
              fontFamily = FontFamily.Default
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
              onGo = {
                activeTab?.let { tab ->
                  engine.navigate(tab.id, inputUrl)
                }
              }
            ),
            decorationBox = { innerTextField ->
              if (inputUrl.isEmpty()) {
                Text(
                  text = when (currentBrowserType) {
                    "chrome", "chromium" -> "Pesquisar no Google ou digitar um URL"
                    "firefox" -> "Pesquisar com o DuckDuckGo ou digitar um endereço"
                    "brave" -> "Pesquisar com Brave Search ou digitar um endereço"
                    else -> "Pesquisar na Web ou digitar URL..."
                  },
                  style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                  )
                )
              }
              innerTextField()
            },
            modifier = Modifier.weight(1f)
          )

          if (inputUrl.isNotEmpty() && inputUrl != activeTab?.url) {
            IconButton(
              onClick = {
                activeTab?.let { tab ->
                  engine.navigate(tab.id, inputUrl)
                }
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Search, contentDescription = "Navegar", modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Desktop Mode Toggle
      IconButton(
        onClick = { activeTab?.let { engine.toggleDesktopMode(it.id) } },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          if (activeTab?.isDesktopMode == true) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
          contentDescription = "Alternar Modo Desktop",
          tint = if (activeTab?.isDesktopMode == true) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }

      // Bookmarks Dropdown
      Box {
        IconButton(
          onClick = { showBookmarksMenu = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Bookmarks, contentDescription = "Favoritos", modifier = Modifier.size(18.dp))
        }

        DropdownMenu(
          expanded = showBookmarksMenu,
          onDismissRequest = { showBookmarksMenu = false }
        ) {
          Text(
            text = "  Favoritos ($brandName)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
          )
          HorizontalDivider()
          bookmarks.forEach { bm ->
            DropdownMenuItem(
              text = { Text("${bm.iconEmoji}  ${bm.title}") },
              onClick = {
                activeTab?.let { engine.navigate(it.id, bm.url) }
                showBookmarksMenu = false
              }
            )
          }
        }
      }

      // History Dropdown
      Box {
        IconButton(
          onClick = { showHistoryMenu = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.History, contentDescription = "Histórico", modifier = Modifier.size(18.dp))
        }

        DropdownMenu(
          expanded = showHistoryMenu,
          onDismissRequest = { showHistoryMenu = false }
        ) {
          Text(
            text = "  Histórico Recente",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
          )
          HorizontalDivider()
          if (history.isEmpty()) {
            DropdownMenuItem(
              text = { Text("Nenhum histórico recente") },
              onClick = { showHistoryMenu = false }
            )
          } else {
            history.take(8).forEach { url ->
              DropdownMenuItem(
                text = { Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                onClick = {
                  activeTab?.let { engine.navigate(it.id, url) }
                  showHistoryMenu = false
                }
              )
            }
          }
        }
      }
    }

    // Loading indicator
    if (activeTab?.isLoading == true) {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().height(2.5.dp),
        color = brandColor
      )
    } else {
      HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    // 3. Web Content Area or Start Page
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      if (activeTab?.url == "about:home") {
        ModernBrowserStartPage(
          browserType = currentBrowserType,
          bookmarks = bookmarks,
          onNavigate = { url ->
            activeTab.let { engine.navigate(it.id, url) }
          },
          onOpenStore = onOpenBrowserStore,
          onSwitchBrowser = { type -> engine.switchBrowserType(type) }
        )
      } else {
        // Real Android WebView with Desktop User-Agent
        activeTab?.let { currentTab ->
          AndroidView(
            factory = { context ->
              WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                  javaScriptEnabled = true
                  domStorageEnabled = true
                  loadWithOverviewMode = true
                  useWideViewPort = true
                  builtInZoomControls = true
                  displayZoomControls = false
                  userAgentString = if (currentTab.isDesktopMode) engine.defaultUserAgent else BrowserEngine.MOBILE_UA
                }

                webChromeClient = object : WebChromeClient() {
                  override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    title?.let { engine.updateTabState(currentTab.id, title = it) }
                  }

                  override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    engine.updateTabState(currentTab.id, isLoading = newProgress < 100)
                  }
                }

                webViewClient = object : WebViewClient() {
                  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                  }

                  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    engine.updateTabState(
                      currentTab.id,
                      isLoading = true,
                      canGoBack = view?.canGoBack(),
                      canGoForward = view?.canGoForward()
                    )
                  }

                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    engine.updateTabState(
                      currentTab.id,
                      isLoading = false,
                      canGoBack = view?.canGoBack(),
                      canGoForward = view?.canGoForward()
                    )
                  }
                }

                loadUrl(currentTab.url)
                tabWebViews[currentTab.id] = this
              }
            },
            update = { webView ->
              tabWebViews[currentTab.id] = webView
              if (webView.url != currentTab.url && currentTab.url != "about:home") {
                webView.loadUrl(currentTab.url)
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  // Privacy Protection Shield Popup
  if (showShieldDialog) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 10.dp,
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(24.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = brandColor, modifier = Modifier.size(28.dp))
          Column {
            Text(
              text = if (currentBrowserType == "firefox") "Proteção Aprimorada contra Rastreamento" else "Brave Shields",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Proteção ativa para Debian Linux ARM64",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF4CAF50).copy(alpha = 0.15f)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("🛡️", fontSize = 18.sp)
            Text(
              text = "Bloqueadores de anúncios, cookies de terceiros e mineradores de cripto estão ATIVADOS.",
              fontSize = 12.sp,
              color = Color(0xFF2E7D32),
              fontWeight = FontWeight.Medium
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Button(onClick = { showShieldDialog = false }) {
            Text("Entendido")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModernBrowserStartPage(
  browserType: String,
  bookmarks: List<Bookmark>,
  onNavigate: (String) -> Unit,
  onOpenStore: () -> Unit,
  onSwitchBrowser: (String) -> Unit
) {
  var searchInput by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    Spacer(modifier = Modifier.height(20.dp))

    // Browser Identity Branding
    when (browserType) {
      "chrome" -> {
        // Chrome Google Logo
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Text("G", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
          Text("o", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
          Text("o", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBC05))
          Text("g", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
          Text("l", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34A853))
          Text("e", fontSize = 54.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
        }
        Text(
          text = "Google Chrome Desktop (Linux ARM64 / Debian)",
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
      "firefox" -> {
        // Firefox Quantum Logo
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Text("🦊", fontSize = 52.sp)
          Column {
            Text(
              text = "Firefox",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF7139)
              )
            )
            Text(
              text = "Quantum ESR 128 (Oficial Debian GNU/Linux)",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
      }
      "brave" -> {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Text("🦁", fontSize = 48.sp)
          Text(
            text = "Brave Browser",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFF5500)
            )
          )
        }
        Text(
          text = "Navegação rápida e privada com Brave Shields em Debian ARM64",
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
      else -> {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Text("🧭", fontSize = 48.sp)
          Text(
            text = "Desktable Browser",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          )
        }
        Text(
          text = "Navegador Nativo e Acelerado por Hardware (ARM64)",
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    Spacer(modifier = Modifier.height(26.dp))

    // Search Box
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
      modifier = Modifier
        .width(560.dp)
        .height(52.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
          value = searchInput,
          onValueChange = { searchInput = it },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = {
              if (searchInput.isNotEmpty()) onNavigate(searchInput)
            }
          ),
          decorationBox = { innerTextField ->
            if (searchInput.isEmpty()) {
              Text(
                text = when (browserType) {
                  "chrome", "chromium" -> "Pesquisar no Google ou digitar um URL..."
                  "firefox" -> "Pesquisar no DuckDuckGo ou digitar endereço..."
                  "brave" -> "Pesquisar na Web com Brave Search..."
                  else -> "Pesquisar ou digitar endereço web..."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 15.sp
              )
            }
            innerTextField()
          },
          modifier = Modifier.weight(1f)
        )

        if (searchInput.isNotEmpty()) {
          Button(
            onClick = { onNavigate(searchInput) },
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text("Ir", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))

    // Quick Bookmarks & Shortcuts Grid
    Text(
      text = "Atalhos Rápidos e Favoritos",
      style = MaterialTheme.typography.labelLarge.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
      )
    )

    Spacer(modifier = Modifier.height(14.dp))

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.width(580.dp)
    ) {
      bookmarks.forEach { bm ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          modifier = Modifier
            .size(98.dp)
            .clickable { onNavigate(bm.url) }
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = bm.iconEmoji, fontSize = 22.sp)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = bm.title,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Switcher & Browser Store Callout
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
      modifier = Modifier.width(580.dp)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "🛍️ Loja de Navegadores Debian",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "Troque entre Chrome, Firefox, Brave, Edge, Tor e gerencie pacotes APT.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              val nextBrowser = if (browserType == "chrome") "firefox" else "chrome"
              onSwitchBrowser(nextBrowser)
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(if (browserType == "chrome") "🦊 Abrir Firefox" else "🌐 Abrir Chrome", fontSize = 12.sp)
          }

          Button(
            onClick = onOpenStore,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD70A53)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text("Ver Loja", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}
