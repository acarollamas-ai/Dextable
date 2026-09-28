/**
 * ==============================================================================
 * WebOS — Desktop Remoto Estilo macOS para Tablet (16:9)
 * ==============================================================================
 * 
 * INSTRUÇÃO DE CONFIGURAÇÃO DO IP DA VPS:
 * ------------------------------------------------------------------------------
 * Para conectar este frontend à sua VPS real:
 * 1. Você pode alterar a variável 'DEFAULT_VPS_IP' abaixo ou simplesmente abrir a 
 *    janela de "Configurações" no dock do WebOS e digitar o IP da sua VPS.
 * 2. O terminal usa WebSocket na porta 3001 (ex: ws://SEU-IP:3001).
 * 3. O gerenciador de arquivos usa HTTP Fetch na porta 3000 (ex: http://SEU-IP:3000/api/files).
 * 4. Certifique-se de rodar 'node server.js' na sua VPS Linux.
 * ==============================================================================
 */

// >>> ONDE O USUÁRIO DEFINE O IP PADRÃO DA VPS <<<
const DEFAULT_VPS_IP = '192.168.1.100'; // Insira o IP público ou de rede local da sua VPS aqui
const DEFAULT_WS_PORT = 3001;          // Porta padrão do servidor WebSocket para o Terminal xterm.js
const DEFAULT_HTTP_PORT = 3000;        // Porta padrão da API HTTP REST para o Gerenciador de Arquivos

// Estado Global da Aplicação
const appState = {
  vpsConfig: {
    ip: localStorage.getItem('webos_vps_ip') || DEFAULT_VPS_IP,
    wsPort: parseInt(localStorage.getItem('webos_vps_ws_port') || DEFAULT_WS_PORT),
    httpPort: parseInt(localStorage.getItem('webos_vps_http_port') || DEFAULT_HTTP_PORT)
  },
  theme: localStorage.getItem('webos_theme') || 'dark',
  activeWindow: 'terminal',
  windows: {
    terminal: { isOpen: true, isMinimized: false, isMaximized: false, originalBounds: null },
    files: { isOpen: true, isMinimized: false, isMaximized: false, originalBounds: null },
    browser: { isOpen: false, isMinimized: false, isMaximized: false, originalBounds: null },
    settings: { isOpen: false, isMinimized: false, isMaximized: false, originalBounds: null },
    editor: { isOpen: false, isMinimized: false, isMaximized: false, originalBounds: null }
  },
  highestZIndex: 100,
  currentFilePath: null
};

// Funções utilitárias de Endereço da VPS
function getVpsHost() {
  return appState.vpsConfig.ip;
}

function getVpsWsUrl() {
  return `ws://${appState.vpsConfig.ip}:${appState.vpsConfig.wsPort}`;
}

function getVpsHttpUrl() {
  return `http://${appState.vpsConfig.ip}:${appState.vpsConfig.httpPort}`;
}

/* 1. GERENCIAMENTO DE JANELAS */
let draggedWindowId = null;
let dragStartX = 0;
let dragStartY = 0;
let windowInitialLeft = 0;
let windowInitialTop = 0;

let resizedWindowId = null;
let resizeStartX = 0;
let resizeStartY = 0;
let windowInitialWidth = 0;
let windowInitialHeight = 0;

function focusWindow(windowId) {
  const win = document.getElementById(`window-${windowId}`);
  if (!win) return;
  appState.highestZIndex += 1;
  win.style.zIndex = appState.highestZIndex;
  
  document.querySelectorAll('.mac-window').forEach(w => w.classList.remove('focused'));
  win.classList.add('focused');
  
  appState.activeWindow = windowId;
  const activeTitle = win.querySelector('.window-title span')?.innerText || 'Desktable WebOS';
  document.getElementById('active-app-name').innerText = activeTitle.split('—')[0].trim();
}

function openWindow(windowId) {
  const win = document.getElementById(`window-${windowId}`);
  if (!win) return;
  
  win.style.display = 'flex';
  win.classList.remove('minimized');
  appState.windows[windowId].isOpen = true;
  appState.windows[windowId].isMinimized = false;
  
  updateDockDot(windowId, true);
  focusWindow(windowId);

  if (windowId === 'terminal' && window.fitAddon) {
    setTimeout(() => {
      try { window.fitAddon.fit(); } catch (_) {}
    }, 100);
  }
}

function closeWindow(windowId) {
  const win = document.getElementById(`window-${windowId}`);
  if (!win) return;
  
  win.style.display = 'none';
  appState.windows[windowId].isOpen = false;
  updateDockDot(windowId, false);
}

function minimizeWindow(windowId) {
  const win = document.getElementById(`window-${windowId}`);
  if (!win) return;
  
  win.classList.add('minimized');
  appState.windows[windowId].isMinimized = true;
}

function maximizeWindow(windowId) {
  const win = document.getElementById(`window-${windowId}`);
  if (!win) return;
  
  const state = appState.windows[windowId];
  if (!state.isMaximized) {
    state.originalBounds = {
      left: win.style.left,
      top: win.style.top,
      width: win.style.width,
      height: win.style.height
    };
    win.classList.add('maximized');
    state.isMaximized = true;
  } else {
    win.classList.remove('maximized');
    if (state.originalBounds) {
      win.style.left = state.originalBounds.left;
      win.style.top = state.originalBounds.top;
      win.style.width = state.originalBounds.width;
      win.style.height = state.originalBounds.height;
    }
    state.isMaximized = false;
  }

  if (windowId === 'terminal' && window.fitAddon) {
    setTimeout(() => {
      try { window.fitAddon.fit(); } catch (_) {}
    }, 150);
  }
}

function updateDockDot(windowId, isOpen) {
  const dot = document.getElementById(`dot-${windowId}`);
  if (dot) {
    if (isOpen) dot.classList.add('active');
    else dot.classList.remove('active');
  }
}

function startDrag(e, windowId) {
  if (e.target.closest('.traffic-lights') || e.target.closest('.window-actions')) return;
  const win = document.getElementById(windowId);
  if (!win || win.classList.contains('maximized')) return;
  
  focusWindow(windowId.replace('window-', ''));
  
  draggedWindowId = windowId;
  const clientX = e.type.startsWith('touch') ? e.touches[0].clientX : e.clientX;
  const clientY = e.type.startsWith('touch') ? e.touches[0].clientY : e.clientY;
  
  dragStartX = clientX;
  dragStartY = clientY;
  windowInitialLeft = win.offsetLeft;
  windowInitialTop = win.offsetTop;
  
  document.addEventListener('mousemove', onDragMove);
  document.addEventListener('mouseup', onDragEnd);
  document.addEventListener('touchmove', onDragMove, { passive: false });
  document.addEventListener('touchend', onDragEnd);
}

function onDragMove(e) {
  if (!draggedWindowId) return;
  e.preventDefault();
  const win = document.getElementById(draggedWindowId);
  if (!win) return;
  
  const clientX = e.type.startsWith('touch') ? e.touches[0].clientX : e.clientX;
  const clientY = e.type.startsWith('touch') ? e.touches[0].clientY : e.clientY;
  
  const deltaX = clientX - dragStartX;
  const deltaY = clientY - dragStartY;
  
  const newLeft = Math.max(0, windowInitialLeft + deltaX);
  const newTop = Math.max(30, windowInitialTop + deltaY);
  
  win.style.left = `${newLeft}px`;
  win.style.top = `${newTop}px`;
}

function onDragEnd() {
  draggedWindowId = null;
  document.removeEventListener('mousemove', onDragMove);
  document.removeEventListener('mouseup', onDragEnd);
  document.removeEventListener('touchmove', onDragMove);
  document.removeEventListener('touchend', onDragEnd);
}

function startResize(e, windowId) {
  e.stopPropagation();
  const win = document.getElementById(windowId);
  if (!win) return;
  
  focusWindow(windowId.replace('window-', ''));
  
  resizedWindowId = windowId;
  const clientX = e.type.startsWith('touch') ? e.touches[0].clientX : e.clientX;
  const clientY = e.type.startsWith('touch') ? e.touches[0].clientY : e.clientY;
  
  resizeStartX = clientX;
  resizeStartY = clientY;
  windowInitialWidth = win.offsetWidth;
  windowInitialHeight = win.offsetHeight;
  
  document.addEventListener('mousemove', onResizeMove);
  document.addEventListener('mouseup', onResizeEnd);
  document.addEventListener('touchmove', onResizeMove, { passive: false });
  document.addEventListener('touchend', onResizeEnd);
}

function onResizeMove(e) {
  if (!resizedWindowId) return;
  e.preventDefault();
  const win = document.getElementById(resizedWindowId);
  if (!win) return;
  
  const clientX = e.type.startsWith('touch') ? e.touches[0].clientX : e.clientX;
  const clientY = e.type.startsWith('touch') ? e.touches[0].clientY : e.clientY;
  
  const deltaX = clientX - resizeStartX;
  const deltaY = clientY - resizeStartY;
  
  const newWidth = Math.max(320, windowInitialWidth + deltaX);
  const newHeight = Math.max(220, windowInitialHeight + deltaY);
  
  win.style.width = `${newWidth}px`;
  win.style.height = `${newHeight}px`;

  if (resizedWindowId === 'window-terminal' && window.fitAddon) {
    try { window.fitAddon.fit(); } catch (_) {}
  }
}

function onResizeEnd() {
  resizedWindowId = null;
  document.removeEventListener('mousemove', onResizeMove);
  document.removeEventListener('mouseup', onResizeEnd);
  document.removeEventListener('touchmove', onResizeMove);
  document.removeEventListener('touchend', onResizeEnd);
}

/* 2. TERMINAL (xterm.js + WEBSOCKET) */
let term = null;
let terminalSocket = null;
let isTerminalConnected = false;

function initTerminal() {
  const container = document.getElementById('terminal-container');
  if (!container) return;

  term = new Terminal({
    cursorBlink: true,
    fontSize: 13,
    fontFamily: '"SF Mono", "Fira Code", "Courier New", monospace',
    theme: {
      background: '#0b0f17',
      foreground: '#38bdf8',
      cursor: '#f0f6fc',
      selectionBackground: '#1e3a8a',
      black: '#000000',
      red: '#ef4444',
      green: '#22c55e',
      yellow: '#eab308',
      blue: '#3b82f6',
      magenta: '#a855f7',
      cyan: '#06b6d4',
      white: '#f3f4f6'
    }
  });

  const fitAddon = new FitAddon.FitAddon();
  window.fitAddon = fitAddon;
  term.loadAddon(fitAddon);

  if (window.WebLinksAddon) {
    term.loadAddon(new WebLinksAddon.WebLinksAddon());
  }

  term.open(container);
  fitAddon.fit();

  printTerminalWelcome();

  term.onData(data => {
    if (terminalSocket && terminalSocket.readyState === WebSocket.OPEN) {
      terminalSocket.send(JSON.stringify({ type: 'input', data: data }));
    } else {
      handleLocalTerminalInput(data);
    }
  });

  connectTerminalWebSocket();

  window.addEventListener('resize', () => {
    try {
      fitAddon.fit();
      if (terminalSocket && terminalSocket.readyState === WebSocket.OPEN) {
        terminalSocket.send(JSON.stringify({
          type: 'resize',
          cols: term.cols,
          rows: term.rows
        }));
      }
    } catch (_) {}
  });
}

function printTerminalWelcome() {
  term.clear();
  term.writeln('\x1b[1;36m========================================================================\x1b[0m');
  term.writeln('\x1b[1;32m  Desktable WebOS — Terminal SSH Conectado via WebSocket xterm.js\x1b[0m');
  term.writeln(`\x1b[1;33m  Endereço configurado: ${getVpsWsUrl()}\x1b[0m`);
  term.writeln('\x1b[1;36m========================================================================\x1b[0m');
  term.writeln('\x1b[90mTentando conectar ao servidor WebSocket da sua VPS...\x1b[0m\r\n');
}

function connectTerminalWebSocket() {
  const wsUrl = getVpsWsUrl();
  document.getElementById('terminal-ws-badge').innerText = `WebSocket: ${wsUrl}`;
  document.getElementById('terminal-window-title').innerText = `Terminal SSH — Conectando a ${appState.vpsConfig.ip}`;

  if (terminalSocket) {
    try { terminalSocket.close(); } catch (_) {}
    terminalSocket = null;
  }

  updateVpsStatus(false, 'Conectando...');

  try {
    terminalSocket = new WebSocket(wsUrl);

    terminalSocket.onopen = () => {
      isTerminalConnected = true;
      updateVpsStatus(true, 'Online');
      document.getElementById('terminal-window-title').innerText = `Terminal SSH — ${appState.vpsConfig.ip} (Ativo)`;
      term.writeln('\r\n\x1b[1;32m✔ Conexão WebSocket estabelecida com sucesso com a VPS!\x1b[0m\r\n');

      terminalSocket.send(JSON.stringify({
        type: 'resize',
        cols: term.cols,
        rows: term.rows
      }));
    };

    terminalSocket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data);
        if (parsed.type === 'output') {
          term.write(parsed.data);
        } else {
          term.write(event.data);
        }
      } catch (_) {
        term.write(event.data);
      }
    };

    terminalSocket.onerror = (err) => {
      console.warn('WebSocket connection error:', err);
    };

    terminalSocket.onclose = () => {
      isTerminalConnected = false;
      updateVpsStatus(false, 'Desconectada');
      document.getElementById('terminal-window-title').innerText = `Terminal SSH — VPS Desconectada`;
      term.writeln('\r\n\x1b[1;31m[WebSocket VPS Desconectado]\x1b[0m');
      term.writeln('\x1b[90mPara conectar à sua VPS real: abra "Configurações" e verifique se o servidor Node.js (server.js) está rodando na VPS.\x1b[0m\r\n');
    };

  } catch (ex) {
    console.error('Falha ao abrir WebSocket:', ex);
    updateVpsStatus(false, 'Offline');
  }
}

function reconnectTerminal() {
  printTerminalWelcome();
  connectTerminalWebSocket();
}

function clearTerminal() {
  if (term) term.clear();
}

function sendTerminalCommand(command) {
  if (terminalSocket && terminalSocket.readyState === WebSocket.OPEN) {
    terminalSocket.send(JSON.stringify({ type: 'input', data: command }));
  } else if (term) {
    term.writeln(`\r\n$ ${command.trim()}`);
    simulateLocalCommand(command.trim());
  }
}

let localInputBuffer = '';
function handleLocalTerminalInput(data) {
  if (data === '\r') {
    term.write('\r\n');
    simulateLocalCommand(localInputBuffer.trim());
    localInputBuffer = '';
    term.write('\x1b[1;32mubuntu@vps-demo\x1b[0m:\x1b[1;34m~\x1b[0m$ ');
  } else if (data === '\u007F') {
    if (localInputBuffer.length > 0) {
      localInputBuffer = localInputBuffer.slice(0, -1);
      term.write('\b \b');
    }
  } else {
    localInputBuffer += data;
    term.write(data);
  }
}

function simulateLocalCommand(cmd) {
  switch (cmd) {
    case 'ls':
    case 'ls -la':
      term.writeln('total 28\r\ndrwxr-xr-x 4 ubuntu ubuntu 4096 Sep 28 12:00 .\r\ndrwxr-xr-x 3 root   root   4096 Sep 20 09:30 ..\r\n-rw-r--r-- 1 ubuntu ubuntu  220 Sep 20 09:30 .bash_logout\r\n-rw-r--r-- 1 ubuntu ubuntu 3771 Sep 20 09:30 .bashrc\r\ndrwxr-xr-x 2 ubuntu ubuntu 4096 Sep 28 11:20 meu-site\r\n-rw-r--r-- 1 ubuntu ubuntu 1240 Sep 28 11:15 docker-compose.yml');
      break;
    case 'pwd':
      term.writeln('/home/ubuntu');
      break;
    case 'uname -a':
      term.writeln('Linux vps-cloud-arm64 6.8.0-45-generic #45-Ubuntu SMP PREEMPT_DYNAMIC aarch64 GNU/Linux');
      break;
    case 'df -h':
      term.writeln('Filesystem      Size  Used Avail Use% Mounted on\r\n/dev/root        40G   12G   28G  30% /\r\ntmpfs           3.9G     0  3.9G   0% /dev/shm');
      break;
    case 'free -m':
      term.writeln('               total        used        free      shared  buff/cache   available\r\nMem:            7942        1420        4820          24        1702        6522');
      break;
    case 'top':
      term.writeln('top - 12:35:10 up 14 days,  2 users,  load average: 0.08, 0.03, 0.01\r\nTasks: 120 total,   1 running, 119 sleeping,   0 stopped,   0 zombie\r\n%Cpu(s):  1.2 us,  0.5 sy,  0.0 ni, 98.3 id,  0.0 wa');
      break;
    default:
      if (cmd.length > 0) {
        term.writeln(`\x1b[90mComando '${cmd}' registrado. Para execução real no kernel Linux da VPS, certifique-se que o backend server.js está rodando em ${getVpsWsUrl()}\x1b[0m`);
      }
      break;
  }
}

/* 3. GERENCIADOR DE ARQUIVOS (HTTP FETCH REAL) */
let currentPath = '/home';

async function fetchFilesList(path) {
  currentPath = path || '/home';
  document.getElementById('files-path-input').value = currentPath;
  
  const loading = document.getElementById('files-loading-indicator');
  const errorState = document.getElementById('files-error-state');
  const grid = document.getElementById('files-grid');
  const statusDot = document.getElementById('files-status-dot');
  const statusLabel = document.getElementById('files-status-label');
  
  loading.style.display = 'flex';
  errorState.style.display = 'none';
  grid.innerHTML = '';
  
  const fetchUrl = `${getVpsHttpUrl()}/api/files?path=${encodeURIComponent(currentPath)}`;
  
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);
    
    const response = await fetch(fetchUrl, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    
    if (!response.ok) {
      throw new Error(`Servidor respondeu com status ${response.status}`);
    }
    
    const data = await response.json();
    loading.style.display = 'none';
    statusDot.style.backgroundColor = '#22c55e';
    statusLabel.innerText = 'Conectado à VPS';
    
    renderFilesList(data);
    
  } catch (error) {
    console.warn(`Erro no fetch para ${fetchUrl}:`, error.message);
    loading.style.display = 'none';
    statusDot.style.backgroundColor = '#eab308';
    statusLabel.innerText = 'Demonstração Local';
    renderFallbackFiles(currentPath);
  }
}

function renderFilesList(files) {
  const grid = document.getElementById('files-grid');
  grid.innerHTML = '';

  if (!files || files.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-muted); padding: 40px;">
        <i data-lucide="folder" style="width: 32px; height: 32px; opacity: 0.5;"></i>
        <p style="margin-top: 8px;">Esta pasta está vazia na VPS.</p>
      </div>
    `;
    lucide.createIcons();
    return;
  }

  const sorted = files.sort((a, b) => {
    if (a.isDirectory && !b.isDirectory) return -1;
    if (!a.isDirectory && b.isDirectory) return 1;
    return a.name.localeCompare(b.name);
  });

  sorted.forEach(item => {
    const itemEl = document.createElement('div');
    itemEl.className = `file-item ${item.isDirectory ? 'directory' : 'file'}`;
    const iconName = item.isDirectory ? 'folder' : getFileIcon(item.name);
    
    itemEl.innerHTML = `
      <div class="file-icon-box">
        <i data-lucide="${iconName}"></i>
      </div>
      <span class="file-name" title="${item.name}">${item.name}</span>
      <span class="file-meta">${item.isDirectory ? 'Pasta' : formatBytes(item.size)}</span>
    `;

    itemEl.onclick = () => {
      if (item.isDirectory) {
        fetchFilesList(item.path);
      } else {
        openTextFileFromVps(item.path, item.name);
      }
    };

    grid.appendChild(itemEl);
  });

  lucide.createIcons();
}

function renderFallbackFiles(path) {
  const mockSystem = {
    '/': [
      { name: 'bin', isDirectory: true, size: 4096, path: '/bin' },
      { name: 'etc', isDirectory: true, size: 4096, path: '/etc' },
      { name: 'home', isDirectory: true, size: 4096, path: '/home' },
      { name: 'var', isDirectory: true, size: 4096, path: '/var' },
      { name: 'tmp', isDirectory: true, size: 4096, path: '/tmp' },
      { name: 'root', isDirectory: true, size: 4096, path: '/root' }
    ],
    '/home': [
      { name: 'ubuntu', isDirectory: true, size: 4096, path: '/home/ubuntu' },
      { name: 'deploy', isDirectory: true, size: 4096, path: '/home/deploy' }
    ],
    '/home/ubuntu': [
      { name: 'meu-site', isDirectory: true, size: 4096, path: '/home/ubuntu/meu-site' },
      { name: 'docker-compose.yml', isDirectory: false, size: 1240, path: '/home/ubuntu/docker-compose.yml' },
      { name: 'server-setup.sh', isDirectory: false, size: 3450, path: '/home/ubuntu/server-setup.sh' },
      { name: 'notas-servidor.txt', isDirectory: false, size: 520, path: '/home/ubuntu/notas-servidor.txt' },
      { name: '.bashrc', isDirectory: false, size: 3771, path: '/home/ubuntu/.bashrc' }
    ],
    '/var/www': [
      { name: 'html', isDirectory: true, size: 4096, path: '/var/www/html' },
      { name: 'index.html', isDirectory: false, size: 612, path: '/var/www/html/index.html' }
    ]
  };

  const list = mockSystem[path] || [
    { name: 'README.txt', isDirectory: false, size: 300, path: `${path}/README.txt` }
  ];

  renderFilesList(list);
}

function navigateParentDirectory() {
  if (currentPath === '/' || !currentPath) return;
  const parts = currentPath.split('/').filter(Boolean);
  parts.pop();
  const parent = '/' + parts.join('/');
  fetchFilesList(parent === '' ? '/' : parent);
}

function promptNewFolder() {
  const name = prompt('Digite o nome da nova pasta na VPS:');
  if (!name) return;
  
  fetch(`${getVpsHttpUrl()}/api/files/mkdir`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: `${currentPath}/${name}` })
  })
  .then(() => fetchFilesList(currentPath))
  .catch(() => {
    alert(`Nota: Não foi possível criar '${name}' na VPS remota. Certifique-se de iniciar o servidor backend.`);
    fetchFilesList(currentPath);
  });
}

async function openTextFileFromVps(path, name) {
  openWindow('editor');
  document.getElementById('editor-title').innerText = `Editor — ${name} (${path})`;
  const textarea = document.getElementById('editor-textarea');
  textarea.value = `Carregando conteúdo de ${path} via HTTP fetch...`;
  appState.currentFilePath = path;

  try {
    const res = await fetch(`${getVpsHttpUrl()}/api/files/read?path=${encodeURIComponent(path)}`);
    if (!res.ok) throw new Error(`Status ${res.status}`);
    const text = await res.text();
    textarea.value = text;
  } catch (err) {
    textarea.value = `# Arquivo: ${path}\n# Data: ${new Date().toLocaleString()}\n# Servidor VPS: ${getVpsHost()}\n\nConteúdo carregado localmente para demonstração.\nConecte à sua VPS real para editar diretamente no disco do servidor remoto.`;
  }
}

async function saveCurrentFile() {
  if (!appState.currentFilePath) return;
  const content = document.getElementById('editor-textarea').value;
  
  try {
    const res = await fetch(`${getVpsHttpUrl()}/api/files/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path: appState.currentFilePath, content: content })
    });
    if (res.ok) {
      alert('Arquivo salvo com sucesso na VPS!');
    } else {
      alert('Aviso: Servidor VPS não confirmou o salvamento.');
    }
  } catch (_) {
    alert('Aviso: Não foi possível conectar ao servidor VPS para salvar.');
  }
}

function getFileIcon(name) {
  const ext = name.split('.').pop().toLowerCase();
  if (['txt', 'md', 'log'].includes(ext)) return 'file-text';
  if (['js', 'ts', 'py', 'sh', 'json', 'yml', 'yaml', 'html', 'css'].includes(ext)) return 'file-code';
  if (['jpg', 'png', 'gif', 'svg', 'webp'].includes(ext)) return 'image';
  if (['zip', 'tar', 'gz', 'deb'].includes(ext)) return 'archive';
  return 'file';
}

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

/* 4. NAVEGADOR COM IFRAME */
let browserHistory = [];
let browserHistoryIndex = -1;

function navigateBrowser(url) {
  if (!url) return;
  
  let formattedUrl = url.trim();
  if (!formattedUrl.startsWith('http://') && !formattedUrl.startsWith('https://')) {
    formattedUrl = 'https://' + formattedUrl;
  }

  const iframe = document.getElementById('browser-iframe');
  const input = document.getElementById('browser-url-input');

  input.value = formattedUrl;
  iframe.src = formattedUrl;

  browserHistory.push(formattedUrl);
  browserHistoryIndex = browserHistory.length - 1;
}

function browserReload() {
  const iframe = document.getElementById('browser-iframe');
  iframe.src = iframe.src;
}

function browserGoBack() {
  if (browserHistoryIndex > 0) {
    browserHistoryIndex--;
    const url = browserHistory[browserHistoryIndex];
    document.getElementById('browser-url-input').value = url;
    document.getElementById('browser-iframe').src = url;
  }
}

function browserGoForward() {
  if (browserHistoryIndex < browserHistory.length - 1) {
    browserHistoryIndex++;
    const url = browserHistory[browserHistoryIndex];
    document.getElementById('browser-url-input').value = url;
    document.getElementById('browser-iframe').src = url;
  }
}

function openUrlInNewTab() {
  const url = document.getElementById('browser-url-input').value;
  if (url) window.open(url, '_blank');
}

/* 5. CONFIGURAÇÕES & STATUS */
function saveVpsConfig() {
  const ipInput = document.getElementById('vps-ip-input').value.trim();
  const wsPort = parseInt(document.getElementById('vps-ws-port').value.trim()) || 3001;
  const httpPort = parseInt(document.getElementById('vps-http-port').value.trim()) || 3000;

  if (!ipInput) {
    alert('Por favor, informe o IP ou Host da VPS.');
    return;
  }

  appState.vpsConfig.ip = ipInput;
  appState.vpsConfig.wsPort = wsPort;
  appState.vpsConfig.httpPort = httpPort;

  localStorage.setItem('webos_vps_ip', ipInput);
  localStorage.setItem('webos_vps_ws_port', wsPort);
  localStorage.setItem('webos_vps_http_port', httpPort);

  alert(`Configurações salvas!\nIP da VPS: ${ipInput}\nWebSocket: ws://${ipInput}:${wsPort}\nHTTP API: http://${ipInput}:${httpPort}`);

  reconnectTerminal();
  fetchFilesList('/home');
}

async function testVpsConnection() {
  const resBox = document.getElementById('vps-test-result');
  resBox.style.display = 'block';
  resBox.className = 'test-result-box';
  resBox.innerText = `Testando conexão com ${getVpsHttpUrl()}...`;

  const startTime = performance.now();
  try {
    const res = await fetch(`${getVpsHttpUrl()}/api/status`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      signal: AbortSignal.timeout(3000)
    });
    const latency = Math.round(performance.now() - startTime);

    if (res.ok) {
      resBox.className = 'test-result-box success';
      resBox.innerText = `✔ Conexão bem-sucedida! Latência: ${latency}ms\nResposta da VPS: Status 200 OK`;
      updateVpsStatus(true, 'Online');
    } else {
      resBox.className = 'test-result-box error';
      resBox.innerText = `Aviso: VPS respondeu com código de erro ${res.status}.`;
    }
  } catch (err) {
    resBox.className = 'test-result-box error';
    resBox.innerText = `✖ Falha na conexão com http://${appState.vpsConfig.ip}:${appState.vpsConfig.httpPort}.\nCertifique-se de que a porta ${appState.vpsConfig.httpPort} está liberada no firewall da VPS e que 'node server.js' está em execução.`;
  }
}

function updateVpsStatus(isOnline, label) {
  const dot = document.getElementById('vps-status-dot');
  const text = document.getElementById('vps-status-text');
  
  if (dot) {
    dot.className = 'status-dot ' + (isOnline ? 'connected' : 'disconnected');
  }
  if (text) {
    text.innerText = `VPS ${label}`;
  }
}

/* 6. TEMA (CLARO/ESCURO) */
function toggleTheme() {
  const body = document.body;
  const isDark = body.classList.contains('theme-dark');
  const newTheme = isDark ? 'light' : 'dark';

  body.classList.remove('theme-dark', 'theme-light');
  body.classList.add(`theme-${newTheme}`);
  appState.theme = newTheme;
  localStorage.setItem('webos_theme', newTheme);

  const icon = document.getElementById('theme-icon');
  if (icon) {
    icon.setAttribute('data-lucide', newTheme === 'dark' ? 'moon' : 'sun');
    lucide.createIcons();
  }
}

function applyInitialTheme() {
  document.body.classList.remove('theme-dark', 'theme-light');
  document.body.classList.add(`theme-${appState.theme}`);
  const icon = document.getElementById('theme-icon');
  if (icon) {
    icon.setAttribute('data-lucide', appState.theme === 'dark' ? 'moon' : 'sun');
  }
}

/* 7. RELÓGIO */
function startSystemClock() {
  const clockEl = document.getElementById('system-clock');
  function update() {
    const now = new Date();
    const days = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
    const dayStr = days[now.getDay()];
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    clockEl.innerText = `${dayStr} ${hours}:${minutes}`;
  }
  update();
  setInterval(update, 1000);
}

/* 8. INICIALIZAÇÃO */
document.addEventListener('DOMContentLoaded', () => {
  applyInitialTheme();
  startSystemClock();

  document.getElementById('vps-ip-input').value = appState.vpsConfig.ip;
  document.getElementById('vps-ws-port').value = appState.vpsConfig.wsPort;
  document.getElementById('vps-http-port').value = appState.vpsConfig.httpPort;

  lucide.createIcons();
  initTerminal();
  fetchFilesList('/home');

  updateDockDot('terminal', true);
  updateDockDot('files', true);
  updateDockDot('browser', false);
  updateDockDot('settings', false);

  focusWindow('terminal');
});
