package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.EntityClearer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;

public class LogDebug {
    public static boolean debugActive = false;
    public static FileWriter debugFile;

    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();

    public void debug(Level level, String worldName, String message) {
        if (!debugActive) return;

        worldName = worldName.toUpperCase() + ": ";
        if (worldName.equals((": "))) worldName = "";

        EntityClearer.getInstance().getLogger().log(level, worldName + message);
        try {
            debugFile.write(worldName + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void error(String worldName, String message) {
        worldName = worldName.toUpperCase();

        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                Component.text("[EntityClearer] " + worldName + ": " + message, NamedTextColor.RED));

        debug(Level.SEVERE, worldName, message);
    }
}
