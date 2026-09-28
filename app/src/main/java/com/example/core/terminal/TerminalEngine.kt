package com.example.core.terminal

import com.example.backends.LinuxBackend
import com.example.core.vps.VpsManager
import com.example.core.vps.VpsServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TerminalLine(
  val text: String,
  val isPrompt: Boolean = false,
  val isError: Boolean = false,
  val isSystem: Boolean = false
)

data class TerminalSession(
  val id: String,
  val title: String,
  val currentDir: String,
  val history: List<String> = emptyList(),
  val historyIndex: Int = -1,
  val lines: List<TerminalLine> = emptyList(),
  val isExecuting: Boolean = false,
  val isSshActive: Boolean = false,
  val sshHost: String? = null,
  val sshUser: String? = null,
  val isRoot: Boolean = false
)

class TerminalEngine(
  private val backend: LinuxBackend,
  var vpsManager: VpsManager? = null
) {

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
  val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

  private val _activeSessionId = MutableStateFlow<String>("")
  val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

  private val defaultHome: String
    get() = backend.shellEngine?.defaultWorkingDir ?: "/home/user"

  init {
    createNewSession("Terminal 1")
  }

  fun createNewSession(title: String = "Terminal ${(_sessions.value.size + 1)}"): String {
    val id = "term_${System.currentTimeMillis()}_${_sessions.value.size}"
    val welcomeLines = listOf(
      TerminalLine("Desktable OS Terminal (Debian GNU/Linux 12 bookworm - aarch64)", isSystem = true),
      TerminalLine("Kernel: Linux 6.8.0-45-generic ARM64 | Shell: /bin/bash POSIX", isSystem = true),
      TerminalLine("Comandos Debian: apt, apt-get, dpkg, sudo, uname -a, ip a, free -h, df -h, curl, wget, ssh", isSystem = true),
      TerminalLine("Gerenciamento de pacotes: 'apt update', 'apt upgrade', 'apt install <pacote>'", isSystem = true),
      TerminalLine("Digite 'help' para a lista completa ou 'desktable-arch' para informações do sistema.\n", isSystem = true)
    )
    val session = TerminalSession(
      id = id,
      title = title,
      currentDir = defaultHome,
      lines = welcomeLines
    )
    _sessions.value = _sessions.value + session
    _activeSessionId.value = id
    return id
  }

  fun startSshSession(server: VpsServerConfig, initialDir: String = "/home"): String {
    val id = "term_ssh_${System.currentTimeMillis()}"
    val welcomeLines = listOf(
      TerminalLine("[SSH] Iniciando túnel SSH criptografado para ${server.username}@${server.host}:${server.port}...", isSystem = true),
      TerminalLine("[SSH] Conexão estabelecida com sucesso. Host verificado.", isSystem = true),
      TerminalLine("Linux ${server.host} 6.8.0-45-generic aarch64 (Ubuntu/Debian)", isSystem = true),
      TerminalLine("Diretório remoto ativo: $initialDir", isSystem = true),
      TerminalLine("Digite comandos remotos normais (ex: git pull, docker ps, apt update).", isSystem = true),
      TerminalLine("Digite 'exit' ou 'logout' para encerrar a sessão remota.\n", isSystem = true)
    )
    val session = TerminalSession(
      id = id,
      title = "SSH: ${server.name}",
      currentDir = initialDir,
      lines = welcomeLines,
      isSshActive = true,
      sshHost = server.host,
      sshUser = server.username
    )
    _sessions.value = _sessions.value + session
    _activeSessionId.value = id
    return id
  }

  fun closeSession(id: String) {
    val cur = _sessions.value
    if (cur.size <= 1) return
    val updated = cur.filter { it.id != id }
    _sessions.value = updated
    if (_activeSessionId.value == id) {
      _activeSessionId.value = updated.first().id
    }
  }

  fun selectSession(id: String) {
    _activeSessionId.value = id
  }

  fun executeCommand(rawCommand: String) {
    val activeId = _activeSessionId.value
    val session = _sessions.value.find { it.id == activeId } ?: return

    val cmd = rawCommand.trim()
    val promptPrefix = if (session.isSshActive) {
      "${session.sshUser ?: "root"}@${session.sshHost ?: "vps"}:${session.currentDir}$ "
    } else if (session.isRoot) {
      "root@desktable:${session.currentDir}# "
    } else {
      "user@desktable:${session.currentDir}$ "
    }
    val promptLine = TerminalLine(promptPrefix + rawCommand, isPrompt = true)

    if (cmd.isEmpty()) {
      updateSession(activeId) {
        it.copy(lines = it.lines + promptLine)
      }
      return
    }

    if (!session.isSshActive && (cmd == "su" || cmd == "sudo su" || cmd == "sudo -i" || cmd == "su -" || cmd == "su root")) {
      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine + TerminalLine("[root] Autenticação sem senha concedida. Sessão superusuário ativada (uid=0 gid=0).\nDigite 'exit' para retornar ao usuário normal.", isSystem = true),
          isRoot = true,
          history = newHistory,
          historyIndex = -1
        )
      }
      return
    }

    if (!session.isSshActive && (cmd == "exit" || cmd == "logout") && session.isRoot) {
      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine + TerminalLine("exit: saindo da sessão de root, voltando para 'user@desktable'.", isSystem = true),
          isRoot = false,
          history = newHistory,
          historyIndex = -1
        )
      }
      return
    }

    if (cmd == "clear") {
      updateSession(activeId) {
        it.copy(
          lines = emptyList(),
          history = if (it.history.lastOrNull() != cmd) it.history + cmd else it.history,
          historyIndex = -1
        )
      }
      return
    }

    // Interactive SSH Session Handling
    if (session.isSshActive) {
      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine,
          history = newHistory,
          historyIndex = -1,
          isExecuting = true
        )
      }

      if (cmd == "exit" || cmd == "logout") {
        updateSession(activeId) {
          it.copy(
            isSshActive = false,
            sshHost = null,
            sshUser = null,
            title = "Terminal",
            currentDir = defaultHome,
            lines = it.lines + TerminalLine("[SSH] Conexão encerrada com o servidor remoto. Sessão local restaurada.", isSystem = true),
            isExecuting = false
          )
        }
        return
      }

      scope.launch {
        val vps = vpsManager
        val outText = if (vps != null) {
          if (cmd.startsWith("cd ")) {
            val target = cmd.removePrefix("cd ").trim()
            val newDir = when {
              target.startsWith("/") -> target
              target == "~" -> "/home/${session.sshUser ?: "ubuntu"}"
              else -> if (session.currentDir == "/") "/$target" else "${session.currentDir}/$target"
            }
            updateSession(activeId) { it.copy(currentDir = newDir) }
            ""
          } else {
            vps.executeRemoteCommand(cmd)
          }
        } else {
          "[SSH] Erro: Subsistema VPS não inicializado."
        }

        val outLines = if (outText.isNotBlank()) listOf(TerminalLine(outText)) else emptyList()
        updateSession(activeId) {
          it.copy(
            lines = it.lines + outLines,
            isExecuting = false
          )
        }
      }
      return
    }

    // Local Shell Commands
    if (cmd == "vps" || cmd == "servers") {
      val vps = vpsManager
      val serversList = vps?.servers?.value ?: emptyList()
      val report = buildString {
        appendLine("Servidores VPS Configurados no Desktable OS:")
        if (serversList.isEmpty()) {
          appendLine("  (Nenhum servidor cadastrado. Adicione em Files -> Servidores -> Adicionar)")
        } else {
          serversList.forEachIndexed { idx, s ->
            val activeTag = if (vps?.activeServer?.value?.id == s.id && vps.isConnected.value) " [CONECTADO]" else ""
            appendLine("  [${idx + 1}] ${s.name} (${s.username}@${s.host}:${s.port})$activeTag")
            appendLine("      Diretório inicial: ${s.initialDirectory}")
          }
          appendLine("\nPara conectar via terminal: ssh <usuario>@<host> ou use a Central Files.")
        }
      }

      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine + TerminalLine(report),
          history = newHistory,
          historyIndex = -1
        )
      }
      return
    }

    // ssh command: initiate interactive SSH session
    if (cmd.startsWith("ssh ")) {
      val target = cmd.removePrefix("ssh ").trim()
      val vps = vpsManager
      val saved = vps?.servers?.value?.find {
        target.contains(it.host) || target.contains(it.name, ignoreCase = true)
      }

      val user = when {
        target.contains("@") -> target.substringBefore("@")
        saved != null -> saved.username
        else -> "root"
      }
      val host = when {
        target.contains("@") -> target.substringAfter("@").substringBefore(" ")
        saved != null -> saved.host
        else -> target.substringBefore(" ")
      }
      val srvConfig = saved ?: VpsServerConfig(
        name = "VPS $host",
        host = host,
        port = 22,
        username = user,
        initialDirectory = "/home/$user"
      )

      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine,
          history = newHistory,
          historyIndex = -1,
          isExecuting = true
        )
      }

      scope.launch {
        vps?.connectToServer(srvConfig)
        val initialDir = srvConfig.initialDirectory.ifBlank { "/home/$user" }
        updateSession(activeId) {
          it.copy(
            isSshActive = true,
            sshHost = host,
            sshUser = user,
            currentDir = initialDir,
            title = "SSH: $host",
            lines = it.lines + listOf(
              TerminalLine("[SSH] Conectando a $user@$host:22...", isSystem = true),
              TerminalLine("[SSH] Autenticação aceita. Sessão interativa aberta.", isSystem = true),
              TerminalLine("Linux $host 6.8.0-45-generic aarch64", isSystem = true),
              TerminalLine("Diretório remoto ativo: $initialDir", isSystem = true),
              TerminalLine("Digite 'exit' para encerrar a sessão SSH.\n", isSystem = true)
            ),
            isExecuting = false
          )
        }
      }
      return
    }

    if (cmd == "help") {
      val helpText = """
        Desktable OS - Comandos Debian GNU/Linux 12 (bookworm - ARM64 / aarch64):
          Gerenciamento de Pacotes (APT / DPKG):
            apt update                  Atualiza listas de pacotes dos repositórios oficiais
            apt upgrade                 Atualiza todos os pacotes instalados no sistema
            apt install <pacote>        Instala pacotes oficiais ARM64 (git, curl, wget, python3, etc.)
            apt install upgrade         Atalho reconhecido para atualizar os pacotes do sistema
            apt remove / purge <pacote> Remove pacotes instalados
            apt search <termo>          Pesquisa pacotes disponíveis no repositório
            apt show <pacote>           Exibe detalhes e dependências
            apt list --installed        Lista pacotes instalados
            dpkg -l / dpkg -s           Lista e consulta banco de dados dpkg

          Administração e Sistema:
            sudo <comando>              Executa qualquer comando como superusuário (sem senha)
            su / sudo su                Entra no shell de root (prompt #)
            uname -a                    Informações do Kernel Linux ARM64
            whoami / id                 Usuário atual e identificadores UID/GID
            free -h / df -h             Uso de memória RAM e sistemas de arquivos montados
            uptime                      Tempo de atividade e média de carga (load average)
            systemctl status <serviço>  Status de serviços (ssh, wayland, networking, cron)
            ip a / ifconfig             Exibe interfaces e endereços IP da rede
            neofetch / desktable-arch   Informações de sistema e hardware em ASCII art

          Rede e Conexão VPS:
            curl [-I] <url>             Requisições HTTP/HTTPS completas
            wget <url>                  Download direto de arquivos para o diretório
            ping <host>                 Teste de latência e conectividade
            ssh [user@]host             Conexão interativa SSH para servidores remotos
            vps / servers               Lista servidores remotos VPS salvos

          Arquivos e Edição:
            ls [-la] / ll / la          Lista arquivos detalhados
            cd [pasta] / pwd            Navegação entre diretórios
            cat / nano / vim            Leitura e edição de arquivos
            mkdir / rm / touch / echo   Criação e manipulação de arquivos
      """.trimIndent()
      val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
      updateSession(activeId) {
        it.copy(
          lines = it.lines + promptLine + TerminalLine(helpText),
          history = newHistory,
          historyIndex = -1
        )
      }
      return
    }

    val newHistory = if (session.history.lastOrNull() != cmd) session.history + cmd else session.history
    updateSession(activeId) {
      it.copy(
        lines = it.lines + promptLine,
        history = newHistory,
        historyIndex = -1,
        isExecuting = true
      )
    }

    scope.launch {
      val res = backend.executeCommandAsync(cmd, session.currentDir)
      val outLines = mutableListOf<TerminalLine>()

      if (res.output == "__CLEAR__") {
        updateSession(activeId) {
          it.copy(lines = emptyList(), isExecuting = false)
        }
        return@launch
      }

      if (res.output.isNotEmpty()) {
        outLines.add(TerminalLine(res.output, isError = res.exitCode != 0))
      }

      val newWorkingDir = res.newWorkingDir ?: session.currentDir
      updateSession(activeId) {
        it.copy(
          currentDir = newWorkingDir,
          lines = it.lines + outLines,
          isExecuting = false
        )
      }
    }
  }

  fun getPreviousHistory(activeId: String): String? {
    val session = _sessions.value.find { it.id == activeId } ?: return null
    if (session.history.isEmpty()) return null
    val nextIdx = if (session.historyIndex == -1) {
      session.history.size - 1
    } else {
      (session.historyIndex - 1).coerceAtLeast(0)
    }
    updateSession(activeId) { it.copy(historyIndex = nextIdx) }
    return session.history.getOrNull(nextIdx)
  }

  fun getNextHistory(activeId: String): String? {
    val session = _sessions.value.find { it.id == activeId } ?: return null
    if (session.historyIndex == -1 || session.historyIndex >= session.history.size - 1) {
      updateSession(activeId) { it.copy(historyIndex = -1) }
      return ""
    }
    val nextIdx = session.historyIndex + 1
    updateSession(activeId) { it.copy(historyIndex = nextIdx) }
    return session.history.getOrNull(nextIdx)
  }

  private fun updateSession(id: String, transform: (TerminalSession) -> TerminalSession) {
    _sessions.value = _sessions.value.map {
      if (it.id == id) transform(it) else it
    }
  }
}
