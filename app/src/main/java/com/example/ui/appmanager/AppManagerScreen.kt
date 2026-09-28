package com.example.ui.appmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.applications.AppRegistry
import com.example.core.applications.DesktableApp
import com.example.core.architecture.SystemArchitecture
import com.example.core.packages.LinuxPackage
import com.example.core.packages.PackageManagerEngine

@Composable
fun AppManagerScreen(
  packageManager: PackageManagerEngine,
  onOpenApp: (String) -> Unit,
  onToggleDesktopShortcut: (String) -> Unit,
  onToggleDockPin: (String) -> Unit,
  isPinned: (String) -> Boolean,
  hasDesktopShortcut: (String) -> Boolean,
  onOpenTerminalWithCommand: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var operationLog by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .padding(20.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Central de Aplicativos & Gerenciador de Pacotes",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Gerenciamento de aplicativos, repositórios oficiais APT e arquitetura Linux ARM64.",
          style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Aplicativos do Desktop") }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Pacotes Linux ARM64 (APT)") }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Camadas da Arquitetura") }
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Operation Log banner if available
    operationLog?.let { log ->
      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Log de Operação do Pacote:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { operationLog = null }, modifier = Modifier.size(20.dp)) {
              Text("✕", fontSize = 12.sp)
            }
          }
          Text(log, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 6)
        }
      }
    }

    when (selectedTab) {
      0 -> DesktopAppsTab(
        onOpenApp = onOpenApp,
        onToggleDesktopShortcut = onToggleDesktopShortcut,
        onToggleDockPin = onToggleDockPin,
        isPinned = isPinned,
        hasDesktopShortcut = hasDesktopShortcut
      )
      1 -> LinuxPackagesTab(
        packageManager = packageManager,
        onRunAptUpdate = {
          val res = packageManager.updateRepositories()
          operationLog = res.output
        },
        onRunAptUpgrade = {
          val res = packageManager.upgradePackages()
          operationLog = res.output
        },
        onInstallPackage = { pkgId ->
          val res = packageManager.installPackage(pkgId)
          operationLog = res.output
        },
        onRemovePackage = { pkgId ->
          val res = packageManager.removePackage(pkgId)
          operationLog = res.output
        },
        onOpenTerminalWithCommand = onOpenTerminalWithCommand
      )
      2 -> ArchitectureTab()
    }
  }
}

@Composable
private fun DesktopAppsTab(
  onOpenApp: (String) -> Unit,
  onToggleDesktopShortcut: (String) -> Unit,
  onToggleDockPin: (String) -> Unit,
  isPinned: (String) -> Boolean,
  hasDesktopShortcut: (String) -> Boolean
) {
  val apps = AppRegistry.APPS

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(apps, key = { it.id }) { app ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(app.iconEmoji, fontSize = 22.sp)
              }
            }

            Column {
              Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = app.description,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = { onToggleDockPin(app.id) },
              modifier = Modifier.height(36.dp)
            ) {
              Text(if (isPinned(app.id)) "Desafixar Dock" else "Fixar Dock", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = { onToggleDesktopShortcut(app.id) },
              modifier = Modifier.height(36.dp)
            ) {
              Text(if (hasDesktopShortcut(app.id)) "Remover Atalho" else "Atalho Desktop", fontSize = 12.sp)
            }

            Button(
              onClick = { onOpenApp(app.id) },
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Abrir", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LinuxPackagesTab(
  packageManager: PackageManagerEngine,
  onRunAptUpdate: () -> Unit,
  onRunAptUpgrade: () -> Unit,
  onInstallPackage: (String) -> Unit,
  onRemovePackage: (String) -> Unit,
  onOpenTerminalWithCommand: (String) -> Unit
) {
  val packages by packageManager.packages.collectAsState()
  val repos by packageManager.repositories.collectAsState()

  Column(modifier = Modifier.fillMaxSize()) {
    // Actions Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRunAptUpdate, modifier = Modifier.height(38.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("apt update", fontSize = 12.sp)
        }

        OutlinedButton(onClick = onRunAptUpgrade, modifier = Modifier.height(38.dp)) {
          Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("apt upgrade", fontSize = 12.sp)
        }
      }

      Text(
        text = "Arquitetura Alvo: aarch64 (Linux ARM64)",
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
      )
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(packages, key = { it.id }) { pkg ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (pkg.isOfficialArm64Build) {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
              Color(0xFF2E1A0E)
            }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = pkg.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (pkg.isOfficialArm64Build) Color(0xFF102A1E) else Color(0xFF451A03)
                ) {
                  Text(
                    text = pkg.architecture,
                    fontSize = 10.sp,
                    color = if (pkg.isOfficialArm64Build) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                if (pkg.isInstalled) {
                  Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                      text = "Instalado",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              Text(
                text = "v${pkg.version} (${pkg.sizeFormatted})",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }

            Text(
              text = pkg.description,
              style = MaterialTheme.typography.bodyMedium
            )

            if (pkg.maintainerNote != null) {
              Text(
                text = "Nota de Arquitetura: ${pkg.maintainerNote}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFBBF24)),
                fontSize = 11.sp
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Repo: ${pkg.repository}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                  onClick = { onOpenTerminalWithCommand("apt show ${pkg.id}") },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Terminal, contentDescription = "Ver no terminal", modifier = Modifier.size(16.dp))
                }

                if (pkg.isInstalled) {
                  if (pkg.id != "desktable-desktop") {
                    OutlinedButton(
                      onClick = { onRemovePackage(pkg.id) },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Remover", fontSize = 11.sp)
                    }
                  }
                } else if (pkg.isOfficialArm64Build) {
                  Button(
                    onClick = { onInstallPackage(pkg.id) },
                    modifier = Modifier.height(34.dp)
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Instalar", fontSize = 11.sp)
                  }
                } else {
                  OutlinedButton(
                    onClick = { onOpenTerminalWithCommand("apt install ${pkg.id}") },
                    enabled = false,
                    modifier = Modifier.height(34.dp)
                  ) {
                    Text("Sem build ARM64", fontSize = 11.sp)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ArchitectureTab() {
  val layers = SystemArchitecture.LAYERS

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Visão da Pilha Operacional: Hardware ARM64 -> Linux Kernel -> Userspace -> Desktop",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
          )
          Text(
            text = "O Desktable OS está desenhado como um Sistema Operacional Linux ARM64 nativo. O APK atual serve como protótipo e banco de testes de interface enquanto a arquitetura e os pacotes aarch64 são organizados.",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }

    items(layers, key = { it.order }) { layer ->
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${layer.order}. ${layer.name}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.secondaryContainer
            ) {
              Text(
                text = layer.status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            text = layer.description,
            style = MaterialTheme.typography.bodySmall
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Componente Alvo (Linux ARM64):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
              Text(layer.targetComponent, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("No Protótipo Atual:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
              Text(layer.prototypeComponent, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}
