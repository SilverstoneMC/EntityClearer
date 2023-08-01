package me.jasonhorkles.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;

@SuppressWarnings("DataFlowIssue")
public class EntityClearer extends JavaPlugin implements Listener {
    private BukkitAudiences adventure;
    private Metrics metrics;
    private MythicPlugin mythicPlugin;
    private Plugin placeholderAPI;
    private static EntityClearer instance;

    // Startup
    @Override
    public void onEnable() {
        instance = this;

        adventure = BukkitAudiences.create(this);

        mythicPlugin = (MythicPlugin) getServer().getPluginManager().getPlugin("MythicMobs");
        if (mythicPlugin != null) getLogger().log(Level.INFO, "Enabled MythicMobs hook!");

        placeholderAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null) {
            new PapiHook().register();
            getLogger().log(Level.INFO, "Enabled PlaceholderAPI hook!");
        }

        metrics = new Metrics(this, 10915);
        new Utils().sendMetrics();

        saveDefaultConfig();

        getCommand("clearentities").setExecutor(new ClearTask());
        getCommand("entityclearer").setTabCompleter(new TabComplete());

        getServer().getPluginManager().registerEvents(new ReloadEvent(this), this);

        new Utils().killTimer();
        if (getConfig().getBoolean("low-tps.enabled")) new TpsMonitoring().tpsTimer(600);
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

                TpsMonitoring.tickList.clear();

                if (Utils.savedCountTask != null && !Utils.savedCountTask.isCancelled())
                    Utils.savedCountTask.cancel();
                if (Countdown.savedCountdownTask != null && !Countdown.savedCountdownTask.isCancelled())
                    Countdown.savedCountdownTask.cancel();
                if (Utils.savedKillTask != null && !Utils.savedKillTask.isCancelled())
                    Utils.savedKillTask.cancel();
                if (TpsMonitoring.savedTpsTask != null && !TpsMonitoring.savedTpsTask.isCancelled())
                    TpsMonitoring.savedTpsTask.cancel();

                if (getConfig().getBoolean("low-tps.enabled")) new TpsMonitoring().tpsTimer(0);

                TpsMonitoring.tpsTimerRan = false;

                new Utils().killTimer();
                new Utils().sendMetrics();

                sender.sendMessage(ChatColor.GREEN + "EntityClearer reloaded!");
                return true;
            }

            if (args[0].equalsIgnoreCase("debug")) {
                if (Utils.debug) {
                    sender.sendMessage(ChatColor.RED + "Debug is already active!");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Starting debug dump... See console for more details.");

                try {
                    File file = new File(getDataFolder(), "debug-" + System.currentTimeMillis() + ".yml");

                    if (!file.createNewFile()) sender.sendMessage(
                        ChatColor.RED + "Failed to create debug file! Check console for the debug output.");

                    else Utils.debugFile = new FileWriter(file, StandardCharsets.UTF_8, true);

                } catch (IOException e) {
                    new Utils().sendError(
                        "Failed to create debug file! Check console for more information...");
                    e.printStackTrace();
                    return true;
                }
                Utils.debug = true;

                // Dump config into debug file
                getLogger().info("Dumping config into debug file...");
                try {
                    Scanner scanner = new Scanner(new File(getDataFolder(), "config.yml"));
                    while (scanner.hasNextLine()) Utils.debugFile.write(scanner.nextLine() + "\n");
                    Utils.debugFile.write("""



                        ###############################################
                        #              END OF CONFIG DUMP             #
                        ###############################################



                        """);
                    getLogger().info("Config file dumped!");
                } catch (IOException e) {
                    new Utils().sendError(
                        "Failed to dump config file! Check console for more information...");
                    e.printStackTrace();
                }

                // Dump stats
                new Utils().logDebug(Level.INFO, "╔══════════════════════════════════════╗");
                new Utils().logDebug(Level.INFO, "║             INFORMATION              ║");
                new Utils().logDebug(Level.INFO, "╚══════════════════════════════════════╝");

                new Utils().logDebug(Level.INFO, "Plugin version: " + getDescription().getVersion());
                new Utils().logDebug(Level.INFO, "Server version: " + Bukkit.getVersion());
                new Utils().logDebug(Level.INFO, "Java version: " + System.getProperty("java.version"));

                new Utils().logDebug(Level.INFO, "Available world list: ");
                for (World world : Bukkit.getWorlds())
                    new Utils().logDebug(Level.INFO, " " + world.getName());

                if (getConfig().getBoolean("countdown-on-command")) new Countdown().countdown();
                else new ClearTask().removeEntities(false);

                return true;
            }
        }
        return false;
    }

    public static EntityClearer getInstance() {
        return instance;
    }

    public BukkitAudiences getAdventure() {
        if (this.adventure == null)
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        return this.adventure;
    }

    public MythicPlugin getMythicPlugin() {
        return mythicPlugin;
    }

    public Plugin getPlaceholderAPI() {
        return placeholderAPI;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}