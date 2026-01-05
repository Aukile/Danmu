package net.ankrya.danmumod.client;

import net.ankrya.danmumod.DanmuMod;
import net.ankrya.danmumod.data.DanmuManager;
import net.ankrya.danmumod.server.DanmuWebServer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class ClientEventHandler {
    private static DanmuWebServer webServer;
    private static boolean serverStarted = false;
    private static Map<String, KeyBinding> keyBindings = new HashMap<>();

    public static void initialize() {
        DanmuMod.info("Initializing client event handlers");

        // 注册客户端停止事件
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            stopWebServer();
        });

        // 注册客户端启动事件
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            startWebServer();
        });

        // 注册按键绑定
        registerKeyBindings();

        // 注册tick事件来检测按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyPress();
        });
    }

    private static void registerKeyBindings() {
        // 打开浏览器
        keyBindings.put("openBrowser", new KeyBinding(
                "key.danmumod.openbrowser",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.danmumod.general"
        ));

        // 显示网络信息
        keyBindings.put("networkInfo", new KeyBinding(
                "key.danmumod.networkinfo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.danmumod.general"
        ));

        // 显示历史消息统计
        keyBindings.put("historyStats", new KeyBinding(
                "key.danmumod.historystats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.danmumod.general"
        ));

        // 注册所有按键绑定
        keyBindings.values().forEach(KeyBindingHelper::registerKeyBinding);
    }

    private static void handleKeyPress() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (keyBindings.get("openBrowser").wasPressed()) {
            openBrowser();
        }

        if (keyBindings.get("networkInfo").wasPressed() && client.player != null) {
            showNetworkInfo();
        }

        if (keyBindings.get("historyStats").wasPressed() && client.player != null) {
            showHistoryStats();
        }
    }

    private static void showNetworkInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && webServer != null) {
            String message = String.format(
                    "§a[弹幕模组] §f网络信息:\n" +
                            "§7• 本地IP: §b%s\n" +
                            "§7• 访问地址: §bhttp://%s:%d\n" +
                            "§7• 二维码: §bhttp://%s:%d/network",
                    webServer.getLocalIp(),
                    webServer.getLocalIp(),
                    webServer.getPort(),
                    webServer.getLocalIp(),
                    webServer.getPort()
            );
            client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }

    private static void showHistoryStats() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String message = String.format(
                    "§a[弹幕模组] §f历史消息功能已启用!\n" +
                            "§7• 发送的消息会自动保存\n" +
                            "§7• 在网页中按 Ctrl+S 保存草稿\n" +
                            "§7• 支持搜索、过滤、收藏功能\n" +
                            "§7• 数据保存在浏览器本地存储"
            );
            client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }

    private static void startWebServer() {
        if (!serverStarted) {
            try {
                webServer = new DanmuWebServer(DanmuManager.getInstance());
                webServer.start();
                serverStarted = true;

                showStartupMessage();
            } catch (Exception e) {
                DanmuMod.error("Failed to start web server: " + e.getMessage(), e);
            }
        }
    }

    private static void showStartupMessage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && webServer != null) {
            String message = String.format(
                    "§a[弹幕模组] §fWeb服务器已启动!\n" +
                            "§7• 手机访问: §bhttp://%s:%d\n" +
                            "§7• 历史消息: §e自动保存发送记录\n" +
                            "§7• 快捷键: §eB§7(打开) §eN§7(网络) §eH§7(历史)"
            );
            client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }

    private static void stopWebServer() {
        if (webServer != null) {
            try {
                webServer.stop();
                serverStarted = false;
                DanmuMod.info("Web server stopped");
            } catch (Exception e) {
                DanmuMod.error("Error stopping web server", e);
            }
        }
    }

    private static void openBrowser() {
        if (webServer != null) {
            try {
                String url = "http://localhost:" + webServer.getPort();
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    DanmuMod.info("Opened browser: " + url);

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(
                                net.minecraft.text.Text.literal("§a[弹幕模组] §f已在浏览器中打开弹幕页面"),
                                false
                        );
                    }
                } else {
                    DanmuMod.info("Please open browser manually: " + url);
                }
            } catch (Exception e) {
                DanmuMod.error("Failed to open browser", e);
            }
        } else {
            DanmuMod.error("Web server not started yet");
        }
    }
}