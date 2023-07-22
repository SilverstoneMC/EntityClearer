package me.jasonhorkles.entityclearer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPI extends PlaceholderExpansion {
    public PlaceholderAPI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private final JavaPlugin plugin;

    @Override
    public @NotNull String getIdentifier() {
        return "entityclearer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JasonHorkles";
    }

    @Override
    public @NotNull String getVersion() {
        return "1";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("time_left_minutes")) return String.valueOf(Utils.nextKillTask / 60000);

        if (params.equalsIgnoreCase("time_left_seconds")) return String.valueOf(Utils.nextKillTask / 1000);

        if (params.equalsIgnoreCase("time_left_remaining_seconds")) {
            int seconds = (int) (Utils.nextKillTask / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        return null; // Placeholder is unknown by the Expansion
    }
}
