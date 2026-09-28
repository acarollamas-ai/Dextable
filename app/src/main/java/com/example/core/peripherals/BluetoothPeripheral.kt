package com.example.core.peripherals

enum class BluetoothDeviceType(val displayName: String, val iconEmoji: String) {
  KEYBOARD("Teclado Bluetooth", "⌨️"),
  MOUSE("Mouse Bluetooth", "🖱️"),
  TRACKPAD("Trackpad Bluetooth", "🔲"),
  HEADSET("Headset com Microfone", "🎧"),
  EARBUDS("Fones TWS / Earbuds", "👂"),
  SPEAKER("Caixa de Som Bluetooth", "🔊"),
  GAMEPAD("Controle / Gamepad", "🎮"),
  AUDIO_OTHER("Dispositivo de Áudio", "🎵"),
  GENERIC("Periférico Linux", "📟")
}

data class BluetoothPeripheral(
  val address: String,
  val name: String,
  val type: BluetoothDeviceType,
  val isPaired: Boolean,
  val isConnected: Boolean,
  val batteryLevel: Int? = null, // e.g. 95%
  val signalStrengthRssi: Int = -55,
  val isAudioInput: Boolean = false,
  val isAudioOutput: Boolean = false
)
