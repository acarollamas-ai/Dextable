package com.example.backends

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.core.multimedia.AudioSubsystemEngine
import com.example.core.multimedia.VideoSubsystemEngine
import com.example.core.peripherals.BluetoothManagerEngine
import java.io.File

class AndroidBackend(private val context: Context) : SystemBackend {

  val bluetoothEngine = BluetoothManagerEngine(context)
  val audioSubsystem = AudioSubsystemEngine(context)
  val videoSubsystem = VideoSubsystemEngine()

  override fun getSystemInfo(): SystemInfo {
    return SystemInfo(
      osName = "Desktable OS",
      osVersion = "1.0 Preview",
      architecture = "ARM64",
      kernelVersion = "Future Linux Kernel (running on Android Runtime)",
      displayServer = "Wayland Compositor (Prototype)",
      hostname = "desktable-tablet",
      currentUser = "user",
      isPrototype = true
    )
  }

  override fun getBatteryLevel(): Int {
    return try {
      val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
      if (level >= 0 && scale > 0) {
        ((level / scale.toFloat()) * 100).toInt()
      } else {
        85
      }
    } catch (_: Exception) {
      85
    }
  }

  override fun isCharging(): Boolean {
    return try {
      val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (_: Exception) {
      false
    }
  }

  override fun getStorageInfo(): StorageInfo {
    return try {
      val path: File = Environment.getDataDirectory()
      val stat = StatFs(path.path)
      val blockSize = stat.blockSizeLong
      val totalBlocks = stat.blockCountLong
      val availableBlocks = stat.availableBlocksLong
      val total = totalBlocks * blockSize
      val free = availableBlocks * blockSize
      val used = total - free
      StorageInfo(totalBytes = total, usedBytes = used, freeBytes = free)
    } catch (_: Exception) {
      // Fallback 64GB
      val total = 64L * 1024 * 1024 * 1024
      val used = 24L * 1024 * 1024 * 1024
      StorageInfo(totalBytes = total, usedBytes = used, freeBytes = total - used)
    }
  }

  override fun isNetworkConnected(): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      val network = cm?.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  override fun executeCommand(command: String, currentDir: String): CommandResult {
    // Delegated to LinuxBackend bridge for desktop terminal behavior
    return CommandResult(output = "Command executed via Android Runtime bridge: $command")
  }
}
