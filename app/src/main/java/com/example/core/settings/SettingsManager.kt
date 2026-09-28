package com.example.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
  DARK, LIGHT, AUTO
}

enum class WallpaperOption(val displayName: String, val resName: String) {
  DEFAULT_DESKTABLE("Desktable Flow (Default)", "desktable_wallpaper"),
  SLATE_GRADIENT("Midnight Slate", "gradient_slate"),
  NEBULA_DARK("Deep Nebula", "gradient_nebula"),
  CLEAN_MINIMAL("Minimalist Grey", "gradient_minimal")
}

data class SystemSettings(
  val themeMode: ThemeMode = ThemeMode.DARK,
  val selectedWallpaper: WallpaperOption = WallpaperOption.DEFAULT_DESKTABLE,
  val brightness: Float = 0.85f,
  val soundVolume: Float = 0.70f,
  val isWifiEnabled: Boolean = true,
  val connectedWifiSsid: String = "Desktable-5G-Ultra",
  val isBluetoothEnabled: Boolean = true,
  val connectedBluetoothDevice: String = "BT Mechanical Keyboard",
  val rotationLock: String = "Auto-Rotate", // Auto-Rotate, Landscape, Portrait
  val displayScale: Float = 1.0f,
  val keyboardLayout: String = "QWERTY (BR-ABNT2 / US-Intl)",
  val language: String = "Português (Brasil)",
  val windowOpacity: Float = 0.96f
)

class SettingsManager {

  private val _settings = MutableStateFlow(SystemSettings())
  val settings: StateFlow<SystemSettings> = _settings.asStateFlow()

  fun updateTheme(mode: ThemeMode) {
    _settings.value = _settings.value.copy(themeMode = mode)
  }

  fun updateWallpaper(wallpaper: WallpaperOption) {
    _settings.value = _settings.value.copy(selectedWallpaper = wallpaper)
  }

  fun setBrightness(brightness: Float) {
    _settings.value = _settings.value.copy(brightness = brightness.coerceIn(0.1f, 1.0f))
  }

  fun setVolume(volume: Float) {
    _settings.value = _settings.value.copy(soundVolume = volume.coerceIn(0f, 1.0f))
  }

  fun toggleWifi() {
    _settings.value = _settings.value.copy(isWifiEnabled = !_settings.value.isWifiEnabled)
  }

  fun toggleBluetooth() {
    _settings.value = _settings.value.copy(isBluetoothEnabled = !_settings.value.isBluetoothEnabled)
  }

  fun setRotation(rotation: String) {
    _settings.value = _settings.value.copy(rotationLock = rotation)
  }

  fun setLanguage(lang: String) {
    _settings.value = _settings.value.copy(language = lang)
  }

  fun setKeyboard(layout: String) {
    _settings.value = _settings.value.copy(keyboardLayout = layout)
  }
}
