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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;

public class DanmuWebServer {
    private HttpServer server;
    private final DanmuManager danmuManager;
    private final Gson gson = new Gson();
    private int port = 8080;
    private String localIp = "localhost";

    public DanmuWebServer(DanmuManager manager) {
        this.danmuManager = manager;
    }

    public void start() {
        try {
            // 绑定到所有网络接口 (0.0.0.0)
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            // 获取局域网IP
            localIp = getLocalNetworkIp();

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

            // 添加网络信息端点
            server.createContext("/network", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    JsonObject response = new JsonObject();
                    response.addProperty("local_ip", localIp);
                    response.addProperty("port", port);
                    response.addProperty("url", "http://" + localIp + ":" + port);
                    response.addProperty("qr_code_url", generateQRCodeUrl());

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

            // 打印访问地址
            printNetworkInfo();
        } catch (IOException e) {
            DanmuMod.error("Failed to start web server", e);
        }
    }

    // 获取局域网IP地址
    private String getLocalNetworkIp() {
        try {
            List<String> ipAddresses = new ArrayList<>();

            // 获取所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // 跳过回环接口和未启用的接口
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }

                // 获取接口的IP地址
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // 只取IPv4地址
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();

                        // 跳过一些特殊地址
                        if (!ip.startsWith("169.254") && !ip.startsWith("127.")) {
                            ipAddresses.add(ip);

                            // 优先选择192.168、10.、172.16-31.开头的地址
                            if (ip.startsWith("192.168") || ip.startsWith("10.")) {
                                return ip; // 返回第一个找到的内网IP
                            }
                        }
                    }
                }
            }

            // 如果没有找到特定的内网IP，返回第一个找到的
            if (!ipAddresses.isEmpty()) {
                return ipAddresses.get(0);
            }

            // 获取本机IP作为备选
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            DanmuMod.error("Failed to get local IP address", e);
            return "127.0.0.1";
        }
    }

    // 获取外部IP（备用方法）
    public static String getExternalIp() {
        try (java.util.Scanner scanner = new java.util.Scanner(
                new java.net.URL("https://api.ipify.org").openStream(),
                "UTF-8").useDelimiter("\\A")) {
            return scanner.next();
        } catch (java.io.IOException e) {
            DanmuMod.error("Failed to get external IP", e);
            return null;
        }
    }

    // 生成二维码图片URL
    private String generateQRCodeUrl() {
        String url = "http://" + localIp + ":" + port;
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" +
                URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    // 打印网络信息
    private void printNetworkInfo() {
        DanmuMod.info("=".repeat(50));
        DanmuMod.info("🎮 Minecraft 弹幕服务器已启动！");
        DanmuMod.info("=".repeat(50));
        DanmuMod.info("📱 手机/平板访问地址:");
        DanmuMod.info("  局域网: http://" + localIp + ":" + port);
        DanmuMod.info("  本机:   http://localhost:" + port);

        // 尝试获取外部IP（如果可能）
        try {
            String externalIp = getExternalIp();
            if (externalIp != null && !externalIp.isEmpty()) {
                DanmuMod.info("  外网:   http://" + externalIp + ":" + port);
                DanmuMod.info("⚠️  注意: 外网访问需要路由器端口映射");
            }
        } catch (Exception e) {
            // 忽略外部IP获取失败
        }

        DanmuMod.info("");
        DanmuMod.info("📱 手机扫描二维码访问（推荐）:");
        DanmuMod.info("  二维码: " + generateQRCodeUrl());
        DanmuMod.info("");
        DanmuMod.info("⚙️  配置信息:");
        DanmuMod.info("  端口: " + port);
        DanmuMod.info("  绑定: 0.0.0.0 (所有网络接口)");
        DanmuMod.info("=".repeat(50));

        // 在游戏聊天栏也显示提示
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                    "§a[弹幕模组] §fWeb服务器已启动！手机访问: §bhttp://" + localIp + ":" + port
            ), false);
        }
    }

    // 修改handleDanmuRequest方法，保持原有功能不变
    private void handleDanmuRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            if (json.has("message") && !json.get("message").getAsString().isEmpty()) {
                String message = json.get("message").getAsString();
                String sender = json.has("sender") ? json.get("sender").getAsString() : "lenhuai";
                String color = json.has("color") ? json.get("color").getAsString() : "#FFFFFF";

                MinecraftClient client = MinecraftClient.getInstance();

                if (client.world != null) {
                    if (client.world.isClient()) {
                        // 客户端：发送到服务器
                        try {
                            Networking.sendDanmuToServer(sender, message, color);
                            DanmuMod.info("Sent danmu from mobile: " + sender + " - " + message);
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
                response.addProperty("message", "弹幕发送成功！");

                String responseStr = gson.toJson(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseStr.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseStr.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("status", "error");
                response.addProperty("message", "消息不能为空");

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
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>🎮 Minecraft 弹幕发送器</title>
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    margin: 0;
                    padding: 15px;
                    min-height: 100vh;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                
                .container {
                    background: rgba(255, 255, 255, 0.98);
                    border-radius: 20px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    padding: 25px;
                    width: 100%;
                    max-width: 600px;
                    backdrop-filter: blur(10px);
                    border: 1px solid rgba(255, 255, 255, 0.2);
                }
                
                .header {
                    text-align: center;
                    margin-bottom: 25px;
                }
                
                h1 {
                    color: #333;
                    font-size: 26px;
                    margin-bottom: 10px;
                    background: linear-gradient(45deg, #667eea, #764ba2);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    background-clip: text;
                }
                
                .subtitle {
                    color: #666;
                    font-size: 14px;
                    margin-bottom: 5px;
                }
                
                .network-info {
                    background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
                    color: white;
                    padding: 15px;
                    border-radius: 12px;
                    margin-bottom: 20px;
                    text-align: center;
                    box-shadow: 0 5px 15px rgba(106, 17, 203, 0.3);
                }
                
                .network-info h3 {
                    margin-bottom: 10px;
                    font-size: 16px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;
                }
                
                .qr-section {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    margin: 15px 0;
                }
                
                .qr-code {
                    width: 180px;
                    height: 180px;
                    background: white;
                    padding: 10px;
                    border-radius: 10px;
                    margin-bottom: 10px;
                    box-shadow: 0 5px 15px rgba(0,0,0,0.1);
                }
                
                .connection-status {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 10px;
                    margin: 15px 0;
                    padding: 12px;
                    background: #f8f9fa;
                    border-radius: 10px;
                }
                
                .status-dot {
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                    background: #ccc;
                }
                
                .status-dot.connected {
                    background: #4CAF50;
                    animation: pulse 2s infinite;
                }
                
                @keyframes pulse {
                    0% { opacity: 1; }
                    50% { opacity: 0.5; }
                    100% { opacity: 1; }
                }
                
                .input-group {
                    margin-bottom: 20px;
                }
                
                label {
                    display: block;
                    margin-bottom: 8px;
                    color: #555;
                    font-weight: 600;
                    font-size: 14px;
                }
                
                input, textarea {
                    width: 100%;
                    padding: 14px;
                    border: 2px solid #e0e0e0;
                    border-radius: 10px;
                    font-size: 16px;
                    transition: all 0.3s;
                    box-sizing: border-box;
                    background: #f8f9fa;
                }
                
                textarea {
                    min-height: 120px;
                    resize: vertical;
                    font-family: inherit;
                }
                
                input:focus, textarea:focus {
                    outline: none;
                    border-color: #667eea;
                    background: white;
                    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                }
                
                .color-picker {
                    display: grid;
                    grid-template-columns: repeat(6, 1fr);
                    gap: 8px;
                    margin-top: 5px;
                }
                
                .color-option {
                    width: 35px;
                    height: 35px;
                    border-radius: 8px;
                    cursor: pointer;
                    border: 2px solid transparent;
                    transition: all 0.3s;
                }
                
                .color-option.selected {
                    border-color: #333;
                    transform: scale(1.1);
                    box-shadow: 0 3px 10px rgba(0,0,0,0.2);
                }
                
                .char-count {
                    text-align: right;
                    font-size: 12px;
                    color: #888;
                    margin-top: 5px;
                }
                
                .shortcut-hint {
                    display: flex;
                    justify-content: space-between;
                    margin: 10px 0;
                    font-size: 12px;
                    color: #666;
                    flex-wrap: wrap;
                    gap: 10px;
                }
                
                .shortcut-item {
                    display: flex;
                    align-items: center;
                    gap: 5px;
                }
                
                .shortcut-key {
                    background: #e0e0e0;
                    padding: 2px 8px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-weight: bold;
                }
                
                .button-container {
                    display: flex;
                    gap: 15px;
                    margin-top: 20px;
                }
                
                button {
                    flex: 1;
                    border: none;
                    padding: 16px;
                    border-radius: 10px;
                    font-size: 16px;
                    font-weight: 600;
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
                
                #sendBtn:active {
                    transform: translateY(2px);
                }
                
                #sendBtn:disabled {
                    background: #cccccc;
                    cursor: not-allowed;
                }
                
                #saveBtn {
                    background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
                    color: white;
                }
                
                .status-message {
                    margin-top: 15px;
                    padding: 12px;
                    border-radius: 10px;
                    text-align: center;
                    display: none;
                    animation: slideIn 0.3s;
                }
                
                .status-success {
                    background-color: #d4edda;
                    color: #155724;
                    border: 1px solid #c3e6cb;
                    display: block;
                }
                
                .status-error {
                    background-color: #f8d7da;
                    color: #721c24;
                    border: 1px solid #f5c6cb;
                    display: block;
                }
                
                .status-info {
                    background-color: #d1ecf1;
                    color: #0c5460;
                    border: 1px solid #bee5eb;
                    display: block;
                }
                
                @keyframes slideIn {
                    from { 
                        opacity: 0;
                        transform: translateY(-10px);
                    }
                    to { 
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                
                /* 历史消息面板样式 */
                .history-panel {
                    margin-top: 25px;
                    border-top: 1px solid #e0e0e0;
                    padding-top: 20px;
                    animation: slideUp 0.5s ease-out;
                }
                
                @keyframes slideUp {
                    from {
                        opacity: 0;
                        transform: translateY(20px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                
                .history-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 15px;
                }
                
                .history-title {
                    font-size: 16px;
                    font-weight: 600;
                    color: #333;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }
                
                .history-controls {
                    display: flex;
                    gap: 10px;
                    align-items: center;
                }
                
                .history-control-btn {
                    background: #f0f0f0;
                    border: none;
                    border-radius: 6px;
                    padding: 6px 12px;
                    font-size: 12px;
                    cursor: pointer;
                    transition: all 0.3s;
                    display: flex;
                    align-items: center;
                    gap: 5px;
                }
                
                .history-control-btn:hover {
                    background: #e0e0e0;
                    transform: translateY(-1px);
                }
                
                .history-control-btn:active {
                    transform: translateY(0);
                }
                
                .clear-history-btn {
                    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                    color: white;
                }
                
                .clear-history-btn:hover {
                    background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
                }
                
                .history-container {
                    max-height: 200px;
                    overflow-y: auto;
                    border: 1px solid #e0e0e0;
                    border-radius: 12px;
                    padding: 10px;
                    background: #f8f9fa;
                    box-shadow: inset 0 2px 10px rgba(0,0,0,0.05);
                }
                
                .history-empty {
                    text-align: center;
                    color: #888;
                    padding: 30px;
                    font-size: 14px;
                }
                
                .history-item {
                    padding: 12px;
                    margin-bottom: 10px;
                    background: white;
                    border-radius: 10px;
                    border-left: 4px solid;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
                    transition: all 0.3s;
                    cursor: pointer;
                    position: relative;
                    overflow: hidden;
                }
                
                .history-item:hover {
                    transform: translateX(5px);
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                }
                
                .history-item:active {
                    transform: translateX(5px) scale(0.98);
                }
                
                .history-item-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 8px;
                }
                
                .history-sender {
                    font-weight: 600;
                    font-size: 14px;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }
                
                .history-color-dot {
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    display: inline-block;
                }
                
                .history-time {
                    font-size: 11px;
                    color: #888;
                    background: #f0f0f0;
                    padding: 2px 6px;
                    border-radius: 10px;
                }
                
                .history-message {
                    font-size: 14px;
                    color: #333;
                    line-height: 1.4;
                    word-break: break-word;
                }
                
                .history-item-footer {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-top: 8px;
                    padding-top: 8px;
                    border-top: 1px dashed #eee;
                }
                
                .history-actions {
                    display: flex;
                    gap: 8px;
                }
                
                .history-action-btn {
                    background: none;
                    border: none;
                    padding: 4px 8px;
                    font-size: 11px;
                    cursor: pointer;
                    border-radius: 4px;
                    transition: all 0.3s;
                    display: flex;
                    align-items: center;
                    gap: 4px;
                }
                
                .history-action-btn:hover {
                    background: #f0f0f0;
                }
                
                .resend-btn {
                    color: #667eea;
                }
                
                .delete-btn {
                    color: #f5576c;
                }
                
                .history-tags {
                    display: flex;
                    gap: 5px;
                }
                
                .history-tag {
                    font-size: 10px;
                    padding: 2px 6px;
                    border-radius: 10px;
                    background: #eef2ff;
                    color: #667eea;
                }
                
                .history-filter {
                    margin-bottom: 15px;
                    display: flex;
                    gap: 10px;
                    flex-wrap: wrap;
                }
                
                .filter-btn {
                    padding: 6px 12px;
                    border: 1px solid #e0e0e0;
                    border-radius: 20px;
                    background: white;
                    font-size: 12px;
                    cursor: pointer;
                    transition: all 0.3s;
                }
                
                .filter-btn:hover {
                    border-color: #667eea;
                    color: #667eea;
                }
                
                .filter-btn.active {
                    background: #667eea;
                    color: white;
                    border-color: #667eea;
                }
                
                /* 滚动条样式 */
                .history-container::-webkit-scrollbar {
                    width: 6px;
                }
                
                .history-container::-webkit-scrollbar-track {
                    background: #f1f1f1;
                    border-radius: 3px;
                }
                
                .history-container::-webkit-scrollbar-thumb {
                    background: #c1c1c1;
                    border-radius: 3px;
                }
                
                .history-container::-webkit-scrollbar-thumb:hover {
                    background: #a8a8a8;
                }
                
                /* 移动端优化 */
                @media (max-width: 480px) {
                    body {
                        padding: 10px;
                    }
                    
                    .container {
                        padding: 20px;
                        border-radius: 15px;
                    }
                    
                    h1 {
                        font-size: 22px;
                    }
                    
                    .color-picker {
                        grid-template-columns: repeat(4, 1fr);
                    }
                    
                    .color-option {
                        width: 45px;
                        height: 45px;
                    }
                    
                    .button-container {
                        flex-direction: column;
                    }
                    
                    .history-container {
                        max-height: 150px;
                    }
                    
                    .history-item {
                        padding: 10px;
                    }
                    
                    .history-controls {
                        flex-direction: column;
                        gap: 5px;
                    }
                    
                    .history-control-btn {
                        width: 100%;
                        justify-content: center;
                    }
                }
                
                /* 触摸设备优化 */
                @media (hover: none) and (pointer: coarse) {
                    input, textarea, button {
                        font-size: 16px; /* 防止iOS缩放 */
                    }
                    
                    button {
                        min-height: 50px;
                    }
                    
                    .color-option {
                        min-width: 45px;
                        min-height: 45px;
                    }
                }
                
                /* 动画效果 */
                .history-item-enter {
                    animation: slideInRight 0.3s ease-out;
                }
                
                @keyframes slideInRight {
                    from {
                        opacity: 0;
                        transform: translateX(30px);
                    }
                    to {
                        opacity: 1;
                        transform: translateX(0);
                    }
                }
                
                .history-item-exit {
                    animation: slideOutLeft 0.3s ease-out;
                }
                
                @keyframes slideOutLeft {
                    from {
                        opacity: 1;
                        transform: translateX(0);
                    }
                    to {
                        opacity: 0;
                        transform: translateX(-30px);
                    }
                }
                
                /* 响应式设计 */
                @media (max-width: 480px) {
                    body {
                        padding: 10px;
                    }
                    
                    .container {
                        padding: 20px;
                        border-radius: 15px;
                    }
                    
                    h1 {
                        font-size: 22px;
                    }
                    
                    .color-picker {
                        grid-template-columns: repeat(4, 1fr);
                    }
                    
                    .color-option {
                        width: 45px;
                        height: 45px;
                    }
                    
                    .button-container {
                        flex-direction: column;
                    }
                }
                
                /* 触摸设备优化 */
                @media (hover: none) and (pointer: coarse) {
                    input, textarea, button {
                        font-size: 16px; /* 防止iOS缩放 */
                    }
                    
                    button {
                        min-height: 50px;
                    }
                    
                    .color-option {
                        min-width: 45px;
                        min-height: 45px;
                    }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🎮 Minecraft 弹幕发送器</h1>
                    <div class="subtitle">在手机或电脑上输入，实时显示在游戏中</div>
                </div>
                
                <div class="network-info">
                    <h3>📱 手机访问地址</h3>
                    <div id="networkAddress" style="font-family: monospace; font-size: 14px; word-break: break-all; padding: 8px; background: rgba(255,255,255,0.1); border-radius: 6px; margin: 8px 0;">
                        正在获取地址...
                    </div>
                    <div class="qr-section">
                        <div class="qr-code" id="qrCodeContainer">
                            <img id="qrCodeImage" style="width: 100%; height: 100%;" alt="QR Code">
                        </div>
                        <div style="font-size: 12px; color: rgba(255,255,255,0.8);">扫描二维码快速访问</div>
                    </div>
                </div>
                
                <div class="connection-status">
                    <div class="status-dot" id="connectionDot"></div>
                    <span id="connectionText">正在连接到 Minecraft...</span>
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
                    </div>
                    <input type="text" id="color" value="#FFFFFF" style="margin-top: 10px; display: none;">
                </div>
                
                <div class="input-group">
                    <label for="message">弹幕消息:</label>
                    <textarea id="message" placeholder="请输入要发送的消息..." 
                              style="font-size: 16px; -webkit-appearance: none;"></textarea>
                    <div class="char-count">
                        <span id="charCount">0/200 字符</span>
                    </div>
                </div>
                
                <div class="shortcut-hint">
                    <div class="shortcut-item">
                        <span class="shortcut-key">Enter</span>
                        <span>发送消息</span>
                    </div>
                    <div class="shortcut-item">
                        <span class="shortcut-key">Shift+Enter</span>
                        <span>换行</span>
                    </div>
                    <div class="shortcut-item">
                        <span class="shortcut-key">Ctrl+S</span>
                        <span>保存草稿</span>
                    </div>
                </div>
                
                <div class="button-container">
                    <button id="sendBtn">
                        <span>🚀 发送弹幕</span>
                    </button>
                    <button id="saveBtn">
                        <span>💾 保存草稿</span>
                    </button>
                </div>
                
                <div class="status-message" id="statusMessage"></div>
                
                <div class="history-panel">
                    <div class="history-header">
                        <div class="history-title">
                            <span>📜 历史消息</span>
                            <span id="historyCount" style="font-size: 12px; color: #888; background: #f0f0f0; padding: 2px 8px; border-radius: 10px;">0</span>
                        </div>
                        <div class="history-controls">
                            <button id="toggleHistoryBtn" class="history-control-btn">
                                <span id="toggleHistoryIcon">▼</span>
                                <span id="toggleHistoryText">展开</span>
                            </button>
                            <button id="clearHistoryBtn" class="history-control-btn clear-history-btn">
                                <span>🗑️ 清空</span>
                            </button>
                        </div>
                    </div>
                    
                    <div class="history-filter" id="historyFilter" style="display: none;">
                        <button class="filter-btn active" data-filter="all">全部</button>
                        <button class="filter-btn" data-filter="today">今天</button>
                        <button class="filter-btn" data-filter="yesterday">昨天</button>
                        <button class="filter-btn" data-filter="week">本周</button>
                        <button class="filter-btn" data-filter="favorite">收藏</button>
                    </div>
                    
                    <div class="history-container" id="historyContainer" style="display: none; max-height: 0; overflow: hidden; transition: all 0.3s;">
                        <div id="historyList">
                            <!-- 历史消息将在这里动态生成 -->
                        </div>
                    </div>
                </div>
            </div>
            
            <script>
                // 全局变量
                let selectedColor = '#FFFFFF';
                let messageHistory = [];
                const MAX_HISTORY_ITEMS = 50;
                let isHistoryExpanded = false;
                let currentFilter = 'all';
                
                // 从localStorage加载历史消息
                function loadHistory() {
                    try {
                        const saved = localStorage.getItem('danmuHistory');
                        if (saved) {
                            messageHistory = JSON.parse(saved);
                            // 确保数据格式正确
                            messageHistory = messageHistory.filter(item => 
                                item && item.sender && item.message && item.color && item.timestamp
                            );
                            updateHistoryDisplay();
                        }
                    } catch (error) {
                        console.error('加载历史记录失败:', error);
                        messageHistory = [];
                    }
                }
                
                // 保存历史消息到localStorage
                function saveHistory() {
                    try {
                        localStorage.setItem('danmuHistory', JSON.stringify(messageHistory));
                    } catch (error) {
                        console.error('保存历史记录失败:', error);
                    }
                }
                
                // 添加消息到历史
                function addToHistory(sender, message, color) {
                    const now = new Date();
                    const historyItem = {
                        id: Date.now() + Math.random().toString(36).substr(2, 9),
                        sender: sender.trim() || '匿名',
                        message: message.trim(),
                        color: color,
                        timestamp: now.getTime(),
                        date: now.toISOString().split('T')[0],
                        time: now.toLocaleTimeString('zh-CN', { 
                            hour: '2-digit', 
                            minute: '2-digit' 
                        }),
                        favorite: false
                    };
                    
                    // 添加到数组开头
                    messageHistory.unshift(historyItem);
                    
                    // 限制数量
                    if (messageHistory.length > MAX_HISTORY_ITEMS) {
                        messageHistory = messageHistory.slice(0, MAX_HISTORY_ITEMS);
                    }
                    
                    // 保存并更新显示
                    saveHistory();
                    updateHistoryDisplay();
                    
                    // 如果有新消息且面板是展开的，滚动到顶部
                    if (isHistoryExpanded) {
                        setTimeout(() => {
                            const historyList = document.getElementById('historyList');
                            if (historyList.firstChild) {
                                historyList.firstChild.scrollIntoView({
                                    behavior: 'smooth',
                                    block: 'nearest'
                                });
                            }
                        }, 100);
                    }
                }
                
                // 删除历史消息
                function deleteHistoryItem(id) {
                    const index = messageHistory.findIndex(item => item.id === id);
                    if (index !== -1) {
                        // 添加删除动画
                        const itemElement = document.querySelector(`[data-history-id="${id}"]`);
                        if (itemElement) {
                            itemElement.classList.add('history-item-exit');
                            setTimeout(() => {
                                messageHistory.splice(index, 1);
                                saveHistory();
                                updateHistoryDisplay();
                            }, 300);
                        } else {
                            messageHistory.splice(index, 1);
                            saveHistory();
                            updateHistoryDisplay();
                        }
                    }
                }
                
                // 切换收藏状态
                function toggleFavorite(id) {
                    const item = messageHistory.find(item => item.id === id);
                    if (item) {
                        item.favorite = !item.favorite;
                        saveHistory();
                        updateHistoryDisplay();
                    }
                }
                
                // 重新发送历史消息
                function resendHistoryItem(id) {
                    const item = messageHistory.find(item => item.id === id);
                    if (item) {
                        // 填充到表单
                        document.getElementById('sender').value = item.sender;
                        document.getElementById('message').value = item.message;
                        
                        // 设置颜色
                        selectedColor = item.color;
                        document.getElementById('color').value = selectedColor;
                        
                        // 更新颜色选择器
                        document.querySelectorAll('.color-option').forEach(option => {
                            option.classList.remove('selected');
                            if (option.dataset.color === selectedColor) {
                                option.classList.add('selected');
                            }
                        });
                        
                        // 聚焦到消息框
                        document.getElementById('message').focus();
                        updateCharCount();
                        
                        showStatus('已加载历史消息，按 Enter 发送', 'info');
                    }
                }
                
                // 清空所有历史消息
                function clearAllHistory() {
                    if (messageHistory.length === 0) {
                        showStatus('历史记录已是空的', 'info');
                        return;
                    }
                    
                    if (confirm(`确定要清空所有历史记录吗？\\n(共 ${messageHistory.length} 条消息)`)) {
                        messageHistory = [];
                        saveHistory();
                        updateHistoryDisplay();
                        showStatus('历史记录已清空', 'success');
                    }
                }
                
                // 过滤历史消息
                function filterHistory(filterType) {
                    currentFilter = filterType;
                    updateHistoryDisplay();
                    
                    // 更新过滤器按钮状态
                    document.querySelectorAll('.filter-btn').forEach(btn => {
                        btn.classList.remove('active');
                        if (btn.dataset.filter === filterType) {
                            btn.classList.add('active');
                        }
                    });
                }
                
                // 根据时间过滤消息
                function filterByTime(item) {
                    const now = new Date();
                    const itemDate = new Date(item.timestamp);
                    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                    const yesterday = new Date(today.getTime() - 86400000);
                    
                    switch (currentFilter) {
                        case 'today':
                            return itemDate >= today;
                        case 'yesterday':
                            return itemDate >= yesterday && itemDate < today;
                        case 'week':
                            const weekAgo = new Date(now.getTime() - 7 * 86400000);
                            return itemDate >= weekAgo;
                        case 'favorite':
                            return item.favorite;
                        default:
                            return true;
                    }
                }
                
                // 更新历史消息显示
                function updateHistoryDisplay() {
                    const historyList = document.getElementById('historyList');
                    const historyCount = document.getElementById('historyCount');
                    
                    // 更新计数
                    historyCount.textContent = messageHistory.length;
                    
                    if (messageHistory.length === 0) {
                        historyList.innerHTML = `
                            <div class="history-empty">
                                <div style="font-size: 48px; margin-bottom: 10px;">📝</div>
                                <div>暂无历史消息</div>
                                <div style="font-size: 12px; margin-top: 5px; color: #aaa;">发送的消息会在这里保存</div>
                            </div>
                        `;
                        return;
                    }
                    
                    // 过滤消息
                    const filteredHistory = messageHistory.filter(filterByTime);
                    
                    if (filteredHistory.length === 0) {
                        historyList.innerHTML = `
                            <div class="history-empty">
                                <div style="font-size: 48px; margin-bottom: 10px;">🔍</div>
                                <div>没有找到符合条件的消息</div>
                                <div style="font-size: 12px; margin-top: 5px; color: #aaa;">试试其他筛选条件</div>
                            </div>
                        `;
                        return;
                    }
                    
                    // 生成历史消息列表
                    historyList.innerHTML = filteredHistory.map(item => {
                        const timeAgo = getTimeAgo(item.timestamp);
                        
                        return `
                            <div class="history-item history-item-enter" data-history-id="${item.id}" 
                                 style="border-left-color: ${item.color}">
                                <div class="history-item-header">
                                    <div class="history-sender">
                                        <span class="history-color-dot" style="background-color: ${item.color}"></span>
                                        ${escapeHtml(item.sender)}
                                    </div>
                                    <div class="history-time" title="${new Date(item.timestamp).toLocaleString('zh-CN')}">
                                        ${timeAgo}
                                    </div>
                                </div>
                                <div class="history-message">
                                    ${escapeHtml(item.message)}
                                </div>
                                <div class="history-item-footer">
                                    <div class="history-tags">
                                        ${item.favorite ? '<span class="history-tag" style="background: #fff3cd; color: #856404;">⭐ 收藏</span>' : ''}
                                        <span class="history-tag">${item.time}</span>
                                    </div>
                                    <div class="history-actions">
                                        <button class="history-action-btn resend-btn" onclick="event.stopPropagation(); resendHistoryItem('${item.id}')">
                                            <span>↻ 重新发送</span>
                                        </button>
                                        <button class="history-action-btn" onclick="event.stopPropagation(); toggleFavorite('${item.id}')" 
                                                style="color: ${item.favorite ? '#FFD700' : '#ccc'}">
                                            <span>${item.favorite ? '★' : '☆'} 收藏</span>
                                        </button>
                                        <button class="history-action-btn delete-btn" onclick="event.stopPropagation(); deleteHistoryItem('${item.id}')">
                                            <span>🗑️ 删除</span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        `;
                    }).join('');
                    
                    // 添加点击事件
                    document.querySelectorAll('.history-item').forEach(item => {
                        item.addEventListener('click', function(e) {
                            if (!e.target.closest('.history-actions')) {
                                const id = this.dataset.historyId;
                                resendHistoryItem(id);
                            }
                        });
                        
                        // 触摸设备优化
                        item.addEventListener('touchstart', (e) => {
                            e.preventDefault();
                            if (!e.target.closest('.history-actions')) {
                                const id = item.dataset.historyId;
                                resendHistoryItem(id);
                            }
                        });
                    });
                }
                
                // 计算时间差
                function getTimeAgo(timestamp) {
                    const now = new Date();
                    const past = new Date(timestamp);
                    const diff = now - past;
                    
                    const minutes = Math.floor(diff / 60000);
                    const hours = Math.floor(diff / 3600000);
                    const days = Math.floor(diff / 86400000);
                    
                    if (minutes < 1) return '刚刚';
                    if (minutes < 60) return `${minutes}分钟前`;
                    if (hours < 24) return `${hours}小时前`;
                    if (days < 7) return `${days}天前`;
                    
                    return past.toLocaleDateString('zh-CN');
                }
                
                // 切换历史消息面板
                function toggleHistoryPanel() {
                    isHistoryExpanded = !isHistoryExpanded;
                    const container = document.getElementById('historyContainer');
                    const filter = document.getElementById('historyFilter');
                    const icon = document.getElementById('toggleHistoryIcon');
                    const text = document.getElementById('toggleHistoryText');
                    
                    if (isHistoryExpanded) {
                        container.style.display = 'block';
                        container.style.maxHeight = '200px';
                        filter.style.display = 'flex';
                        icon.textContent = '▲';
                        text.textContent = '收起';
                        
                        // 确保消息加载
                        updateHistoryDisplay();
                    } else {
                        container.style.maxHeight = '0';
                        filter.style.display = 'none';
                        icon.textContent = '▼';
                        text.textContent = '展开';
                        
                        setTimeout(() => {
                            container.style.display = 'none';
                        }, 300);
                    }
                }
                
                // 保存草稿
                function saveDraft() {
                    const sender = document.getElementById('sender').value.trim() || '草稿';
                    const message = document.getElementById('message').value.trim();
                    
                    if (!message) {
                        showStatus('请输入消息内容才能保存草稿', 'error');
                        return;
                    }
                    
                    const now = new Date();
                    const draftItem = {
                        id: 'draft_' + Date.now(),
                        sender: sender,
                        message: message,
                        color: selectedColor,
                        timestamp: now.getTime(),
                        date: now.toISOString().split('T')[0],
                        time: now.toLocaleTimeString('zh-CN', { 
                            hour: '2-digit', 
                            minute: '2-digit' 
                        }),
                        favorite: false,
                        isDraft: true
                    };
                    
                    // 添加到历史
                    messageHistory.unshift(draftItem);
                    if (messageHistory.length > MAX_HISTORY_ITEMS) {
                        messageHistory = messageHistory.slice(0, MAX_HISTORY_ITEMS);
                    }
                    
                    saveHistory();
                    updateHistoryDisplay();
                    showStatus('草稿已保存到历史记录', 'success');
                    
                    // 振动反馈
                    if (navigator.vibrate) navigator.vibrate([50, 50, 50]);
                }
                
                // 导出历史记录
                function exportHistory() {
                    if (messageHistory.length === 0) {
                        showStatus('没有历史记录可导出', 'info');
                        return;
                    }
                    
                    const exportData = {
                        version: '1.0',
                        exportDate: new Date().toISOString(),
                        totalItems: messageHistory.length,
                        history: messageHistory
                    };
                    
                    const dataStr = JSON.stringify(exportData, null, 2);
                    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
                    
                    const exportFileDefaultName = `danmu_history_${new Date().toISOString().split('T')[0]}.json`;
                    
                    const linkElement = document.createElement('a');
                    linkElement.setAttribute('href', dataUri);
                    linkElement.setAttribute('download', exportFileDefaultName);
                    linkElement.click();
                    
                    showStatus(`已导出 ${messageHistory.length} 条历史记录`, 'success');
                }
                
                // 导入历史记录
                function importHistory() {
                    const input = document.createElement('input');
                    input.type = 'file';
                    input.accept = '.json';
                    
                    input.onchange = e => {
                        const file = e.target.files[0];
                        if (!file) return;
                        
                        const reader = new FileReader();
                        reader.onload = event => {
                            try {
                                const importedData = JSON.parse(event.target.result);
                                
                                if (!importedData.history || !Array.isArray(importedData.history)) {
                                    throw new Error('无效的历史记录文件格式');
                                }
                                
                                // 合并历史记录
                                const newItems = importedData.history.filter(newItem => 
                                    !messageHistory.some(existingItem => 
                                        existingItem.id === newItem.id || 
                                        (existingItem.timestamp === newItem.timestamp && 
                                         existingItem.message === newItem.message)
                                    )
                                );
                                
                                if (newItems.length === 0) {
                                    showStatus('没有新的历史记录可导入', 'info');
                                    return;
                                }
                                
                                messageHistory = [...newItems, ...messageHistory];
                                
                                // 限制数量
                                if (messageHistory.length > MAX_HISTORY_ITEMS * 2) {
                                    messageHistory = messageHistory.slice(0, MAX_HISTORY_ITEMS);
                                    showStatus(`已导入部分历史记录（最多保留 ${MAX_HISTORY_ITEMS} 条）`, 'info');
                                } else {
                                    showStatus(`已导入 ${newItems.length} 条历史记录`, 'success');
                                }
                                
                                saveHistory();
                                updateHistoryDisplay();
                            } catch (error) {
                                console.error('导入失败:', error);
                                showStatus('导入失败：文件格式错误', 'error');
                            }
                        };
                        
                        reader.readAsText(file);
                    };
                    
                    input.click();
                }
                
                // 搜索历史消息
                function searchHistory() {
                    const searchTerm = prompt('请输入要搜索的关键词:');
                    if (!searchTerm) return;
                    
                    const results = messageHistory.filter(item => 
                        item.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        item.sender.toLowerCase().includes(searchTerm.toLowerCase())
                    );
                    
                    if (results.length === 0) {
                        showStatus(`没有找到包含"${searchTerm}"的历史消息`, 'info');
                        return;
                    }
                    
                    // 临时显示搜索结果
                    const originalFilter = currentFilter;
                    const originalHistory = [...messageHistory];
                    
                    messageHistory = results;
                    currentFilter = 'all';
                    updateHistoryDisplay();
                    
                    showStatus(`找到 ${results.length} 条相关消息`, 'success');
                    
                    // 5秒后恢复
                    setTimeout(() => {
                        messageHistory = originalHistory;
                        currentFilter = originalFilter;
                        updateHistoryDisplay();
                        showStatus('已恢复显示所有历史消息', 'info');
                    }, 5000);
                }
                
                // 获取网络信息
                async function fetchNetworkInfo() {
                    try {
                        const response = await fetch('/network');
                        if (response.ok) {
                            const data = await response.json();
                            document.getElementById('networkAddress').textContent = data.url;
                            
                            // 生成二维码图片
                            const qrCodeImg = document.getElementById('qrCodeImage');
                            qrCodeImg.src = data.qr_code_url;
                            qrCodeImg.style.display = 'block';
                        }
                    } catch (error) {
                        console.error('Failed to fetch network info:', error);
                        document.getElementById('networkAddress').textContent = 
                            window.location.hostname + ':' + window.location.port;
                    }
                }
                
                // 初始化颜色选择器
                const colorOptions = document.querySelectorAll('.color-option');
                
                colorOptions.forEach(option => {
                    option.addEventListener('click', () => {
                        colorOptions.forEach(o => o.classList.remove('selected'));
                        option.classList.add('selected');
                        selectedColor = option.dataset.color;
                        document.getElementById('color').value = selectedColor;
                    });
                    
                    // 触摸设备优化
                    option.addEventListener('touchstart', (e) => {
                        e.preventDefault();
                        option.click();
                    });
                });
                
                // 初始化第一个颜色
                if (colorOptions.length > 0) {
                    colorOptions[0].classList.add('selected');
                    selectedColor = colorOptions[0].dataset.color;
                    document.getElementById('color').value = selectedColor;
                }
                
                // 字符计数器
                const messageInput = document.getElementById('message');
                const charCount = document.getElementById('charCount');
                
                function updateCharCount() {
                    const length = messageInput.value.length;
                    charCount.textContent = length + '/200 字符';
                    
                    if (length > 200) {
                        charCount.style.color = '#FF5555';
                    } else if (length > 180) {
                        charCount.style.color = '#FFAA00';
                    } else {
                        charCount.style.color = '#888';
                    }
                    
                    // 更新发送按钮状态
                    document.getElementById('sendBtn').disabled = length === 0 || length > 200;
                }
                
                messageInput.addEventListener('input', updateCharCount);
                
                // 回车发送功能
                messageInput.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        if (!document.getElementById('sendBtn').disabled) {
                            sendDanmu();
                        }
                    }
                });
                
                // 发送弹幕
                async function sendDanmu() {
                    const sender = document.getElementById('sender').value.trim() || 'lenhuai';
                    const message = messageInput.value.trim();
                    
                    if (!message) {
                        showStatus('请输入消息内容！', 'error');
                        messageInput.focus();
                        return;
                    }
                    
                    if (message.length > 200) {
                        showStatus('消息过长，最多200个字符！', 'error');
                        return;
                    }
                    
                    // 禁用发送按钮，防止重复发送
                    const sendBtn = document.getElementById('sendBtn');
                    const originalText = sendBtn.innerHTML;
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
                        
                        const result = await response.json();
                        
                        if (response.ok) {
                            // 添加到历史记录
                            addToHistory(sender, message, selectedColor);
                            
                            // 清空消息框
                            messageInput.value = '';
                            messageInput.focus();
                            updateCharCount();
                            
                            showStatus('弹幕发送成功！🎉', 'success');
                            
                            // 振动反馈
                            if (navigator.vibrate) {
                                navigator.vibrate([100]);
                            }
                        } else {
                            showStatus('发送失败: ' + (result.message || '服务器错误'), 'error');
                        }
                    } catch (error) {
                        console.error('发送错误:', error);
                        showStatus('网络连接失败，请检查连接', 'error');
                        
                        // 即使网络失败也保存到本地历史
                        addToHistory(sender, message + ' (发送失败)', selectedColor);
                    } finally {
                        // 恢复发送按钮
                        setTimeout(() => {
                            sendBtn.disabled = false;
                            sendBtn.innerHTML = originalText;
                        }, 1000);
                    }
                }
                
                // 显示状态消息
                function showStatus(message, type) {
                    const statusDiv = document.getElementById('statusMessage');
                    statusDiv.textContent = message;
                    statusDiv.className = 'status-message status-' + type;
                    
                    // 自动隐藏
                    setTimeout(() => {
                        statusDiv.className = 'status-message';
                        statusDiv.textContent = '';
                    }, 3000);
                }
                
                // 检查连接状态
                async function checkConnection() {
                    try {
                        const response = await fetch('/danmu', { 
                            method: 'HEAD',
                            headers: { 'Cache-Control': 'no-cache' }
                        });
                        
                        const dot = document.getElementById('connectionDot');
                        const text = document.getElementById('connectionText');
                        
                        if (response.ok || response.status === 405) {
                            dot.className = 'status-dot connected';
                            text.textContent = '已连接到 Minecraft';
                        } else {
                            dot.className = 'status-dot';
                            text.textContent = '连接不稳定';
                        }
                    } catch (error) {
                        const dot = document.getElementById('connectionDot');
                        const text = document.getElementById('connectionText');
                        dot.className = 'status-dot';
                        text.textContent = '未连接到 Minecraft';
                    }
                }
                
                // 初始化历史消息功能
                function initHistoryFeatures() {
                    // 加载历史记录
                    loadHistory();
                    
                    // 切换历史面板按钮
                    document.getElementById('toggleHistoryBtn').addEventListener('click', toggleHistoryPanel);
                    
                    // 清空历史按钮
                    document.getElementById('clearHistoryBtn').addEventListener('click', clearAllHistory);
                    
                    // 过滤器按钮
                    document.querySelectorAll('.filter-btn').forEach(btn => {
                        btn.addEventListener('click', () => {
                            filterHistory(btn.dataset.filter);
                        });
                    });
                    
                    // 保存草稿按钮
                    document.getElementById('saveBtn').addEventListener('click', saveDraft);
                    
                    // 快捷键：Ctrl+S 保存草稿
                    document.addEventListener('keydown', e => {
                        if (e.ctrlKey && e.key === 's') {
                            e.preventDefault();
                            saveDraft();
                        }
                        
                        // Ctrl+F 搜索历史
                        if (e.ctrlKey && e.key === 'f') {
                            e.preventDefault();
                            searchHistory();
                        }
                        
                        // Ctrl+E 导出历史
                        if (e.ctrlKey && e.key === 'e') {
                            e.preventDefault();
                            exportHistory();
                        }
                        
                        // Ctrl+I 导入历史
                        if (e.ctrlKey && e.key === 'i') {
                            e.preventDefault();
                            importHistory();
                        }
                    });
                    
                    // 自动展开历史面板（如果有历史记录）
                    if (messageHistory.length > 0) {
                        setTimeout(() => {
                            toggleHistoryPanel();
                        }, 1000);
                    }
                }
                
                // 初始化页面
                function initPage() {
                    // 获取网络信息
                    fetchNetworkInfo();
                    
                    // 设置按钮事件
                    document.getElementById('sendBtn').addEventListener('click', sendDanmu);
                    
                    // 触摸设备优化
                    document.getElementById('sendBtn').addEventListener('touchstart', (e) => {
                        e.preventDefault();
                        if (!document.getElementById('sendBtn').disabled) {
                            document.getElementById('sendBtn').click();
                        }
                    });
                    
                    // 初始化历史消息功能
                    initHistoryFeatures();
                    
                    // 自动聚焦到消息框（如果不是触摸设备）
                    if (!('ontouchstart' in window || navigator.maxTouchPoints > 0)) {
                        messageInput.focus();
                    }
                    
                    // 定期检查连接状态
                    setInterval(checkConnection, 5000);
                    checkConnection(); // 初始检查
                    
                    // 页面激活时检查连接
                    document.addEventListener('visibilitychange', () => {
                        if (!document.hidden) {
                            checkConnection();
                            fetchNetworkInfo();
                        }
                    });
                    
                    // 监听网络状态变化
                    window.addEventListener('online', checkConnection);
                    window.addEventListener('offline', () => {
                        const dot = document.getElementById('connectionDot');
                        const text = document.getElementById('connectionText');
                        dot.className = 'status-dot';
                        text.textContent = '网络已断开';
                    });
                    
                    // 初始化字符计数
                    updateCharCount();
                    
                    // 显示欢迎消息
                    setTimeout(() => {
                        showStatus('✨ 欢迎使用 Minecraft 弹幕发送器！在文本框中按 Enter 键即可发送消息。', 'info');
                    }, 1000);
                }
                
                // HTML转义函数
                function escapeHtml(text) {
                    const div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                }
                
                // 防止双击缩放
                let lastTouchEnd = 0;
                document.addEventListener('touchend', function(event) {
                    const now = (new Date()).getTime();
                    if (now - lastTouchEnd <= 300) {
                        event.preventDefault();
                    }
                    lastTouchEnd = now;
                }, false);
                
                // 页面加载完成后初始化
                document.addEventListener('DOMContentLoaded', initPage);
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

    public String getLocalIp() {
        return localIp;
    }
}