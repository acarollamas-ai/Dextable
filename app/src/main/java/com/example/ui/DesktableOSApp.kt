package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.backends.AndroidBackend
import com.example.backends.LinuxBackend
import com.example.core.applications.AppRegistry
import com.example.core.browser.BrowserEngine
import com.example.core.browser.DebianBrowserStore
import com.example.core.filesystem.FileSystemEngine
import com.example.core.packages.PackageManagerEngine
import com.example.core.settings.SettingsManager
import com.example.core.settings.ThemeMode
import com.example.core.terminal.RealLinuxShellEngine
import com.example.core.terminal.TerminalEngine
import com.example.core.vps.VpsManager
import com.example.ui.appmanager.AppManagerScreen
import com.example.ui.browser.BrowserScreen
import com.example.ui.browser.BrowserStoreScreen
import com.example.ui.browser.ChromeDesktopScreen
import com.example.ui.browser.FirefoxDesktopScreen
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.desktop.DesktopIconItem
import com.example.ui.desktop.DesktopView
import com.example.ui.dock.DockView
import com.example.ui.editor.TextEditorScreen
import com.example.ui.filemanager.FileManagerScreen
import com.example.ui.launcher.LauncherDialog
import com.example.ui.settings.SettingsScreen
import com.example.ui.systembar.SystemBar
import com.example.ui.terminal.TerminalScreen
import com.example.ui.theme.DesktableOSTheme
import com.example.ui.windowmanager.WindowFrame
import com.example.ui.windowmanager.WindowManagerViewModel

@Composable
fun DesktableOSApp(
  onSwitchToWebOs: (() -> Unit)? = null,
  windowManager: WindowManagerViewModel = viewModel()
) {
  val context = LocalContext.current

  // Backend Systems
  val androidBackend = remember { AndroidBackend(context) }
  val packageManagerEngine = remember { PackageManagerEngine(context) }
  val realShellEngine = remember { RealLinuxShellEngine(context, packageManagerEngine) }
  val linuxBackend = remember {
    LinuxBackend(androidBackend = androidBackend, shellEngine = realShellEngine)
  }
  val fileSystemEngine = remember {
    FileSystemEngine(context).also { linuxBackend.fsBridge = it }
  }
  val vpsManager = remember { VpsManager(context) }
  val terminalEngine = remember { TerminalEngine(linuxBackend, vpsManager) }

  // Distinct Browser Engines & Debian Browser Store
  val debianBrowserStore = remember { DebianBrowserStore(context, packageManagerEngine) }
  val chromeBrowserEngine = remember { BrowserEngine("chrome") }
  val firefoxBrowserEngine = remember { BrowserEngine("firefox") }
  val desktableBrowserEngine = remember { BrowserEngine("desktable_browser") }

  val settingsManager = remember { SettingsManager() }

  val systemSettings by settingsManager.settings.collectAsState()
  val isDark = when (systemSettings.themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.AUTO -> true
  }

  // Windows State
  val windows by windowManager.windows.collectAsState()
  val focusedWindowId by windowManager.focusedWindowId.collectAsState()

  // Launcher state
  var isLauncherOpen by remember { mutableStateOf(false) }

  // Dock pinned apps state
  var pinnedDockAppIds by remember {
    mutableStateOf(
      listOf("browser_store", "chrome", "firefox", "files", "terminal", "settings", "appmanager")
    )
  }

  // Desktop Icons state
  var desktopItems by remember {
    mutableStateOf(
      listOf(
        DesktopIconItem("dt_bstore", "Loja Navegadores", "🛍️", "app", "browser_store", 24f, 48f),
        DesktopIconItem("dt_chrome", "Google Chrome", "🌐", "app", "chrome", 24f, 150f),
        DesktopIconItem("dt_firefox", "Mozilla Firefox", "🦊", "app", "firefox", 24f, 252f),
        DesktopIconItem("dt_dtbrowser", "Desktable Browser", "🧭", "app", "desktable_browser", 24f, 354f),
        DesktopIconItem("dt_files", "Files", "📁", "app", "files", 24f, 456f),
        DesktopIconItem("dt_terminal", "Terminal", ">_", "app", "terminal", 124f, 48f),
        DesktopIconItem("dt_settings", "Settings", "⚙", "app", "settings", 124f, 150f),
        DesktopIconItem("dt_calc", "Calculator", "🧮", "app", "calculator", 124f, 252f),
        DesktopIconItem("dt_editor", "Text Editor", "📝", "app", "editor", 124f, 354f),
        DesktopIconItem("dt_welcome", "Welcome.txt", "📄", "file", "/home/user/Documents/Welcome_to_Desktable.txt", 124f, 456f),
        DesktopIconItem("dt_webos", "WebOS macOS", "💻", "app", "webos", 224f, 48f)
      )
    )
  }

  val runningAppIds = windows.map { it.appId }.toSet()

  fun organizeIconsInGrid() {
    val items = desktopItems
    var col = 0
    var row = 0
    val startX = 24f
    val startY = 48f
    val stepX = 100f
    val stepY = 104f
    val maxRows = 5

    val reorganized = items.map { item ->
      val x = startX + (col * stepX)
      val y = startY + (row * stepY)
      row++
      if (row >= maxRows) {
        row = 0
        col++
      }
      item.copy(posX = x, posY = y)
    }
    desktopItems = reorganized
  }

  DesktableOSTheme(darkTheme = isDark) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthFloat = maxWidth.value
        val heightFloat = maxHeight.value

        LaunchedEffect(widthFloat, heightFloat) {
          windowManager.updateViewport(widthFloat, heightFloat)
        }

        // 1. Desktop Workspace (Wallpaper, Desktop Shortcuts & Dragging)
        DesktopView(
          settingsManager = settingsManager,
          fileSystem = fileSystemEngine,
          desktopItems = desktopItems,
          onOpenApp = { appId ->
            if (appId == "webos") {
              onSwitchToWebOs?.invoke()
            } else {
              windowManager.openApp(appId)
            }
          },
          onOpenPath = { path ->
            if (path.endsWith(".txt") || path.endsWith(".md") || path.endsWith(".kt") || path.endsWith(".sh")) {
              val name = path.substringAfterLast('/')
              windowManager.openApp("editor", customTitle = name, param = path)
            } else {
              fileSystemEngine.navigateTo(path)
              windowManager.openApp("files")
            }
          },
          onRemoveShortcut = { id ->
            desktopItems = desktopItems.filter { it.id != id }
          },
          onRenameShortcut = { id, newName ->
            desktopItems = desktopItems.map { if (it.id == id) it.copy(name = newName) else it }
          },
          onMoveItem = { id, dx, dy ->
            desktopItems = desktopItems.map {
              if (it.id == id) {
                it.copy(
                  posX = (it.posX + dx).coerceAtLeast(0f),
                  posY = (it.posY + dy).coerceAtLeast(36f)
                )
              } else it
            }
          },
          onOpenSettings = { windowManager.openApp("settings") },
          onOpenTerminal = { windowManager.openApp("terminal") },
          onOrganizeGrid = { organizeIconsInGrid() },
          modifier = Modifier.fillMaxSize()
        )

        // 2. Desktop Windows Layer (Multi-window compositor)
        windows.forEach { window ->
          key(window.id) {
            val isFocused = window.id == focusedWindowId

            WindowFrame(
              window = window,
              isFocused = isFocused,
              onFocus = { windowManager.focusWindow(window.id) },
              onClose = { windowManager.closeWindow(window.id) },
              onMinimize = { windowManager.minimizeWindow(window.id) },
              onMaximize = { windowManager.toggleMaximize(window.id) },
              onFullscreen = { windowManager.toggleFullscreen(window.id) },
              onMove = { dx, dy -> windowManager.moveWindow(window.id, dx, dy) },
              onResize = { dw, dh -> windowManager.resizeWindow(window.id, dw, dh) }
            ) {
              // Render specific desktop app content inside the window
              when (window.appId) {
                "terminal" -> {
                  TerminalScreen(engine = terminalEngine)
                }
                "browser_store" -> {
                  BrowserStoreScreen(
                    browserStore = debianBrowserStore,
                    onOpenBrowser = { browserId ->
                      when (browserId) {
                        "chrome" -> windowManager.openApp("chrome")
                        "firefox" -> windowManager.openApp("firefox")
                        "desktable_browser" -> windowManager.openApp("desktable_browser")
                        else -> {
                          chromeBrowserEngine.switchBrowserType(browserId)
                          windowManager.openApp("chrome")
                        }
                      }
                    },
                    onOpenTerminalWithCommand = { cmd ->
                      windowManager.openApp("terminal")
                      terminalEngine.executeCommand(cmd)
                    }
                  )
                }
                "chrome" -> {
                  ChromeDesktopScreen(
                    engine = chromeBrowserEngine,
                    onOpenBrowserStore = { windowManager.openApp("browser_store") },
                    onOpenTerminalWithCommand = { cmd ->
                      windowManager.openApp("terminal")
                      terminalEngine.executeCommand(cmd)
                    }
                  )
                }
                "firefox" -> {
                  FirefoxDesktopScreen(
                    engine = firefoxBrowserEngine,
                    onOpenBrowserStore = { windowManager.openApp("browser_store") },
                    onOpenTerminalWithCommand = { cmd ->
                      windowManager.openApp("terminal")
                      terminalEngine.executeCommand(cmd)
                    }
                  )
                }
                "desktable_browser" -> {
                  BrowserScreen(
                    engine = desktableBrowserEngine,
                    onOpenBrowserStore = { windowManager.openApp("browser_store") }
                  )
                }
                "files" -> {
                  FileManagerScreen(
                    fileSystem = fileSystemEngine,
                    vpsManager = vpsManager,
                    onOpenFile = { entry ->
                      if (!entry.isDirectory) {
                        windowManager.openApp("editor", customTitle = entry.name, param = entry.path)
                      }
                    },
                    onOpenTerminalInVps = { server, path ->
                      windowManager.openApp("terminal")
                      terminalEngine.startSshSession(server, path)
                    },
                    onOpenTerminalInLocal = { path ->
                      windowManager.openApp("terminal")
                      terminalEngine.executeCommand("cd $path")
                    }
                  )
                }
                "settings" -> {
                  SettingsScreen(
                    settingsManager = settingsManager,
                    backend = linuxBackend
                  )
                }
                "appmanager" -> {
                  AppManagerScreen(
                    packageManager = packageManagerEngine,
                    onOpenApp = { appId -> windowManager.openApp(appId) },
                    onToggleDesktopShortcut = { appId ->
                      val hasIt = desktopItems.any { it.targetId == appId && it.type == "app" }
                      if (hasIt) {
                        desktopItems = desktopItems.filterNot { it.targetId == appId && it.type == "app" }
                      } else {
                        val app = AppRegistry.getApp(appId)
                        if (app != null) {
                          val newItem = DesktopIconItem(
                            id = "dt_${app.id}_${System.currentTimeMillis()}",
                            name = app.name,
                            iconEmoji = app.iconEmoji,
                            type = "app",
                            targetId = app.id,
                            posX = 124f,
                            posY = 456f
                          )
                          desktopItems = desktopItems + newItem
                        }
                      }
                    },
                    onToggleDockPin = { appId ->
                      if (pinnedDockAppIds.contains(appId)) {
                        pinnedDockAppIds = pinnedDockAppIds.filter { it != appId }
                      } else {
                        pinnedDockAppIds = pinnedDockAppIds + appId
                      }
                    },
                    isPinned = { appId -> pinnedDockAppIds.contains(appId) },
                    hasDesktopShortcut = { appId -> desktopItems.any { it.targetId == appId && it.type == "app" } },
                    onOpenTerminalWithCommand = { cmd ->
                      windowManager.openApp("terminal")
                      terminalEngine.executeCommand(cmd)
                    }
                  )
                }
                "editor" -> {
                  TextEditorScreen(
                    fileSystem = fileSystemEngine,
                    filePath = window.initialParam
                  )
                }
                "calculator" -> {
                  CalculatorScreen()
                }
              }
            }
          }
        }

        // 3. Discrete Top System Bar (Desktable OS, Desktop menus, Quick status, Clock)
        SystemBar(
          backend = linuxBackend,
          settingsManager = settingsManager,
          onOpenSettings = { windowManager.openApp("settings") },
          onOpenTerminal = { windowManager.openApp("terminal") },
          onOpenFiles = { windowManager.openApp("files") },
          onSwitchToWebOs = onSwitchToWebOs,
          modifier = Modifier.align(Alignment.TopCenter)
        )

        // 4. Modern Bottom Dock
        DockView(
          pinnedAppIds = pinnedDockAppIds,
          runningAppIds = runningAppIds,
          onAppClick = { appId -> windowManager.toggleAppWindow(appId) },
          onOpenLauncher = { isLauncherOpen = true },
          onUnpinApp = { appId -> pinnedDockAppIds = pinnedDockAppIds.filter { it != appId } },
          onCloseApp = { appId ->
            windows.find { it.appId == appId }?.let { windowManager.closeWindow(it.id) }
          },
          modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. App Launcher Dialog Modal
        if (isLauncherOpen) {
          LauncherDialog(
            onDismiss = { isLauncherOpen = false },
            onLaunchApp = { appId -> windowManager.openApp(appId) },
            onToggleDockPin = { appId ->
              if (pinnedDockAppIds.contains(appId)) {
                pinnedDockAppIds = pinnedDockAppIds.filter { it != appId }
              } else {
                pinnedDockAppIds = pinnedDockAppIds + appId
              }
            },
            onToggleDesktopShortcut = { appId ->
              val hasIt = desktopItems.any { it.targetId == appId && it.type == "app" }
              if (hasIt) {
                desktopItems = desktopItems.filterNot { it.targetId == appId && it.type == "app" }
              } else {
                val app = AppRegistry.getApp(appId)
                if (app != null) {
                  val newItem = DesktopIconItem(
                    id = "dt_${app.id}_${System.currentTimeMillis()}",
                    name = app.name,
                    iconEmoji = app.iconEmoji,
                    type = "app",
                    targetId = app.id,
                    posX = 124f,
                    posY = 456f
                  )
                  desktopItems = desktopItems + newItem
                }
              }
            },
            isPinnedToDock = { appId -> pinnedDockAppIds.contains(appId) },
            hasDesktopShortcut = { appId -> desktopItems.any { it.targetId == appId && it.type == "app" } }
          )
        }
      }
    }
  }
}
