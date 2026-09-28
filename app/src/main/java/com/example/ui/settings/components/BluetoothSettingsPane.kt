package com.example.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.peripherals.BluetoothDeviceType
import com.example.core.peripherals.BluetoothManagerEngine
import com.example.core.peripherals.BluetoothPeripheral

@Composable
fun BluetoothSettingsPane(
  bluetoothEngine: BluetoothManagerEngine,
  modifier: Modifier = Modifier
) {
  val isEnabled by bluetoothEngine.isEnabled.collectAsState()
  val isScanning by bluetoothEngine.isScanning.collectAsState()
  val pairedDevices by bluetoothEngine.pairedDevices.collectAsState()
  val discoveredDevices by bluetoothEngine.discoveredDevices.collectAsState()

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Title & Global Toggle
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
          )
          Column {
            Text(
              text = "Bluetooth do Sistema",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = if (isEnabled) "Visível como \"Desktable OS Tablet (ARM64)\"" else "Desativado",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Switch(
          checked = isEnabled,
          onCheckedChange = { bluetoothEngine.toggleBluetooth() }
        )
      }
    }

    AnimatedVisibility(visible = isEnabled) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Paired Peripherals Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Dispositivos Pareados (${pairedDevices.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )

          Button(
            onClick = {
              if (isScanning) bluetoothEngine.stopScan() else bluetoothEngine.startScan()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isScanning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
          ) {
            if (isScanning) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onSecondary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Procurando...")
            } else {
              Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Procurar Dispositivos")
            }
          }
        }

        if (pairedDevices.isEmpty()) {
          Text(
            text = "Nenhum periférico Bluetooth pareado no momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          pairedDevices.forEach { device ->
            PeripheralCard(
              device = device,
              onToggleConnect = { bluetoothEngine.toggleConnection(device.address) },
              onForget = { bluetoothEngine.forgetDevice(device.address) }
            )
          }
        }

        // Discovered Devices Section (Nearby)
        if (isScanning || discoveredDevices.isNotEmpty()) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          Text(
            text = "Dispositivos Próximos Encontrados (${discoveredDevices.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )

          if (discoveredDevices.isEmpty()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 8.dp)
            ) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Text("Buscando teclados, mouses, controles e fones por perto...", style = MaterialTheme.typography.bodySmall)
            }
          } else {
            discoveredDevices.forEach { device ->
              DiscoveredPeripheralCard(
                device = device,
                onPair = { bluetoothEngine.pairDevice(device) }
              )
            }
          }
        }

        // Linux Hardware Subsystem Info
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("• Stack Bluetooth: Linux BlueZ 5.72 / Android HAL Bridge", style = MaterialTheme.typography.bodySmall)
            Text("• Perfis Ativos: HID (Teclado/Mouse), A2DP/AVRCP (Áudio Alta Fidelidade), HFP (Microfone)", style = MaterialTheme.typography.bodySmall)
            Text("• Latência de Entrada: <8ms (Modo Desktop de Baixa Latência)", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

@Composable
private fun PeripheralCard(
  device: BluetoothPeripheral,
  onToggleConnect: () -> Unit,
  onForget: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (device.isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color.Black.copy(alpha = 0.2f),
          modifier = Modifier.size(42.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(text = device.type.iconEmoji, fontSize = 22.sp)
          }
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = device.name,
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (device.isConnected) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
              ) {
                Text(
                  text = "Conectado",
                  style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Text(
              text = "${device.type.displayName} • ${device.address}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (device.batteryLevel != null && device.isConnected) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                  imageVector = Icons.Default.BatteryFull,
                  contentDescription = "Bateria",
                  modifier = Modifier.size(13.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "${device.batteryLevel}%",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onToggleConnect,
          colors = ButtonDefaults.outlinedButtonColors()
        ) {
          Text(if (device.isConnected) "Desconectar" else "Conectar")
        }

        IconButton(
          onClick = onForget,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Esquecer",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun DiscoveredPeripheralCard(
  device: BluetoothPeripheral,
  onPair: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(text = device.type.iconEmoji, fontSize = 22.sp)
        Column {
          Text(text = device.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
          Text(text = "${device.type.displayName} • Sinal: ${device.signalStrengthRssi} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Button(onClick = onPair) {
        Text("Parear")
      }
    }
  }
}
