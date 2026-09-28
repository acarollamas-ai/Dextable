package com.example.ui.systembar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backends.AndroidBackend
import com.example.backends.SystemBackend
import com.example.core.settings.SettingsManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemBar(
  backend: SystemBackend,
  settingsManager: SettingsManager,
  onOpenSettings: () -> Unit,
  onOpenTerminal: () -> Unit,
  onOpenFiles: () -> Unit,
  onSwitchToWebOs: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val settings by settingsManager.settings.collectAsState()

  var currentTime by remember { mutableStateOf("") }
  var currentDate by remember { mutableStateOf("") }

  // Live Clock Updater
  LaunchedEffect(Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    while (true) {
      val now = Date()
      currentTime = timeFormat.format(now)
      currentDate = dateFormat.format(now)
      delay(1000)
    }
  }

  var showOsMenu by remember { mutableStateOf(false) }
  var showFileMenu by remember { mutableStateOf(false) }
  var showEditMenu by remember { mutableStateOf(false) }
  var showViewMenu by remember { mutableStateOf(false) }
  var showWindowMenu by remember { mutableStateOf(false) }
  var showHelpMenu by remember { mutableStateOf(false) }

  var showQuickControls by remember { mutableStateOf(false) }
  var showVolumeSlider by remember { mutableStateOf(false) }
  var showBluetoothMenu by remember { mutableStateOf(false) }

  val androidBackend = backend as? AndroidBackend
  val btEngine = androidBackend?.bluetoothEngine
  val audioEngine = androidBackend?.audioSubsystem

  val isBtEnabled = btEngine?.isEnabled?.collectAsState()?.value ?: settings.isBluetoothEnabled
  val pairedBtDevices = btEngine?.pairedDevices?.collectAsState()?.value ?: emptyList()
  val audioOutputs = audioEngine?.outputEndpoints?.collectAsState()?.value ?: emptyList()
  val activeAudioOutId = audioEngine?.activeOutputId?.collectAsState()?.value ?: "internal_speaker"

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(32.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: OS Identity & Desktop Menus
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // Desktable OS Brand / System Menu
        Box {
          Row(
            modifier = Modifier
              .clickable { showOsMenu = true }
              .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(text = "🖥️", fontSize = 13.sp)
            Text(
              text = "Desktable OS",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          DropdownMenu(
            expanded = showOsMenu,
            onDismissRequest = { showOsMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Sobre o Desktable OS") },
              onClick = {
                showOsMenu = false
                onOpenSettings()
              }
            )
            DropdownMenuItem(
              text = { Text("Preferências do Sistema...") },
              onClick = {
                showOsMenu = false
                onOpenSettings()
              }
            )
            HorizontalDivider()
            DropdownMenuItem(
              text = { Text("Terminal Wayland") },
              onClick = {
                showOsMenu = false
                onOpenTerminal()
              }
            )
            DropdownMenuItem(
              text = { Text("Gerenciador de Arquivos") },
              onClick = {
                showOsMenu = false
                onOpenFiles()
              }
            )
            HorizontalDivider()
            DropdownMenuItem(
              text = { Text("Suspender Tablet") },
              onClick = { showOsMenu = false }
            )
            DropdownMenuItem(
              text = { Text("Reiniciar Sistema") },
              onClick = { showOsMenu = false }
            )
            DropdownMenuItem(
              text = { Text("Desligar Desktable") },
              onClick = { showOsMenu = false }
            )
          }
        }

        // Standard Desktop Menus
        DesktopMenuItem(title = "Arquivo", expanded = showFileMenu, onToggle = { showFileMenu = !showFileMenu }) {
          DropdownMenuItem(text = { Text("Nova Janela") }, onClick = { showFileMenu = false; onOpenTerminal() })
          DropdownMenuItem(text = { Text("Novo Arquivo...") }, onClick = { showFileMenu = false; onOpenFiles() })
          DropdownMenuItem(text = { Text("Fechar Janela Ativa") }, onClick = { showFileMenu = false })
        }

        DesktopMenuItem(title = "Editar", expanded = showEditMenu, onToggle = { showEditMenu = !showEditMenu }) {
          DropdownMenuItem(text = { Text("Copiar") }, onClick = { showEditMenu = false })
          DropdownMenuItem(text = { Text("Colar") }, onClick = { showEditMenu = false })
          DropdownMenuItem(text = { Text("Selecionar Tudo") }, onClick = { showEditMenu = false })
        }

        DesktopMenuItem(title = "Exibir", expanded = showViewMenu, onToggle = { showViewMenu = !showViewMenu }) {
          DropdownMenuItem(text = { Text("Organizar Área de Trabalho") }, onClick = { showViewMenu = false })
          DropdownMenuItem(text = { Text("Modo Tela Cheia") }, onClick = { showViewMenu = false })
        }

        DesktopMenuItem(title = "Janela", expanded = showWindowMenu, onToggle = { showWindowMenu = !showWindowMenu }) {
          DropdownMenuItem(text = { Text("Minimizar Tudo") }, onClick = { showWindowMenu = false })
          DropdownMenuItem(text = { Text("Restaurar Todas") }, onClick = { showWindowMenu = false })
        }

        DesktopMenuItem(title = "Ajuda", expanded = showHelpMenu, onToggle = { showHelpMenu = !showHelpMenu }) {
          DropdownMenuItem(text = { Text("Documentação Desktable OS") }, onClick = { showHelpMenu = false; onOpenSettings() })
        }
      }

      // Right: System Status & Quick Indicators
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (onSwitchToWebOs != null) {
          Surface(
            color = Color(0x3338BDF8),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .clickable { onSwitchToWebOs() }
              .padding(end = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("🌐 WebOS macOS", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
          }
        }

        // Wi-Fi
        Box {
          Icon(
            imageVector = if (settings.isWifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = "Wi-Fi",
            tint = if (settings.isWifiEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .size(15.dp)
              .clickable { settingsManager.toggleWifi() }
          )
        }

        // Bluetooth with device switcher popup
        Box {
          val connectedCount = pairedBtDevices.count { it.isConnected }
          Icon(
            imageVector = if (!isBtEnabled) Icons.Default.BluetoothDisabled
              else if (connectedCount > 0) Icons.Default.BluetoothConnected
              else Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            tint = if (isBtEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .size(15.dp)
              .clickable { showBluetoothMenu = !showBluetoothMenu }
          )

          DropdownMenu(
            expanded = showBluetoothMenu,
            onDismissRequest = { showBluetoothMenu = false }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Bluetooth", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Switch(
                checked = isBtEnabled,
                onCheckedChange = {
                  btEngine?.toggleBluetooth() ?: settingsManager.toggleBluetooth()
                }
              )
            }
            HorizontalDivider()

            if (isBtEnabled) {
              if (pairedBtDevices.isNotEmpty()) {
                Text(
                  text = "Periféricos Pareados:",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )

                pairedBtDevices.forEach { dev ->
                  DropdownMenuItem(
                    text = {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                          Text(dev.type.iconEmoji, fontSize = 14.sp)
                          Text(dev.name, fontSize = 12.sp, maxLines = 1)
                        }
                        if (dev.isConnected) {
                          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (dev.batteryLevel != null) {
                              Text("${dev.batteryLevel}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                          }
                        }
                      }
                    },
                    onClick = {
                      btEngine?.toggleConnection(dev.address)
                    }
                  )
                }
              }

              HorizontalDivider()
            }

            DropdownMenuItem(
              text = { Text("Configurações de Bluetooth...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) },
              onClick = {
                showBluetoothMenu = false
                onOpenSettings()
              }
            )
          }
        }

        // Volume Control & Audio Endpoint Router
        Box {
          val volIcon = when {
            settings.soundVolume == 0f -> Icons.Default.VolumeMute
            settings.soundVolume < 0.5f -> Icons.Default.VolumeDown
            else -> Icons.Default.VolumeUp
          }
          Icon(
            imageVector = volIcon,
            contentDescription = "Volume",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
              .size(15.dp)
              .clickable { showVolumeSlider = !showVolumeSlider }
          )

          DropdownMenu(
            expanded = showVolumeSlider,
            onDismissRequest = { showVolumeSlider = false }
          ) {
            Column(modifier = Modifier.padding(14.dp).width(230.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(text = "Volume do Sistema: ${(settings.soundVolume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Slider(
                value = settings.soundVolume,
                onValueChange = {
                  settingsManager.setVolume(it)
                  audioEngine?.setMasterVolume(it)
                }
              )

              if (audioOutputs.isNotEmpty()) {
                HorizontalDivider()
                Text("Saída de Áudio Ativa:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                audioOutputs.forEach { out ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { audioEngine?.selectOutput(out.id) }
                      .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text(out.type.iconEmoji, fontSize = 13.sp)
                      Text(out.name, fontSize = 11.sp, maxLines = 1)
                    }
                    if (out.id == activeAudioOutId) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // Battery
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          val level = backend.getBatteryLevel()
          val isCharging = backend.isCharging()
          Icon(
            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
            contentDescription = "Bateria",
            tint = if (level > 20) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444),
            modifier = Modifier.size(15.dp)
          )
          Text(text = "$level%", fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        // Date & Time (Clickable for Quick Control Center)
        Box {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clickable { showQuickControls = !showQuickControls }
              .padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Text(
              text = "$currentDate  $currentTime",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          DropdownMenu(
            expanded = showQuickControls,
            onDismissRequest = { showQuickControls = false }
          ) {
            QuickControlCenterView(
              settings = settings,
              settingsManager = settingsManager,
              onOpenSettings = {
                showQuickControls = false
                onOpenSettings()
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopMenuItem(
  title: String,
  expanded: Boolean,
  onToggle: () -> Unit,
  content: @Composable () -> Unit
) {
  Box {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
      modifier = Modifier
        .clickable { onToggle() }
        .padding(horizontal = 6.dp, vertical = 4.dp)
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onToggle) {
      content()
    }
  }
}

@Composable
private fun QuickControlCenterView(
  settings: com.example.core.settings.SystemSettings,
  settingsManager: SettingsManager,
  onOpenSettings: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(260.dp)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(text = "Central de Controle", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "Wi-Fi (${if (settings.isWifiEnabled) settings.connectedWifiSsid else "Desativado"})", fontSize = 12.sp)
      Switch(checked = settings.isWifiEnabled, onCheckedChange = { settingsManager.toggleWifi() })
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = "Bluetooth", fontSize = 12.sp)
      Switch(checked = settings.isBluetoothEnabled, onCheckedChange = { settingsManager.toggleBluetooth() })
    }

    Column {
      Text(text = "Brilho da Tela", fontSize = 12.sp)
      Slider(value = settings.brightness, onValueChange = { settingsManager.setBrightness(it) })
    }

    Column {
      Text(text = "Volume de Áudio", fontSize = 12.sp)
      Slider(value = settings.soundVolume, onValueChange = { settingsManager.setVolume(it) })
    }

    HorizontalDivider()

    Text(
      text = "Abrir Configurações do Sistema...",
      color = MaterialTheme.colorScheme.primary,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .clickable { onOpenSettings() }
        .padding(vertical = 4.dp)
    )
  }
}
