package com.example.ui.filemanager

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.filesystem.FileEntry
import com.example.core.filesystem.FileSystemEngine
import com.example.core.vps.RemoteFileEntry
import com.example.core.vps.TransferDirection
import com.example.core.vps.TransferStatus
import com.example.core.vps.VpsAuthType
import com.example.core.vps.VpsManager
import com.example.core.vps.VpsServerConfig
import kotlinx.coroutines.launch
import java.io.File

private enum class FileExplorerMode {
  LOCAL_DEVICE,
  REMOTE_VPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
  fileSystem: FileSystemEngine,
  vpsManager: VpsManager? = null,
  onOpenFile: (FileEntry) -> Unit = {},
  onOpenTerminalInVps: (VpsServerConfig, String) -> Unit = { _, _ -> },
  onOpenTerminalInLocal: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // Local State
  val localCurrentPath by fileSystem.currentPath.collectAsState()
  val localItems by fileSystem.items.collectAsState()
  val clipboard by fileSystem.clipboard.collectAsState()

  // VPS State
  val servers by (vpsManager?.servers?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
  val activeServer by (vpsManager?.activeServer?.collectAsState() ?: remember { mutableStateOf(null) })
  val isConnectedToVps by (vpsManager?.isConnected?.collectAsState() ?: remember { mutableStateOf(false) })
  val isConnectingToVps by (vpsManager?.isConnecting?.collectAsState() ?: remember { mutableStateOf(false) })
  val remoteCurrentPath by (vpsManager?.currentRemotePath?.collectAsState() ?: remember { mutableStateOf("/home") })
  val remoteItems by (vpsManager?.remoteItems?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
  val transfers by (vpsManager?.transfers?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
  val connectionError by (vpsManager?.connectionError?.collectAsState() ?: remember { mutableStateOf(null) })

  // Active Explorer Mode: Local Device vs Remote VPS
  var explorerMode by remember { mutableStateOf(FileExplorerMode.LOCAL_DEVICE) }
  var selectedVpsServerId by remember { mutableStateOf<String?>(null) }

  var isGridView by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var isTransfersExpanded by remember { mutableStateOf(false) }

  // Dialog States
  var showAddServerDialog by remember { mutableStateOf(false) }
  var editingServerConfig by remember { mutableStateOf<VpsServerConfig?>(null) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var showNewFileDialog by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showInfoDialog by remember { mutableStateOf(false) }
  var selectedLocalEntry by remember { mutableStateOf<FileEntry?>(null) }
  var selectedRemoteEntry by remember { mutableStateOf<RemoteFileEntry?>(null) }
  var showUploadSelectDialog by remember { mutableStateOf(false) }
  var showSendToVpsDialog by remember { mutableStateOf(false) }
  var entryToSendToVps by remember { mutableStateOf<FileEntry?>(null) }

  val standardLocations = listOf(
    Pair("Home", "/home/user"),
    Pair("Downloads", "/home/user/Downloads"),
    Pair("Documents", "/home/user/Documents"),
    Pair("Pictures", "/home/user/Pictures"),
    Pair("Desktop", "/home/user/Desktop"),
    Pair("Projects", "/home/user/Projects")
  )

  val filteredLocalItems = localItems.filter {
    searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
  }

  val filteredRemoteItems = remoteItems.filter {
    searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
  }

  val selectedVpsServer = servers.find { it.id == selectedVpsServerId } ?: servers.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    // 1. Unified Top Navigation Toolbar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Up directory button
      IconButton(
        onClick = {
          if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
            fileSystem.navigateUp()
          } else {
            scope.launch { vpsManager?.navigateUp() }
          }
        },
        enabled = if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
          localCurrentPath != "/home/user" && localCurrentPath != "/"
        } else {
          isConnectedToVps && remoteCurrentPath != "/"
        }
      ) {
        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir nível", modifier = Modifier.size(20.dp))
      }

      // Breadcrumb Display
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .weight(1f)
          .height(34.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (explorerMode == FileExplorerMode.REMOTE_VPS) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isConnectedToVps) Color(0xFF10B981) else Color(0xFFF59E0B),
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = if (isConnectedToVps) "SFTP" else "VPS",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
            Text(
              text = if (isConnectedToVps) "${selectedVpsServer?.name ?: "VPS"}: $remoteCurrentPath" else "${selectedVpsServer?.name ?: "VPS"} (Desconectado)",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          } else {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = "Tablet",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
            Text(
              text = localCurrentPath,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Search field
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .width(160.dp)
          .height(34.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          androidx.compose.foundation.text.BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Action: New Folder
      IconButton(onClick = { showNewFolderDialog = true }) {
        Icon(Icons.Default.CreateNewFolder, contentDescription = "Nova Pasta", tint = MaterialTheme.colorScheme.primary)
      }

      // Action: New File (Local) or Upload to VPS (Remote)
      if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
        IconButton(onClick = { showNewFileDialog = true }) {
          Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "Novo Arquivo", tint = MaterialTheme.colorScheme.primary)
        }
        if (clipboard != null) {
          IconButton(onClick = { fileSystem.paste(localCurrentPath) }) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Colar", tint = MaterialTheme.colorScheme.primary)
          }
        }
      } else {
        IconButton(
          onClick = { showUploadSelectDialog = true },
          enabled = isConnectedToVps
        ) {
          Icon(Icons.Default.CloudUpload, contentDescription = "Enviar arquivo do Tablet", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(
          onClick = { scope.launch { vpsManager?.navigateToRemote(remoteCurrentPath) } },
          enabled = isConnectedToVps
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      // Integration: Abrir no Terminal
      IconButton(
        onClick = {
          if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
            onOpenTerminalInLocal(localCurrentPath)
          } else {
            selectedVpsServer?.let { onOpenTerminalInVps(it, remoteCurrentPath) }
          }
        },
        enabled = explorerMode == FileExplorerMode.LOCAL_DEVICE || isConnectedToVps
      ) {
        Icon(Icons.Default.Terminal, contentDescription = "Abrir no Terminal", tint = Color(0xFF10B981))
      }

      // Grid / List toggle
      IconButton(onClick = { isGridView = !isGridView }) {
        Icon(
          imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
          contentDescription = "Alternar visualização"
        )
      }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // 2. Main Two-Pane Explorer View
    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
      // Sidebar: Este dispositivo + Servidores Remotos (VPS)
      Column(
        modifier = Modifier
          .width(220.dp)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
          .padding(vertical = 8.dp)
      ) {
        // Section: Este dispositivo
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Este dispositivo",
            style = MaterialTheme.typography.labelMedium.copy(
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          )
        }

        standardLocations.forEach { (name, path) ->
          val isSelected = explorerMode == FileExplorerMode.LOCAL_DEVICE && localCurrentPath == path
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                explorerMode = FileExplorerMode.LOCAL_DEVICE
                fileSystem.navigateTo(path)
              }
              .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
              .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            val icon = when (name) {
              "Home" -> "🏠"
              "Downloads" -> "📥"
              "Documents" -> "📑"
              "Pictures" -> "🖼"
              "Desktop" -> "🖥"
              "Projects" -> "🔨"
              else -> "📁"
            }
            Text(text = icon, fontSize = 14.sp)
            Text(
              text = name,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Section: Servidores Remotos (VPS)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Servidores (VPS)",
              style = MaterialTheme.typography.labelMedium.copy(
                color = Color(0xFFD97706),
                fontWeight = FontWeight.Bold
              )
            )
          }

          IconButton(
            onClick = {
              editingServerConfig = null
              showAddServerDialog = true
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar servidor", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
          }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
          items(servers, key = { it.id }) { srv ->
            val isSelected = explorerMode == FileExplorerMode.REMOTE_VPS && selectedVpsServerId == srv.id
            val isSrvConnected = activeServer?.id == srv.id && isConnectedToVps

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  explorerMode = FileExplorerMode.REMOTE_VPS
                  selectedVpsServerId = srv.id
                  if (!isSrvConnected) {
                    scope.launch { vpsManager?.connectToServer(srv) }
                  }
                }
                .background(if (isSelected) Color(0xFFD97706).copy(alpha = 0.18f) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Online / Offline Status Dot
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (isSrvConnected) Color(0xFF10B981) else Color(0xFF94A3B8))
              )
              Spacer(modifier = Modifier.width(8.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = srv.name,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${srv.username}@${srv.host}",
                  style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              IconButton(
                onClick = {
                  editingServerConfig = srv
                  showAddServerDialog = true
                },
                modifier = Modifier.size(22.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
              }
            }
          }
        }

        // Add Server Button (Quick link at bottom of sidebar)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable {
              editingServerConfig = null
              showAddServerDialog = true
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(
              text = "Adicionar servidor",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            )
          }
        }
      }

      VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      // Main Explorer Area
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
          // --- LOCAL TABLET FILES ---
          if (filteredLocalItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = if (searchQuery.isNotEmpty()) "Nenhum arquivo encontrado para '$searchQuery'" else "Esta pasta está vazia",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
          } else if (isGridView) {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(110.dp),
              modifier = Modifier.fillMaxSize().padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(filteredLocalItems, key = { it.path }) { item ->
                FileGridCard(
                  item = item,
                  onClick = {
                    if (item.isDirectory) fileSystem.navigateTo(item.path)
                    else onOpenFile(item)
                  },
                  onMoreClick = { selectedLocalEntry = item }
                )
              }
            }
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
              items(filteredLocalItems, key = { it.path }) { item ->
                FileListItem(
                  item = item,
                  onClick = {
                    if (item.isDirectory) fileSystem.navigateTo(item.path)
                    else onOpenFile(item)
                  },
                  onMoreClick = { selectedLocalEntry = item }
                )
              }
            }
          }
        } else {
          // --- REMOTE VPS SFTP FILES ---
          if (selectedVpsServer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(48.dp))
                Text("Nenhum servidor VPS selecionado", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showAddServerDialog = true }) {
                  Text("Adicionar Servidor VPS")
                }
              }
            }
          } else if (isConnectingToVps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = Color(0xFFD97706))
                Text(
                  text = "Conectando via SSH/SFTP a ${selectedVpsServer.name}...",
                  style = MaterialTheme.typography.bodyMedium
                )
                Text(
                  text = "${selectedVpsServer.username}@${selectedVpsServer.host}:${selectedVpsServer.port}",
                  style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
            }
          } else if (!isConnectedToVps) {
            // Disconnected State with Connect Action
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.width(360.dp).padding(16.dp)
              ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(44.dp))
                  Text(text = selectedVpsServer.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                  Text(
                    text = "${selectedVpsServer.username}@${selectedVpsServer.host}:${selectedVpsServer.port}\nAuth: ${selectedVpsServer.authType.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  )

                  if (connectionError != null) {
                    Text(
                      text = connectionError ?: "",
                      style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error),
                      modifier = Modifier.padding(vertical = 4.dp)
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { scope.launch { vpsManager?.connectToServer(selectedVpsServer) } },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Conectar SFTP")
                    }

                    OutlinedButton(
                      onClick = { onOpenTerminalInVps(selectedVpsServer, selectedVpsServer.initialDirectory) },
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("SSH Terminal")
                    }
                  }
                }
              }
            }
          } else {
            // Remote Files View
            if (filteredRemoteItems.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                  text = if (searchQuery.isNotEmpty()) "Nenhum arquivo remoto para '$searchQuery'" else "Diretório remoto vazio",
                  style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
            } else if (isGridView) {
              LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(filteredRemoteItems, key = { it.path }) { item ->
                  RemoteFileGridCard(
                    item = item,
                    onClick = {
                      if (item.isDirectory) {
                        scope.launch { vpsManager?.navigateToRemote(item.path) }
                      }
                    },
                    onMoreClick = { selectedRemoteEntry = item }
                  )
                }
              }
            } else {
              LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(filteredRemoteItems, key = { it.path }) { item ->
                  RemoteFileListItem(
                    item = item,
                    onClick = {
                      if (item.isDirectory) {
                        scope.launch { vpsManager?.navigateToRemote(item.path) }
                      }
                    },
                    onMoreClick = { selectedRemoteEntry = item }
                  )
                }
              }
            }
          }
        }

        // 3. Floating Transfers Indicator / Drawer
        if (transfers.isNotEmpty()) {
          val activeCount = transfers.count { it.status == TransferStatus.IN_PROGRESS }
          Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { isTransfersExpanded = !isTransfersExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(
                    imageVector = if (activeCount > 0) Icons.Default.Refresh else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (activeCount > 0) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = if (activeCount > 0) "Transferências: $activeCount em andamento" else "Transferências concluídas (${transfers.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }

                IconButton(onClick = { isTransfersExpanded = !isTransfersExpanded }, modifier = Modifier.size(24.dp)) {
                  Icon(
                    imageVector = if (isTransfersExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null
                  )
                }
              }

              AnimatedVisibility(visible = isTransfersExpanded) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 6.dp)) {
                  items(transfers.reversed(), key = { it.id }) { task ->
                    TransferTaskItem(
                      task = task,
                      onCancel = { vpsManager?.cancelTransfer(task.id) }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // --- CONTEXT MENUS & DIALOGS ---

  // Local File Action Menu
  selectedLocalEntry?.let { entry ->
    DropdownMenu(
      expanded = true,
      onDismissRequest = { selectedLocalEntry = null }
    ) {
      if (!entry.isDirectory) {
        DropdownMenuItem(
          text = { Text("Abrir no Editor") },
          leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
          onClick = {
            selectedLocalEntry = null
            onOpenFile(entry)
          }
        )
      }

      if (servers.isNotEmpty()) {
        DropdownMenuItem(
          text = { Text("Enviar para Servidor VPS...") },
          leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFD97706)) },
          onClick = {
            entryToSendToVps = entry
            selectedLocalEntry = null
            showSendToVpsDialog = true
          }
        )
      }

      DropdownMenuItem(
        text = { Text("Abrir no Terminal") },
        leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
        onClick = {
          selectedLocalEntry = null
          val target = if (entry.isDirectory) entry.path else entry.path.substringBeforeLast("/")
          onOpenTerminalInLocal(target)
        }
      )

      DropdownMenuItem(
        text = { Text("Copiar") },
        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
        onClick = {
          fileSystem.copy(entry)
          selectedLocalEntry = null
        }
      )

      DropdownMenuItem(
        text = { Text("Recortar") },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
        onClick = {
          fileSystem.cut(entry)
          selectedLocalEntry = null
        }
      )

      DropdownMenuItem(
        text = { Text("Renomear") },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
        onClick = {
          showRenameDialog = true
        }
      )

      DropdownMenuItem(
        text = { Text("Excluir") },
        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        onClick = {
          fileSystem.delete(entry.path)
          selectedLocalEntry = null
        }
      )

      DropdownMenuItem(
        text = { Text("Propriedades") },
        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
        onClick = {
          showInfoDialog = true
        }
      )
    }
  }

  // Remote File Action Menu
  selectedRemoteEntry?.let { entry ->
    DropdownMenu(
      expanded = true,
      onDismissRequest = { selectedRemoteEntry = null }
    ) {
      if (!entry.isDirectory) {
        DropdownMenuItem(
          text = { Text("Baixar para Tablet") },
          leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          onClick = {
            selectedRemoteEntry = null
            val downloadsDir = File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
            vpsManager?.downloadFile(entry, downloadsDir) { downloadedFile ->
              fileSystem.registerDownloadedFile("/home/user/Downloads", entry.name, downloadedFile.readText(), entry.size)
              Toast.makeText(context, "Arquivo ${entry.name} salvo em Downloads", Toast.LENGTH_SHORT).show()
            }
          }
        )

        DropdownMenuItem(
          text = { Text("Abrir no Editor de Texto") },
          leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
          onClick = {
            selectedRemoteEntry = null
            val cacheDir = File(context.cacheDir, "vps_edit").apply { if (!exists()) mkdirs() }
            vpsManager?.downloadFile(entry, cacheDir) { downloadedFile ->
              fileSystem.registerDownloadedFile("/home/user/Documents", entry.name, downloadedFile.readText(), entry.size)
              val tempEntry = FileEntry(
                id = "/home/user/Documents/${entry.name}",
                name = entry.name,
                path = "/home/user/Documents/${entry.name}",
                isDirectory = false,
                size = entry.size,
                content = downloadedFile.readText()
              )
              onOpenFile(tempEntry)
            }
          }
        )
      } else {
        DropdownMenuItem(
          text = { Text("Abrir no Terminal SSH") },
          leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF10B981)) },
          onClick = {
            selectedRemoteEntry = null
            selectedVpsServer?.let { onOpenTerminalInVps(it, entry.path) }
          }
        )
      }

      DropdownMenuItem(
        text = { Text("Renomear") },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
        onClick = {
          showRenameDialog = true
        }
      )

      DropdownMenuItem(
        text = { Text("Excluir") },
        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        onClick = {
          scope.launch { vpsManager?.deleteRemoteFile(entry) }
          selectedRemoteEntry = null
        }
      )

      DropdownMenuItem(
        text = { Text("Propriedades Linux") },
        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
        onClick = {
          showInfoDialog = true
        }
      )
    }
  }

  // Dialog: Add / Edit VPS Server
  if (showAddServerDialog) {
    VpsServerConfigDialog(
      initialConfig = editingServerConfig,
      onDismiss = {
        showAddServerDialog = false
        editingServerConfig = null
      },
      onSave = { newConfig ->
        if (editingServerConfig != null) {
          vpsManager?.updateServer(newConfig)
        } else {
          vpsManager?.addServer(newConfig)
        }
        showAddServerDialog = false
        editingServerConfig = null
      },
      onDelete = { serverId ->
        vpsManager?.removeServer(serverId)
        showAddServerDialog = false
        editingServerConfig = null
      }
    )
  }

  // Dialog: Upload from Tablet to VPS
  if (showUploadSelectDialog) {
    AlertDialog(
      onDismissRequest = { showUploadSelectDialog = false },
      title = { Text("Enviar arquivo para a VPS") },
      text = {
        Column(modifier = Modifier.fillMaxWidth().height(260.dp)) {
          Text("Selecione um arquivo local para enviar para '$remoteCurrentPath':", style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(8.dp))
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(localItems.filter { !it.isDirectory }) { file ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    val localF = fileSystem.getLocalFileForEntry(file)
                    vpsManager?.uploadFile(localF, remoteCurrentPath) {
                      Toast.makeText(context, "Arquivo ${file.name} enviado para a VPS!", Toast.LENGTH_SHORT).show()
                    }
                    showUploadSelectDialog = false
                  }
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(file.iconEmoji, fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f)) {
                  Text(file.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                  Text(file.formattedSize, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Icon(Icons.Default.CloudUpload, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showUploadSelectDialog = false }) {
          Text("Fechar")
        }
      }
    )
  }

  // Dialog: Send selected local file to a chosen VPS
  if (showSendToVpsDialog && entryToSendToVps != null) {
    AlertDialog(
      onDismissRequest = {
        showSendToVpsDialog = false
        entryToSendToVps = null
      },
      title = { Text("Enviar para Servidor VPS") },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text("Arquivo: ${entryToSendToVps?.name}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          Spacer(modifier = Modifier.height(8.dp))
          Text("Escolha o servidor de destino:", style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(8.dp))
          servers.forEach { srv ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  entryToSendToVps?.let { entry ->
                    val localF = fileSystem.getLocalFileForEntry(entry)
                    val targetDir = srv.initialDirectory.ifBlank { "/home/${srv.username}" }
                    vpsManager?.uploadFile(localF, targetDir) {
                      Toast.makeText(context, "${entry.name} enviado com sucesso para ${srv.name}!", Toast.LENGTH_SHORT).show()
                    }
                  }
                  showSendToVpsDialog = false
                  entryToSendToVps = null
                }
            ) {
              Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(srv.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                  Text("${srv.username}@${srv.host} (${srv.initialDirectory})", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          showSendToVpsDialog = false
          entryToSendToVps = null
        }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Dialog: New Folder
  if (showNewFolderDialog) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showNewFolderDialog = false },
      title = { Text(if (explorerMode == FileExplorerMode.LOCAL_DEVICE) "Nova Pasta no Tablet" else "Nova Pasta na VPS") },
      text = {
        OutlinedTextField(
          value = folderName,
          onValueChange = { folderName = it },
          label = { Text("Nome da pasta") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (folderName.isNotBlank()) {
              if (explorerMode == FileExplorerMode.LOCAL_DEVICE) {
                fileSystem.createDirectory(localCurrentPath, folderName)
              } else {
                scope.launch { vpsManager?.createRemoteDirectory(folderName) }
              }
            }
            showNewFolderDialog = false
          }
        ) {
          Text("Criar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancelar") }
      }
    )
  }

  // Dialog: New File (Local)
  if (showNewFileDialog) {
    var fileName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showNewFileDialog = false },
      title = { Text("Novo Arquivo") },
      text = {
        OutlinedTextField(
          value = fileName,
          onValueChange = { fileName = it },
          label = { Text("Nome do arquivo (ex: notas.txt)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (fileName.isNotBlank()) {
              fileSystem.createFile(localCurrentPath, fileName, "")
            }
            showNewFileDialog = false
          }
        ) {
          Text("Criar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewFileDialog = false }) { Text("Cancelar") }
      }
    )
  }

  // Dialog: Rename
  if (showRenameDialog) {
    val curName = selectedLocalEntry?.name ?: selectedRemoteEntry?.name ?: ""
    var newName by remember { mutableStateOf(curName) }
    AlertDialog(
      onDismissRequest = {
        showRenameDialog = false
        selectedLocalEntry = null
        selectedRemoteEntry = null
      },
      title = { Text("Renomear") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Novo nome") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              if (selectedLocalEntry != null) {
                fileSystem.rename(selectedLocalEntry!!.path, newName)
              } else if (selectedRemoteEntry != null) {
                scope.launch { vpsManager?.renameRemoteFile(selectedRemoteEntry!!.path, newName) }
              }
            }
            showRenameDialog = false
            selectedLocalEntry = null
            selectedRemoteEntry = null
          }
        ) {
          Text("Salvar")
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showRenameDialog = false
          selectedLocalEntry = null
          selectedRemoteEntry = null
        }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Dialog: Properties / Info
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = {
        showInfoDialog = false
        selectedLocalEntry = null
        selectedRemoteEntry = null
      },
      title = { Text("Propriedades") },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (selectedLocalEntry != null) {
            val entry = selectedLocalEntry!!
            Text("Nome: ${entry.name}", fontWeight = FontWeight.Bold)
            Text("Caminho: ${entry.path}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("Tipo: ${if (entry.isDirectory) "Diretório" else "Arquivo ${entry.extension}"}")
            Text("Tamanho: ${entry.formattedSize}")
            Text("Modificado: ${entry.formattedDate}")
          } else if (selectedRemoteEntry != null) {
            val entry = selectedRemoteEntry!!
            Text("Nome: ${entry.name}", fontWeight = FontWeight.Bold)
            Text("Caminho Remoto: ${entry.path}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("Permissões Linux: ${entry.permissions}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
            Text("Tipo: ${if (entry.isDirectory) "Diretório Linux" else "Arquivo Linux"}")
            Text("Tamanho: ${entry.formattedSize}")
            Text("Modificado: ${entry.formattedDate}")
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          showInfoDialog = false
          selectedLocalEntry = null
          selectedRemoteEntry = null
        }) {
          Text("Fechar")
        }
      }
    )
  }
}

// Dialog Component to Add/Edit VPS Server
@Composable
fun VpsServerConfigDialog(
  initialConfig: VpsServerConfig?,
  onDismiss: () -> Unit,
  onSave: (VpsServerConfig) -> Unit,
  onDelete: (String) -> Unit
) {
  var name by remember { mutableStateOf(initialConfig?.name ?: "") }
  var host by remember { mutableStateOf(initialConfig?.host ?: "") }
  var portStr by remember { mutableStateOf(initialConfig?.port?.toString() ?: "22") }
  var username by remember { mutableStateOf(initialConfig?.username ?: "root") }
  var authType by remember { mutableStateOf(initialConfig?.authType ?: VpsAuthType.PASSWORD) }
  var password by remember { mutableStateOf(initialConfig?.password ?: "") }
  var privateKey by remember { mutableStateOf(initialConfig?.privateKey ?: "") }
  var passphrase by remember { mutableStateOf(initialConfig?.passphrase ?: "") }
  var initialDir by remember { mutableStateOf(initialConfig?.initialDirectory ?: "/home") }
  var showPassword by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initialConfig == null) "Adicionar Servidor VPS" else "Editar Servidor VPS") },
    text = {
      LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome do servidor (ex: Web Prod)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = host,
              onValueChange = { host = it },
              label = { Text("Host / IP") },
              singleLine = true,
              modifier = Modifier.weight(2.5f)
            )
            OutlinedTextField(
              value = portStr,
              onValueChange = { portStr = it },
              label = { Text("Porta") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
        }

        item {
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuário SSH (ex: ubuntu, root)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        item {
          Text("Método de Autenticação:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
              selected = authType == VpsAuthType.PASSWORD,
              onClick = { authType = VpsAuthType.PASSWORD }
            )
            Text("Senha", modifier = Modifier.clickable { authType = VpsAuthType.PASSWORD })
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
              selected = authType == VpsAuthType.PRIVATE_KEY,
              onClick = { authType = VpsAuthType.PRIVATE_KEY }
            )
            Text("Chave SSH", modifier = Modifier.clickable { authType = VpsAuthType.PRIVATE_KEY })
          }
        }

        if (authType == VpsAuthType.PASSWORD) {
          item {
            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Senha") },
              singleLine = true,
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                  Text(if (showPassword) "Ocultar" else "Ver", style = MaterialTheme.typography.labelSmall)
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
        } else {
          item {
            OutlinedTextField(
              value = privateKey,
              onValueChange = { privateKey = it },
              label = { Text("Chave Privada SSH (PEM / OpenSSH)") },
              modifier = Modifier.fillMaxWidth().height(110.dp),
              textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
          }
          item {
            OutlinedTextField(
              value = passphrase,
              onValueChange = { passphrase = it },
              label = { Text("Passphrase da chave (opcional)") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        item {
          OutlinedTextField(
            value = initialDir,
            onValueChange = { initialDir = it },
            label = { Text("Diretório Inicial (opcional, ex: /var/www)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (host.isNotBlank() && username.isNotBlank()) {
            val cfg = VpsServerConfig(
              id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
              name = name.ifBlank { "VPS $host" },
              host = host.trim(),
              port = portStr.toIntOrNull() ?: 22,
              username = username.trim(),
              authType = authType,
              password = password,
              privateKey = privateKey,
              passphrase = passphrase,
              initialDirectory = initialDir.ifBlank { "/home" }
            )
            onSave(cfg)
          }
        }
      ) {
        Text("Salvar")
      }
    },
    dismissButton = {
      Row {
        if (initialConfig != null) {
          TextButton(
            onClick = { onDelete(initialConfig.id) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
          ) {
            Text("Excluir")
          }
        }
        TextButton(onClick = onDismiss) { Text("Cancelar") }
      }
    }
  )
}

// Transfer Task Row Item
@Composable
fun TransferTaskItem(
  task: com.example.core.vps.VpsTransferTask,
  onCancel: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = if (task.direction == TransferDirection.DOWNLOAD) Icons.Default.CloudDownload else Icons.Default.CloudUpload,
      contentDescription = null,
      tint = if (task.direction == TransferDirection.DOWNLOAD) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(task.fileName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = when (task.status) {
            TransferStatus.IN_PROGRESS -> task.formattedProgress
            TransferStatus.COMPLETED -> "Concluído"
            TransferStatus.FAILED -> "Falha"
            TransferStatus.CANCELLED -> "Cancelado"
            TransferStatus.PENDING -> "Pendente"
          },
          style = MaterialTheme.typography.labelSmall.copy(
            color = when (task.status) {
              TransferStatus.COMPLETED -> Color(0xFF10B981)
              TransferStatus.FAILED -> MaterialTheme.colorScheme.error
              TransferStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        )
      }

      if (task.status == TransferStatus.IN_PROGRESS) {
        LinearProgressIndicator(
          progress = { task.progress },
          modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp)
        )
      }
    }

    if (task.status == TransferStatus.IN_PROGRESS) {
      IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
        Icon(Icons.Default.Cancel, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
      }
    }
  }
}

// Grid Item for Local Files
@Composable
private fun FileGridCard(
  item: FileEntry,
  onClick: () -> Unit,
  onMoreClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    modifier = Modifier
      .fillMaxWidth()
      .height(100.dp)
      .clickable { onClick() }
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
      IconButton(
        onClick = onMoreClick,
        modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
      ) {
        Icon(Icons.Default.MoreVert, contentDescription = "Mais", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
      }

      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(text = item.iconEmoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = item.formattedSize,
          style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        )
      }
    }
  }
}

// List Item for Local Files
@Composable
private fun FileListItem(
  item: FileEntry,
  onClick: () -> Unit,
  onMoreClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = item.iconEmoji, fontSize = 20.sp)
    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = item.formattedSize, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = item.formattedDate, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
      }
    }

    IconButton(onClick = onMoreClick, modifier = Modifier.size(28.dp)) {
      Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
  }
}

// Grid Item for Remote VPS Files
@Composable
private fun RemoteFileGridCard(
  item: RemoteFileEntry,
  onClick: () -> Unit,
  onMoreClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    modifier = Modifier
      .fillMaxWidth()
      .height(100.dp)
      .clickable { onClick() }
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
      IconButton(
        onClick = onMoreClick,
        modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
      ) {
        Icon(Icons.Default.MoreVert, contentDescription = "Mais", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
      }

      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(text = item.iconEmoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = item.formattedSize,
          style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        )
      }
    }
  }
}

// List Item for Remote VPS Files
@Composable
private fun RemoteFileListItem(
  item: RemoteFileEntry,
  onClick: () -> Unit,
  onMoreClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = item.iconEmoji, fontSize = 20.sp)
    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = item.permissions, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color(0xFF10B981), fontSize = 10.sp))
        Text(text = item.formattedSize, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = item.formattedDate, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
      }
    }

    IconButton(onClick = onMoreClick, modifier = Modifier.size(28.dp)) {
      Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
  }
}
