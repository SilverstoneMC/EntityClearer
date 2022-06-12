package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@SuppressWarnings("ConstantConditions")
public class EntityClearer extends JavaPlugin implements Listener {

    private Metrics metrics;
    private BukkitAudiences adventure;

    private static BukkitTask savedKillTask;
    private static BukkitTask savedTpsTask;
    private static boolean tpsTimerRan = false;
    private static final ArrayList<Integer> tickList = new ArrayList<>();

    private static EntityClearer instance;

    public static EntityClearer getInstance() {
        return instance;
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null)
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        return this.adventure;
    }

    // Startup
    @Override
    public void onEnable() {
        instance = this;

        this.adventure = BukkitAudiences.create(this);

        metrics = new Metrics(this, 10915);
        sendMetrics();

        saveDefaultConfig();

        getCommand("clearentities").setExecutor(new ClearTask());
        getCommand("entityclearer").setTabCompleter(new TabComplete());

        getServer().getPluginManager().registerEvents(new ReloadEvent(this), this);

        killTimer();
        if (getConfig().getBoolean("low-tps.enabled")) tpsTimer(600);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                saveDefaultConfig();
                reloadConfig();

                tickList.clear();
                if (savedKillTask != null && !savedKillTask.isCancelled()) savedKillTask.cancel();
                if (savedTpsTask != null && !savedTpsTask.isCancelled()) savedTpsTask.cancel();
                if (getConfig().getBoolean("low-tps.enabled")) tpsTimer(0);
                tpsTimerRan = false;
                killTimer();
                sendMetrics();

                sender.sendMessage(ChatColor.GREEN + "EntityClearer reloaded!");
                return true;
            }

            if (args[0].equalsIgnoreCase("debug")) {
                if (ClearTask.debug) {
                    sender.sendMessage(ChatColor.RED + "Debug is already active!");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Starting debug dump... See console for more details.");

                try {
                    File file = new File(instance.getDataFolder(), "debug-" + System.currentTimeMillis() + ".txt");
                    if (!file.createNewFile()) sender.sendMessage(
                        ChatColor.RED + "Failed to create debug file! Check console for the debug output.");
                    else ClearTask.debugFile = new FileWriter(file, StandardCharsets.UTF_8, true);
                } catch (IOException e) {
                    sender.sendMessage(
                        ChatColor.RED + "Failed to create debug file! Check console for the debug output.");
                    if (instance.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                }
                ClearTask.debug = true;

                if (instance.getConfig().getBoolean("countdown-on-command")) new ClearTask().countdown();
                else new ClearTask().removeEntitiesTask(false);

                return true;
            }
        }
        return false;
    }

    public void killTimer() {
        if (getConfig().getBoolean("debug")) {
            getLogger().info("╔══════════════════════════════════════╗");
            getLogger().info("║        STARTING REMOVAL TIMER        ║");
            getLogger().info("╚══════════════════════════════════════╝");
        }

        if (getConfig().getInt("interval") <= 0) {
            if (getConfig().getBoolean("debug"))
                getLogger().warning("The interval is set to a value less than 1, so it's been disabled!");
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                new ClearTask().countdown();
            }
        };
        savedKillTask = task.runTaskTimer(this, ((getConfig().getInt("interval") * 60L) * 20),
            ((getConfig().getInt("interval") * 60L) * 20));
    }

    public void tpsTimer(int delay) {
        getLogger().info("TPS monitoring activated.");
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
                countTicks.runTaskTimer(instance, 0, 1);
            }
        };
        savedTpsTask = taskTimer.runTaskTimerAsynchronously(this, delay, 20);
    }

    private static void averageTPS(int tps) {
        tickList.add(tps);
        if (tickList.size() > 10) tickList.remove(0);
        else return;

        int sum = 0;
        double average;
        for (int x : tickList) sum += x;
        average = (double) sum / tickList.size();
        tpsLow(average);
    }

    private static void tpsLow(double tps) {
        // If TPS is not below the threshold, return
        if (!(tps < instance.getConfig().getInt("low-tps.threshold"))) return;

        // If a chat message should be sent
        if (instance.getConfig().getBoolean("low-tps.chat")) for (Player player : Bukkit.getOnlinePlayers())
            if (player.hasPermission("entityclearer.lowtps")) getInstance().adventure().player(player).sendMessage(
                MiniMessage.miniMessage().deserialize(
                    EntityClearer.parseMessage(instance.getConfig().getString("low-tps.chat-message"))
                        .replace("{TPS}", String.valueOf(tps))));

        // If the entities should be removed instantly
        new ClearTask().removeEntitiesTask(true);

        // Cooldown
        tpsTimerRan = true;
        tickList.clear();
        BukkitRunnable resetTimerRan = new BukkitRunnable() {
            @Override
            public void run() {
                tpsTimerRan = false;
            }
        };
        resetTimerRan.runTaskLaterAsynchronously(instance, 1800);
    }

    public Metrics getMetrics() {
        return metrics;
    }

    private void sendMetrics() {
        // Interval
        int interval = getConfig().getInt("interval");
        metrics.addCustomChart(new SimplePie("interval", () -> String.valueOf(interval)));

        // Sound
        String sound = getConfig().getString("sound");
        metrics.addCustomChart(new SimplePie("sound", () -> sound));

        // TPS
        String tpsEnabled;
        if (getConfig().getBoolean("low-tps.enabled")) tpsEnabled = "Enabled";
        else tpsEnabled = "Disabled";
        metrics.addCustomChart(new SimplePie("low_tps_check", () -> tpsEnabled));

        // Nearby
        String nearbyEnabled;
        if (getConfig().getBoolean("nearby-entities.enabled")) nearbyEnabled = "Enabled";
        else nearbyEnabled = "Disabled";
        metrics.addCustomChart(new SimplePie("nearby_entities_check", () -> nearbyEnabled));
    }

    public static String parseMessage(String message) {
        return message.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
            .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>").replace("&d", "<light_purple>")
            .replace("&e", "<yellow>").replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
            .replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }
}