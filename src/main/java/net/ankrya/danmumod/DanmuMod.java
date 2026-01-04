package net.ankrya.danmumod;

import com.mojang.logging.LogUtils;
import net.ankrya.danmumod.config.ModConfig;
import net.ankrya.danmumod.network.DanmuMessage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(DanmuMod.MODID)
public class DanmuMod {
    public static final String MODID = "danmu";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DanmuMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerPayloads);

        modContainer.registerConfig(Type.CLIENT, ModConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Danmu Mod Common Setup");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Danmu Mod Client Setup");
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID)
                .versioned("1.0.0")
                .optional();
        registrar.playBidirectional(DanmuMessage.TYPE, DanmuMessage.STREAM_CODEC, new DirectionalPayloadHandler<>(DanmuMessage::handleClient, DanmuMessage::handleServer));
    }

    public static Supplier<net.minecraft.resources.ResourceLocation> res(String path) {
        return () -> net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}