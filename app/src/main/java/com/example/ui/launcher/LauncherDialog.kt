package com.example.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.applications.AppRegistry
import com.example.core.applications.DesktableApp

@Composable
fun LauncherDialog(
  onDismiss: () -> Unit,
  onLaunchApp: (String) -> Unit,
  onToggleDockPin: (String) -> Unit,
  onToggleDesktopShortcut: (String) -> Unit,
  isPinnedToDock: (String) -> Boolean,
  hasDesktopShortcut: (String) -> Boolean
) {
  var searchQuery by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  val filteredApps = AppRegistry.APPS.filter {
    searchQuery.isEmpty() ||
      it.name.contains(searchQuery, ignoreCase = true) ||
      it.description.contains(searchQuery, ignoreCase = true) ||
      it.category.contains(searchQuery, ignoreCase = true)
  }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
      tonalElevation = 8.dp,
      modifier = Modifier
        .width(520.dp)
        .height(440.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
      ) {
        // Search bar at the top of launcher
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(
                onSearch = {
                  filteredApps.firstOrNull()?.let {
                    onLaunchApp(it.id)
                    onDismiss()
                  }
                }
              ),
              modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
            )
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(10.dp))

        // Application Grid
        LazyVerticalGrid(
          columns = GridCells.Adaptive(110.dp),
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredApps, key = { it.id }) { app ->
            LauncherItemCard(
              app = app,
              onClick = {
                onLaunchApp(app.id)
                onDismiss()
              },
              isPinned = isPinnedToDock(app.id),
              hasDesktop = hasDesktopShortcut(app.id),
              onToggleDock = { onToggleDockPin(app.id) },
              onToggleDesktop = { onToggleDesktopShortcut(app.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LauncherItemCard(
  app: DesktableApp,
  onClick: () -> Unit,
  isPinned: Boolean,
  hasDesktop: Boolean,
  onToggleDock: () -> Unit,
  onToggleDesktop: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Box {
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
      modifier = Modifier
        .width(110.dp)
        .height(105.dp)
        .clickable { onClick() }
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(text = app.iconEmoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = app.name,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = app.category,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
          maxLines = 1
        )
      }
    }

    DropdownMenu(
      expanded = showMenu,
      onDismissRequest = { showMenu = false }
    ) {
      DropdownMenuItem(
        text = { Text(if (isPinned) "Remover da Dock" else "Fixar na Dock") },
        onClick = {
          showMenu = false
          onToggleDock()
        }
      )
      DropdownMenuItem(
        text = { Text(if (hasDesktop) "Remover da Área de Trabalho" else "Adicionar à Área de Trabalho") },
        onClick = {
          showMenu = false
          onToggleDesktop()
        }
      )
    }
  }
}
