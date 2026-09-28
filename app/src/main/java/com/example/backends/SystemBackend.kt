package com.example.backends

data class SystemInfo(
  val osName: String = "Desktable OS",
  val osVersion: String = "1.0 Preview",
  val architecture: String = "ARM64",
  val kernelVersion: String = "Linux 6.8.0-arm64-generic (Prototype)",
  val displayServer: String = "Wayland (Future ARM64 Native)",
  val hostname: String = "desktable",
  val currentUser: String = "user",
  val isPrototype: Boolean = true
)

data class StorageInfo(
  val totalBytes: Long,
  val usedBytes: Long,
  val freeBytes: Long
) {
  val totalGb: Double get() = totalBytes / (1024.0 * 1024.0 * 1024.0)
  val usedGb: Double get() = usedBytes / (1024.0 * 1024.0 * 1024.0)
  val freeGb: Double get() = freeBytes / (1024.0 * 1024.0 * 1024.0)
  val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f
}

data class CommandResult(
  val output: String,
  val exitCode: Int = 0,
  val newWorkingDir: String? = null
)

interface SystemBackend {
  fun getSystemInfo(): SystemInfo
  fun getBatteryLevel(): Int
  fun isCharging(): Boolean
  fun getStorageInfo(): StorageInfo
  fun isNetworkConnected(): Boolean
  fun executeCommand(command: String, currentDir: String): CommandResult
}
