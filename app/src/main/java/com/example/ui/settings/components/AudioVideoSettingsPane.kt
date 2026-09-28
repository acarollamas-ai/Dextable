package com.example.ui.settings.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.example.core.multimedia.AudioEndpoint
import com.example.core.multimedia.AudioSubsystemEngine
import com.example.core.multimedia.VideoOutputPort
import com.example.core.multimedia.VideoSubsystemEngine

@Composable
fun AudioVideoSettingsPane(
  audioEngine: AudioSubsystemEngine,
  videoEngine: VideoSubsystemEngine,
  modifier: Modifier = Modifier
) {
  val outputs by audioEngine.outputEndpoints.collectAsState()
  val inputs by audioEngine.inputEndpoints.collectAsState()
  val activeOutputId by audioEngine.activeOutputId.collectAsState()
  val activeInputId by audioEngine.activeInputId.collectAsState()
  val masterVolume by audioEngine.masterVolume.collectAsState()
  val isMuted by audioEngine.isMuted.collectAsState()

  val videoStatus by videoEngine.status.collectAsState()
  val availableVideoOutputs by videoEngine.availableOutputs.collectAsState()

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 1. Audio Master Controls
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
              imageVector = if (isMuted || masterVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Volume Geral do Sistema: ${(masterVolume * 100).toInt()}%",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          OutlinedButton(onClick = { audioEngine.toggleMute() }) {
            Text(if (isMuted) "Desmutar" else "Mutar")
          }
        }

        Slider(
          value = if (isMuted) 0f else masterVolume,
          onValueChange = { audioEngine.setMasterVolume(it) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // 2. Audio Output Selection (Speakers, Bluetooth, USB DAC)
    Text(
      text = "Dispositivo de Saída de Áudio (Playback)",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    outputs.forEach { endpoint ->
      AudioDeviceCard(
        endpoint = endpoint,
        isActive = endpoint.id == activeOutputId,
        onSelect = { audioEngine.selectOutput(endpoint.id) }
      )
    }

    // 3. Audio Input Selection (Microphones)
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
      text = "Dispositivo de Entrada de Áudio (Microfone)",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    inputs.forEach { endpoint ->
      AudioDeviceCard(
        endpoint = endpoint,
        isActive = endpoint.id == activeInputId,
        onSelect = { audioEngine.selectInput(endpoint.id) }
      )
    }

    // 4. Video & Display Subsystems
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
      text = "Arquitetura de Vídeo & Monitores Externos (DisplayPort / HDMI)",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )

    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = "Aceleração de Vídeo por Hardware", fontWeight = FontWeight.SemiBold)
            Text(
              text = "GPU: ${videoStatus.gpuRenderer}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = videoStatus.hardwareAccelerationEnabled,
            onCheckedChange = { videoEngine.toggleHardwareAcceleration() }
          )
        }

        HorizontalDivider()

        Text(text = "Monitores e Saídas de Vídeo:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        availableVideoOutputs.forEach { port ->
          val isConnected = videoStatus.activeOutputs.contains(port)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { videoEngine.toggleExternalMonitor(port) }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(
                imageVector = if (port == VideoOutputPort.INTERNAL_DISPLAY) Icons.Default.DisplaySettings else Icons.Default.Tv,
                contentDescription = null,
                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Column {
                Text(text = port.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text(text = port.resolution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
            ) {
              Text(
                text = if (isConnected) "Ativo / Espelhado" else "Conectar",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AudioDeviceCard(
  endpoint: AudioEndpoint,
  isActive: Boolean,
  onSelect: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelect() }
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
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(text = endpoint.type.iconEmoji, fontSize = 20.sp)
          }
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = endpoint.name,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
              )
            )
            if (isActive) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Ativo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Text(
            text = "${endpoint.type.displayName} • ${endpoint.sampleRate / 1000}kHz / ${if (endpoint.channels == 2) "Stereo" else "Mono"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (isActive) {
        Text(
          text = "Em Uso",
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.labelMedium
        )
      } else {
        Text(
          text = "Selecionar",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium
        )
      }
    }
  }
}
