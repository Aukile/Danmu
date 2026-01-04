package net.ankrya.danmumod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ankrya.danmumod.data.DanmuData;
import net.ankrya.danmumod.data.DanmuManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Iterator;

public class DanmuRenderer {
    private static final DanmuRenderer INSTANCE = new DanmuRenderer();
    private final Minecraft mc = Minecraft.getInstance();

    private DanmuRenderer() {}

    public static DanmuRenderer getInstance() {
        return INSTANCE;
    }

    public void render(GuiGraphics guiGraphics, float partialTicks) {
        if (mc.options.hideGui) return;

        DanmuManager manager = DanmuManager.getInstance();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float y = screenHeight * 0.1f; // Start from 10% from top
        int index = 0;

        Iterator<DanmuData> iterator = manager.getActiveDanmus().iterator();
        while (iterator.hasNext()) {
            DanmuData danmu = iterator.next();

            if (danmu.x == 0) {
                danmu.x = screenWidth;
                danmu.y = y;
            }

            danmu.x -= 1.0f * danmu.speed;

            String displayText = "<" + danmu.sender + "> " + danmu.message;
            int textWidth = font.width(displayText);

            if (danmu.x + textWidth < 0) {
                iterator.remove();
                continue;
            }

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(danmu.x, danmu.y, 0);

            // Draw shadow
            guiGraphics.drawString(font, Component.literal(displayText), 1, 1, 0x000000, false);
            // Draw text
            guiGraphics.drawString(font, Component.literal(displayText), 0, 0, danmu.getColor(), false);

            poseStack.popPose();

            y += font.lineHeight + 2;
            index++;

            if (y > screenHeight * 0.8f) {
                break; // Don't render too many danmus
            }
        }
    }
}