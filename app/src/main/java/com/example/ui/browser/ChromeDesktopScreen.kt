package com.example.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.browser.BrowserEngine

@Composable
fun ChromeDesktopScreen(
  engine: BrowserEngine? = null,
  onOpenBrowserStore: () -> Unit = {},
  onOpenTerminalWithCommand: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val browserEngine = engine ?: remember { BrowserEngine("chrome") }

  // Ensure engine is set to chrome
  if (browserEngine.browserType != "chrome") {
    browserEngine.switchBrowserType("chrome")
  }

  BrowserScreen(
    engine = browserEngine,
    onOpenBrowserStore = onOpenBrowserStore,
    modifier = modifier
  )
}
