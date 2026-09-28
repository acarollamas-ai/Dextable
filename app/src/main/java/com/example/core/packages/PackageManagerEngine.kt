package com.example.core.packages

import android.content.Context
import com.example.core.architecture.SystemArchitecture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LinuxPackage(
  val id: String,
  val name: String,
  val version: String,
  val architecture: String, // "arm64" or "all"
  val sizeBytes: Long,
  val isInstalled: Boolean,
  val isOfficialArm64Build: Boolean,
  val repository: String,
  val category: String,
  val description: String,
  val dependencies: List<String> = emptyList(),
  val updateAvailable: Boolean = false,
  val newVersion: String? = null,
  val maintainerNote: String? = null
) {
  val sizeFormatted: String
    get() = when {
      sizeBytes > 1024 * 1024 -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
      sizeBytes > 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
      else -> "$sizeBytes bytes"
    }
}

data class PackageOperationResult(
  val success: Boolean,
  val output: String,
  val exitCode: Int = 0
)

class PackageManagerEngine(private val context: Context) {

  private val _packages = MutableStateFlow<List<LinuxPackage>>(emptyList())
  val packages: StateFlow<List<LinuxPackage>> = _packages.asStateFlow()

  private val _repositories = MutableStateFlow(
    listOf(
      "deb [arch=arm64] https://deb.desktable.org/os stable main desktop",
      "deb [arch=arm64] https://deb.debian.org/debian bookworm main contrib non-free",
      "deb [arch=arm64] http://ports.ubuntu.com/ubuntu-ports noble main universe"
    )
  )
  val repositories: StateFlow<List<String>> = _repositories.asStateFlow()

  private val _lastUpdateTimestamp = MutableStateFlow<Long?>(null)
  val lastUpdateTimestamp: StateFlow<Long?> = _lastUpdateTimestamp.asStateFlow()

  init {
    loadInitialPackageDatabase()
  }

  private fun loadInitialPackageDatabase() {
    _packages.value = listOf(
      LinuxPackage(
        id = "desktable-desktop",
        name = "desktable-desktop",
        version = "1.0.0-preview",
        architecture = "arm64",
        sizeBytes = 24 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.desktable.org/os",
        category = "x11-wm",
        description = "Desktable Desktop Environment (Wayland Compositor & Shell)",
        dependencies = listOf("wayland", "libinput", "mesa", "pipewire")
      ),
      LinuxPackage(
        id = "desktable-browser",
        name = "desktable-browser",
        version = "1.0.2",
        architecture = "arm64",
        sizeBytes = 38 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.desktable.org/os",
        category = "web",
        description = "Desktable OS Native Web Browser Engine (ARM64 Optimized)",
        dependencies = listOf("libwebkit2gtk-4.0-37", "glib-networking")
      ),
      LinuxPackage(
        id = "firefox-esr",
        name = "firefox-esr",
        version = "128.3.0esr-1",
        architecture = "arm64",
        sizeBytes = 64 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "web",
        description = "Mozilla Firefox Extended Support Release (Official Debian/ARM64 build)",
        dependencies = listOf("libgtk-3-0", "libasound2", "libdbus-glib-1-2", "libfontconfig1"),
        maintainerNote = "Compilado oficialmente para aarch64 com suporte a Wayland nativo (MOZ_ENABLE_WAYLAND=1)."
      ),
      LinuxPackage(
        id = "chromium",
        name = "chromium",
        version = "128.0.6613.119-1",
        architecture = "arm64",
        sizeBytes = 98 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "web",
        description = "Chromium Web Browser (Official Open-Source ARM64 Desktop Browser)",
        dependencies = listOf("libnss3", "libxss1", "libasound2", "libwayland-client0"),
        maintainerNote = "Suporte completo a aceleração de GPU em ARM64 com backend Ozone/Wayland."
      ),
      LinuxPackage(
        id = "google-chrome-stable",
        name = "google-chrome-stable",
        version = "128.0.6613.137-1",
        architecture = "arm64",
        sizeBytes = 88 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "dl.google.com/linux/chrome/deb stable [arch=arm64]",
        category = "web",
        description = "Google Chrome Desktop (Official Debian/ARM64 Build with V8 Engine & Material You)",
        dependencies = listOf("libnss3", "libasound2", "libgtk-3-0", "libx11-6"),
        maintainerNote = "Versão oficial Google Chrome para Linux ARM64 (aarch64) com aceleração gráfica Ozone/Wayland."
      ),
      LinuxPackage(
        id = "brave-browser",
        name = "brave-browser",
        version = "1.69.160",
        architecture = "arm64",
        sizeBytes = 92 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "brave-browser-apt-release.s3.brave.com stable [arch=arm64]",
        category = "web",
        description = "Brave Browser Desktop (Privacidade Nativa com Brave Shields e Bloqueador de Anúncios)",
        dependencies = listOf("libnss3", "libgtk-3-0", "libasound2"),
        maintainerNote = "Bloqueador de anúncios e telemetria integrado de fábrica para Debian ARM64."
      ),
      LinuxPackage(
        id = "microsoft-edge-stable",
        name = "microsoft-edge-stable",
        version = "128.0.2733.67-1",
        architecture = "arm64",
        sizeBytes = 96 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "packages.microsoft.com/repos/edge stable [arch=arm64]",
        category = "web",
        description = "Microsoft Edge Desktop para Linux (Abas Verticais e Integração com Copilot)",
        dependencies = listOf("libnss3", "libgtk-3-0", "libasound2"),
        maintainerNote = "Pacote Debian oficial corporativo para plataformas aarch64."
      ),
      LinuxPackage(
        id = "torbrowser-launcher",
        name = "torbrowser-launcher",
        version = "13.5.3-1",
        architecture = "arm64",
        sizeBytes = 82 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm-backports [arch=arm64]",
        category = "web",
        description = "Tor Browser Launcher & Navegador Anônimo em Camadas Criptografadas",
        dependencies = listOf("tor", "libgtk-3-0", "python3"),
        maintainerNote = "Auditado pelo Debian Security Team para navegação anônima sem rastreamento."
      ),
      LinuxPackage(
        id = "epiphany-browser",
        name = "epiphany-browser",
        version = "46.0-1",
        architecture = "arm64",
        sizeBytes = 28 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm [arch=arm64]",
        category = "web",
        description = "GNOME Web / Midori (Navegador Ultraleve com Motor WebKitGTK)",
        dependencies = listOf("libwebkit2gtk-4.1-0", "libgtk-4-1"),
        maintainerNote = "Navegador ultraleve nativo do ecossistema GNOME para consumo reduzido de RAM."
      ),
      LinuxPackage(
        id = "python3",
        name = "python3",
        version = "3.12.3-1",
        architecture = "arm64",
        sizeBytes = 18 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "python",
        description = "Interactive high-level object-oriented language (ARM64 aarch64)",
        dependencies = listOf("python3-minimal", "libpython3-stdlib")
      ),
      LinuxPackage(
        id = "git",
        name = "git",
        version = "2.45.2-1",
        architecture = "arm64",
        sizeBytes = 14 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "devel",
        description = "Fast, scalable, distributed revision control system (ARM64)",
        dependencies = listOf("libc6", "libcurl4", "libzstd1")
      ),
      LinuxPackage(
        id = "openssh-client",
        name = "openssh-client",
        version = "9.7p1-1",
        architecture = "arm64",
        sizeBytes = 6 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "Secure shell (SSH) client for remote login and terminal execution",
        dependencies = listOf("libc6", "libssl3")
      ),
      LinuxPackage(
        id = "htop",
        name = "htop",
        version = "3.3.0-1",
        architecture = "arm64",
        sizeBytes = 1 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "Interactive process viewer and system monitor for Linux",
        dependencies = listOf("libncursesw6")
      ),
      LinuxPackage(
        id = "fastfetch",
        name = "fastfetch",
        version = "2.15.0-1",
        architecture = "arm64",
        sizeBytes = 2 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.desktable.org/os",
        category = "utils",
        description = "Fast system information display utility for Desktable OS Linux ARM64",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "curl",
        name = "curl",
        version = "8.5.0-2",
        architecture = "arm64",
        sizeBytes = 3 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "Command line tool for transferring data with URL syntax (HTTP/HTTPS/FTP)",
        dependencies = listOf("libc6", "libcurl4", "zlib1g")
      ),
      LinuxPackage(
        id = "wget",
        name = "wget",
        version = "1.24.5-1",
        architecture = "arm64",
        sizeBytes = 2 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "Retrieves files from the web via HTTP, HTTPS, and FTP",
        dependencies = listOf("libc6", "libssl3")
      ),
      LinuxPackage(
        id = "nano",
        name = "nano",
        version = "8.0-1",
        architecture = "arm64",
        sizeBytes = 1 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "editors",
        description = "Small, friendly text editor inspired by Pico",
        dependencies = listOf("libc6", "libncursesw6")
      ),
      LinuxPackage(
        id = "vim",
        name = "vim",
        version = "9.1.0496-1",
        architecture = "arm64",
        sizeBytes = 4 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "editors",
        description = "Vi IMproved - enhanced vi text editor",
        dependencies = listOf("libc6", "libncursesw6")
      ),
      LinuxPackage(
        id = "tree",
        name = "tree",
        version = "2.1.1-1",
        architecture = "arm64",
        sizeBytes = 50 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "Displays directory tree in color",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "jq",
        name = "jq",
        version = "1.7.1-3",
        architecture = "arm64",
        sizeBytes = 450 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "Lightweight and flexible command-line JSON processor",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "nodejs",
        name = "nodejs",
        version = "20.15.1-1nodesource1",
        architecture = "arm64",
        sizeBytes = 32 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.nodesource.com/node_20.x bookworm",
        category = "javascript",
        description = "Evented I/O for V8 JavaScript (Debian ARM64 official binary)",
        dependencies = listOf("libc6", "libssl3", "zlib1g")
      ),
      LinuxPackage(
        id = "npm",
        name = "npm",
        version = "10.8.2-1",
        architecture = "all",
        sizeBytes = 8 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.nodesource.com/node_20.x bookworm",
        category = "javascript",
        description = "Package manager for JavaScript and Node.js runtime",
        dependencies = listOf("nodejs")
      ),
      LinuxPackage(
        id = "net-tools",
        name = "net-tools",
        version = "2.10-0.1",
        architecture = "arm64",
        sizeBytes = 250 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "NET-3 networking toolkit (ifconfig, netstat, route, arp)",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "iproute2",
        name = "iproute2",
        version = "6.9.0-1",
        architecture = "arm64",
        sizeBytes = 1200 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "Networking and traffic control tools (ip, ss, bridge)",
        dependencies = listOf("libc6", "libelf1")
      ),
      LinuxPackage(
        id = "tmux",
        name = "tmux",
        version = "3.4-1",
        architecture = "arm64",
        sizeBytes = 600 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "Terminal multiplexer with split window and session support",
        dependencies = listOf("libc6", "libevent-2.1-7", "libncursesw6")
      ),
      LinuxPackage(
        id = "zsh",
        name = "zsh",
        version = "5.9-5",
        architecture = "arm64",
        sizeBytes = 4 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "shells",
        description = "Advanced programmable command interpreter (shell)",
        dependencies = listOf("libc6", "libcap2", "libncursesw6")
      ),
      LinuxPackage(
        id = "neofetch",
        name = "neofetch",
        version = "7.1.0-4",
        architecture = "all",
        sizeBytes = 120 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "Fast, highly customizable system information tool",
        dependencies = listOf("bash")
      ),
      LinuxPackage(
        id = "rsync",
        name = "rsync",
        version = "3.3.0-1",
        architecture = "arm64",
        sizeBytes = 550 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "net",
        description = "Fast, versatile, remote (and local) file-copying tool",
        dependencies = listOf("libc6", "libssl3", "libzstd1")
      ),
      LinuxPackage(
        id = "tar",
        name = "tar",
        version = "1.35+dfsg-3",
        architecture = "arm64",
        sizeBytes = 850 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "GNU version of the tar archiving utility",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "gzip",
        name = "gzip",
        version = "1.12-1.1",
        architecture = "arm64",
        sizeBytes = 180 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "GNU compression utilities",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "unzip",
        name = "unzip",
        version = "6.0-28",
        architecture = "arm64",
        sizeBytes = 190 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "utils",
        description = "De-archiver for .zip files",
        dependencies = listOf("libc6")
      ),
      LinuxPackage(
        id = "sqlite3",
        name = "sqlite3",
        version = "3.45.3-1",
        architecture = "arm64",
        sizeBytes = 1400 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "database",
        description = "Command line interface for SQLite 3",
        dependencies = listOf("libc6", "libreadline8")
      ),
      LinuxPackage(
        id = "docker.io",
        name = "docker.io",
        version = "26.1.4+dfsg1-1",
        architecture = "arm64",
        sizeBytes = 85 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "admin",
        description = "Linux container runtime engine (official ARM64 build)",
        dependencies = listOf("containerd", "iptables", "libseccomp2")
      ),
      LinuxPackage(
        id = "nginx",
        name = "nginx",
        version = "1.26.0-1~bookworm",
        architecture = "arm64",
        sizeBytes = 2 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "httpd",
        description = "High performance web server and reverse proxy for ARM64",
        dependencies = listOf("libc6", "libssl3", "zlib1g")
      ),
      LinuxPackage(
        id = "python3-pip",
        name = "python3-pip",
        version = "24.0+dfsg-1",
        architecture = "all",
        sizeBytes = 3 * 1024 * 1024,
        isInstalled = true,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "python",
        description = "Python package installer (pip for Python 3)",
        dependencies = listOf("python3")
      ),
      LinuxPackage(
        id = "build-essential",
        name = "build-essential",
        version = "12.10",
        architecture = "arm64",
        sizeBytes = 45 * 1024 * 1024,
        isInstalled = false,
        isOfficialArm64Build = true,
        repository = "deb.debian.org/debian bookworm",
        category = "devel",
        description = "Informational list of build-essential packages (gcc, g++, make, libc6-dev for aarch64)",
        dependencies = listOf("gcc", "g++", "make", "dpkg-dev")
      )
    )
  }

  fun executeAptCommand(args: List<String>): PackageOperationResult {
    if (args.isEmpty()) {
      return PackageOperationResult(
        success = false,
        output = """
          apt 2.6.1 (arm64) - Interface de Linha de Comando de Pacotes do Debian GNU/Linux
          Uso: apt [opções] comando

          Comandos de manipulação de pacotes mais usados:
            update           - atualiza a lista de novos pacotes dos repositórios
            upgrade          - atualiza todos os pacotes instalados no sistema
            install <pacote> - instala novos pacotes oficiais ARM64 (ex: git, curl, htop)
            remove <pacote>  - remove pacotes instalados
            purge <pacote>   - remove pacotes e seus arquivos de configuração
            autoremove       - remove pacotes órfãos e dependências não utilizadas
            search <termo>   - procura por termos na descrição dos pacotes
            show <pacote>    - exibe detalhes e dependências do pacote
            list             - lista pacotes (--installed, --upgradable)
            clean / autoclean- limpa arquivos de pacotes baixados do cache
            edit-sources     - exibe os repositórios em /etc/apt/sources.list

          Opções comuns suportadas:
            -y, --yes        - assume resposta 'Sim' para todas as perguntas
            -q, --quiet      - modo silencioso
            -f, --fix-broken - tenta corrigir dependências quebradas
        """.trimIndent(),
        exitCode = 1
      )
    }

    // Separate flags from words
    val flags = args.filter { it.startsWith("-") }
    val words = args.filterNot { it.startsWith("-") }

    if (words.isEmpty()) {
      if (flags.contains("-h") || flags.contains("--help")) {
        return executeAptCommand(emptyList())
      }
      if (flags.contains("-v") || flags.contains("--version")) {
        return PackageOperationResult(true, "apt 2.6.1 (arm64)\nDebian APT Package Engine for Desktable OS (aarch64)", 0)
      }
      return PackageOperationResult(false, "E: Nenhuma operação informada para o apt.", 1)
    }

    val subcmd = words[0].lowercase()
    val subWords = words.drop(1)

    return when (subcmd) {
      "update" -> updateRepositories()
      "upgrade", "dist-upgrade", "full-upgrade" -> upgradePackages()
      "install" -> {
        if (subWords.isEmpty()) {
          PackageOperationResult(false, "apt install: informe o nome do pacote para instalar. Ex: apt install curl git", 1)
        } else if (subWords.any { it.equals("upgrade", ignoreCase = true) }) {
          // Handle 'apt install upgrade' / 'sudo apt install upgrade'
          val result = upgradePackages()
          PackageOperationResult(
            success = result.success,
            output = "Nota: O comando 'apt install upgrade' foi interpretado como 'apt upgrade' do sistema Debian/ARM64.\n\n" + result.output,
            exitCode = result.exitCode
          )
        } else if (subWords.any { it.equals("update", ignoreCase = true) }) {
          // Handle 'apt install update'
          val result = updateRepositories()
          PackageOperationResult(
            success = result.success,
            output = "Nota: O comando 'apt install update' foi interpretado como 'apt update' dos repositórios.\n\n" + result.output,
            exitCode = result.exitCode
          )
        } else {
          installMultiplePackages(subWords)
        }
      }
      "remove", "purge" -> {
        if (subWords.isEmpty()) {
          PackageOperationResult(false, "apt remove: informe o nome do pacote para remover.", 1)
        } else {
          removeMultiplePackages(subWords)
        }
      }
      "autoremove" -> {
        PackageOperationResult(
          true,
          """
            Lendo listas de pacotes... Pronto
            Construindo árvore de dependências... Pronto
            Lendo informação de estado... Pronto
            0 pacotes atualizados, 0 novos instalados, 0 a serem removidos e 0 não atualizados.
            Nenhum pacote órfão encontrado para remoção.
          """.trimIndent(),
          0
        )
      }
      "clean", "autoclean" -> {
        PackageOperationResult(
          true,
          "Limpeza de cache APT concluída: /var/cache/apt/archives limpo com sucesso (0 bytes liberados).",
          0
        )
      }
      "search" -> {
        if (subWords.isEmpty()) {
          PackageOperationResult(false, "apt search: informe o termo de pesquisa.", 1)
        } else {
          searchPackages(subWords.joinToString(" "))
        }
      }
      "show" -> {
        if (subWords.isEmpty()) {
          PackageOperationResult(false, "apt show: informe o nome do pacote.", 1)
        } else {
          showPackage(subWords[0])
        }
      }
      "list" -> {
        if (flags.contains("--installed") || subWords.contains("--installed")) {
          listInstalled()
        } else if (flags.contains("--upgradable") || subWords.contains("--upgradable")) {
          listUpgradable()
        } else {
          listAll()
        }
      }
      "edit-sources", "sources" -> {
        PackageOperationResult(
          true,
          """
            # /etc/apt/sources.list
            deb https://deb.debian.org/debian bookworm main contrib non-free non-free-firmware
            deb https://deb.debian.org/debian-security bookworm-security main contrib non-free non-free-firmware
            deb https://deb.debian.org/debian bookworm-updates main contrib non-free non-free-firmware
            deb [arch=arm64] https://deb.desktable.org/os stable main desktop
          """.trimIndent(),
          0
        )
      }
      "cache" -> {
        handleAptCacheCommand(subWords)
      }
      else -> {
        PackageOperationResult(false, "E: Operação '$subcmd' inválida. Consulte 'apt --help'.", 100)
      }
    }
  }

  fun handleAptCacheCommand(args: List<String>): PackageOperationResult {
    if (args.isEmpty()) {
      return PackageOperationResult(
        false,
        "apt-cache: informe uma suboperação (search, show, depends, pkgnames). Ex: apt-cache search python",
        1
      )
    }
    val sub = args[0].lowercase()
    val param = args.drop(1).joinToString(" ")
    return when (sub) {
      "search" -> searchPackages(param)
      "show" -> showPackage(args.getOrNull(1) ?: "")
      "depends" -> {
        val pkg = _packages.value.find { it.id.equals(args.getOrNull(1), ignoreCase = true) }
        if (pkg != null) {
          PackageOperationResult(
            true,
            "${pkg.name}\n  Depends: ${pkg.dependencies.joinToString("\n  Depends: ").ifEmpty { "(none)" }}",
            0
          )
        } else {
          PackageOperationResult(false, "E: Pacote '${args.getOrNull(1)}' não encontrado.", 1)
        }
      }
      "pkgnames" -> {
        val names = _packages.value.joinToString("\n") { it.name }
        PackageOperationResult(true, names, 0)
      }
      else -> PackageOperationResult(false, "apt-cache: comando '$sub' não reconhecido.", 1)
    }
  }

  fun installMultiplePackages(packageNames: List<String>): PackageOperationResult {
    val results = mutableListOf<String>()
    var overallSuccess = true
    var lastExit = 0

    for (pkgName in packageNames) {
      val res = installPackage(pkgName)
      results.add(res.output)
      if (!res.success) {
        overallSuccess = false
        lastExit = res.exitCode
      }
    }

    return PackageOperationResult(
      success = overallSuccess,
      output = results.joinToString("\n\n"),
      exitCode = lastExit
    )
  }

  fun removeMultiplePackages(packageNames: List<String>): PackageOperationResult {
    val results = mutableListOf<String>()
    var overallSuccess = true
    var lastExit = 0

    for (pkgName in packageNames) {
      val res = removePackage(pkgName)
      results.add(res.output)
      if (!res.success) {
        overallSuccess = false
        lastExit = res.exitCode
      }
    }

    return PackageOperationResult(
      success = overallSuccess,
      output = results.joinToString("\n\n"),
      exitCode = lastExit
    )
  }

  fun updateRepositories(): PackageOperationResult {
    _lastUpdateTimestamp.value = System.currentTimeMillis()
    val repoOutputs = _repositories.value.mapIndexed { idx, repo ->
      val domain = repo.substringAfter("https://").substringAfter("http://").substringBefore(" ")
      "Hit:${idx + 1} $repo InRelease\nGet:${idx + 1} $domain arm64 Packages [1,248 kB]"
    }.joinToString("\n")

    val summary = """
      $repoOutputs
      Reading package lists... Done
      Building dependency tree... Done
      All packages are up to date for architecture aarch64 (ARM64).
    """.trimIndent()

    return PackageOperationResult(true, summary, 0)
  }

  fun upgradePackages(): PackageOperationResult {
    val updatable = _packages.value.filter { it.isInstalled && it.updateAvailable }
    if (updatable.isEmpty()) {
      return PackageOperationResult(
        true,
        """
          Reading package lists... Done
          Building dependency tree... Done
          0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.
          Sistema Desktable OS está totalmente atualizado na versão mais recente.
        """.trimIndent(),
        0
      )
    }

    _packages.value = _packages.value.map { pkg ->
      if (pkg.isInstalled && pkg.updateAvailable) {
        pkg.copy(
          version = pkg.newVersion ?: pkg.version,
          updateAvailable = false,
          newVersion = null
        )
      } else pkg
    }

    return PackageOperationResult(
      true,
      """
        Reading package lists... Done
        Building dependency tree... Done
        The following packages were upgraded:
          ${updatable.joinToString(", ") { it.name }}
        ${updatable.size} upgraded, 0 newly installed, 0 to remove.
      """.trimIndent(),
      0
    )
  }

  fun installPackage(packageName: String): PackageOperationResult {
    val target = _packages.value.find { it.id.equals(packageName, ignoreCase = true) || it.name.equals(packageName, ignoreCase = true) }

    if (target == null) {
      return PackageOperationResult(
        false,
        """
          Reading package lists... Done
          Building dependency tree... Done
          E: Impossível encontrar o pacote '$packageName' nos repositórios ARM64 configurados.
          Dica: use 'apt search $packageName' para procurar pacotes disponíveis.
        """.trimIndent(),
        100
      )
    }

    // Strict validation: Don't pretend x86_64 packages work on ARM64!
    if (!target.isOfficialArm64Build) {
      val reason = target.maintainerNote ?: "Este pacote não possui build binário oficial compatível com ARM64."
      return PackageOperationResult(
        false,
        """
          Reading package lists... Done
          Building dependency tree... Done
          E: Pacote '${target.name}' incompatível com a arquitetura do sistema (aarch64/ARM64).
          [Arquitetura Desktable OS ARM64 Policy]:
          $reason
        """.trimIndent(),
        1
      )
    }

    if (target.isInstalled) {
      return PackageOperationResult(
        true,
        """
          Reading package lists... Done
          Building dependency tree... Done
          ${target.name} já está na versão mais recente (${target.version}).
          0 atualizados, 0 novos instalados, 0 a serem removidos.
        """.trimIndent(),
        0
      )
    }

    // Perform actual package installation in local DB
    _packages.value = _packages.value.map {
      if (it.id == target.id) it.copy(isInstalled = true) else it
    }

    return PackageOperationResult(
      true,
      """
        Reading package lists... Done
        Building dependency tree... Done
        The following NEW packages will be installed:
          ${target.name} (v${target.version}) [${target.architecture}]
        Get:1 ${target.repository} ${target.name} ${target.version} [${target.sizeFormatted}]
        Fetched ${target.sizeFormatted} in 1s
        Selecting previously unselected package ${target.name}.
        (Reading database ... 28,490 files and directories currently installed.)
        Preparing to unpack ${target.name}_${target.version}_${target.architecture}.deb ...
        Unpacking ${target.name} (${target.version}) ...
        Setting up ${target.name} (${target.version}) ...
        Processing triggers for desktable-desktop-launcher (1.0) ...
        Instalação concluída com sucesso. O aplicativo está disponível no Terminal e no Launcher.
      """.trimIndent(),
      0
    )
  }

  fun removePackage(packageName: String): PackageOperationResult {
    val target = _packages.value.find { it.id.equals(packageName, ignoreCase = true) || it.name.equals(packageName, ignoreCase = true) }

    if (target == null) {
      return PackageOperationResult(false, "E: Pacote '$packageName' não encontrado.", 1)
    }

    if (!target.isInstalled) {
      return PackageOperationResult(false, "O pacote '${target.name}' não está instalado.", 1)
    }

    if (target.id == "desktable-desktop") {
      return PackageOperationResult(false, "E: Não é permitido remover 'desktable-desktop' pois é o ambiente gráfico essencial do sistema.", 1)
    }

    _packages.value = _packages.value.map {
      if (it.id == target.id) it.copy(isInstalled = false) else it
    }

    return PackageOperationResult(
      true,
      """
        Reading package lists... Done
        Building dependency tree... Done
        The following packages will be REMOVED:
          ${target.name}*
        0 upgraded, 0 newly installed, 1 to remove.
        Removing ${target.name} (${target.version}) ...
        Processing triggers for desktable-desktop-launcher (1.0) ...
        Pacote removido com sucesso.
      """.trimIndent(),
      0
    )
  }

  fun searchPackages(query: String): PackageOperationResult {
    val matches = _packages.value.filter {
      it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }

    if (matches.isEmpty()) {
      return PackageOperationResult(true, "Nenhum pacote encontrado para o termo '$query'.", 0)
    }

    val out = matches.joinToString("\n\n") { pkg ->
      val status = if (pkg.isInstalled) "[instalado]" else "[disponível]"
      val archBadge = if (pkg.isOfficialArm64Build) "[arm64 oficial]" else "[x86_64 não compatível]"
      "${pkg.name}/${pkg.repository} ${pkg.version} ${pkg.architecture} $status $archBadge\n  ${pkg.description}"
    }

    return PackageOperationResult(true, out, 0)
  }

  fun showPackage(packageName: String): PackageOperationResult {
    val pkg = _packages.value.find { it.id.equals(packageName, ignoreCase = true) || it.name.equals(packageName, ignoreCase = true) }

    if (pkg == null) {
      return PackageOperationResult(false, "E: Pacote '$packageName' não encontrado.", 1)
    }

    val info = """
      Package: ${pkg.name}
      Version: ${pkg.version}
      Architecture: ${pkg.architecture}
      Section: ${pkg.category}
      Installed-Size: ${pkg.sizeFormatted}
      Repository: ${pkg.repository}
      Official-ARM64: ${if (pkg.isOfficialArm64Build) "Yes (Verified aarch64 binary)" else "No"}
      Status: ${if (pkg.isInstalled) "installed" else "available"}
      Depends: ${pkg.dependencies.joinToString(", ").ifEmpty { "none" }}
      Description: ${pkg.description}
      ${if (pkg.maintainerNote != null) "Note: " + pkg.maintainerNote else ""}
    """.trimIndent()

    return PackageOperationResult(true, info, 0)
  }

  fun listInstalled(): PackageOperationResult {
    val installed = _packages.value.filter { it.isInstalled }
    val out = installed.joinToString("\n") { pkg ->
      String.format("%-24s %-16s %-8s %s", pkg.name, pkg.version, pkg.architecture, pkg.description.take(45))
    }
    return PackageOperationResult(
      true,
      "Listing... Done\n" + out,
      0
    )
  }

  fun listUpgradable(): PackageOperationResult {
    val upgradable = _packages.value.filter { it.isInstalled && it.updateAvailable }
    if (upgradable.isEmpty()) {
      return PackageOperationResult(true, "Listing... Done\nTodos os pacotes estão atualizados.", 0)
    }
    val out = upgradable.joinToString("\n") { pkg ->
      String.format("%-24s %-16s [upgradable from: %s]", pkg.name, pkg.newVersion ?: pkg.version, pkg.version)
    }
    return PackageOperationResult(true, "Listing... Done\n$out", 0)
  }

  fun listAll(): PackageOperationResult {
    val out = _packages.value.joinToString("\n") { pkg ->
      val flag = if (pkg.isInstalled) "[installed]" else ""
      String.format("%-24s %-16s %-8s %s", pkg.name, pkg.version, pkg.architecture, flag)
    }
    return PackageOperationResult(true, out, 0)
  }
}
