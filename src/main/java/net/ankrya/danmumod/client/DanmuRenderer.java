package net.ankrya.danmumod.client;

import net.ankrya.danmumod.data.DanmuData;
import net.ankrya.danmumod.data.DanmuManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Iterator;

public class DanmuRenderer {
    private static final DanmuRenderer INSTANCE = new DanmuRenderer();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private DanmuRenderer() {}

    public static DanmuRenderer getInstance() {
        return INSTANCE;
    }

    public void render(DrawContext context, float tickDelta) {
        if (mc.options.hudHidden) {
            return;
        }

        DanmuManager manager = DanmuManager.getInstance();
        TextRenderer font = mc.textRenderer;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float y = screenHeight * 0.1f;
        int index = 0;

        Iterator<DanmuData> iterator = manager.getActiveDanmus().iterator();
        while (iterator.hasNext()) {
            DanmuData danmu = iterator.next();

            if (danmu.x == 0) {
                danmu.x = screenWidth;
                danmu.y = y;
            }

            // 设置速度
            danmu.speed = (float) net.ankrya.danmumod.config.ModConfig.getConfig().danmuSpeed;
            danmu.x -= 0.8f * danmu.speed;

            String displayText = "<" + danmu.sender + "> " + danmu.message;
            int textWidth = font.getWidth(displayText);

            if (danmu.x + textWidth < 0) {
                iterator.remove();
                continue;
            }

            // 绘制文本阴影
            context.drawText(font, Text.literal(displayText), (int)danmu.x + 1, (int)danmu.y + 1, 0x000000, false);

            // 绘制文本
            try {
                int color = Integer.parseInt(danmu.color.replace("#", ""), 16);
                context.drawText(font, Text.literal(displayText), (int)danmu.x, (int)danmu.y, color, false);
            } catch (NumberFormatException e) {
                // 默认白色
                context.drawText(font, Text.literal(displayText), (int)danmu.x, (int)danmu.y, 0xFFFFFF, false);
            }

            y += font.fontHeight + 2;
            index++;

            if (y > screenHeight * 0.8f) {
                break;
            }
        }
    }
}