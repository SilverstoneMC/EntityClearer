package me.jasonhorkles.entityclearer;

import me.jasonhorkles.entityclearer.utils.CancelTasks;
import me.jasonhorkles.entityclearer.utils.KillTimer;
import me.jasonhorkles.entityclearer.utils.LogDebug;
import me.jasonhorkles.entityclearer.utils.MetricsUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Level;

public class Commands implements CommandExecutor {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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

        sender.sendMessage(ChatColor.GREEN + "EntityClearer reloaded!");
    }

    private void debug(CommandSender sender) {
        if (LogDebug.debugActive) {
            sender.sendMessage(ChatColor.RED + "Debug is already active!");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Activating debug... See console for more details.");

        try {
            Path path = Path.of(plugin.getDataFolder().getPath(), "debug");
            Files.createDirectories(path);
            File file = new File(path.toFile(), "debug-" + System.currentTimeMillis() + ".yml");

            if (!file.createNewFile()) sender.sendMessage(
                ChatColor.RED + "Failed to create debug file! Check console for the debug output.");

            else LogDebug.debugFile = new FileWriter(file, StandardCharsets.UTF_8, true);

        } catch (IOException e) {
            new LogDebug().error("Failed to create debug file! Check console for more information...");
            e.printStackTrace();
            return;
        }
        LogDebug.debugActive = true;

        // Dump config into debug file
        plugin.getLogger().info("Dumping config into debug file...");
        try {
            Scanner scanner = new Scanner(new File(plugin.getDataFolder(), "config.yml"));
            while (scanner.hasNextLine()) LogDebug.debugFile.write(scanner.nextLine() + "\n");
            LogDebug.debugFile.write("""



                ###############################################
                #              END OF CONFIG DUMP             #
                ###############################################



                """);
            plugin.getLogger().info("Config file dumped!");
        } catch (IOException e) {
            new LogDebug().error("Failed to dump config file! Check console for more information...");
            e.printStackTrace();
        }

        // Dump stats
        new LogDebug().debug(Level.INFO, "");
        new LogDebug().debug(Level.INFO, "╔══════════════════════════════════════╗");
        new LogDebug().debug(Level.INFO, "║             INFORMATION              ║");
        new LogDebug().debug(Level.INFO, "╚══════════════════════════════════════╝");

        new LogDebug().debug(Level.INFO, "Plugin version: " + plugin.getDescription().getVersion());
        new LogDebug().debug(Level.INFO, "Server version: " + Bukkit.getVersion());
        new LogDebug().debug(Level.INFO, "Java version: " + System.getProperty("java.version"));

        new LogDebug().debug(Level.INFO, "Available world list: ");
        for (World world : Bukkit.getWorlds())
            new LogDebug().debug(Level.INFO, " " + world.getName());

        int interval = plugin.getConfig().getInt("interval");
        if (interval < 1) new Countdown().countdown();
        else {
            bukkitAudiences.sender(sender).sendMessage(Component.text(
                    "Your interval is currently set to " + interval + " minutes. If you don't want to wait for the clear task to start, type ",
                    NamedTextColor.GOLD).append(Component.text("/ecl clearnow", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/ecl clearnow")))
                .append(Component.text(" to start it now.", NamedTextColor.GOLD)));
            new LogDebug().debug(Level.INFO, "Cancelling other tasks...");
            new CancelTasks().all();
            new KillTimer().start();
        }
    }

    private void clearnow(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Starting entity removal task...");
        // If it should count down first
        // Otherwise just go
        if (plugin.getConfig().getBoolean("countdown-on-command")) new Countdown().countdown();
        else new ClearTask().removeEntitiesPreTask(false);
    }
}
