package com.example.core.applications

data class DesktableApp(
  val id: String,
  val name: String,
  val description: String,
  val iconEmoji: String,
  val category: String,
  val defaultWidth: Int = 680,
  val defaultHeight: Int = 480,
  val isPinnedDock: Boolean = true,
  val isDesktopShortcut: Boolean = true
)

object AppRegistry {

  val APPS = listOf(
    DesktableApp(
      id = "browser_store",
      name = "Loja de Navegadores",
      description = "Catálogo e Gerenciador de Navegadores Debian (ARM64)",
      iconEmoji = "🛍️",
      category = "Internet",
      defaultWidth = 840,
      defaultHeight = 580,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "chrome",
      name = "Google Chrome",
      description = "Google Chrome Desktop (Motor V8, Material You & Sincronização)",
      iconEmoji = "🌐",
      category = "Internet",
      defaultWidth = 780,
      defaultHeight = 540,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "firefox",
      name = "Mozilla Firefox",
      description = "Mozilla Firefox Quantum (Motor Gecko ESR 128 & Anti-Rastreamento)",
      iconEmoji = "🦊",
      category = "Internet",
      defaultWidth = 780,
      defaultHeight = 540,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "desktable_browser",
      name = "Desktable Browser",
      description = "Navegador Nativo do Sistema (Motor Web Real ARM64)",
      iconEmoji = "🧭",
      category = "Internet",
      defaultWidth = 760,
      defaultHeight = 540,
      isPinnedDock = false,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "files",
      name = "Files",
      description = "Gerenciador de Arquivos do Linux e Armazenamento",
      iconEmoji = "📁",
      category = "System",
      defaultWidth = 680,
      defaultHeight = 480,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "terminal",
      name = "Terminal",
      description = "Terminal Linux Real & Shell POSIX (/system/bin/sh)",
      iconEmoji = ">_",
      category = "Development",
      defaultWidth = 660,
      defaultHeight = 460,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "settings",
      name = "Settings",
      description = "Configuração do Sistema, Arquitetura e Monitores",
      iconEmoji = "⚙",
      category = "System",
      defaultWidth = 760,
      defaultHeight = 540,
      isPinnedDock = true,
      isDesktopShortcut = true
    ),
    DesktableApp(
      id = "appmanager",
      name = "App Manager",
      description = "Gerenciador de Pacotes Linux ARM64 (APT / DPKG)",
      iconEmoji = "📦",
      category = "System",
      defaultWidth = 740,
      defaultHeight = 520,
      isPinnedDock = true,
      isDesktopShortcut = false
    ),
    DesktableApp(
      id = "editor",
      name = "Text Editor",
      description = "Editor de Texto e Scripts Linux",
      iconEmoji = "📝",
      category = "Accessories",
      defaultWidth = 600,
      defaultHeight = 440,
      isPinnedDock = false,
      isDesktopShortcut = false
    ),
    DesktableApp(
      id = "calculator",
      name = "Calculator",
      description = "Calculadora Desktop Científica",
      iconEmoji = "🧮",
      category = "Accessories",
      defaultWidth = 340,
      defaultHeight = 460,
      isPinnedDock = false,
      isDesktopShortcut = false
    )
  )

  fun getApp(id: String): DesktableApp? = APPS.find { it.id == id }
}
