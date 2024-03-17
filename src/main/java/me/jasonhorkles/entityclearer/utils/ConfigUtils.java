package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.EntityClearer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.logging.Level;

public class ConfigUtils {
    public static boolean isAll;

    private final JavaPlugin plugin = EntityClearer.getInstance();

    public ArrayList<World> getWorlds(String path) {
        // Add all the worlds to a list
        ArrayList<World> worlds = new ArrayList<>();
        //noinspection DataFlowIssue
        ArrayList<String> keys = new ArrayList<>(plugin.getConfig().getConfigurationSection(path)
            .getKeys(false));

        if (keys.contains("ALL")) {
            new LogDebug().debug(Level.INFO, "", "'ALL' found! Adding all worlds to message list...");
            isAll = true;
            worlds.addAll(Bukkit.getWorlds());
        } else {
            new LogDebug().debug(Level.INFO, "", "Adding all worlds defined in config to message list...");
            isAll = false;

            int index = -1;
            for (String world : keys) {
                index++;

                // If that world doesn't exist, complain
                if (Bukkit.getWorld(world) == null) {
                    new LogDebug().error("SERVER",
                        "Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    continue;
                }

                worlds.add(Bukkit.getWorld(world));
            }
        }

        return worlds;
    }
}
