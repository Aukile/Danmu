package net.ankrya.danmumod.data;

import net.ankrya.danmumod.DanmuMod;

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
        // 检查是否启用弹幕
        if (!net.ankrya.danmumod.config.ModConfig.getConfig().enableDanmu) {
            return;
        }

        // 限制最大弹幕数量
        if (activeDanmus.size() >= net.ankrya.danmumod.config.ModConfig.getConfig().maxDanmus) {
            activeDanmus.poll();
        }

        activeDanmus.add(danmu);

        synchronized (danmuHistory) {
            danmuHistory.add(danmu);

            if (danmuHistory.size() > MAX_HISTORY) {
                danmuHistory.remove(0);
            }
        }

        DanmuMod.info("Danmu added: " + danmu.sender + " - " + danmu.message);
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        int displayTime = net.ankrya.danmumod.config.ModConfig.getConfig().displayTime;

        activeDanmus.removeIf(danmu -> currentTime - danmu.timestamp > displayTime);
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
}