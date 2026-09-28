package com.example.core.multimedia

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioSubsystemEngine(private val context: Context) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _outputEndpoints = MutableStateFlow<List<AudioEndpoint>>(emptyList())
  val outputEndpoints: StateFlow<List<AudioEndpoint>> = _outputEndpoints.asStateFlow()

  private val _inputEndpoints = MutableStateFlow<List<AudioEndpoint>>(emptyList())
  val inputEndpoints: StateFlow<List<AudioEndpoint>> = _inputEndpoints.asStateFlow()

  private val _activeOutputId = MutableStateFlow("internal_speaker")
  val activeOutputId: StateFlow<String> = _activeOutputId.asStateFlow()

  private val _activeInputId = MutableStateFlow("internal_mic")
  val activeInputId: StateFlow<String> = _activeInputId.asStateFlow()

  private val _masterVolume = MutableStateFlow(0.70f)
  val masterVolume: StateFlow<Float> = _masterVolume.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  init {
    loadAudioHardware()
  }

  fun loadAudioHardware() {
    val outputs = mutableListOf<AudioEndpoint>()
    val inputs = mutableListOf<AudioEndpoint>()

    // Check hardware audio devices if supported
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
      try {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        devices.forEach { dev ->
          if (dev.isSink) {
            when (dev.type) {
              AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                outputs.add(AudioEndpoint("hw_speaker", "Alto-falante Integrado (${dev.productName})", AudioDeviceType.INTERNAL_SPEAKER, false, true))
              }
              AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                outputs.add(AudioEndpoint("hw_bt_${dev.id}", "${dev.productName} (Bluetooth Audio)", AudioDeviceType.BLUETOOTH_AUDIO, false, false))
              }
              AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                outputs.add(AudioEndpoint("hw_wired_${dev.id}", "${dev.productName} (Fones com fio / USB)", AudioDeviceType.WIRED_HEADPHONES, false, false))
              }
              AudioDeviceInfo.TYPE_USB_DEVICE -> {
                outputs.add(AudioEndpoint("hw_usb_${dev.id}", "${dev.productName} (USB DAC/Interface)", AudioDeviceType.USB_AUDIO, false, false))
              }
            }
          }
          if (dev.isSource) {
            when (dev.type) {
              AudioDeviceInfo.TYPE_BUILTIN_MIC -> {
                inputs.add(AudioEndpoint("hw_mic", "Microfone Integrado (${dev.productName})", AudioDeviceType.INTERNAL_MIC, true, true))
              }
              AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                inputs.add(AudioEndpoint("hw_bt_mic_${dev.id}", "${dev.productName} (Bluetooth Mic)", AudioDeviceType.BLUETOOTH_MIC, true, false))
              }
              AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                inputs.add(AudioEndpoint("hw_ext_mic_${dev.id}", "${dev.productName} (Microfone Externo)", AudioDeviceType.EXTERNAL_MIC, true, false))
              }
            }
          }
        }
      } catch (_: Exception) {
        // Fallback
      }
    }

    if (outputs.isEmpty()) {
      outputs.addAll(
        listOf(
          AudioEndpoint("internal_speaker", "Alto-falante Integrado do Tablet", AudioDeviceType.INTERNAL_SPEAKER, false, true),
          AudioEndpoint("bt_sony_xm5", "Sony WH-1000XM5 (Bluetooth LDAC)", AudioDeviceType.BLUETOOTH_AUDIO, false, false),
          AudioEndpoint("wired_p2", "Fones P2 / USB-C DAC", AudioDeviceType.WIRED_HEADPHONES, false, false),
          AudioEndpoint("bt_speaker_flip", "JBL Flip 6 (Bluetooth)", AudioDeviceType.BLUETOOTH_AUDIO, false, false)
        )
      )
    }

    if (inputs.isEmpty()) {
      inputs.addAll(
        listOf(
          AudioEndpoint("internal_mic", "Microfone Interno Stereo do Tablet", AudioDeviceType.INTERNAL_MIC, true, true),
          AudioEndpoint("bt_headset_mic", "Microfone do Headset Bluetooth (Sony XM5)", AudioDeviceType.BLUETOOTH_MIC, true, false),
          AudioEndpoint("external_usb_mic", "Microfone Externo USB-C (Rode / Blue)", AudioDeviceType.EXTERNAL_MIC, true, false)
        )
      )
    }

    _outputEndpoints.value = outputs
    _inputEndpoints.value = inputs
  }

  fun selectOutput(endpointId: String) {
    _activeOutputId.value = endpointId
    _outputEndpoints.value = _outputEndpoints.value.map {
      it.copy(isActive = (it.id == endpointId))
    }
  }

  fun selectInput(endpointId: String) {
    _activeInputId.value = endpointId
    _inputEndpoints.value = _inputEndpoints.value.map {
      it.copy(isActive = (it.id == endpointId))
    }
  }

  fun setMasterVolume(volume: Float) {
    _masterVolume.value = volume.coerceIn(0f, 1f)
    if (_masterVolume.value > 0f) _isMuted.value = false
  }

  fun toggleMute() {
    _isMuted.value = !_isMuted.value
  }

  fun addBluetoothDevice(name: String, isInput: Boolean, isOutput: Boolean) {
    val id = "bt_${System.currentTimeMillis()}"
    if (isOutput) {
      val item = AudioEndpoint(id, "$name (Bluetooth)", AudioDeviceType.BLUETOOTH_AUDIO, false, true)
      _outputEndpoints.value = listOf(item) + _outputEndpoints.value.map { it.copy(isActive = false) }
      _activeOutputId.value = id
    }
    if (isInput) {
      val micId = "bt_mic_${System.currentTimeMillis()}"
      val micItem = AudioEndpoint(micId, "Microfone de $name (Bluetooth)", AudioDeviceType.BLUETOOTH_MIC, true, true)
      _inputEndpoints.value = listOf(micItem) + _inputEndpoints.value.map { it.copy(isActive = false) }
      _activeInputId.value = micId
    }
  }

  fun removeBluetoothDevice(name: String) {
    _outputEndpoints.value = _outputEndpoints.value.filterNot { it.name.contains(name) }
    _inputEndpoints.value = _inputEndpoints.value.filterNot { it.name.contains(name) }
    if (_outputEndpoints.value.none { it.id == _activeOutputId.value }) {
      _outputEndpoints.value.firstOrNull()?.let { selectOutput(it.id) }
    }
    if (_inputEndpoints.value.none { it.id == _activeInputId.value }) {
      _inputEndpoints.value.firstOrNull()?.let { selectInput(it.id) }
    }
  }

  fun getActiveOutput(): AudioEndpoint? = _outputEndpoints.value.find { it.id == _activeOutputId.value }
  fun getActiveInput(): AudioEndpoint? = _inputEndpoints.value.find { it.id == _activeInputId.value }
}
