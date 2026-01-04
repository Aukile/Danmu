package net.ankrya.danmumod.client.gui;

import net.ankrya.danmumod.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DanmuConfigScreen extends Screen {
    private final Screen parent;
    private EditBox displayTimeField;
    private EditBox portField;
    private EditBox maxDanmusField;

    public DanmuConfigScreen(Screen parent) {
        super(Component.literal("Danmu Mod Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;
        int fieldWidth = 100;
        int fieldHeight = 20;
        int spacing = 25;

        // 显示时间设置
        this.addRenderableWidget(Button.builder(
                        Component.literal("Display Time: " + ModConfig.DISPLAY_TIME.get() + "s"),
                        button -> {
                            int newValue = ModConfig.DISPLAY_TIME.get() + 5;
                            if (newValue > 300) newValue = 5;
                            ModConfig.DISPLAY_TIME.set(newValue);
                            button.setMessage(Component.literal("Display Time: " + newValue + "s"));
                        })
                .pos(centerX - 100, startY)
                .size(200, 20)
                .build()
        );

        startY += spacing;

        // 弹幕速度设置
        this.addRenderableWidget(Button.builder(
                        Component.literal("Speed: " + String.format("%.1f", ModConfig.DANMU_SPEED.get())),
                        button -> {
                            double newValue = ModConfig.DANMU_SPEED.get() + 0.5;
                            if (newValue > 10.0) newValue = 0.5;
                            ModConfig.DANMU_SPEED.set(newValue);
                            button.setMessage(Component.literal("Speed: " + String.format("%.1f", newValue)));
                        })
                .pos(centerX - 100, startY)
                .size(200, 20)
                .build()
        );

        startY += spacing;

        // 最大弹幕数量
        this.addRenderableWidget(Button.builder(
                        Component.literal("Max Danmus: " + ModConfig.MAX_DANMUS.get()),
                        button -> {
                            int newValue = ModConfig.MAX_DANMUS.get() + 10;
                            if (newValue > 200) newValue = 10;
                            ModConfig.MAX_DANMUS.set(newValue);
                            button.setMessage(Component.literal("Max Danmus: " + newValue));
                        })
                .pos(centerX - 100, startY)
                .size(200, 20)
                .build()
        );

        startY += spacing;

        // 返回按钮
        this.addRenderableWidget(Button.builder(
                        Component.literal("Save and Back"),
                        button -> {
                            ModConfig.SPEC.save();
                            Minecraft.getInstance().setScreen(parent);
                        })
                .pos(centerX - 100, this.height - 40)
                .size(200, 20)
                .build()
        );

        // 打开浏览器按钮
        this.addRenderableWidget(Button.builder(
                        Component.literal("Open Browser"),
                        button -> {
                            net.ankrya.danmumod.client.ClientEventHandler.reopenBrowser();
                        })
                .pos(centerX - 100, this.height - 70)
                .size(200, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ModConfig.SPEC.save();
        Minecraft.getInstance().setScreen(parent);
    }
}