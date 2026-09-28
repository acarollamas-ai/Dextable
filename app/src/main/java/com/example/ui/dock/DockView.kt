package com.example.ui.dock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.applications.AppRegistry
import com.example.core.applications.DesktableApp
import com.example.ui.theme.DockBg
import com.example.ui.theme.DockBorder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockView(
  pinnedAppIds: List<String>,
  runningAppIds: Set<String>,
  onAppClick: (String) -> Unit,
  onOpenLauncher: () -> Unit,
  onUnpinApp: (String) -> Unit,
  onCloseApp: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.padding(bottom = 12.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = DockBg,
      tonalElevation = 10.dp,
      modifier = Modifier
        .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
        .border(width = 1.dp, color = DockBorder, shape = RoundedCornerShape(22.dp))
        .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // 1. Launcher Trigger Button
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onOpenLauncher)
            .padding(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Apps,
              contentDescription = "Launcher",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          // Tiny spacer placeholder for alignment with running indicator
          Box(modifier = Modifier.size(4.dp))
        }

        // Vertical divider between launcher and apps
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(DockBorder)
        )

        // 2. Dock Applications
        pinnedAppIds.forEach { appId ->
          val app = AppRegistry.getApp(appId)
          if (app != null) {
            val isRunning = runningAppIds.contains(appId)
            var showMenu by remember { mutableStateOf(false) }

            Box {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .combinedClickable(
                    onClick = { onAppClick(app.id) },
                    onLongClick = { showMenu = true }
                  )
                  .padding(6.dp)
              ) {
                // App Icon Container
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = app.iconEmoji, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Running Indicator Dot
                if (isRunning) {
                  Box(
                    modifier = Modifier
                      .size(5.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                  )
                } else {
                  Box(modifier = Modifier.size(5.dp))
                }
              }

              // Long-press Context Menu for Dock item
              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Abrir ${app.name}") },
                  onClick = {
                    showMenu = false
                    onAppClick(app.id)
                  }
                )
                if (isRunning) {
                  DropdownMenuItem(
                    text = { Text("Fechar Janela") },
                    onClick = {
                      showMenu = false
                      onCloseApp(app.id)
                    }
                  )
                }
                DropdownMenuItem(
                  text = { Text("Desafixar da Dock") },
                  onClick = {
                    showMenu = false
                    onUnpinApp(app.id)
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}
