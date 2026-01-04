package net.ankrya.danmumod.server;

import net.ankrya.danmumod.DanmuMod;
import net.ankrya.danmumod.data.DanmuManager;
import net.ankrya.danmumod.network.Networking;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class DanmuWebServer {
    private HttpServer server;
    private final DanmuManager danmuManager;
    private final Gson gson = new Gson();
    private int port = 8080;

    public DanmuWebServer(DanmuManager manager) {
        this.danmuManager = manager;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/danmu", exchange -> {
                try {
                    handleDanmuRequest(exchange);
                } catch (Exception e) {
                    DanmuMod.error("Error handling request", e);
                }
            });

            server.createContext("/", exchange -> {
                String response = getHTMLPage();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            });

            // 添加状态检查端点
            server.createContext("/status", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "ok");
                    response.addProperty("server", "Danmu Web Server");
                    response.addProperty("port", port);

                    String responseStr = gson.toJson(response);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseStr.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseStr.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            DanmuMod.info("Danmu web server started on port " + port);
        } catch (IOException e) {
            DanmuMod.error("Failed to start web server", e);
        }
    }

    private void handleDanmuRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            if (json.has("message") && !json.get("message").getAsString().isEmpty()) {
                String message = json.get("message").getAsString();
                String sender = json.has("sender") ? json.get("sender").getAsString() : "浏览器";
                String color = json.has("color") ? json.get("color").getAsString() : "#FFFFFF";

                MinecraftClient client = MinecraftClient.getInstance();

                if (client.world != null) {
                    if (client.world.isClient()) {
                        // 客户端：发送到服务器
                        try {
                            Networking.sendDanmuToServer(sender, message, color);
                            DanmuMod.info("Sent danmu to server: " + sender + " - " + message);
                        } catch (Exception e) {
                            DanmuMod.error("Failed to send danmu to server", e);
                            // 失败时本地添加
                            danmuManager.addDanmu(sender, message, color);
                        }
                    } else {
                        // 服务器端：直接添加到管理器
                        danmuManager.addDanmu(sender, message, color);
                    }
                } else {
                    // 未进入游戏世界：本地添加
                    danmuManager.addDanmu(sender, message, color);
                }

                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("message", "Danmu sent successfully");

                String responseStr = gson.toJson(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseStr.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseStr.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("status", "error");
                response.addProperty("message", "Message cannot be empty");

                String responseStr = gson.toJson(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(400, responseStr.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseStr.getBytes(StandardCharsets.UTF_8));
                }
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private String getHTMLPage() {
        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Minecraft 弹幕发送器</title>
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                
                body {
                    font-family: 'Microsoft YaHei', Arial, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    margin: 0;
                    padding: 20px;
                    min-height: 100vh;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                
                .container {
                    background: rgba(255, 255, 255, 0.95);
                    border-radius: 15px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    padding: 30px;
                    width: 100%;
                    max-width: 600px;
                    backdrop-filter: blur(10px);
                }
                
                h1 {
                    color: #333;
                    text-align: center;
                    margin-bottom: 30px;
                    font-size: 28px;
                    background: linear-gradient(45deg, #667eea, #764ba2);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    background-clip: text;
                }
                
                .subtitle {
                    text-align: center;
                    color: #666;
                    margin-bottom: 30px;
                    font-size: 14px;
                }
                
                .input-group {
                    margin-bottom: 20px;
                    position: relative;
                }
                
                label {
                    display: block;
                    margin-bottom: 8px;
                    color: #555;
                    font-weight: bold;
                    font-size: 14px;
                }
                
                input, textarea, select {
                    width: 100%;
                    padding: 12px 15px;
                    border: 2px solid #e0e0e0;
                    border-radius: 10px;
                    font-size: 16px;
                    transition: all 0.3s;
                    box-sizing: border-box;
                    font-family: 'Microsoft YaHei', Arial, sans-serif;
                    background: #f8f9fa;
                }
                
                input:focus, textarea:focus, select:focus {
                    outline: none;
                    border-color: #667eea;
                    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                    background: white;
                }
                
                textarea {
                    min-height: 150px;
                    resize: vertical;
                    line-height: 1.5;
                }
                
                .input-hint {
                    font-size: 12px;
                    color: #888;
                    margin-top: 5px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                
                .color-picker {
                    display: grid;
                    grid-template-columns: repeat(6, 1fr);
                    gap: 10px;
                    margin-top: 5px;
                }
                
                .color-option {
                    width: 40px;
                    height: 40px;
                    border-radius: 10px;
                    cursor: pointer;
                    border: 2px solid transparent;
                    transition: all 0.3s;
                    position: relative;
                    overflow: hidden;
                }
                
                .color-option:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 5px 15px rgba(0,0,0,0.1);
                }
                
                .color-option.selected {
                    border-color: #333;
                    transform: scale(1.05);
                }
                
                .color-option::after {
                    content: '✓';
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    color: white;
                    font-weight: bold;
                    font-size: 16px;
                    opacity: 0;
                    text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
                }
                
                .color-option.selected::after {
                    opacity: 1;
                }
                
                .button-container {
                    display: flex;
                    gap: 15px;
                    margin-top: 30px;
                }
                
                button {
                    flex: 1;
                    border: none;
                    padding: 15px 30px;
                    border-radius: 10px;
                    font-size: 16px;
                    font-weight: bold;
                    cursor: pointer;
                    transition: all 0.3s;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 10px;
                }
                
                #sendBtn {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                }
                
                #sendBtn:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
                }
                
                #sendBtn:active {
                    transform: translateY(0);
                }
                
                #sendBtn:disabled {
                    background: #cccccc;
                    cursor: not-allowed;
                    transform: none;
                    box-shadow: none;
                }
                
                #clearBtn {
                    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                    color: white;
                }
                
                #clearBtn:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 10px 20px rgba(240, 147, 251, 0.3);
                }
                
                .status {
                    margin-top: 20px;
                    padding: 12px;
                    border-radius: 10px;
                    text-align: center;
                    font-weight: bold;
                    animation: fadeIn 0.3s;
                    display: none;
                }
                
                .success {
                    background-color: #d4edda;
                    color: #155724;
                    border: 1px solid #c3e6cb;
                    display: block;
                }
                
                .error {
                    background-color: #f8d7da;
                    color: #721c24;
                    border: 1px solid #f5c6cb;
                    display: block;
                }
                
                .info {
                    background-color: #d1ecf1;
                    color: #0c5460;
                    border: 1px solid #bee5eb;
                    display: block;
                }
                
                .history-section {
                    margin-top: 30px;
                    border-top: 1px solid #eee;
                    padding-top: 20px;
                }
                
                .history-title {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 15px;
                }
                
                #historyList {
                    max-height: 200px;
                    overflow-y: auto;
                    border: 1px solid #e0e0e0;
                    border-radius: 10px;
                    padding: 10px;
                    background: #f8f9fa;
                }
                
                .history-item {
                    padding: 8px 12px;
                    margin-bottom: 8px;
                    background: white;
                    border-radius: 8px;
                    border-left: 4px solid #667eea;
                    font-size: 14px;
                    animation: slideIn 0.3s;
                }
                
                .history-item:hover {
                    background: #f0f0f0;
                }
                
                .history-sender {
                    font-weight: bold;
                    color: #667eea;
                }
                
                .history-message {
                    color: #333;
                }
                
                .history-time {
                    font-size: 12px;
                    color: #888;
                    text-align: right;
                    margin-top: 2px;
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                
                @keyframes slideIn {
                    from { 
                        opacity: 0;
                        transform: translateX(-10px);
                    }
                    to { 
                        opacity: 1;
                        transform: translateX(0);
                    }
                }
                
                .shortcut-hint {
                    display: flex;
                    justify-content: space-between;
                    margin-top: 10px;
                    font-size: 12px;
                    color: #666;
                }
                
                .shortcut-item {
                    display: flex;
                    align-items: center;
                    gap: 5px;
                }
                
                .shortcut-key {
                    background: #e0e0e0;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-weight: bold;
                }
                
                /* 滚动条样式 */
                ::-webkit-scrollbar {
                    width: 8px;
                }
                
                ::-webkit-scrollbar-track {
                    background: #f1f1f1;
                    border-radius: 4px;
                }
                
                ::-webkit-scrollbar-thumb {
                    background: #c1c1c1;
                    border-radius: 4px;
                }
                
                ::-webkit-scrollbar-thumb:hover {
                    background: #a8a8a8;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🎮 Minecraft 弹幕发送器</h1>
                <div class="subtitle">在浏览器中输入消息，实时显示在Minecraft游戏中</div>
                
                <div id="connectionStatus" class="status info">
                    <div style="display: flex; align-items: center; justify-content: center; gap: 10px;">
                        <div id="connectionDot" style="width: 12px; height: 12px; border-radius: 50%; background-color: #4CAF50;"></div>
                        <span id="connectionText">正在连接到 Minecraft...</span>
                    </div>
                </div>
                
                <div class="input-group">
                    <label for="sender">发送者名称:</label>
                    <input type="text" id="sender" placeholder="请输入你的名字" value="lenhuai">
                </div>
                
                <div class="input-group">
                    <label for="color">弹幕颜色:</label>
                    <div class="color-picker">
                        <div class="color-option" style="background-color: #FF5555;" data-color="#FF5555" title="红色"></div>
                        <div class="color-option" style="background-color: #55FF55;" data-color="#55FF55" title="绿色"></div>
                        <div class="color-option" style="background-color: #5555FF;" data-color="#5555FF" title="蓝色"></div>
                        <div class="color-option" style="background-color: #FFFF55;" data-color="#FFFF55" title="黄色"></div>
                        <div class="color-option" style="background-color: #FF55FF;" data-color="#FF55FF" title="粉色"></div>
                        <div class="color-option" style="background-color: #55FFFF;" data-color="#55FFFF" title="青色"></div>
                        <div class="color-option" style="background-color: #FFAA55;" data-color="#FFAA55" title="橙色"></div>
                        <div class="color-option" style="background-color: #AA55FF;" data-color="#AA55FF" title="紫色"></div>
                        <div class="color-option" style="background-color: #55AAFF;" data-color="#55AAFF" title="浅蓝"></div>
                        <div class="color-option" style="background-color: #FF5555; background: linear-gradient(45deg, #FF5555 50%, #FFFF55 50%);" data-color="#FF5555" title="红黄渐变"></div>
                        <div class="color-option" style="background-color: #5555FF; background: linear-gradient(45deg, #5555FF 50%, #55FFFF 50%);" data-color="#5555FF" title="蓝青渐变"></div>
                        <div class="color-option" style="background-color: #FF55FF; background: linear-gradient(45deg, #FF55FF 50%, #AA55FF 50%);" data-color="#FF55FF" title="粉紫渐变"></div>
                    </div>
                    <input type="text" id="color" value="#FFFFFF" style="margin-top: 10px;">
                </div>
                
                <div class="input-group">
                    <label for="message">弹幕消息:</label>
                    <textarea id="message" placeholder="请输入要发送的消息..."></textarea>
                    <div class="input-hint">
                        <span>支持多行文本</span>
                        <span id="charCount">0/500 字符</span>
                    </div>
                </div>
                
                <div class="shortcut-hint">
                    <div class="shortcut-item">
                        <span class="shortcut-key">Enter</span>
                        <span>发送消息</span>
                    </div>
                    <div class="shortcut-item">
                        <span class="shortcut-key">Shift + Enter</span>
                        <span>换行</span>
                    </div>
                    <div class="shortcut-item">
                        <span class="shortcut-key">Ctrl + Enter</span>
                        <span>发送消息</span>
                    </div>
                </div>
                
                <div class="button-container">
                    <button id="sendBtn">
                        <span>🚀 发送弹幕到 Minecraft</span>
                    </button>
                    <button id="clearBtn">
                        <span>🗑️ 清空消息</span>
                    </button>
                </div>
                
                <div id="status" class="status"></div>
                
                <div class="history-section">
                    <div class="history-title">
                        <h3>📜 发送历史</h3>
                        <button id="clearHistoryBtn" style="background: #6c757d; color: white; padding: 5px 15px; font-size: 12px;">
                            清空历史
                        </button>
                    </div>
                    <div id="historyList">
                        <!-- 历史记录将在这里显示 -->
                    </div>
                </div>
            </div>
            
            <script>
                // DOM 元素
                const colorOptions = document.querySelectorAll('.color-option');
                const colorInput = document.getElementById('color');
                const messageInput = document.getElementById('message');
                const senderInput = document.getElementById('sender');
                const sendBtn = document.getElementById('sendBtn');
                const clearBtn = document.getElementById('clearBtn');
                const clearHistoryBtn = document.getElementById('clearHistoryBtn');
                const charCount = document.getElementById('charCount');
                const statusDiv = document.getElementById('status');
                const connectionStatus = document.getElementById('connectionStatus');
                const connectionText = document.getElementById('connectionText');
                const connectionDot = document.getElementById('connectionDot');
                const historyList = document.getElementById('historyList');
                
                // 全局变量
                let selectedColor = '#FFFFFF';
                let messageHistory = JSON.parse(localStorage.getItem('danmuHistory') || '[]');
                const MAX_HISTORY = 20;
                
                // 初始化颜色选择器
                function initColorPicker() {
                    colorOptions.forEach(option => {
                        option.addEventListener('click', () => {
                            colorOptions.forEach(o => o.classList.remove('selected'));
                            option.classList.add('selected');
                            selectedColor = option.dataset.color;
                            colorInput.value = selectedColor;
                        });
                    });
                    
                    colorInput.addEventListener('input', (e) => {
                        selectedColor = e.target.value;
                        colorOptions.forEach(o => o.classList.remove('selected'));
                    });
                    
                    // 默认选择第一个颜色
                    if (colorOptions.length > 0) {
                        colorOptions[2].classList.add('selected');
                        selectedColor = colorOptions[2].dataset.color;
                        colorInput.value = selectedColor;
                    }
                }
                
                // 初始化字符计数器
                function initCharCounter() {
                    messageInput.addEventListener('input', updateCharCount);
                    updateCharCount();
                }
                
                function updateCharCount() {
                    const length = messageInput.value.length;
                    charCount.textContent = `${length}/500 字符`;
                    
                    if (length > 500) {
                        charCount.style.color = '#dc3545';
                    } else if (length > 450) {
                        charCount.style.color = '#ffc107';
                    } else {
                        charCount.style.color = '#28a745';
                    }
                    
                    // 更新发送按钮状态
                    sendBtn.disabled = length === 0 || length > 500;
                }
                
                // 初始化快捷键
                function initShortcuts() {
                    // 在消息框中按 Enter 发送（同时阻止默认换行行为）
                    messageInput.addEventListener('keydown', function(e) {
                        if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey) {
                            e.preventDefault(); // 阻止默认的换行行为
                            if (!sendBtn.disabled) {
                                sendDanmu();
                            }
                        }
                        
                        // Ctrl+Enter 也发送
                        if (e.key === 'Enter' && e.ctrlKey) {
                            e.preventDefault();
                            if (!sendBtn.disabled) {
                                sendDanmu();
                            }
                        }
                        
                        // Shift+Enter 允许换行
                        if (e.key === 'Enter' && e.shiftKey) {
                            // 允许默认行为（换行）
                        }
                    });
                    
                    // 在发送者框中按 Enter 聚焦到消息框
                    senderInput.addEventListener('keydown', function(e) {
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            messageInput.focus();
                        }
                    });
                    
                    // 在颜色框中按 Enter 发送
                    colorInput.addEventListener('keydown', function(e) {
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            if (!sendBtn.disabled) {
                                sendDanmu();
                            }
                        }
                    });
                    
                    // 全局快捷键：Ctrl+Shift+D 清空消息
                    document.addEventListener('keydown', function(e) {
                        if (e.ctrlKey && e.shiftKey && e.key === 'D') {
                            e.preventDefault();
                            clearMessage();
                        }
                    });
                }
                
                // 发送弹幕
                async function sendDanmu() {
                    const sender = senderInput.value.trim() || '浏览器玩家';
                    const message = messageInput.value.trim();
                    
                    if (!message) {
                        showStatus('请输入消息内容！', 'error');
                        messageInput.focus();
                        return;
                    }
                    
                    if (message.length > 500) {
                        showStatus('消息过长，最多500个字符！', 'error');
                        return;
                    }
                    
                    // 禁用发送按钮，防止重复发送
                    sendBtn.disabled = true;
                    sendBtn.innerHTML = '<span>⏳ 发送中...</span>';
                    
                    const danmuData = {
                        sender: sender,
                        message: message,
                        color: selectedColor
                    };
                    
                    try {
                        const response = await fetch('/danmu', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify(danmuData)
                        });
                        
                        if (response.ok) {
                            // 添加到历史记录
                            addToHistory(sender, message, selectedColor);
                            
                            // 清空消息框并保持焦点
                            messageInput.value = '';
                            updateCharCount();
                            messageInput.focus();
                            
                            showStatus('弹幕发送成功！🎉', 'success');
                            
                            // 3秒后恢复发送按钮
                            setTimeout(() => {
                                sendBtn.disabled = false;
                                sendBtn.innerHTML = '<span>🚀 发送弹幕到 Minecraft</span>';
                            }, 1000);
                        } else {
                            const errorData = await response.json().catch(() => ({}));
                            showStatus('发送失败: ' + (errorData.message || '服务器错误'), 'error');
                            sendBtn.disabled = false;
                            sendBtn.innerHTML = '<span>🚀 发送弹幕到 Minecraft</span>';
                        }
                    } catch (error) {
                        console.error('发送错误:', error);
                        showStatus('网络连接失败，请检查Minecraft是否正在运行', 'error');
                        sendBtn.disabled = false;
                        sendBtn.innerHTML = '<span>🚀 发送弹幕到 Minecraft</span>';
                    }
                }
                
                // 添加到历史记录
                function addToHistory(sender, message, color) {
                    const now = new Date();
                    const timeString = now.toLocaleTimeString('zh-CN', {
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit'
                    });
                    
                    const historyItem = {
                        sender: sender,
                        message: message,
                        color: color,
                        time: timeString,
                        timestamp: now.getTime()
                    };
                    
                    messageHistory.unshift(historyItem);
                    
                    // 限制历史记录数量
                    if (messageHistory.length > MAX_HISTORY) {
                        messageHistory = messageHistory.slice(0, MAX_HISTORY);
                    }
                    
                    // 保存到本地存储
                    localStorage.setItem('danmuHistory', JSON.stringify(messageHistory));
                    
                    // 更新历史记录显示
                    updateHistoryDisplay();
                }
                
                // 更新历史记录显示
                function updateHistoryDisplay() {
                    historyList.innerHTML = '';
                    
                    if (messageHistory.length === 0) {
                        historyList.innerHTML = '<div style="text-align: center; color: #888; padding: 20px;">暂无发送历史</div>';
                        return;
                    }
                    
                    messageHistory.forEach(item => {
                        const historyItem = document.createElement('div');
                        historyItem.className = 'history-item';
                        historyItem.innerHTML = `
                            <div style="display: flex; align-items: center; margin-bottom: 5px;">
                                <div style="width: 12px; height: 12px; border-radius: 50%; background-color: ${item.color}; margin-right: 8px;"></div>
                                <span class="history-sender">${escapeHtml(item.sender)}</span>
                            </div>
                            <div class="history-message">${escapeHtml(item.message)}</div>
                            <div class="history-time">${item.time}</div>
                        `;
                        
                        // 点击历史记录可以重新发送
                        historyItem.addEventListener('click', () => {
                            senderInput.value = item.sender;
                            colorInput.value = item.color;
                            selectedColor = item.color;
                            messageInput.value = item.message;
                            updateCharCount();
                            messageInput.focus();
                            
                            // 更新颜色选择器
                            colorOptions.forEach(o => o.classList.remove('selected'));
                            const matchingColor = Array.from(colorOptions).find(opt => 
                                opt.dataset.color === item.color
                            );
                            if (matchingColor) {
                                matchingColor.classList.add('selected');
                            }
                            
                            showStatus('已加载历史消息，按 Enter 发送', 'info');
                        });
                        
                        historyList.appendChild(historyItem);
                    });
                }
                
                // 清空消息
                function clearMessage() {
                    messageInput.value = '';
                    updateCharCount();
                    messageInput.focus();
                    showStatus('消息已清空', 'info');
                }
                
                // 清空历史记录
                function clearHistory() {
                    if (messageHistory.length > 0) {
                        if (confirm('确定要清空所有发送历史吗？')) {
                            messageHistory = [];
                            localStorage.removeItem('danmuHistory');
                            updateHistoryDisplay();
                            showStatus('历史记录已清空', 'info');
                        }
                    } else {
                        showStatus('历史记录已是空的', 'info');
                    }
                }
                
                // 显示状态消息
                function showStatus(text, type) {
                    statusDiv.textContent = text;
                    statusDiv.className = 'status ' + type;
                    
                    // 自动隐藏信息类消息
                    if (type === 'info') {
                        setTimeout(() => {
                            statusDiv.className = 'status';
                            statusDiv.textContent = '';
                        }, 3000);
                    }
                }
                
                // 检查连接状态
                async function checkConnection() {
                    try {
                        const response = await fetch('/danmu', { 
                            method: 'HEAD',
                            headers: { 'Cache-Control': 'no-cache' }
                        });
                        
                        if (response.ok || response.status === 405) {
                            connectionDot.style.backgroundColor = '#28a745';
                            connectionText.textContent = '已连接到 Minecraft';
                            connectionStatus.className = 'status success';
                        } else {
                            connectionDot.style.backgroundColor = '#ffc107';
                            connectionText.textContent = '连接不稳定';
                            connectionStatus.className = 'status info';
                        }
                    } catch (error) {
                        connectionDot.style.backgroundColor = '#dc3545';
                        connectionText.textContent = '未连接到 Minecraft';
                        connectionStatus.className = 'status error';
                    }
                }
                
                // HTML转义函数
                function escapeHtml(text) {
                    const div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                }
                
                // 初始化页面
                function initPage() {
                    initColorPicker();
                    initCharCounter();
                    initShortcuts();
                    updateHistoryDisplay();
                    
                    // 按钮事件监听
                    sendBtn.addEventListener('click', sendDanmu);
                    clearBtn.addEventListener('click', clearMessage);
                    clearHistoryBtn.addEventListener('click', clearHistory);
                    
                    // 自动聚焦到消息框
                    messageInput.focus();
                    
                    // 定期检查连接状态
                    setInterval(checkConnection, 5000);
                    checkConnection(); // 初始检查
                    
                    // 页面激活时检查连接
                    document.addEventListener('visibilitychange', () => {
                        if (!document.hidden) {
                            checkConnection();
                        }
                    });
                    
                    // 显示欢迎消息
                    setTimeout(() => {
                        showStatus('✨ 欢迎使用 Minecraft 弹幕发送器！在文本框中按 Enter 键即可发送消息。', 'info');
                    }, 1000);
                }
                
                // 页面加载完成后初始化
                document.addEventListener('DOMContentLoaded', initPage);
                
                // 添加一个简单的离线检测
                window.addEventListener('offline', () => {
                    connectionDot.style.backgroundColor = '#dc3545';
                    connectionText.textContent = '网络已断开';
                    connectionStatus.className = 'status error';
                });
                
                window.addEventListener('online', checkConnection);
            </script>
        </body>
        </html>
        """;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            DanmuMod.info("Danmu web server stopped");
        }
    }

    public int getPort() {
        return port;
    }
}