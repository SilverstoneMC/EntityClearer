package net.silverstonemc.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.silverstonemc.entityclearer.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;

public class Commands implements CommandExecutor {
    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> reload(sender);
                case "debug" -> debug(sender);
                case "clearnow" -> clearnow(sender);

                default -> {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void reload(CommandSender sender) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        if (TpsMonitoring.savedTpsTask != null && !TpsMonitoring.savedTpsTask.isCancelled())
            TpsMonitoring.savedTpsTask.cancel();
        TpsMonitoring.tickList.clear();

        new CancelTasks().all();

        if (plugin.getConfig().getBoolean("low-tps.enabled")) new TpsMonitoring().tpsTimer(0);

        TpsMonitoring.tpsTimerRan = false;
        LogDebug.debugActive = false;

        new KillTimer().start();
        new MetricsUtils().send();

        bukkitAudiences.sender(sender).sendMessage(Component.text(
            "EntityClearer reloaded!",
            NamedTextColor.GREEN));
    }

    private void debug(CommandSender sender) {
        LogDebug debug = new LogDebug();

        if (LogDebug.debugActive) {
            bukkitAudiences.sender(sender).sendMessage(Component.text(
                "Debug is already active!",
                NamedTextColor.RED));
            return;
        }

        bukkitAudiences.sender(sender).sendMessage(Component.text("Activating debug... See console for more details.",
            NamedTextColor.YELLOW));

        try {
            Path path = Path.of(plugin.getDataFolder().getPath(), "debug");
            Files.createDirectories(path);
            LogDebug.fileId = System.currentTimeMillis();
            File file = new File(path.toFile(), "debug-" + LogDebug.fileId + ".yml");

            if (file.createNewFile()) LogDebug.debugFile = new FileWriter(file, StandardCharsets.UTF_8, true);
            else bukkitAudiences.sender(sender).sendMessage(Component.text("Failed to create debug file! Check console for the debug output.",
                NamedTextColor.RED));

        } catch (IOException e) {
            debug.error("SERVER", "Failed to create debug file! Check console for more information...");
            e.printStackTrace();
            return;
        }
        LogDebug.debugActive = true;

        // Dump config into debug file
        plugin.getLogger().info("Dumping config into debug file...");
        try {
            Scanner scanner = new Scanner(
                new File(plugin.getDataFolder(), "config.yml"),
                StandardCharsets.UTF_8);
            while (scanner.hasNextLine()) LogDebug.debugFile.write(scanner.nextLine() + "\n");
            scanner.close();
            LogDebug.debugFile.write("""
                
                
                
                ###############################################
                #              END OF CONFIG DUMP             #
                ###############################################
                
                
                
                """);
            plugin.getLogger().info("Config file dumped!");
        } catch (IOException e) {
            debug.error("SERVER", "Failed to dump config file! Check console for more information...");
            e.printStackTrace();
        }

        // Dump stats
        debug.debug(Level.INFO, "", "");
        debug.debug(Level.INFO, "", "╔══════════════════════════════════════╗");
        debug.debug(Level.INFO, "", "║             INFORMATION              ║");
        debug.debug(Level.INFO, "", "╚══════════════════════════════════════╝");

        debug.debug(Level.INFO, "", "Plugin version: " + plugin.getPluginMeta().getVersion());
        debug.debug(Level.INFO, "", "Server version: " + Bukkit.getName() + " " + Bukkit.getVersion());
        debug.debug(Level.INFO, "", "API version: " + Bukkit.getBukkitVersion());

        Plugin mmobs = (Plugin) EntityClearer.getInstance().getMythicPlugin();
        if (mmobs != null) debug.debug(
            Level.INFO,
            "",
            "MythicMobs version: " + mmobs.getPluginMeta().getVersion());

        Plugin papi = EntityClearer.getInstance().getPlaceholderAPI();
        boolean papiEnabled = papi != null;
        if (papiEnabled) debug.debug(
            Level.INFO,
            "",
            "PlaceholderAPI version: " + papi.getPluginMeta().getVersion());

        //noinspection AccessOfSystemProperties
        debug.debug(Level.INFO, "", "Java version: " + System.getProperty("java.version"));
        debug.debug(Level.INFO, "", "Players online: " + Bukkit.getOnlinePlayers().size());

        debug.debug(Level.INFO, "", "Available world list: ");
        for (World world : Bukkit.getWorlds()) {
            debug.debug(Level.INFO, "", " " + world.getName());
            debug.debug(Level.INFO, "", "  Players: " + world.getPlayers().size());

            if (papiEnabled) {
                String nextKillTask = KillTimer.nextKillTask.containsKey(world.getName()) ? (KillTimer.nextKillTask.get(
                    world.getName()) - System.currentTimeMillis()) / 1000 + " seconds" : "null";
                debug.debug(Level.INFO, "", "  Next kill task: " + nextKillTask);
            }
        }

        debug.debug(Level.INFO, "", "");
        new ClearTask().removeEntitiesPreTask(new ConfigUtils().getWorlds("worlds"), false, false);
    }

    private void clearnow(CommandSender sender) {
        bukkitAudiences.sender(sender).sendMessage(Component.text(
            "Starting entity removal task...",
            NamedTextColor.YELLOW));
        new ClearTask().removeEntitiesPreTask(new ConfigUtils().getWorlds("worlds"), false, false);
    }
}
