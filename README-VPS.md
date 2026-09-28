# WebOS (Desktop Remoto) para Tablet — Guia de Conexão VPS

Este projeto contém o frontend completo do **WebOS (estética macOS em proporção 16:9)** e o servidor backend para rodar na sua **VPS Linux**.

---

## 1. Onde Configurar o IP da sua VPS

Você tem duas formas fáceis de apontar o WebOS para sua VPS:

### Opção A (Diretamente pela Interface Visual do WebOS)
1. No WebOS (no tablet ou no navegador), clique no ícone **Configurações** (engrenagem no Dock inferior ou na Área de Trabalho).
2. No campo **IP ou Domínio da VPS**, digite o endereço IP da sua máquina remota (ex: `192.168.1.100` ou `seu-dominio.com`).
3. Clique em **Salvar e Reconectar**.

### Opção B (No código JavaScript)
No arquivo `app.js` (ou `app/src/main/assets/webos/app.js`), edite as primeiras linhas:
```javascript
const DEFAULT_VPS_IP = '192.168.1.100'; // Substitua pelo IP da sua VPS
const DEFAULT_WS_PORT = 3001;          // Porta do WebSocket (Terminal xterm.js)
const DEFAULT_HTTP_PORT = 3000;        // Porta HTTP (API de Arquivos)
```

---

## 2. Como Rodar o Backend na sua VPS Linux (Ubuntu / Debian / CentOS)

1. Conecte-se à sua VPS via SSH:
   ```bash
   ssh ubuntu@seu-ip-vps
   ```

2. Instale o Node.js e npm (caso ainda não tenha):
   ```bash
   sudo apt update
   sudo apt install -y nodejs npm
   ```

3. Copie os arquivos `server.js` e `package.json` para a VPS e instale as dependências:
   ```bash
   npm install
   ```

4. Inicie o servidor:
   ```bash
   node server.js
   ```
   *(Dica para produção: use `pm2 start server.js --name webos-backend`)*

5. Certifique-se de liberar as portas no firewall da VPS se necessário:
   ```bash
   sudo ufw allow 3000/tcp
   sudo ufw allow 3001/tcp
   ```

---

## 3. Funcionalidades Incluídas

- **Layout 16:9 Estilo macOS**: Barra superior com relógio em tempo real, menus e status da VPS; Dock inferior com ícones animados e reflexos; tema Claro/Escuro via variáveis CSS.
- **Terminal xterm.js Real**: Conexão WebSocket bidirecional para execução real de comandos interativos (SSH/PTY).
- **Gerenciador de Arquivos via HTTP Fetch**: Faz requisições HTTP `fetch()` reais à API da VPS para listar pastas, navegar por diretórios e visualizar/editar arquivos de texto.
- **Navegador com iframe**: Barra de endereço com suporte a navegação por URL, botões Voltar, Avançar e Recarregar.
