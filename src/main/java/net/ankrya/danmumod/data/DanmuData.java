package net.ankrya.danmumod.data;

public class DanmuData {
    public final String sender;
    public final String message;
    public final String color;
    public final long timestamp;
    public float x;
    public float y;
    public float speed;

    public DanmuData(String sender, String message, String color, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.color = color;
        this.timestamp = timestamp;
        this.speed = 1.0f;
    }

    public int getColor() {
        try {
            return Integer.parseInt(color.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF; // Default white
        }
    }
}