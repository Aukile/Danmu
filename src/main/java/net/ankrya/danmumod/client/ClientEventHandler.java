package net.ankrya.danmumod.client;

import net.ankrya.danmumod.DanmuMod;
import net.ankrya.danmumod.data.DanmuManager;
import net.ankrya.danmumod.server.DanmuWebServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = DanmuMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static DanmuWebServer webServer;
    private static boolean serverStarted = false;
    private static String lastServerAddress = "";

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        // 更新弹幕管理器
        DanmuManager.getInstance().update();

        // 启动Web服务器（只在客户端启动）
        if (!serverStarted) {
            startWebServer();
            serverStarted = true;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        net.ankrya.danmumod.client.DanmuRenderer.getInstance().render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaTicks());
    }

    private static void startWebServer() {
        try {
            webServer = new DanmuWebServer(DanmuManager.getInstance());
            webServer.start();
            DanmuMod.LOGGER.info("Danmu web server started at http://localhost:{}", webServer.getPort());
            DanmuMod.LOGGER.info("Open this URL in browser to send danmu");
        } catch (Exception e) {
            DanmuMod.LOGGER.error("Failed to start web server", e);
        }
    }

    public static void stopWebServer() {
        if (webServer != null) {
            webServer.stop();
            serverStarted = false;
        }
    }

    public static void reopenBrowser() {
        if (webServer != null) {
            try {
                int port = webServer.getPort();
                String url = "http://localhost:" + port;

                // 使用系统默认浏览器打开URL
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    DanmuMod.LOGGER.info("Opened browser: {}", url);
                } else {
                    DanmuMod.LOGGER.warn("Cannot open browser automatically. Please open manually: {}", url);
                }
            } catch (Exception e) {
                DanmuMod.LOGGER.error("Failed to open browser", e);
            }
        } else {
            DanmuMod.LOGGER.error("Web server not started yet");
        }
    }
}