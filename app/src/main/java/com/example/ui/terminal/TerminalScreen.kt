package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.terminal.TerminalEngine
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalPromptPath
import com.example.ui.theme.TerminalPromptUser
import com.example.ui.theme.TerminalText
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
  engine: TerminalEngine,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val sessions by engine.sessions.collectAsState()
  val activeSessionId by engine.activeSessionId.collectAsState()
  val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()

  var inputCommand by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  // Scroll to bottom on new lines
  LaunchedEffect(activeSession?.lines?.size) {
    if ((activeSession?.lines?.size ?: 0) > 0) {
      listState.animateScrollToItem((activeSession?.lines?.size ?: 1) - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBg)
  ) {
    // Terminal Tab Bar for multiple sessions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(34.dp)
        .background(Color(0xFF0F141C)),
      verticalAlignment = Alignment.CenterVertically
    ) {
      sessions.forEach { session ->
        val isActive = session.id == activeSessionId
        Row(
          modifier = Modifier
            .background(if (isActive) TerminalBg else Color.Transparent)
            .clickable { engine.selectSession(session.id) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          if (session.isSshActive) {
            Surface(
              shape = RoundedCornerShape(3.dp),
              color = Color(0xFFD97706),
              modifier = Modifier.padding(end = 2.dp)
            ) {
              Text(
                text = "SSH",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          Text(
            text = session.title,
            style = TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              color = if (isActive) Color.White else Color.Gray,
              fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
          )
          if (sessions.size > 1) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close tab",
              tint = Color.Gray,
              modifier = Modifier
                .size(14.dp)
                .clickable { engine.closeSession(session.id) }
            )
          }
        }
      }

      IconButton(
        onClick = { engine.createNewSession() },
        modifier = Modifier.size(30.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "New Terminal Session",
          tint = Color.LightGray,
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Terminal fast action buttons
      IconButton(
        onClick = {
          val allText = activeSession?.lines?.joinToString("\n") { it.text } ?: ""
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("terminal", allText))
          Toast.makeText(context, "Terminal content copied", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.size(30.dp)
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray, modifier = Modifier.size(15.dp))
      }

      IconButton(
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
          inputCommand += item
        },
        modifier = Modifier.size(30.dp)
      ) {
        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color.LightGray, modifier = Modifier.size(15.dp))
      }

      IconButton(
        onClick = { engine.executeCommand("clear") },
        modifier = Modifier.size(30.dp)
      ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(15.dp))
      }
    }

    HorizontalDivider(thickness = 1.dp, color = Color(0xFF1E293B))

    // Terminal Output Log
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      items(activeSession?.lines ?: emptyList()) { line ->
        when {
          line.isPrompt -> {
            Text(
              text = line.text,
              style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = TerminalPromptUser,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
          line.isError -> {
            Text(
              text = line.text,
              style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFF87171)
              ),
              modifier = Modifier.padding(vertical = 1.dp)
            )
          }
          line.isSystem -> {
            Text(
              text = line.text,
              style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
              ),
              modifier = Modifier.padding(vertical = 1.dp)
            )
          }
          else -> {
            Text(
              text = line.text,
              style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = TerminalText
              ),
              modifier = Modifier.padding(vertical = 1.dp)
            )
          }
        }
      }
    }

    // Touchscreen Terminal Toolbar (Key helpers: Tab, Ctrl, Esc, |, ~, /, -, Up, Down)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .background(Color(0xFF131A26))
        .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val keys = listOf("Tab", "apt update", "apt upgrade", "|", "~", "/", "-", "$", "clear", "neofetch")
      keys.forEach { key ->
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFF1E293B),
          modifier = Modifier.clickable {
            when (key) {
              "Tab" -> inputCommand += "\t"
              "clear" -> engine.executeCommand("clear")
              "neofetch" -> {
                engine.executeCommand("neofetch")
                inputCommand = ""
              }
              "apt update" -> {
                engine.executeCommand("apt update")
                inputCommand = ""
              }
              "apt upgrade" -> {
                engine.executeCommand("apt upgrade")
                inputCommand = ""
              }
              else -> inputCommand += key
            }
          }
        ) {
          Text(
            text = key,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.LightGray),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // History navigation (Up / Down)
      IconButton(
        onClick = {
          if (activeSessionId.isNotEmpty()) {
            engine.getPreviousHistory(activeSessionId)?.let { inputCommand = it }
          }
        },
        modifier = Modifier.size(28.dp)
      ) {
        Icon(Icons.Default.ArrowUpward, contentDescription = "History Up", tint = Color.LightGray, modifier = Modifier.size(16.dp))
      }

      IconButton(
        onClick = {
          if (activeSessionId.isNotEmpty()) {
            engine.getNextHistory(activeSessionId)?.let { inputCommand = it }
          }
        },
        modifier = Modifier.size(28.dp)
      ) {
        Icon(Icons.Default.ArrowDownward, contentDescription = "History Down", tint = Color.LightGray, modifier = Modifier.size(16.dp))
      }
    }

    // Active Command Prompt Input
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0D121A))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val isSsh = activeSession?.isSshActive == true
      if (isSsh) {
        Surface(
          shape = RoundedCornerShape(3.dp),
          color = Color(0xFFD97706),
          modifier = Modifier.padding(end = 6.dp)
        ) {
          Text(
            text = "SSH",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }

      val isRoot = activeSession?.isRoot == true
      if (isRoot) {
        Surface(
          shape = RoundedCornerShape(3.dp),
          color = Color(0xFFDC2626),
          modifier = Modifier.padding(end = 4.dp)
        ) {
          Text(
            text = "ROOT",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }

      Text(
        text = if (isSsh) "${activeSession?.sshUser ?: "root"}@${activeSession?.sshHost ?: "vps"}:" else if (isRoot) "root@desktable:" else "user@desktable:",
        style = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = if (isSsh) Color(0xFFFBBF24) else if (isRoot) Color(0xFFF87171) else TerminalPromptUser,
          fontWeight = FontWeight.Bold
        )
      )
      Text(
        text = "${activeSession?.currentDir ?: "/home/user"}${if (isRoot) "# " else "$ "}",
        style = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = if (isSsh) Color(0xFF38BDF8) else TerminalPromptPath,
          fontWeight = FontWeight.Bold
        )
      )

      BasicTextField(
        value = inputCommand,
        onValueChange = { inputCommand = it },
        textStyle = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = Color.White
        ),
        cursorBrush = SolidColor(TerminalText),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
          onSend = {
            if (inputCommand.isNotEmpty()) {
              engine.executeCommand(inputCommand)
              inputCommand = ""
            }
          }
        ),
        modifier = Modifier
          .weight(1f)
          .focusRequester(focusRequester)
          .padding(start = 4.dp)
      )

      IconButton(
        onClick = {
          if (inputCommand.isNotEmpty()) {
            engine.executeCommand(inputCommand)
            inputCommand = ""
          }
        },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Send,
          contentDescription = "Execute",
          tint = TerminalText,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
