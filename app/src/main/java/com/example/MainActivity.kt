package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.DesktableOSApp
import com.example.ui.webos.WebOSScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var isWebOsMode by remember { mutableStateOf(true) }
      if (isWebOsMode) {
        WebOSScreen(onSwitchToNative = { isWebOsMode = false })
      } else {
        DesktableOSApp(onSwitchToWebOs = { isWebOsMode = true })
      }
    }
  }
}
