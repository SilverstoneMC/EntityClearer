package net.silverstonemc.entityclearer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.silverstonemc.entityclearer.utils.KillTimer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PapiHook extends PlaceholderExpansion {
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
        return EntityClearer.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.toLowerCase().startsWith("remaining_minutes_")) {
            String worldName = params.replace("remaining_minutes_", "");

            if (Bukkit.getWorld(worldName) == null) return "UNKNOWN";
            if (!KillTimer.nextKillTask.containsKey(worldName)) return "DISABLED";
            if (KillTimer.nextKillTask.get(worldName) == -1) return "DISABLED";

            return String.valueOf((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 60000);
        }

        if (params.startsWith("remaining_seconds_")) {
            String worldName = params.replace("remaining_seconds_", "");

            if (Bukkit.getWorld(worldName) == null) return "UNKNOWN";
            if (!KillTimer.nextKillTask.containsKey(worldName)) return "DISABLED";
            if (KillTimer.nextKillTask.get(worldName) == -1) return "DISABLED";

            return String.valueOf((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 1000);
        }

        if (params.startsWith("remaining_seconds_left_")) {
            String worldName = params.replace("remaining_seconds_left_", "");

            if (Bukkit.getWorld(worldName) == null) return "UNKNOWN";
            if (!KillTimer.nextKillTask.containsKey(worldName)) return "DISABLED";
            if (KillTimer.nextKillTask.get(worldName) == -1) return "DISABLED";

            int seconds = (int) ((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 1000);
            seconds %= 60;
            return String.valueOf(seconds);
        }

        return null; // Placeholder is unknown by the Expansion
    }
}
