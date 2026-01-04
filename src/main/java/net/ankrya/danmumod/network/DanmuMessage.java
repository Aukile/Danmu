package net.ankrya.danmumod.network;

import net.ankrya.danmumod.DanmuMod;
import net.ankrya.danmumod.data.DanmuData;
import net.ankrya.danmumod.data.DanmuManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record DanmuMessage(String sender, String message, String color)
        implements CustomPacketPayload {

    public static final Type<DanmuMessage> TYPE = new Type<>(DanmuMod.res("danmu").get());

    public static final StreamCodec<FriendlyByteBuf, DanmuMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DanmuMessage::sender,
                    ByteBufCodecs.STRING_UTF8, DanmuMessage::message,
                    ByteBufCodecs.STRING_UTF8, DanmuMessage::color,
                    DanmuMessage::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 客户端接收处理
    public static void handleClient(final DanmuMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            DanmuMod.LOGGER.info("Client received danmu: {} - {}", message.sender(), message.message());
            DanmuManager.getInstance().addDanmu(
                    new DanmuData(message.sender(), message.message(), message.color(),
                            System.currentTimeMillis())
            );
        });
    }

    // 服务器接收处理
    public static void handleServer(final DanmuMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player sender = context.player();
            String finalSender = message.sender();
            String finalMessage = message.message();
            String finalColor = message.color();

            DanmuMod.LOGGER.info("Server received danmu from {}: {}", finalSender, finalMessage);

            // 广播给所有玩家
            DanmuMessage broadcast = new DanmuMessage(finalSender, finalMessage, finalColor);
            PacketDistributor.sendToAllPlayers(broadcast);
        });
    }
}