package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.EntityClearer;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

public class MetricsUtils {
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void send() {
        // Interval
        int interval = plugin.getConfig().getInt("interval");
        String parsedInterval;
        if (interval <= 0) parsedInterval = "Disabled";
        else if (interval <= 10) parsedInterval = "1-10 Minutes";
        else if (interval <= 30) parsedInterval = "11-30 Minutes";
        else if (interval < 60) parsedInterval = "31-59 Minutes";
        else if (interval < 120) parsedInterval = "1-2 Hours";
        else if (interval < 240) parsedInterval = "2-4 Hours";
        else if (interval < 480) parsedInterval = "4-8 Hours";
        else if (interval < 720) parsedInterval = "8-12 Hours";
        else if (interval < 1440) parsedInterval = "12-24 Hours";
        else parsedInterval = "24+ Hours";
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("interval", () -> parsedInterval));

        // Sound
        String sound = plugin.getConfig().getString("sound");
        EntityClearer.getInstance().getMetrics().addCustomChart(new SimplePie("sound", () -> sound));

        // TPS
        String tpsEnabled = plugin.getConfig().getBoolean("low-tps.enabled") ? "Enabled" : "Disabled";
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("low_tps_check", () -> tpsEnabled));

        // Nearby
        String nearbyEnabled = plugin.getConfig()
            .getBoolean("nearby-entities.enabled") ? "Enabled" : "Disabled";
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("nearby_entities_check", () -> nearbyEnabled));

        // MythicMobs
        String mmEnabled = EntityClearer.getInstance().getMythicPlugin() != null ? "Yes" : "No";
        EntityClearer.getInstance().getMetrics().addCustomChart(new SimplePie("mythicmobs", () -> mmEnabled));
    }
}
