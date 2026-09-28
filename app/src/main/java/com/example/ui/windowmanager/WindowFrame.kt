package com.example.ui.windowmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.WindowCloseRed
import com.example.ui.theme.WindowMaxGreen
import com.example.ui.theme.WindowMinYellow
import kotlin.math.roundToInt

@Composable
fun WindowFrame(
  window: WindowState,
  isFocused: Boolean,
  onFocus: () -> Unit,
  onClose: () -> Unit,
  onMinimize: () -> Unit,
  onMaximize: () -> Unit,
  onFullscreen: () -> Unit,
  onMove: (Float, Float) -> Unit,
  onResize: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  if (window.isMinimized) return

  val density = LocalDensity.current

  val windowModifier = if (window.isFullscreen) {
    Modifier
      .fillMaxSize()
      .zIndex(window.zIndex)
  } else if (window.isMaximized) {
    Modifier
      .fillMaxSize()
      .padding(start = 8.dp, top = 36.dp, end = 8.dp, bottom = 80.dp)
      .zIndex(window.zIndex)
  } else {
    Modifier
      .offset {
        IntOffset(
          (window.offsetX * density.density).roundToInt(),
          (window.offsetY * density.density).roundToInt()
        )
      }
      .width(window.width.dp)
      .height(window.height.dp)
      .zIndex(window.zIndex)
  }

  Surface(
    modifier = windowModifier
      .then(modifier)
      .shadow(
        elevation = if (isFocused) 16.dp else 6.dp,
        shape = if (window.isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
      )
      .border(
        width = 1.dp,
        color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = if (window.isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onFocus
      ),
    shape = if (window.isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = if (isFocused) 4.dp else 1.dp
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Modern Desktop Window Title Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp)
          .background(
            if (isFocused) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          )
          .pointerInput(window.id) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              onFocus()
              onMove(dragAmount.x / density.density, dragAmount.y / density.density)
            }
          }
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left: Traffic Light Control Buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Close button (Red)
          Box(
            modifier = Modifier
              .size(13.dp)
              .clip(CircleShape)
              .background(WindowCloseRed)
              .clickable { onClose() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = Color.Black.copy(alpha = 0.6f),
              modifier = Modifier.size(9.dp)
            )
          }

          // Minimize button (Yellow)
          Box(
            modifier = Modifier
              .size(13.dp)
              .clip(CircleShape)
              .background(WindowMinYellow)
              .clickable { onMinimize() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Remove,
              contentDescription = "Minimize",
              tint = Color.Black.copy(alpha = 0.6f),
              modifier = Modifier.size(9.dp)
            )
          }

          // Maximize button (Green)
          Box(
            modifier = Modifier
              .size(13.dp)
              .clip(CircleShape)
              .background(WindowMaxGreen)
              .clickable { onMaximize() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CropSquare,
              contentDescription = "Maximize",
              tint = Color.Black.copy(alpha = 0.6f),
              modifier = Modifier.size(8.dp)
            )
          }
        }

        // Center: App Title & Icon
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          Text(text = window.iconEmoji, fontSize = 13.sp)
          Text(
            text = window.title,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Right: Fullscreen toggle
        IconButton(
          onClick = onFullscreen,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = if (window.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            contentDescription = "Fullscreen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Main Window Content Area with bottom-right resize grip
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        content()

        // Corner resize handle (only active when not maximized or fullscreen)
        if (!window.isMaximized && !window.isFullscreen) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(24.dp)
              .pointerInput(window.id) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  onFocus()
                  onResize(dragAmount.x / density.density, dragAmount.y / density.density)
                }
              }
              .padding(4.dp)
          ) {
            // Subtle diagonal grip visual
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(10.dp)
                .background(
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                  RoundedCornerShape(topStart = 4.dp)
                )
            )
          }
        }
      }
    }
  }
}
