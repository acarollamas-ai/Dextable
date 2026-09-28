package com.example.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.browser.BrowserStoreCategory
import com.example.core.browser.DebianBrowserInfo
import com.example.core.browser.DebianBrowserStore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserStoreScreen(
  browserStore: DebianBrowserStore,
  onOpenBrowser: (String) -> Unit,
  onOpenTerminalWithCommand: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val browsers by browserStore.browsersList.collectAsState()
  val defaultBrowserId by browserStore.defaultBrowserId.collectAsState()
  val installingId by browserStore.installingBrowserId.collectAsState()
  val installProgress by browserStore.installProgress.collectAsState()
  val installLog by browserStore.installLog.collectAsState()

  var selectedCategory by remember { mutableStateOf(BrowserStoreCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var inspectingBrowser by remember { mutableStateOf<DebianBrowserInfo?>(null) }

  val filteredBrowsers = remember(browsers, selectedCategory, searchQuery) {
    browsers.filter { b ->
      val matchesCat = selectedCategory == BrowserStoreCategory.ALL || b.category == selectedCategory
      val matchesQuery = searchQuery.isBlank() ||
        b.name.contains(searchQuery, ignoreCase = true) ||
        b.debPackageName.contains(searchQuery, ignoreCase = true) ||
        b.engineName.contains(searchQuery, ignoreCase = true) ||
        b.description.contains(searchQuery, ignoreCase = true)
      matchesCat && matchesQuery
    }
  }

  val defaultBrowser = browsers.find { it.id == defaultBrowserId }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    // 1. Header Banner
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFD70A53).copy(alpha = 0.15f), // Debian red accent
              modifier = Modifier.size(46.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("🍥", fontSize = 26.sp)
              }
            }

            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Loja de Navegadores Debian",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFD70A53),
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  Text(
                    text = "Debian ARM64",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = "Catálogo de navegadores compatíveis com Debian GNU/Linux 12 (bookworm / aarch64)",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          }

          // Active Default Browser Indicator
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(start = 8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("Navegador Padrão:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = "${defaultBrowser?.iconEmoji ?: "🌐"} ${defaultBrowser?.name ?: "Nenhum"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        // Search Bar & Filter Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Search Input
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                  if (searchQuery.isEmpty()) {
                    Text("Buscar navegadores, pacotes (.deb) ou motores...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 13.sp)
                  }
                  innerTextField()
                },
                modifier = Modifier.weight(1f)
              )
            }
          }

          Button(
            onClick = { onOpenTerminalWithCommand("apt update && apt-cache search browser") },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Terminal APT", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        // Category Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(top = 2.dp)
        ) {
          items(BrowserStoreCategory.entries) { category ->
            val isSelected = selectedCategory == category
            FilterChip(
              selected = isSelected,
              onClick = { selectedCategory = category },
              label = {
                Text(
                  text = category.label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }
        }
      }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // 2. Active APT Installation Progress Banner (if installing)
    if (installingId != null) {
      val instBrowser = browsers.find { it.id == installingId }
      Surface(
        color = Color(0xFF1E1E1E),
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .clip(RoundedCornerShape(12.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(instBrowser?.iconEmoji ?: "📦", fontSize = 20.sp)
              Text(
                text = "Instalando ${instBrowser?.name ?: "Navegador"} via APT...",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Text(
              text = "${(installProgress * 100).toInt()}%",
              color = Color(0xFF4CAF50),
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }

          LinearProgressIndicator(
            progress = { installProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF333333)
          )

          Text(
            text = installLog.takeLast(240),
            color = Color(0xFFB0BEC5),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 3,
            lineHeight = 15.sp
          )
        }
      }
    }

    // 3. Browsers Grid / List
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      items(filteredBrowsers, key = { it.id }) { browser ->
        BrowserStoreCard(
          browser = browser,
          isDefault = browser.id == defaultBrowserId,
          isInstalling = browser.id == installingId,
          onOpen = { onOpenBrowser(browser.id) },
          onSetDefault = { browserStore.setDefaultBrowser(browser.id) },
          onInstall = { browserStore.installBrowser(browser) },
          onUninstall = { browserStore.uninstallBrowser(browser.id) },
          onOpenTerminalInstall = { onOpenTerminalWithCommand("apt update && apt install ${browser.debPackageName} -y") },
          onInspect = { inspectingBrowser = browser }
        )
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Inspect Browser Technical Details Dialog
  inspectingBrowser?.let { browser ->
    BrowserTechnicalDetailsDialog(
      browser = browser,
      onDismiss = { inspectingBrowser = null },
      onOpenTerminal = {
        inspectingBrowser = null
        onOpenTerminalWithCommand("apt-cache show ${browser.debPackageName}")
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowserStoreCard(
  browser: DebianBrowserInfo,
  isDefault: Boolean,
  isInstalling: Boolean,
  onOpen: () -> Unit,
  onSetDefault: () -> Unit,
  onInstall: () -> Unit,
  onUninstall: () -> Unit,
  onOpenTerminalInstall: () -> Unit,
  onInspect: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Top Row: Icon, Name, Version, Status Badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(browser.brandColorHex).copy(alpha = 0.18f),
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(browser.iconEmoji, fontSize = 30.sp)
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = browser.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              if (isDefault) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Text("Padrão", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "v${browser.version}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
              Text(
                text = browser.engineName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Debian Package Pill
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface
        ) {
          Text(
            text = "${browser.debPackageName} (${browser.packageSizeBytes / (1024 * 1024)} MB)",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Description
      Text(
        text = browser.description,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)),
        lineHeight = 20.sp
      )

      // Feature Chips
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        browser.highlights.forEach { tag ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
          ) {
            Text(
              text = "✓ $tag",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

      // Bottom Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left info / inspect button
        OutlinedButton(
          onClick = onInspect,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Detalhes Debian", fontSize = 12.sp)
        }

        // Right Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          if (browser.isInstalled) {
            if (!isDefault) {
              OutlinedButton(
                onClick = onSetDefault,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text("Definir como Padrão", fontSize = 12.sp)
              }
            }

            Button(
              onClick = onOpen,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(browser.brandColorHex)),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Launch, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Abrir Navegador", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            OutlinedButton(
              onClick = onOpenTerminalInstall,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
            }

            Button(
              onClick = onInstall,
              enabled = !isInstalling,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isInstalling) "Instalando..." else "Instalar via APT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BrowserTechnicalDetailsDialog(
  browser: DebianBrowserInfo,
  onDismiss: () -> Unit,
  onOpenTerminal: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp)
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(browser.iconEmoji, fontSize = 32.sp)
        Column {
          Text(browser.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("Pacote Debian: ${browser.debPackageName}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
      }

      HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRow(label = "Arquitetura Alvo", value = "arm64 (aarch64 / 64-bit ARM)")
        DetailRow(label = "Repositório APT", value = browser.repository)
        DetailRow(label = "Versão Empacotada", value = browser.version)
        DetailRow(label = "Motor de Renderização", value = browser.engineName)
        DetailRow(label = "Tamanho do Download (.deb)", value = "${browser.packageSizeBytes / (1024 * 1024)} MB")
        DetailRow(label = "Comando de Instalação", value = "apt install ${browser.debPackageName} -y", isCode = true)
      }

      if (browser.maintainerNote.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(browser.maintainerNote, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(onClick = onOpenTerminal) {
          Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Ver no Terminal APT", fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(onClick = onDismiss) {
          Text("Fechar", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String, isCode: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (isCode) {
      Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E1E1E)) {
        Text(
          text = value,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = Color(0xFF4CAF50),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    } else {
      Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}
