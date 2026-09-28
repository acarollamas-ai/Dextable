package com.example.ui.windowmanager

import androidx.lifecycle.ViewModel
import com.example.core.applications.AppRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WindowManagerViewModel : ViewModel() {

  private val _windows = MutableStateFlow<List<WindowState>>(emptyList())
  val windows: StateFlow<List<WindowState>> = _windows.asStateFlow()

  private val _focusedWindowId = MutableStateFlow<String?>(null)
  val focusedWindowId: StateFlow<String?> = _focusedWindowId.asStateFlow()

  private var nextZIndex = 10f
  private var cascadeOffset = 0f

  var viewportWidth: Float = 1280f
    private set
  var viewportHeight: Float = 800f
    private set

  fun updateViewport(width: Float, height: Float) {
    if (width > 100f && height > 100f) {
      viewportWidth = width
      viewportHeight = height
    }
  }

  fun openApp(appId: String, customTitle: String? = null, param: String? = null) {
    val existing = _windows.value.find { it.appId == appId }
    if (existing != null) {
      if (existing.isMinimized) {
        restoreWindow(existing.id)
      } else {
        focusWindow(existing.id)
      }
      return
    }

    val appDef = AppRegistry.getApp(appId)
    val title = customTitle ?: appDef?.name ?: "Application"
    val icon = appDef?.iconEmoji ?: "🖥"

    // Responsive tablet proportional sizing
    val maxW = (viewportWidth * 0.88f).coerceAtLeast(320f)
    val maxH = (viewportHeight * 0.82f).coerceAtLeast(260f)
    val w = (appDef?.defaultWidth?.toFloat() ?: 680f).coerceIn(320f, maxW)
    val h = (appDef?.defaultHeight?.toFloat() ?: 480f).coerceIn(260f, maxH)

    val maxX = (viewportWidth - w - 20f).coerceAtLeast(20f)
    val maxY = (viewportHeight - h - 70f).coerceAtLeast(20f)
    val newX = (40f + cascadeOffset).coerceIn(20f, maxX)
    val newY = (30f + cascadeOffset).coerceIn(20f, maxY)
    cascadeOffset = (cascadeOffset + 32f) % 160f

    val winId = "win_${appId}_${System.currentTimeMillis()}"
    nextZIndex += 1f

    val newWindow = WindowState(
      id = winId,
      appId = appId,
      title = title,
      iconEmoji = icon,
      offsetX = newX,
      offsetY = newY,
      width = w,
      height = h,
      zIndex = nextZIndex,
      initialParam = param
    )

    _windows.value = _windows.value + newWindow
    _focusedWindowId.value = winId
  }

  fun focusWindow(id: String) {
    nextZIndex += 1f
    _windows.value = _windows.value.map {
      if (it.id == id) it.copy(zIndex = nextZIndex, isMinimized = false) else it
    }
    _focusedWindowId.value = id
  }

  fun minimizeWindow(id: String) {
    _windows.value = _windows.value.map {
      if (it.id == id) it.copy(isMinimized = true) else it
    }
    if (_focusedWindowId.value == id) {
      val nextTop = _windows.value.filter { !it.isMinimized && it.id != id }.maxByOrNull { it.zIndex }
      _focusedWindowId.value = nextTop?.id
    }
  }

  fun maximizeWindow(id: String) {
    focusWindow(id)
    _windows.value = _windows.value.map {
      if (it.id == id) it.copy(isMaximized = true, isFullscreen = false, isMinimized = false) else it
    }
  }

  fun restoreWindow(id: String) {
    focusWindow(id)
    _windows.value = _windows.value.map {
      if (it.id == id) it.copy(isMaximized = false, isFullscreen = false, isMinimized = false) else it
    }
  }

  fun toggleMaximize(id: String) {
    val win = _windows.value.find { it.id == id } ?: return
    if (win.isMaximized || win.isFullscreen) {
      restoreWindow(id)
    } else {
      maximizeWindow(id)
    }
  }

  fun toggleFullscreen(id: String) {
    val win = _windows.value.find { it.id == id } ?: return
    focusWindow(id)
    _windows.value = _windows.value.map {
      if (it.id == id) it.copy(isFullscreen = !it.isFullscreen, isMinimized = false) else it
    }
  }

  fun closeWindow(id: String) {
    _windows.value = _windows.value.filter { it.id != id }
    if (_focusedWindowId.value == id) {
      val nextTop = _windows.value.filter { !it.isMinimized }.maxByOrNull { it.zIndex }
      _focusedWindowId.value = nextTop?.id
    }
  }

  fun moveWindow(id: String, dx: Float, dy: Float) {
    val limitX = (viewportWidth - 100f).coerceAtLeast(100f)
    val limitY = (viewportHeight - 80f).coerceAtLeast(80f)

    _windows.value = _windows.value.map {
      if (it.id == id && !it.isMaximized && !it.isFullscreen) {
        val newX = (it.offsetX + dx).coerceIn(0f, limitX)
        val newY = (it.offsetY + dy).coerceIn(0f, limitY)
        it.copy(offsetX = newX, offsetY = newY)
      } else it
    }
  }

  fun resizeWindow(id: String, dw: Float, dh: Float) {
    _windows.value = _windows.value.map {
      if (it.id == id && !it.isMaximized && !it.isFullscreen) {
        val newW = (it.width + dw).coerceIn(300f, viewportWidth)
        val newH = (it.height + dh).coerceIn(220f, viewportHeight)
        it.copy(width = newW, height = newH)
      } else it
    }
  }

  fun isAppRunning(appId: String): Boolean {
    return _windows.value.any { it.appId == appId }
  }

  fun toggleAppWindow(appId: String) {
    val existing = _windows.value.find { it.appId == appId }
    if (existing == null) {
      openApp(appId)
    } else {
      if (existing.isMinimized) {
        restoreWindow(existing.id)
      } else if (_focusedWindowId.value == existing.id) {
        minimizeWindow(existing.id)
      } else {
        focusWindow(existing.id)
      }
    }
  }
}
