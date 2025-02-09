package org.littlesheep.utils;

public class TimeFormatter {
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            return "0";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        StringBuilder time = new StringBuilder();
        
        if (days > 0) {
            time.append(days).append("d ");
            hours %= 24;
        }
        if (hours > 0 || days > 0) {
            time.append(hours).append("h ");
            minutes %= 60;
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            time.append(minutes).append("m ");
            seconds %= 60;
        }
        if (seconds > 0 || minutes > 0 || hours > 0 || days > 0) {
            time.append(seconds).append("s");
        }
        
        return time.toString().trim();
    }
} 