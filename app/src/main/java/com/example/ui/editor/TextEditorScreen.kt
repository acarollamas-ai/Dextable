package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.filesystem.FileSystemEngine

@Composable
fun TextEditorScreen(
  fileSystem: FileSystemEngine,
  filePath: String? = null,
  modifier: Modifier = Modifier
) {
  val initialContent = remember(filePath) {
    if (filePath != null) fileSystem.getFileContent(filePath) ?: ""
    else "// Desktable OS Text Editor\n// Digite suas anotações ou códigos aqui...\n"
  }

  var textContent by remember { mutableStateOf(initialContent) }
  var isSaved by remember { mutableStateOf(true) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(42.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = (filePath ?: "Sem título.txt") + if (!isSaved) " *" else "",
        style = MaterialTheme.typography.bodyMedium
      )

      IconButton(
        onClick = {
          if (filePath != null) {
            fileSystem.updateFileContent(filePath, textContent)
          } else {
            fileSystem.createFile("/home/user/Documents", "nota_${System.currentTimeMillis()}.txt", textContent)
          }
          isSaved = true
        },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    BasicTextField(
      value = textContent,
      onValueChange = {
        textContent = it
        isSaved = false
      },
      textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
      ),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp)
    )
  }
}
