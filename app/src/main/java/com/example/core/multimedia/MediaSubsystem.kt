package com.example.core.multimedia

enum class AudioDeviceType(val displayName: String, val iconEmoji: String) {
  INTERNAL_SPEAKER("Alto-falante Interno (Tablet)", "🔊"),
  BLUETOOTH_AUDIO("Áudio Bluetooth (Fones/Caixas)", "🎧"),
  WIRED_HEADPHONES("Fones com Fio (P2 / USB-C DAC)", "🎧"),
  USB_AUDIO("Interface de Áudio USB", "🎛️"),
  INTERNAL_MIC("Microfone Interno do Tablet", "🎙️"),
  BLUETOOTH_MIC("Microfone Headset Bluetooth", "🎙️"),
  EXTERNAL_MIC("Microfone Externo USB-C", "🎙️")
}

data class AudioEndpoint(
  val id: String,
  val name: String,
  val type: AudioDeviceType,
  val isInput: Boolean, // true for mic, false for speaker/headphone
  val isActive: Boolean,
  val volumeLevel: Float = 0.75f,
  val sampleRate: Int = 48000,
  val channels: Int = 2 // stereo
)

enum class VideoOutputPort(val displayName: String, val isConnected: Boolean, val resolution: String) {
  INTERNAL_DISPLAY("Tela Principal do Tablet (Touch)", true, "2560x1600 @ 120Hz"),
  USB_C_DISPLAYPORT("USB-C DisplayPort Alt Mode", false, "3840x2160 @ 60Hz"),
  HDMI_EXTERNAL("Saída HDMI Direta", false, "1920x1080 @ 60Hz"),
  WIRELESS_DISPLAY("Wireless Display (Miracast/WiDi)", false, "1920x1080 @ 60Hz")
}

data class VideoSubsystemStatus(
  val hardwareAccelerationEnabled: Boolean = true,
  val gpuRenderer: String = "Adreno / Mali GPU (Mesa Vulkan / Wayland EGL)",
  val activeOutputs: List<VideoOutputPort> = listOf(VideoOutputPort.INTERNAL_DISPLAY),
  val externalMonitorConnected: Boolean = false,
  val cameraPresent: Boolean = true,
  val cameraResolution: String = "1080p FHD (Front & Rear)"
)
