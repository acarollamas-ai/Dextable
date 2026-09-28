package com.example.core.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserTab(
  val id: String,
  val title: String = "Nova Guia",
  val url: String = "about:home",
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val isLoading: Boolean = false,
  val isDesktopMode: Boolean = true
)

data class Bookmark(
  val title: String,
  val url: String,
  val iconEmoji: String
)

class BrowserEngine(initialBrowserType: String = "desktable_browser") {

  companion object {
    const val DESKTOP_CHROME_UA =
      "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.137 Safari/537.36 DesktableOS/1.2"
    const val DESKTOP_FIREFOX_UA =
      "Mozilla/5.0 (X11; Linux aarch64; rv:128.0) Gecko/20100101 Firefox/128.3.0esr DesktableOS/1.2"
    const val DESKTOP_BRAVE_UA =
      "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Brave/1.69.160 DesktableOS/1.2"
    const val DESKTOP_EDGE_UA =
      "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.2733.67 DesktableOS/1.2"
    const val MOBILE_UA =
      "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"
  }

  private val _browserTypeFlow = MutableStateFlow(initialBrowserType)
  val browserTypeFlow: StateFlow<String> = _browserTypeFlow.asStateFlow()

  val browserType: String
    get() = _browserTypeFlow.value

  val defaultUserAgent: String
    get() = when (_browserTypeFlow.value) {
      "chrome", "chromium" -> DESKTOP_CHROME_UA
      "firefox" -> DESKTOP_FIREFOX_UA
      "brave" -> DESKTOP_BRAVE_UA
      "edge" -> DESKTOP_EDGE_UA
      else -> DESKTOP_CHROME_UA
    }

  private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
  val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

  private val _activeTabId = MutableStateFlow<String>("")
  val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

  private val _bookmarks = MutableStateFlow<List<Bookmark>>(getDefaultBookmarks(initialBrowserType))
  val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

  private val _history = MutableStateFlow<List<String>>(emptyList())
  val history: StateFlow<List<String>> = _history.asStateFlow()

  init {
    createNewTab("about:home", getStartPageTitle(initialBrowserType))
  }

  fun switchBrowserType(newType: String) {
    if (_browserTypeFlow.value == newType) return
    _browserTypeFlow.value = newType
    _bookmarks.value = getDefaultBookmarks(newType)
    // Update tabs on start page to reflect new browser title
    _tabs.value = _tabs.value.map { tab ->
      if (tab.url == "about:home") tab.copy(title = getStartPageTitle(newType)) else tab
    }
  }

  fun getStartPageTitle(type: String): String {
    return when (type) {
      "chrome" -> "Nova Guia - Google Chrome"
      "firefox" -> "Página Inicial do Firefox"
      "brave" -> "Brave - Nova Guia"
      "edge" -> "Microsoft Edge"
      "chromium" -> "Chromium - Nova Guia"
      else -> "Início - Desktable Browser"
    }
  }

  private fun getDefaultBookmarks(type: String): List<Bookmark> {
    return when (type) {
      "chrome", "chromium" -> listOf(
        Bookmark("Google", "https://www.google.com", "🌐"),
        Bookmark("YouTube", "https://www.youtube.com", "▶️"),
        Bookmark("Gmail", "https://mail.google.com", "✉️"),
        Bookmark("GitHub", "https://github.com", "🐙"),
        Bookmark("Wikipedia", "https://pt.wikipedia.org", "📚"),
        Bookmark("Debian", "https://www.debian.org", "🍥"),
        Bookmark("Google Drive", "https://drive.google.com", "📁")
      )
      "firefox" -> listOf(
        Bookmark("DuckDuckGo", "https://duckduckgo.com", "🦆"),
        Bookmark("Mozilla MDN", "https://developer.mozilla.org", "🦎"),
        Bookmark("Debian Packages", "https://packages.debian.org", "🍥"),
        Bookmark("Linux Kernel", "https://www.kernel.org", "🐧"),
        Bookmark("Wikipedia", "https://pt.wikipedia.org", "📚"),
        Bookmark("Reddit", "https://www.reddit.com", "💬"),
        Bookmark("GitHub", "https://github.com", "🐙")
      )
      "brave" -> listOf(
        Bookmark("Brave Search", "https://search.brave.com", "🦁"),
        Bookmark("DuckDuckGo", "https://duckduckgo.com", "🦆"),
        Bookmark("GitHub", "https://github.com", "🐙"),
        Bookmark("Debian", "https://www.debian.org", "🍥"),
        Bookmark("Wikipedia", "https://pt.wikipedia.org", "📚")
      )
      else -> listOf(
        Bookmark("Google", "https://www.google.com", "🌐"),
        Bookmark("DuckDuckGo", "https://duckduckgo.com", "🦆"),
        Bookmark("Debian Linux", "https://www.debian.org", "🍥"),
        Bookmark("Wikipedia", "https://pt.wikipedia.org", "📚"),
        Bookmark("GitHub", "https://github.com", "🐙")
      )
    }
  }

  fun createNewTab(url: String = "about:home", title: String? = null): String {
    val tabTitle = title ?: if (url == "about:home") getStartPageTitle(browserType) else "Nova Guia"
    val id = "tab_${System.currentTimeMillis()}_${_tabs.value.size}"
    val newTab = BrowserTab(id = id, title = tabTitle, url = url, isDesktopMode = true)
    _tabs.value = _tabs.value + newTab
    _activeTabId.value = id
    return id
  }

  fun closeTab(tabId: String) {
    val cur = _tabs.value
    if (cur.size <= 1) {
      updateTab(tabId) { it.copy(url = "about:home", title = getStartPageTitle(browserType)) }
      return
    }
    val updated = cur.filter { it.id != tabId }
    _tabs.value = updated
    if (_activeTabId.value == tabId) {
      _activeTabId.value = updated.last().id
    }
  }

  fun selectTab(tabId: String) {
    _activeTabId.value = tabId
  }

  fun navigate(tabId: String, rawInput: String) {
    val input = rawInput.trim()
    val searchEngineUrl = when (browserType) {
      "chrome", "chromium" -> "https://www.google.com/search?q="
      "firefox" -> "https://duckduckgo.com/?q="
      "brave" -> "https://search.brave.com/search?q="
      "edge" -> "https://www.bing.com/search?q="
      else -> "https://www.google.com/search?q="
    }

    val finalUrl = when {
      input.startsWith("http://") || input.startsWith("https://") -> input
      input == "about:home" -> "about:home"
      input.contains(".") && !input.contains(" ") -> "https://$input"
      else -> "$searchEngineUrl${input.replace(" ", "+")}"
    }

    val pageTitle = if (finalUrl == "about:home") getStartPageTitle(browserType) else input

    updateTab(tabId) {
      it.copy(url = finalUrl, title = pageTitle)
    }

    if (finalUrl != "about:home") {
      _history.value = listOf(finalUrl) + _history.value.take(49)
    }
  }

  fun toggleDesktopMode(tabId: String) {
    updateTab(tabId) { it.copy(isDesktopMode = !it.isDesktopMode) }
  }

  fun updateTabState(
    tabId: String,
    title: String? = null,
    canGoBack: Boolean? = null,
    canGoForward: Boolean? = null,
    isLoading: Boolean? = null
  ) {
    updateTab(tabId) {
      it.copy(
        title = title ?: it.title,
        canGoBack = canGoBack ?: it.canGoBack,
        canGoForward = canGoForward ?: it.canGoForward,
        isLoading = isLoading ?: it.isLoading
      )
    }
  }

  private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
    _tabs.value = _tabs.value.map { if (it.id == tabId) transform(it) else it }
  }
}
