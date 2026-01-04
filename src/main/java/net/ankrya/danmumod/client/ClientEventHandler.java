package net.ankrya.danmumod.client;

import net.ankrya.danmumod.DanmuMod;
import net.ankrya.danmumod.data.DanmuManager;
import net.ankrya.danmumod.server.DanmuWebServer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientEventHandler {
    private static DanmuWebServer webServer;
    private static boolean serverStarted = false;
    private static KeyBinding openBrowserKey;

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
        openBrowserKey = new KeyBinding(
                "key.danmumod.openbrowser",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.danmumod.general"
        );

        KeyBindingHelper.registerKeyBinding(openBrowserKey);
    }

    private static void handleKeyPress() {
        if (openBrowserKey.wasPressed()) {
            openBrowser();
        }
    }

    private static void startWebServer() {
        if (!serverStarted) {
            try {
                webServer = new DanmuWebServer(DanmuManager.getInstance());
                webServer.start();
                serverStarted = true;

                DanmuMod.info("Web server started on port " + webServer.getPort());

                // 自动打开浏览器
//                openBrowser();
            } catch (Exception e) {
                DanmuMod.error("Failed to start web server: " + e.getMessage(), e);
            }
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
                int port = webServer.getPort();
                String url = "http://localhost:" + port;

                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    DanmuMod.info("Opened browser: " + url);
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