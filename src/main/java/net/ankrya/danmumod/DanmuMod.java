package net.ankrya.danmumod;

import net.ankrya.danmumod.config.ModConfig;
import net.ankrya.danmumod.network.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class DanmuMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "danmu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path CONFIG_PATH;

    @Override
    public void onInitialize() {
        LOGGER.info("Danmu Mod (Server) Initializing");
        CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

        // 初始化网络
        Networking.initialize();
        Networking.registerServerReceiver();

        // 注册服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping");
        });
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Danmu Mod (Client) Initializing");

        // 加载配置
        ModConfig.loadConfig();

        // 注册客户端网络接收器
        Networking.registerClientReceiver();

        // 注册HUD渲染
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            net.ankrya.danmumod.client.DanmuRenderer.getInstance().render(drawContext, tickDelta.getTickDelta(true));
        });

        // 注册客户端tick事件
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            net.ankrya.danmumod.data.DanmuManager.getInstance().update();
        });

        // 初始化客户端事件处理器
        net.ankrya.danmumod.client.ClientEventHandler.initialize();
    }

    public static void info(String message) {
        LOGGER.info("[DanmuMod] " + message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error("[DanmuMod] " + message, throwable);
    }

    public static void error(String message) {
        LOGGER.error("[DanmuMod] " + message);
    }
}