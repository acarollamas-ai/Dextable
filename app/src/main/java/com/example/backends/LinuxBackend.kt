package com.example.backends

import com.example.core.architecture.SystemArchitecture
import com.example.core.terminal.RealLinuxShellEngine
import kotlinx.coroutines.runBlocking

class LinuxBackend(
  private val androidBackend: AndroidBackend? = null,
  val shellEngine: RealLinuxShellEngine? = null,
  var fsBridge: LinuxFilesystemBridge? = null
) : SystemBackend {

  interface LinuxFilesystemBridge {
    fun listFiles(path: String): List<String>
    fun createDirectory(path: String, name: String): Boolean
    fun createFile(path: String, name: String, content: String = ""): Boolean
    fun readFile(path: String): String?
    fun delete(path: String): Boolean
  }

  override fun getSystemInfo(): SystemInfo {
    return SystemInfo(
      osName = "Desktable OS",
      osVersion = "1.0 Preview",
      architecture = if (SystemArchitecture.isNativeArm64()) "aarch64 (ARM64 Native)" else "aarch64 (ARM64 Compatible)",
      kernelVersion = SystemArchitecture.getKernelVersion(),
      displayServer = "Wayland Compositor (Android Prototype Compositor)",
      hostname = "desktable",
      currentUser = "user",
      isPrototype = true
    )
  }

  override fun getBatteryLevel(): Int = androidBackend?.getBatteryLevel() ?: 90
  override fun isCharging(): Boolean = androidBackend?.isCharging() ?: false
  override fun getStorageInfo(): StorageInfo = androidBackend?.getStorageInfo() ?: StorageInfo(
    totalBytes = 128L * 1024 * 1024 * 1024,
    usedBytes = 36L * 1024 * 1024 * 1024,
    freeBytes = 92L * 1024 * 1024 * 1024
  )
  override fun isNetworkConnected(): Boolean = androidBackend?.isNetworkConnected() ?: true

  override fun executeCommand(command: String, currentDir: String): CommandResult {
    val engine = shellEngine
    return if (engine != null) {
      runBlocking {
        engine.execute(command, currentDir)
      }
    } else {
      CommandResult(
        output = "[Desktable OS Shell Engine starting...]",
        exitCode = 0
      )
    }
  }

  suspend fun executeCommandAsync(command: String, currentDir: String): CommandResult {
    val engine = shellEngine ?: return CommandResult(output = "[Shell Engine not initialized]", exitCode = 1)
    return engine.execute(command, currentDir)
  }
}
