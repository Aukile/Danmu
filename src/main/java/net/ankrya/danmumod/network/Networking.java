package net.ankrya.danmumod.network;

import net.ankrya.danmumod.DanmuMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Networking {
    // 定义数据包ID
    public static final CustomPayload.Id<DanmuPayload> DANMU_PACKET_ID =
            new CustomPayload.Id<>(Identifier.of(DanmuMod.MOD_ID, "danmu"));

    // 初始化网络（需要在客户端和服务端都调用）
    public static void initialize() {
        DanmuMod.info("Initializing networking");

        // 注册payload类型（服务端接收客户端发送）
        PayloadTypeRegistry.playC2S().register(DANMU_PACKET_ID, DanmuPayload.PACKET_CODEC);

        // 注册payload类型（客户端接收服务端广播）
        PayloadTypeRegistry.playS2C().register(DANMU_PACKET_ID, DanmuPayload.PACKET_CODEC);
    }

    // 注册服务器端处理器
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(DANMU_PACKET_ID,
                (payload, context) -> handleServerDanmu(payload, context));
    }

    // 注册客户端处理器
    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(DANMU_PACKET_ID,
                (payload, context) -> handleClientDanmu(payload, context));
    }

    // 服务器端处理
    private static void handleServerDanmu(DanmuPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            DanmuMod.info("Server received danmu from " + payload.sender() + ": " + payload.message());

            // 广播给所有玩家
            for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }
        });
    }

    // 客户端处理
    private static void handleClientDanmu(DanmuPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            DanmuMod.info("Client received danmu: " + payload.sender() + " - " + payload.message());
            net.ankrya.danmumod.data.DanmuManager.getInstance().addDanmu(
                    payload.sender(), payload.message(), payload.color()
            );
        });
    }

    // 发送数据包到服务器
    public static void sendDanmuToServer(String sender, String message, String color) {
        DanmuPayload payload = new DanmuPayload(sender, message, color);
        ClientPlayNetworking.send(payload);
    }

    // Payload数据类
    public record DanmuPayload(String sender, String message, String color) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DanmuPayload> PACKET_CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, DanmuPayload::sender,
                        PacketCodecs.STRING, DanmuPayload::message,
                        PacketCodecs.STRING, DanmuPayload::color,
                        DanmuPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return DANMU_PACKET_ID;
        }
    }
}