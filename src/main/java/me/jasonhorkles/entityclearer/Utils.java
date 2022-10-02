package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

public class Utils {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public static boolean debug = false;
    public static boolean tpsTimerRan = false;
    public static BukkitTask savedKillTask;
    public static BukkitTask savedTpsTask;
    public static FileWriter debugFile;
    public static final ArrayList<Integer> tickList = new ArrayList<>();

    public void killTimer() {
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("╔══════════════════════════════════════╗");
            plugin.getLogger().info("║        STARTING REMOVAL TIMER        ║");
            plugin.getLogger().info("╚══════════════════════════════════════╝");
        }

        if (plugin.getConfig().getInt("interval") <= 0) {
            if (plugin.getConfig().getBoolean("debug"))
                plugin.getLogger().warning("The interval is set to a value less than 1, so it's been disabled!");
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                new Countdown().countdown();
            }
        };
        savedKillTask = task.runTaskTimer(plugin, ((plugin.getConfig().getInt("interval") * 60L) * 20),
            ((plugin.getConfig().getInt("interval") * 60L) * 20));
    }

    public void logDebug(Level level, String message) {
        if (!debug) return;

        plugin.getLogger().log(level, message);
        try {
            debugFile.write(message + "\n");
        } catch (IOException e) {
            if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }

    public void sendError(String message) {
        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players)
                .sendMessage(Component.text("[EntityClearer] " + message).color(NamedTextColor.RED));

        logDebug(Level.SEVERE, message);
    }

    public void tpsTimer(int delay) {
        plugin.getLogger().info("TPS monitoring activated.");
        BukkitRunnable taskTimer = new BukkitRunnable() {
            @Override
            public void run() {
                // If the timer was already run
                if (tpsTimerRan) return;

                long now = System.currentTimeMillis();
                BukkitRunnable countTicks = new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        ticks++;
                        if (now + 1000 <= System.currentTimeMillis()) {
                            this.cancel();
                            averageTPS(ticks);
                        }
                    }
                };
                countTicks.runTaskTimer(plugin, 0, 1);
            }
        };
        savedTpsTask = taskTimer.runTaskTimerAsynchronously(plugin, delay, 20);
    }

    private void averageTPS(int tps) {
        tickList.add(tps);
        if (tickList.size() > 10) tickList.remove(0);
        else return;

        int sum = 0;
        double average;
        for (int x : tickList) sum += x;
        average = (double) sum / tickList.size();
        tpsLow(average);
    }

    @SuppressWarnings("ConstantConditions")
    private void tpsLow(double tps) {
        // If TPS is not below the threshold, return
        if (!(tps < plugin.getConfig().getInt("low-tps.threshold"))) return;

        // If a chat message should be sent
        if (plugin.getConfig().getBoolean("low-tps.chat")) for (Player player : Bukkit.getOnlinePlayers())
            if (player.hasPermission("entityclearer.lowtps")) bukkitAudiences.player(player).sendMessage(
                MiniMessage.miniMessage().deserialize(
                    parseMessage(plugin.getConfig().getString("low-tps.chat-message")).replace("{TPS}",
                        String.valueOf(tps))));

        // If the entities should be removed instantly
        new ClearTask().removeEntities(true);

        // Cooldown
        tpsTimerRan = true;
        tickList.clear();
        BukkitRunnable resetTimerRan = new BukkitRunnable() {
            @Override
            public void run() {
                tpsTimerRan = false;
            }
        };
        resetTimerRan.runTaskLaterAsynchronously(plugin, 1800);
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
        EntityClearer.getInstance().getMetrics().addCustomChart(new SimplePie("low_tps_check", () -> tpsEnabled));

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
            .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
            .replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }
}
