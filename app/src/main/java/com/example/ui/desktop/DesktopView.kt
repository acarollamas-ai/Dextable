package com.example.ui.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.applications.AppRegistry
import com.example.core.filesystem.FileSystemEngine
import com.example.core.settings.SettingsManager
import com.example.core.settings.WallpaperOption
import kotlin.math.roundToInt

data class DesktopIconItem(
  val id: String,
  val name: String,
  val iconEmoji: String,
  val type: String, // "app", "folder", "file"
  val targetId: String, // appId or filePath
  var posX: Float,
  var posY: Float
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopView(
  settingsManager: SettingsManager,
  fileSystem: FileSystemEngine,
  desktopItems: List<DesktopIconItem>,
  onOpenApp: (String) -> Unit,
  onOpenPath: (String) -> Unit,
  onRemoveShortcut: (String) -> Unit,
  onRenameShortcut: (String, String) -> Unit,
  onMoveItem: (String, Float, Float) -> Unit,
  onOpenSettings: () -> Unit,
  onOpenTerminal: () -> Unit,
  onOrganizeGrid: () -> Unit,
  modifier: Modifier = Modifier
) {
  val settings by settingsManager.settings.collectAsState()
  val density = LocalDensity.current

  var showDesktopContextMenu by remember { mutableStateOf(false) }
  var contextMenuOffset by remember { mutableStateOf(Pair(0f, 0f)) }

  var selectedItemForMenu by remember { mutableStateOf<DesktopIconItem?>(null) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var showNewFileDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .combinedClickable(
        onClick = { /* clicked empty desktop */ },
        onLongClick = {
          showDesktopContextMenu = true
        }
      )
  ) {
    // 1. Desktop Wallpaper Background
    when (settings.selectedWallpaper) {
      WallpaperOption.DEFAULT_DESKTABLE -> {
        Image(
          painter = painterResource(id = R.drawable.desktable_wallpaper),
          contentDescription = "Desktable Wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      WallpaperOption.SLATE_GRADIENT -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.radialGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
              )
            )
        )
      }
      WallpaperOption.NEBULA_DARK -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF311042), Color(0xFF090D16))
              )
            )
        )
      }
      WallpaperOption.CLEAN_MINIMAL -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181C24))
        )
      }
    }

    // Subtle dark scrim so icons and windows stand out clearly
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.20f))
    )

    // 2. Desktop Shortcuts and Files
    desktopItems.forEach { item ->
      var showItemMenu by remember { mutableStateOf(false) }

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              (item.posX * density.density).roundToInt(),
              (item.posY * density.density).roundToInt()
            )
          }
          .pointerInput(item.id) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              onMoveItem(item.id, dragAmount.x / density.density, dragAmount.y / density.density)
            }
          }
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
              onClick = {
                when (item.type) {
                  "app" -> onOpenApp(item.targetId)
                  "folder", "file" -> onOpenPath(item.targetId)
                }
              },
              onLongClick = {
                selectedItemForMenu = item
                showItemMenu = true
              }
            )
            .padding(6.dp)
        ) {
          // Icon Container
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.40f),
            modifier = Modifier.size(56.dp)
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(text = item.iconEmoji, fontSize = 30.sp)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Label with shadow backdrop
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 2.dp)
          ) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color.White,
                fontSize = 11.sp
              ),
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        }

        // Context Menu for Specific Desktop Item
        DropdownMenu(
          expanded = showItemMenu,
          onDismissRequest = { showItemMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Abrir") },
            leadingIcon = { Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp)) },
            onClick = {
              showItemMenu = false
              when (item.type) {
                "app" -> onOpenApp(item.targetId)
                "folder", "file" -> onOpenPath(item.targetId)
              }
            }
          )
          DropdownMenuItem(
            text = { Text("Renomear Atalho") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
            onClick = {
              showItemMenu = false
              selectedItemForMenu = item
              showRenameDialog = true
            }
          )
          HorizontalDivider()
          DropdownMenuItem(
            text = { Text("Remover do Desktop", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
            onClick = {
              showItemMenu = false
              onRemoveShortcut(item.id)
            }
          )
        }
      }
    }

    // 3. Desktop Empty Space Context Menu (Long press on wallpaper)
    DropdownMenu(
      expanded = showDesktopContextMenu,
      onDismissRequest = { showDesktopContextMenu = false }
    ) {
      DropdownMenuItem(
        text = { Text("Nova Pasta no Desktop") },
        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) },
        onClick = {
          showDesktopContextMenu = false
          showNewFolderDialog = true
        }
      )
      DropdownMenuItem(
        text = { Text("Novo Arquivo de Texto") },
        leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
        onClick = {
          showDesktopContextMenu = false
          showNewFileDialog = true
        }
      )
      HorizontalDivider()
      DropdownMenuItem(
        text = { Text("Organizar Ícones em Grade") },
        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
        onClick = {
          showDesktopContextMenu = false
          onOrganizeGrid()
        }
      )
      DropdownMenuItem(
        text = { Text("Abrir Terminal Aqui") },
        leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
        onClick = {
          showDesktopContextMenu = false
          onOpenTerminal()
        }
      )
      DropdownMenuItem(
        text = { Text("Alterar Papel de Parede...") },
        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
        onClick = {
          showDesktopContextMenu = false
          onOpenSettings()
        }
      )
    }

    // Rename Shortcut Dialog
    if (showRenameDialog && selectedItemForMenu != null) {
      var newName by remember { mutableStateOf(selectedItemForMenu!!.name) }
      AlertDialog(
        onDismissRequest = {
          showRenameDialog = false
          selectedItemForMenu = null
        },
        title = { Text("Renomear Atalho") },
        text = {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          Button(
            onClick = {
              if (newName.isNotBlank()) {
                onRenameShortcut(selectedItemForMenu!!.id, newName.trim())
              }
              showRenameDialog = false
              selectedItemForMenu = null
            }
          ) {
            Text("Salvar")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showRenameDialog = false
              selectedItemForMenu = null
            }
          ) {
            Text("Cancelar")
          }
        }
      )
    }

    // New Folder Dialog from Desktop
    if (showNewFolderDialog) {
      var folderName by remember { mutableStateOf("Nova Pasta") }
      AlertDialog(
        onDismissRequest = { showNewFolderDialog = false },
        title = { Text("Nova Pasta no Desktop") },
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
              fileSystem.createDirectory("/home/user/Desktop", folderName)
              showNewFolderDialog = false
              onOrganizeGrid()
            }
          ) {
            Text("Criar")
          }
        },
        dismissButton = {
          TextButton(onClick = { showNewFolderDialog = false }) {
            Text("Cancelar")
          }
        }
      )
    }

    // New File Dialog from Desktop
    if (showNewFileDialog) {
      var fileName by remember { mutableStateOf("nota.txt") }
      AlertDialog(
        onDismissRequest = { showNewFileDialog = false },
        title = { Text("Novo Arquivo no Desktop") },
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
              fileSystem.createFile("/home/user/Desktop", fileName, "")
              showNewFileDialog = false
              onOrganizeGrid()
            }
          ) {
            Text("Criar")
          }
        },
        dismissButton = {
          TextButton(onClick = { showNewFileDialog = false }) {
            Text("Cancelar")
          }
        }
      )
    }
  }
}
