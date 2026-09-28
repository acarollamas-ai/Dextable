package com.example.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.browser.BrowserEngine

@Composable
fun FirefoxDesktopScreen(
  engine: BrowserEngine? = null,
  onOpenBrowserStore: () -> Unit = {},
  onOpenTerminalWithCommand: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val browserEngine = engine ?: remember { BrowserEngine("firefox") }

  // Ensure engine is set to firefox
  if (browserEngine.browserType != "firefox") {
    browserEngine.switchBrowserType("firefox")
  }

  BrowserScreen(
    engine = browserEngine,
    onOpenBrowserStore = onOpenBrowserStore,
    modifier = modifier
  )
}
