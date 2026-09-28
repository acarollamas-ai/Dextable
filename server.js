/**
 * ==============================================================================
 * WebOS VPS Backend Server (Node.js + Express + WebSocket + PTY)
 * ==============================================================================
 * 
 * Este servidor roda na sua VPS Linux e atende:
 * 1. API HTTP (Porta 3000): Gerenciador de Arquivos (listar, ler, salvar) e Status
 * 2. WebSocket (Porta 3001): Terminal Interativo Real (xterm.js via node-pty ou bash)
 * 
 * COMO EXECUTAR NA VPS:
 * ------------------------------------------------------------------------------
 * 1. Instale o Node.js:
 *    sudo apt update && sudo apt install -y nodejs npm
 * 
 * 2. Instale as dependências:
 *    npm install express ws cors node-pty
 * 
 * 3. Inicie o servidor:
 *    node server.js
 * 
 * 4. No frontend WebOS (Tablet), insira o IP da sua VPS na janela Configurações!
 * ==============================================================================
 */

const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const os = require('os');

// Portas do Servidor
const HTTP_PORT = process.env.HTTP_PORT || 3000;
const WS_PORT = process.env.WS_PORT || 3001;

/* ==============================================================================
   1. SERVIDOR HTTP (API DE ARQUIVOS E STATUS)
   ============================================================================== */
const app = express();
app.use(cors());
app.use(express.json());

// Servir os arquivos estáticos do frontend caso queira hospedar direto na VPS
app.use(express.static(path.join(__dirname, 'webos')));

// Health check / Status da VPS
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    hostname: os.hostname(),
    platform: os.platform(),
    arch: os.arch(),
    uptime: os.uptime(),
    loadavg: os.loadavg(),
    memory: {
      total: os.totalmem(),
      free: os.freemem()
    },
    time: new Date().toISOString()
  });
});

// Listar arquivos e pastas de um diretório (JSON)
app.get('/api/files', (req, res) => {
  const reqPath = req.query.path || '/home';
  const targetDir = path.resolve(reqPath);

  fs.readdir(targetDir, { withFileTypes: true }, (err, items) => {
    if (err) {
      return res.status(500).json({ error: err.message, path: targetDir });
    }

    const fileList = items.map(item => {
      const fullPath = path.join(targetDir, item.name);
      let stats = null;
      try {
        stats = fs.statSync(fullPath);
      } catch (_) {}

      return {
        name: item.name,
        path: fullPath,
        isDirectory: item.isDirectory(),
        size: stats ? stats.size : 0,
        modifiedTime: stats ? stats.mtimeMs : 0,
        permissions: stats ? (stats.mode & parseInt('777', 8)).toString(8) : ''
      };
    });

    res.json(fileList);
  });
});

// Ler conteúdo de arquivo de texto
app.get('/api/files/read', (req, res) => {
  const filePath = req.query.path;
  if (!filePath) {
    return res.status(400).send('Caminho não especificado');
  }

  fs.readFile(path.resolve(filePath), 'utf8', (err, data) => {
    if (err) {
      return res.status(500).send(`Erro ao ler arquivo: ${err.message}`);
    }
    res.type('text/plain').send(data);
  });
});

// Salvar conteúdo em arquivo de texto
app.post('/api/files/save', (req, res) => {
  const { path: filePath, content } = req.body;
  if (!filePath) {
    return res.status(400).json({ error: 'Caminho não especificado' });
  }

  fs.writeFile(path.resolve(filePath), content || '', 'utf8', (err) => {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    res.json({ success: true, path: filePath });
  });
});

// Criar novo diretório
app.post('/api/files/mkdir', (req, res) => {
  const { path: dirPath } = req.body;
  if (!dirPath) {
    return res.status(400).json({ error: 'Caminho não especificado' });
  }

  fs.mkdir(path.resolve(dirPath), { recursive: true }, (err) => {
    if (err) {
      return res.status(500).json({ error: err.message });
    }
    res.json({ success: true, path: dirPath });
  });
});

// Inicializar Servidor HTTP
app.listen(HTTP_PORT, '0.0.0.0', () => {
  console.log(`[HTTP API] Servidor de Arquivos rodando em http://0.0.0.0:${HTTP_PORT}`);
});


/* ==============================================================================
   2. SERVIDOR WEBSOCKET (TERMINAL INTERATIVO xterm.js)
   ============================================================================== */
const wss = new WebSocket.Server({ port: WS_PORT, host: '0.0.0.0' });

let pty = null;
try {
  pty = require('node-pty');
} catch (e) {
  console.log('[Terminal] Módulo "node-pty" não encontrado, utilizando fallback child_process spawn.');
}

wss.on('connection', (ws) => {
  console.log('[WebSocket] Novo cliente terminal conectado!');

  const shell = process.env.SHELL || (os.platform() === 'win32' ? 'powershell.exe' : 'bash');

  if (pty) {
    // Modo PTY Completo com suporte ANSI interativo (top, vim, htop, etc.)
    const ptyProcess = pty.spawn(shell, [], {
      name: 'xterm-256color',
      cols: 80,
      rows: 24,
      cwd: process.env.HOME || '/home',
      env: process.env
    });

    ptyProcess.onData((data) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'output', data: data }));
      }
    });

    ws.on('message', (message) => {
      try {
        const parsed = JSON.parse(message);
        if (parsed.type === 'input') {
          ptyProcess.write(parsed.data);
        } else if (parsed.type === 'resize') {
          ptyProcess.resize(parsed.cols, parsed.rows);
        }
      } catch (_) {
        ptyProcess.write(message.toString());
      }
    });

    ws.on('close', () => {
      ptyProcess.kill();
      console.log('[WebSocket] Cliente desconectado.');
    });

  } else {
    // Fallback usando child_process se node-pty não estiver instalado
    const { spawn } = require('child_process');
    const child = spawn(shell, ['-i'], {
      cwd: process.env.HOME || '/home',
      env: process.env
    });

    child.stdout.on('data', (data) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'output', data: data.toString() }));
      }
    });

    child.stderr.on('data', (data) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'output', data: data.toString() }));
      }
    });

    ws.on('message', (message) => {
      try {
        const parsed = JSON.parse(message);
        if (parsed.type === 'input') {
          child.stdin.write(parsed.data);
        }
      } catch (_) {
        child.stdin.write(message.toString());
      }
    });

    ws.on('close', () => {
      child.kill();
    });
  }
});

console.log(`[WebSocket] Servidor de Terminal rodando em ws://0.0.0.0:${WS_PORT}`);
console.log(`[VPS Backend] Pronto para receber conexões do tablet WebOS!`);
