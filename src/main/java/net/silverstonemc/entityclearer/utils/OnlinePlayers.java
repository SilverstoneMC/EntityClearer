package net.silverstonemc.entityclearer.utils;

import net.silverstonemc.entityclearer.EntityClearer;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class OnlinePlayers {
    /**
     * Returns an object array containing the notEnoughPlayers boolean and a string indicating if the check was server-wide or world-wide.
     *
     * @return Object[] {boolean, String}
     */
    public Object[] isNotEnough(World world, String worldConfigName) {
        EntityClearer plugin = EntityClearer.getInstance();

        // Check for the minimum players
        boolean isServerWide = false;
        int minPlayers = plugin.getConfig().getInt("worlds." + worldConfigName + ".min-players");
        // If the minimum player value for the world is -1, then we want the server-wide value
        if (minPlayers <= -1) {
            minPlayers = plugin.getConfig().getInt("global-min-players");
            isServerWide = true;
        }

        // Determine if there are enough players either server-wide or world-wide depending on the settings
        boolean notEnoughPlayers;
        if (isServerWide) notEnoughPlayers = Bukkit.getOnlinePlayers().size() < minPlayers;
        else notEnoughPlayers = world.getPlayers().size() < minPlayers;

        return new Object[]{notEnoughPlayers, isServerWide ? "server" : "world"};
    }
}
