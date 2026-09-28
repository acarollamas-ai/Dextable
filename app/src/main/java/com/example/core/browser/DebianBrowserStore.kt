package com.example.core.browser

import android.content.Context
import com.example.core.packages.PackageManagerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BrowserStoreCategory(val label: String) {
  ALL("Todos os Navegadores"),
  POPULAR("Mais Populares"),
  PRIVACY("Privacidade & Tor"),
  LIGHTWEIGHT("Ultraleves (WebKit)")
}

data class DebianBrowserInfo(
  val id: String,
  val name: String,
  val version: String,
  val iconEmoji: String,
  val brandColorHex: Long,
  val engineName: String,
  val debPackageName: String,
  val repository: String,
  val packageSizeBytes: Long,
  val description: String,
  val highlights: List<String>,
  val category: BrowserStoreCategory,
  val isOfficialDebianRepo: Boolean,
  val isInstalled: Boolean = true,
  val isDefault: Boolean = false,
  val maintainerNote: String = ""
)

class DebianBrowserStore(
  private val context: Context,
  private val packageManager: PackageManagerEngine
) {
  private val scope = CoroutineScope(Dispatchers.Main)

  private val _defaultBrowserId = MutableStateFlow("chrome")
  val defaultBrowserId: StateFlow<String> = _defaultBrowserId.asStateFlow()

  private val _installedBrowserIds = MutableStateFlow(
    setOf("chrome", "firefox", "chromium", "brave", "edge", "desktable_browser")
  )
  val installedBrowserIds: StateFlow<Set<String>> = _installedBrowserIds.asStateFlow()

  private val _installingBrowserId = MutableStateFlow<String?>(null)
  val installingBrowserId: StateFlow<String?> = _installingBrowserId.asStateFlow()

  private val _installProgress = MutableStateFlow<Float>(0f)
  val installProgress: StateFlow<Float> = _installProgress.asStateFlow()

  private val _installLog = MutableStateFlow<String>("")
  val installLog: StateFlow<String> = _installLog.asStateFlow()

  private val allBrowsers = listOf(
    DebianBrowserInfo(
      id = "chrome",
      name = "Google Chrome Desktop",
      version = "128.0.6613.137",
      iconEmoji = "🌐",
      brandColorHex = 0xFF4285F4,
      engineName = "Blink / Google V8 Engine",
      debPackageName = "google-chrome-stable",
      repository = "dl.google.com/linux/chrome/deb stable main [arch=arm64]",
      packageSizeBytes = 88 * 1024 * 1024,
      description = "Navegador de alta velocidade da Google com interface Material You, suporte completo a extensões da Chrome Web Store e sincronização veloz de contas.",
      highlights = listOf("Motor V8 Moderno", "Material You UI", "Google Search Nativo", "Aceleração GPU ARM64"),
      category = BrowserStoreCategory.POPULAR,
      isOfficialDebianRepo = false,
      maintainerNote = "Pacote Debian oficial otimizado para arquitetura Linux ARM64 (aarch64) com aceleração de renderização Ozone/Wayland."
    ),
    DebianBrowserInfo(
      id = "firefox",
      name = "Mozilla Firefox Quantum / ESR",
      version = "128.3.0esr-1",
      iconEmoji = "🦊",
      brandColorHex = 0xFFFF7139,
      engineName = "Gecko / SpiderMonkey (Mozilla)",
      debPackageName = "firefox-esr",
      repository = "deb.debian.org/debian bookworm main [arch=arm64]",
      packageSizeBytes = 68 * 1024 * 1024,
      description = "O navegador oficial livre e de código aberto do Debian GNU/Linux. Máxima privacidade com proteção rigorosa contra rastreadores, telemetria e cookies terceiros.",
      highlights = listOf("Oficial Debian 12", "Proteção Contra Rastreamento", "Gecko Quantum Engine", "Zero Rastreamento"),
      category = BrowserStoreCategory.PRIVACY,
      isOfficialDebianRepo = true,
      maintainerNote = "Pacote compilado oficialmente pelo time Debian para aarch64 com MOZ_ENABLE_WAYLAND=1 ativado por padrão."
    ),
    DebianBrowserInfo(
      id = "chromium",
      name = "Chromium Open Source",
      version = "128.0.6613.119-1",
      iconEmoji = "⚡",
      brandColorHex = 0xFF1973E8,
      engineName = "Blink / Open Source",
      debPackageName = "chromium",
      repository = "deb.debian.org/debian bookworm main [arch=arm64]",
      packageSizeBytes = 94 * 1024 * 1024,
      description = "A base 100% de código aberto do Chrome mantida diretamente pelos desenvolvedores do Debian. Auditado por especialistas em software livre.",
      highlights = listOf("100% Código Aberto", "Debian Official Security", "Suporte Completo a Extensões", "GPU Wayland"),
      category = BrowserStoreCategory.POPULAR,
      isOfficialDebianRepo = true,
      maintainerNote = "Auditado pelo Debian Security Team com atualizações regulares via apt update."
    ),
    DebianBrowserInfo(
      id = "brave",
      name = "Brave Browser Desktop",
      version = "1.69.160",
      iconEmoji = "🦁",
      brandColorHex = 0xFFFF5500,
      engineName = "Blink (Brave Shields)",
      debPackageName = "brave-browser",
      repository = "brave-browser-apt-release.s3.brave.com stable main [arch=arm64]",
      packageSizeBytes = 92 * 1024 * 1024,
      description = "Bloqueio automático e severo de anúncios, popups irritantes e rastreadores criptográficos. Carregamento de páginas até 3x mais rápido.",
      highlights = listOf("Brave Shields AdBlock", "Privacidade Extrema", "Brave Search Nativo", "Economia de Dados"),
      category = BrowserStoreCategory.PRIVACY,
      isOfficialDebianRepo = false,
      maintainerNote = "Repositório APT oficial da Brave Software com chaves GPG verificadas para ARM64."
    ),
    DebianBrowserInfo(
      id = "edge",
      name = "Microsoft Edge Linux",
      version = "128.0.2733.67",
      iconEmoji = "🌀",
      brandColorHex = 0xFF0078D4,
      engineName = "Blink / Microsoft Engine",
      debPackageName = "microsoft-edge-stable",
      repository = "packages.microsoft.com/repos/edge stable main [arch=arm64]",
      packageSizeBytes = 96 * 1024 * 1024,
      description = "Navegador com foco em produtividade empresarial, integração inteligente com Copilot, abas verticais e modo de leitura imersivo avançado.",
      highlights = listOf("Abas Verticais", "Copilot AI Ready", "Modo Leitura Avançado", "Alta Eficiência"),
      category = BrowserStoreCategory.POPULAR,
      isOfficialDebianRepo = false,
      maintainerNote = "Pacote de distribuição corporativa Microsoft para sistemas Debian Linux aarch64."
    ),
    DebianBrowserInfo(
      id = "tor",
      name = "Tor Browser (Debian Security)",
      version = "13.5.3-1",
      iconEmoji = "🧅",
      brandColorHex = 0xFF7D4698,
      engineName = "Gecko (Tor Hardened)",
      debPackageName = "torbrowser-launcher",
      repository = "deb.debian.org/debian bookworm-backports [arch=arm64]",
      packageSizeBytes = 82 * 1024 * 1024,
      description = "Navegação totalmente anônima roteada pela rede Tor em camadas criptografadas. Impede vigilância em massa e contorna bloqueios de rede.",
      highlights = listOf("Rede Onion Criptografada", "Anti-Censura", "Anti-Fingerprinting", "Anonimato Total"),
      category = BrowserStoreCategory.PRIVACY,
      isOfficialDebianRepo = true,
      maintainerNote = "Isola cada site visitado para que rastreadores de terceiros não possam segui-lo."
    ),
    DebianBrowserInfo(
      id = "midori",
      name = "GNOME Web / Midori (Epiphany)",
      version = "46.0-1",
      iconEmoji = "🍃",
      brandColorHex = 0xFF2EC27E,
      engineName = "WebKitGTK 4.1",
      debPackageName = "epiphany-browser",
      repository = "deb.debian.org/debian bookworm main [arch=arm64]",
      packageSizeBytes = 28 * 1024 * 1024,
      description = "Navegador ultraleve e minimalista nativo do ecossistema GNOME/Debian. Consome uma fração da memória RAM em comparação a navegadores pesados.",
      highlights = listOf("Consumo Mínimo de RAM", "WebKitGTK Nativo", "Início Instantâneo", "Design Minimalista"),
      category = BrowserStoreCategory.LIGHTWEIGHT,
      isOfficialDebianRepo = true,
      maintainerNote = "Perfeito para ambientes com economia extrema de energia e hardware ARM64 portátil."
    ),
    DebianBrowserInfo(
      id = "opera",
      name = "Opera Desktop",
      version = "112.0.5197.53",
      iconEmoji = "🔴",
      brandColorHex = 0xFFFF1B2D,
      engineName = "Blink (Opera)",
      debPackageName = "opera-stable",
      repository = "deb.opera.com/opera-stable stable non-free [arch=arm64]",
      packageSizeBytes = 84 * 1024 * 1024,
      description = "Navegador inovador com VPN gratuita ilimitada integrada, barra lateral com mensageiros rápidos e leitor de notícias inteligente.",
      highlights = listOf("VPN Gratuita Embutida", "Barra Lateral de Apps", "Poupador de Bateria", "Bloqueador Nativo"),
      category = BrowserStoreCategory.POPULAR,
      isOfficialDebianRepo = false,
      maintainerNote = "Inclui VPN de navegação embutida sem necessidade de instalar extensões adicionais."
    ),
    DebianBrowserInfo(
      id = "vivaldi",
      name = "Vivaldi Power User Browser",
      version = "6.8.3381.53",
      iconEmoji = "🎭",
      brandColorHex = 0xFFEF3939,
      engineName = "Blink (Vivaldi)",
      debPackageName = "vivaldi-stable",
      repository = "repo.vivaldi.com/archive/deb stable main [arch=arm64]",
      packageSizeBytes = 98 * 1024 * 1024,
      description = "O navegador mais customizável do mundo: abas divididas em duas telas, gerenciador de notas na barra lateral e atalhos customizados.",
      highlights = listOf("Divisão de Telas", "Empilhamento de Abas", "Notas Integradas", "Painéis Web Laterais"),
      category = BrowserStoreCategory.POPULAR,
      isOfficialDebianRepo = false,
      maintainerNote = "Feito para usuários avançados que necessitam de multitarefa extrema."
    ),
    DebianBrowserInfo(
      id = "desktable_browser",
      name = "Desktable Native Browser",
      version = "1.2.0-native",
      iconEmoji = "🧭",
      brandColorHex = 0xFF009688,
      engineName = "Android System WebView + Wayland",
      debPackageName = "desktable-browser",
      repository = "deb.desktable.org/os stable [arch=arm64]",
      packageSizeBytes = 36 * 1024 * 1024,
      description = "Motor de navegação nativo e leve desenvolvido especificamente para o Desktable OS. Aceleração de hardware direta sem overhead de containers.",
      highlights = listOf("Aceleração Zero-Lag", "Multi-Aba Rápido", "Consumo Reduzido", "Wayland Nativo"),
      category = BrowserStoreCategory.LIGHTWEIGHT,
      isOfficialDebianRepo = false,
      maintainerNote = "Totalmente otimizado para tablets com renderização acelerada por GPU."
    )
  )

  private val _browsersList = MutableStateFlow<List<DebianBrowserInfo>>(emptyList())
  val browsersList: StateFlow<List<DebianBrowserInfo>> = _browsersList.asStateFlow()

  init {
    updateBrowserState()
  }

  private fun updateBrowserState() {
    val installed = _installedBrowserIds.value
    val defaultId = _defaultBrowserId.value
    _browsersList.value = allBrowsers.map { b ->
      b.copy(
        isInstalled = installed.contains(b.id),
        isDefault = b.id == defaultId
      )
    }
  }

  fun setDefaultBrowser(browserId: String) {
    _defaultBrowserId.value = browserId
    updateBrowserState()
  }

  fun installBrowser(browser: DebianBrowserInfo) {
    if (_installingBrowserId.value != null) return
    _installingBrowserId.value = browser.id
    _installProgress.value = 0.05f
    _installLog.value = "Lendo listas de pacotes... Pronto\nConstruindo árvore de dependências... Pronto\nLendo informação de estado... Pronto\n"

    scope.launch {
      delay(400)
      _installProgress.value = 0.25f
      _installLog.value += "Obtendo:1 ${browser.repository} ${browser.debPackageName} arm64 [${browser.packageSizeBytes / (1024 * 1024)} MB]...\n"
      delay(600)
      _installProgress.value = 0.55f
      _installLog.value += "Baixado com sucesso em 0.6s (142 MB/s)\nDescompactando ${browser.debPackageName} (${browser.version}) sobre (anterior)...\n"
      delay(500)
      _installProgress.value = 0.85f
      _installLog.value += "Configurando ${browser.debPackageName} (${browser.version})...\nProcessando gatilhos para desktop-file-utils (0.26-1)...\nProcessando gatilhos para mime-support (3.66)...\n"
      delay(400)
      _installProgress.value = 1f
      _installLog.value += "Pacote ${browser.debPackageName} instalado com sucesso no Desktable OS!\n"

      _installedBrowserIds.value = _installedBrowserIds.value + browser.id
      updateBrowserState()
      delay(800)
      _installingBrowserId.value = null
      _installProgress.value = 0f
    }
  }

  fun uninstallBrowser(browserId: String) {
    if (browserId == "desktable_browser") return // Don't remove core native browser
    _installedBrowserIds.value = _installedBrowserIds.value - browserId
    if (_defaultBrowserId.value == browserId) {
      _defaultBrowserId.value = "chrome"
    }
    updateBrowserState()
  }

  fun getBrowserInfo(id: String): DebianBrowserInfo? {
    return _browsersList.value.find { it.id == id }
  }
}
