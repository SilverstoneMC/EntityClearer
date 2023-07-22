package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class Utils {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public static boolean debug = false;
    public static BukkitTask savedKillTask;
    public static FileWriter debugFile;
    public static long nextKillTask = -1;

    public void killTimer() {
        if (debug) {
            plugin.getLogger().info("╔══════════════════════════════════════╗");
            plugin.getLogger().info("║        STARTING REMOVAL TIMER        ║");
            plugin.getLogger().info("╚══════════════════════════════════════╝");
        }

        if (plugin.getConfig().getInt("interval") < 1) {
            if (debug) plugin.getLogger()
                .warning("The interval is set to a value less than 1, so it's been disabled!");
            return;
        }

        long interval = plugin.getConfig().getInt("interval") * 60L * 20;
        setNextKillTask(interval);

        savedKillTask = new BukkitRunnable() {
            @Override
            public void run() {
                new Countdown().countdown();
                setNextKillTask(interval);
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void setNextKillTask(long interval) {
        // ticks * 50 = ms
        if (EntityClearer.getInstance().getPlaceholderAPI() != null)
            if (interval != -1) nextKillTask = System.currentTimeMillis() + (interval * 50);
    }

    public void logDebug(Level level, String message) {
        if (!debug) return;

        plugin.getLogger().log(level, message);
        try {
            debugFile.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendError(String message) {
        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players)
                .sendMessage(Component.text("[EntityClearer] " + message).color(NamedTextColor.RED));

        logDebug(Level.SEVERE, message);
    }

    public void sendMetrics() {
        // Interval
        int interval = plugin.getConfig().getInt("interval");
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("interval", () -> String.valueOf(interval)));

        // Sound
        String sound = plugin.getConfig().getString("sound");
        EntityClearer.getInstance().getMetrics().addCustomChart(new SimplePie("sound", () -> sound));

        // TPS
        String tpsEnabled;
        if (plugin.getConfig().getBoolean("low-tps.enabled")) tpsEnabled = "Enabled";
        else tpsEnabled = "Disabled";
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("low_tps_check", () -> tpsEnabled));

        // Nearby
        String nearbyEnabled;
        if (plugin.getConfig().getBoolean("nearby-entities.enabled")) nearbyEnabled = "Enabled";
        else nearbyEnabled = "Disabled";
        EntityClearer.getInstance().getMetrics()
            .addCustomChart(new SimplePie("nearby_entities_check", () -> nearbyEnabled));

        // MythicMobs
        String mmEnabled;
        if (EntityClearer.getInstance().getMythicPlugin() != null) mmEnabled = "Yes";
        else mmEnabled = "No";
        EntityClearer.getInstance().getMetrics().addCustomChart(new SimplePie("mythicmobs", () -> mmEnabled));
    }

    public String parseMessage(String message) {
        return message.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
            .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>")
            .replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
            .replace("&k", "<obfuscated>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
    }
}
