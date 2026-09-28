package com.example.core.multimedia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoSubsystemEngine {

  private val _status = MutableStateFlow(VideoSubsystemStatus())
  val status: StateFlow<VideoSubsystemStatus> = _status.asStateFlow()

  private val _availableOutputs = MutableStateFlow(
    listOf(
      VideoOutputPort.INTERNAL_DISPLAY,
      VideoOutputPort.USB_C_DISPLAYPORT,
      VideoOutputPort.HDMI_EXTERNAL,
      VideoOutputPort.WIRELESS_DISPLAY
    )
  )
  val availableOutputs: StateFlow<List<VideoOutputPort>> = _availableOutputs.asStateFlow()

  fun toggleHardwareAcceleration() {
    _status.value = _status.value.copy(
      hardwareAccelerationEnabled = !_status.value.hardwareAccelerationEnabled
    )
  }

  fun toggleExternalMonitor(port: VideoOutputPort) {
    val current = _status.value.activeOutputs
    val newOutputs = if (current.contains(port)) {
      if (current.size > 1) current - port else current
    } else {
      current + port
    }
    _status.value = _status.value.copy(
      activeOutputs = newOutputs,
      externalMonitorConnected = newOutputs.any { it != VideoOutputPort.INTERNAL_DISPLAY }
    )
  }
}
