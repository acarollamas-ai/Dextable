package com.example.core.architecture

import android.os.Build
import java.io.File

/**
 * Desktable OS Architectural Model.
 *
 * Clearly defines the transition from:
 * - Current Prototype: Android APK / SurfaceFlinger runtime / UX & Window Manager testbed
 * - Final System Target: Linux ARM64 (aarch64) / Native Kernel / Wayland Compositor / Rootfs / Real Linux Applications
 */
enum class SystemPhase {
  PROTOTYPE_ANDROID_RUNTIME,
  FINAL_TARGET_LINUX_ARM64
}

data class ArchitectureLayer(
  val order: Int,
  val name: String,
  val targetComponent: String,
  val prototypeComponent: String,
  val status: String,
  val description: String
)

object SystemArchitecture {

  val currentPhase: SystemPhase = SystemPhase.PROTOTYPE_ANDROID_RUNTIME

  fun getSupportedAbis(): List<String> {
    return Build.SUPPORTED_ABIS.toList()
  }

  fun isNativeArm64(): Boolean {
    return Build.SUPPORTED_ABIS.any { it.contains("arm64", ignoreCase = true) || it.contains("aarch64", ignoreCase = true) }
  }

  fun getKernelVersion(): String {
    return try {
      val procVersion = File("/proc/version")
      if (procVersion.exists()) {
        procVersion.readText().trim()
      } else {
        System.getProperty("os.version") ?: "Linux 6.8.0-arm64-generic"
      }
    } catch (_: Exception) {
      System.getProperty("os.version") ?: "Linux 6.8.0-arm64-generic"
    }
  }

  fun getCpuInfo(): String {
    return try {
      val cpuInfo = File("/proc/cpuinfo")
      if (cpuInfo.exists()) {
        val lines = cpuInfo.readLines().take(12)
        lines.joinToString("\n")
      } else {
        "Processor: ARM64 (aarch64)\nFeatures: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp\nHardware: ARM64 Tablet Reference Platform"
      }
    } catch (_: Exception) {
      "ARM64 Architecture"
    }
  }

  val LAYERS = listOf(
    ArchitectureLayer(
      order = 1,
      name = "Hardware ARM64",
      targetComponent = "ARM64 SoC (Cortex-A78 / X1 / X2 / Dimensity / Snapdragon) + Display 1920x1080+",
      prototypeComponent = "Dispositivo Host Android ARM64",
      status = "Ativo (Hardware Real)",
      description = "Processador 64-bit ARM64 com aceleração gráfica, touchscreen, teclado e mouse USB/Bluetooth."
    ),
    ArchitectureLayer(
      order = 2,
      name = "Bootloader & Kernel",
      targetComponent = "U-Boot / EDK2 UEFI -> Linux Kernel 6.8+ (arm64-generic) + Device Trees",
      prototypeComponent = "Kernel Linux do Android (${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT})",
      status = "Ativo sobre Kernel Linux Real",
      description = "O Android executa sobre o Linux Kernel real. No sistema final, o kernel inicializa direto do bootloader para o userspace GNU/Linux."
    ),
    ArchitectureLayer(
      order = 3,
      name = "Linux Userspace",
      targetComponent = "Debian/Ubuntu/Arch aarch64 Rootfs (/bin, /sbin, /usr, /lib, /etc, /home)",
      prototypeComponent = "Android Userspace (/system/bin/sh, toybox, bionic libc)",
      status = "Em Transição / Shell Real",
      description = "Execução de shell real (/system/bin/sh), processos POSIX, variáveis de ambiente e gerenciamento de arquivos."
    ),
    ArchitectureLayer(
      order = 4,
      name = "System Services",
      targetComponent = "systemd, BlueZ (Bluetooth), PipeWire (Áudio/Vídeo), APT / DPKG Package Manager",
      prototypeComponent = "Desktable Package Engine + Android Subsystems Bridge",
      status = "Camada de Compatibilidade",
      description = "Gerenciamento de pacotes ARM64 (apt/dpkg), controle de áudio, conectividade e energia."
    ),
    ArchitectureLayer(
      order = 5,
      name = "Display Server",
      targetComponent = "Wayland Compositor Nativo (wlroots / Weston)",
      prototypeComponent = "SurfaceFlinger / Jetpack Compose Window Compositor",
      status = "Protótipo Gráfico",
      description = "Servidor de exibição e compositor multi-janela para aceleração por hardware sem depender de VNC."
    ),
    ArchitectureLayer(
      order = 6,
      name = "Desktable Desktop",
      targetComponent = "Desktable Desktop Environment (Dock, Top Bar, Window Manager, Workspaces)",
      prototypeComponent = "Desktable Desktop UI (Compose Engine)",
      status = "Interface Operacional Ativa",
      description = "Camada gráfica desktop para tablets: gerenciamento de janelas livres, atalhos, dock dinâmica e bandeja do sistema."
    ),
    ArchitectureLayer(
      order = 7,
      name = "Aplicações",
      targetComponent = "Desktable Browser, Firefox aarch64, Chromium aarch64, Terminal, Editor, Files",
      prototypeComponent = "Aplicações Nativas Desktable + Portais de Aplicativos Oficiais ARM64",
      status = "Ativo com Navegador Real e Terminal Real",
      description = "Ecossistema de aplicativos desktop separados do núcleo do sistema operacional."
    )
  )
}
