package net.ankrya.danmumod.data;

import net.ankrya.danmumod.DanmuMod;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DanmuManager {
    private static DanmuManager instance;
    private final Queue<DanmuData> activeDanmus = new ConcurrentLinkedQueue<>();
    private final List<DanmuData> danmuHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 100;
    private static final int DISPLAY_TIME = 7000; // 7 seconds

    private DanmuManager() {}

    public static DanmuManager getInstance() {
        if (instance == null) {
            instance = new DanmuManager();
        }
        return instance;
    }

    public void addDanmu(String sender, String message, String color) {
        DanmuData danmu = new DanmuData(sender, message, color, System.currentTimeMillis());
        addDanmu(danmu);
    }

    public void addDanmu(DanmuData danmu) {
        // 添加弹幕到显示列表
        activeDanmus.add(danmu);

        // 添加弹幕到历史记录
        synchronized (danmuHistory) {
            danmuHistory.add(danmu);

            // 限制历史记录大小
            if (danmuHistory.size() > MAX_HISTORY) {
                danmuHistory.remove(0);
            }
        }

        DanmuMod.LOGGER.info("Danmu added: {} - {}", danmu.sender, danmu.message);
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        activeDanmus.removeIf(danmu -> currentTime - danmu.timestamp > DISPLAY_TIME);
    }

    public Queue<DanmuData> getActiveDanmus() {
        return new ConcurrentLinkedQueue<>(activeDanmus);
    }

    public List<DanmuData> getDanmuHistory() {
        synchronized (danmuHistory) {
            return new ArrayList<>(danmuHistory);
        }
    }

    public void clear() {
        activeDanmus.clear();
        synchronized (danmuHistory) {
            danmuHistory.clear();
        }
    }

    // 获取当前服务器信息
    public String getServerInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            return mc.getCurrentServer() != null ?
                    mc.getCurrentServer().ip : "Local Server";
        }
        return "Single Player";
    }
}