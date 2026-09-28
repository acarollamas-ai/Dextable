package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backends.AndroidBackend
import com.example.backends.SystemBackend
import com.example.core.settings.SettingsManager
import com.example.core.settings.ThemeMode
import com.example.core.settings.WallpaperOption
import com.example.ui.settings.components.AudioVideoSettingsPane
import com.example.ui.settings.components.BluetoothSettingsPane

enum class SettingsCategory(val title: String, val icon: ImageVector) {
  APPEARANCE("Aparência", Icons.Default.Palette),
  THEME("Tema", Icons.Default.DarkMode),
  DISPLAY("Tela", Icons.Default.DisplaySettings),
  BRIGHTNESS("Brilho", Icons.Default.Brightness6),
  ROTATION("Rotação", Icons.Default.ScreenRotation),
  WIFI("Wi-Fi", Icons.Default.Wifi),
  BLUETOOTH("Bluetooth", Icons.Default.Bluetooth),
  AUDIO_VIDEO("Áudio & Vídeo", Icons.Default.VolumeUp),
  STORAGE("Armazenamento", Icons.Default.SdStorage),
  KEYBOARD("Teclado", Icons.Default.Keyboard),
  LANGUAGE("Idioma", Icons.Default.Language),
  SYSTEM("Sistema", Icons.Default.SettingsSystemDaydream),
  ABOUT("Sobre", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
  settingsManager: SettingsManager,
  backend: SystemBackend,
  modifier: Modifier = Modifier
) {
  val settings by settingsManager.settings.collectAsState()
  var selectedCategory by remember { mutableStateOf(SettingsCategory.ABOUT) }

  Row(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    // Left Navigation Pane
    Column(
      modifier = Modifier
        .width(210.dp)
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        .verticalScroll(rememberScrollState())
        .padding(vertical = 12.dp)
    ) {
      Text(
        text = "Configurações",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )

      SettingsCategory.values().forEach { category ->
        val isSelected = selectedCategory == category
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { selectedCategory = category }
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = category.title,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          )
        }
      }
    }

    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Right Details Pane
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)
    ) {
      when (selectedCategory) {
        SettingsCategory.APPEARANCE -> {
          AppearanceSettings(settingsManager = settingsManager)
        }
        SettingsCategory.THEME -> {
          ThemeSettings(settingsManager = settingsManager)
        }
        SettingsCategory.DISPLAY -> {
          DisplaySettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.BRIGHTNESS -> {
          BrightnessSettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.ROTATION -> {
          RotationSettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.WIFI -> {
          WifiSettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.BLUETOOTH -> {
          val androidBackend = backend as? AndroidBackend
          if (androidBackend != null) {
            BluetoothSettingsPane(bluetoothEngine = androidBackend.bluetoothEngine)
          } else {
            BluetoothSettingsView(settingsManager = settingsManager)
          }
        }
        SettingsCategory.AUDIO_VIDEO -> {
          val androidBackend = backend as? AndroidBackend
          if (androidBackend != null) {
            AudioVideoSettingsPane(
              audioEngine = androidBackend.audioSubsystem,
              videoEngine = androidBackend.videoSubsystem
            )
          } else {
            BrightnessSettingsView(settingsManager = settingsManager)
          }
        }
        SettingsCategory.STORAGE -> {
          StorageSettingsView(backend = backend)
        }
        SettingsCategory.KEYBOARD -> {
          KeyboardSettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.LANGUAGE -> {
          LanguageSettingsView(settingsManager = settingsManager)
        }
        SettingsCategory.SYSTEM -> {
          SystemInfoView(backend = backend)
        }
        SettingsCategory.ABOUT -> {
          AboutScreenView(backend = backend)
        }
      }
    }
  }
}

@Composable
private fun AppearanceSettings(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Papel de Parede da Área de Trabalho", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

    WallpaperOption.values().forEach { wp ->
      val isSelected = settings.selectedWallpaper == wp
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { settingsManager.updateWallpaper(wp) }
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(text = wp.displayName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(text = "Tema de fundo do desktop Desktable OS", style = MaterialTheme.typography.bodySmall)
          }
          if (isSelected) {
            Text(text = "Ativo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeSettings(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Modo de Tema", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      FilterChip(
        selected = settings.themeMode == ThemeMode.DARK,
        onClick = { settingsManager.updateTheme(ThemeMode.DARK) },
        label = { Text("Modo Escuro (Padrão)") }
      )
      FilterChip(
        selected = settings.themeMode == ThemeMode.LIGHT,
        onClick = { settingsManager.updateTheme(ThemeMode.LIGHT) },
        label = { Text("Modo Claro") }
      )
    }
  }
}

@Composable
private fun DisplaySettingsView(settingsManager: SettingsManager) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Resolução & Escala da Interface", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Text(text = "Otimizado para tablets de 8\", 10\", 11\", 12\" e 13\" polegadas com densidade desktop nativa.", style = MaterialTheme.typography.bodyMedium)
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Modo: Tablet Desktop Workstation")
        Text("Densidade da Grade: 6 x 4 Ícones Dinâmicos")
        Text("Gerenciador de Janelas: Wayland Compositor Engine")
      }
    }
  }
}

@Composable
private fun BrightnessSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Brilho da Tela", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Slider(
      value = settings.brightness,
      onValueChange = { settingsManager.setBrightness(it) },
      modifier = Modifier.fillMaxWidth()
    )
    Text(text = "${(settings.brightness * 100).toInt()}% de luminosidade")
  }
}

@Composable
private fun RotationSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Orientação e Rotação", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    listOf("Auto-Rotate (Recomendado)", "Landscape Lock (Paisagem)", "Portrait Lock (Retrato)").forEach { option ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { settingsManager.setRotation(option) }
          .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(text = option, style = MaterialTheme.typography.bodyLarge)
        if (settings.rotationLock.startsWith(option.substring(0, 4))) {
          Text(text = "✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun WifiSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(text = "Wi-Fi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = if (settings.isWifiEnabled) "Conectado a ${settings.connectedWifiSsid}" else "Desativado")
      }
      Switch(checked = settings.isWifiEnabled, onCheckedChange = { settingsManager.toggleWifi() })
    }
  }
}

@Composable
private fun BluetoothSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(text = "Bluetooth", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = if (settings.isBluetoothEnabled) "Conectado a ${settings.connectedBluetoothDevice}" else "Desativado")
      }
      Switch(checked = settings.isBluetoothEnabled, onCheckedChange = { settingsManager.toggleBluetooth() })
    }
  }
}

@Composable
private fun StorageSettingsView(backend: SystemBackend) {
  val storage = backend.getStorageInfo()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Armazenamento", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    LinearProgressIndicator(
      progress = { storage.usedPercentage },
      modifier = Modifier.fillMaxWidth().height(10.dp)
    )
    Text(
      text = String.format("Usado: %.1f GB de %.1f GB (%.1f GB livres)", storage.usedGb, storage.totalGb, storage.freeGb),
      style = MaterialTheme.typography.bodyMedium
    )
    Card(
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("• Sistema Desktable OS: 8.4 GB")
        Text("• Aplicativos & Pacotes: 12.6 GB")
        Text("• Arquivos do Usuário (/home/user): 3.2 GB")
      }
    }
  }
}

@Composable
private fun KeyboardSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Teclado & Entrada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Text(text = "Layout ativo: ${settings.keyboardLayout}", style = MaterialTheme.typography.bodyMedium)
    Text(text = "Suporte a teclado virtual touchscreen e teclados físicos USB-C / Bluetooth.", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun LanguageSettingsView(settingsManager: SettingsManager) {
  val settings by settingsManager.settings.collectAsState()
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(text = "Idioma do Sistema", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    listOf("Português (Brasil)", "English (US)", "Español").forEach { lang ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { settingsManager.setLanguage(lang) }
          .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(text = lang, style = MaterialTheme.typography.bodyLarge)
        if (settings.language == lang) {
          Text(text = "✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SystemInfoView(backend: SystemBackend) {
  val info = backend.getSystemInfo()
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(text = "Informações do Sistema & Arquitetura", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sistema Operacional: ${info.osName} ${info.osVersion}")
        Text("Arquitetura do Processador: ${info.architecture}")
        Text("Servidor de Exibição: ${info.displayServer}")
        Text("Linux Kernel Real: ${info.kernelVersion}")
        Text("Hostname do Sistema: ${info.hostname}")
        Text("Usuário do Userspace: ${info.currentUser}")
      }
    }

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Separação Arquitetural:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(
          "• Protótipo Atual: Android APK / Compose Desktop Shell / Bridge para Shell Linux Real (/system/bin/sh)\n" +
          "• Sistema Final Alvo: Linux ARM64 Nativo / Kernel 6.8+ / Wayland Compositor / Rootfs Debian-Ubuntu / Aplicações Linux Nativas",
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}

@Composable
private fun AboutScreenView(backend: SystemBackend) {
  val info = backend.getSystemInfo()
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(text = "🖥️", fontSize = 48.sp)
    Text(
      text = "DESKTABLE OS",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    )
    Text(
      text = "Sistema Operacional Linux ARM64 para Tablets",
      style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    )

    Spacer(modifier = Modifier.height(10.dp))

    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.width(460.dp)
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Arquitetura Alvo:", fontWeight = FontWeight.Bold)
          Text(text = "Linux ARM64 (aarch64)")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Interface do Usuário:", fontWeight = FontWeight.Bold)
          Text(text = "Desktable Desktop Shell")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Servidor Gráfico:", fontWeight = FontWeight.Bold)
          Text(text = "Wayland Compositor")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Kernel Subjacente:", fontWeight = FontWeight.Bold)
          Text(text = info.kernelVersion.take(28) + "...")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Hardware Alvo:", fontWeight = FontWeight.Bold)
          Text(text = "Tablets ARM64 (Full HD 1080p, 1200p, 1440p)")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Gerenciador de Pacotes:", fontWeight = FontWeight.Bold)
          Text(text = "APT / DPKG (deb.desktable.org)")
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
      modifier = Modifier.width(460.dp).padding(top = 8.dp)
    ) {
      Text(
        text = "Diretiva Arquitetural: O Desktable OS não é concebido como um simples launcher Android. O projeto evolui para um sistema operacional Linux ARM64 autônomo com terminal real, gerenciador de pacotes real e navegadores oficiais para a plataforma.",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.padding(12.dp)
      )
    }
  }
}
