//package net.ankrya.danmumod.config;
//
//import com.terraformersmc.modmenu.api.ConfigScreenFactory;
//import com.terraformersmc.modmenu.api.ModMenuApi;
//import me.shedaniel.clothconfig2.api.ConfigBuilder;
//import me.shedaniel.clothconfig2.api.ConfigCategory;
//import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
//import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.text.Text;
//
//public class ModMenuIntegration implements ModMenuApi {
//    @Override
//    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
//        return parent -> {
//            ConfigBuilder builder = ConfigBuilder.create()
//                    .setParentScreen(parent)
//                    .setTitle(Text.translatable("config.danmumod.title"));
//
//            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
//
//            // 获取当前配置
//            ModConfig.ConfigData config = ModConfig.getConfig();
//
//            // 基本设置分类
//            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.danmumod.category.general"));
//
//            // Web服务器端口
//            general.addEntry(entryBuilder.startIntField(
//                            Text.translatable("config.danmumod.port"),
//                            config.webPort)
//                    .setDefaultValue(8080)
//                    .setMin(1024)
//                    .setMax(65535)
//                    .setTooltip(Text.translatable("config.danmumod.port.tooltip"))
//                    .setSaveConsumer(newValue -> {
//                        config.webPort = newValue;
//                    })
//                    .build()
//            );
//
//            // 启用弹幕
//            general.addEntry(entryBuilder.startBooleanToggle(
//                            Text.translatable("config.danmumod.enabled"),
//                            config.enableDanmu)
//                    .setDefaultValue(true)
//                    .setTooltip(Text.translatable("config.danmumod.enabled.tooltip"))
//                    .setSaveConsumer(newValue -> {
//                        config.enableDanmu = newValue;
//                    })
//                    .build()
//            );
//
//            // 弹幕显示时间
//            general.addEntry(entryBuilder.startIntField(
//                            Text.translatable("config.danmumod.displayTime"),
//                            config.displayTime)
//                    .setDefaultValue(5000)
//                    .setMin(1000)
//                    .setMax(30000)
//                    .setTooltip(Text.translatable("config.danmumod.displayTime.tooltip"))
//                    .setSaveConsumer(newValue -> {
//                        config.displayTime = newValue;
//                    })
//                    .build()
//            );
//
//            // 最大弹幕数量
//            general.addEntry(entryBuilder.startIntSlider(
//                            Text.translatable("config.danmumod.maxDanmus"),
//                            config.maxDanmus, 1, 100)
//                    .setDefaultValue(20)
//                    .setTooltip(Text.translatable("config.danmumod.maxDanmus.tooltip"))
//                    .setSaveConsumer(newValue -> {
//                        config.maxDanmus = newValue;
//                    })
//                    .build()
//            );
//
//            // 弹幕速度
//            general.addEntry(entryBuilder.startDoubleField(
//                            Text.translatable("config.danmumod.speed"),
//                            config.danmuSpeed)
//                    .setDefaultValue(1.0)
//                    .setMin(0.1)
//                    .setMax(5.0)
//                    .setTooltip(Text.translatable("config.danmumod.speed.tooltip"))
//                    .setSaveConsumer(newValue -> {
//                        config.danmuSpeed = newValue;
//                    })
//                    .build()
//            );
//
//            // 设置保存回调
//            builder.setSavingRunnable(() -> {
//                ModConfig.setConfig(config);
//            });
//
//            return builder.build();
//        };
//    }
//}