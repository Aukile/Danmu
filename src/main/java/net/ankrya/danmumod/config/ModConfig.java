package net.ankrya.danmumod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> WEB_PORT;
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_DANMU;
    public static final ModConfigSpec.ConfigValue<Integer> DISPLAY_TIME;
    public static final ModConfigSpec.ConfigValue<Double> DANMU_SPEED;
    public static final ModConfigSpec.ConfigValue<Integer> MAX_DANMUS;

    static {
        BUILDER.push("Danmu Settings");

        WEB_PORT = BUILDER
                .comment("Web server port (default: 8080)")
                .defineInRange("webPort", 8080, 1024, 65535);

        ENABLE_DANMU = BUILDER
                .comment("Enable danmu display")
                .define("enableDanmu", true);

        DISPLAY_TIME = BUILDER
                .comment("Danmu display time in milliseconds (default: 10000)")
                .defineInRange("displayTime", 10000, 1000, 30000);

        DANMU_SPEED = BUILDER
                .comment("Danmu movement speed (default: 1.0)")
                .defineInRange("danmuSpeed", 1.0, 0.1, 5.0);

        MAX_DANMUS = BUILDER
                .comment("Maximum number of danmus displayed at once (default: 20)")
                .defineInRange("maxDanmus", 20, 1, 100);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}