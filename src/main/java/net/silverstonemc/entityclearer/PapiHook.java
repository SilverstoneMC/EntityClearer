package net.silverstonemc.entityclearer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.silverstonemc.entityclearer.utils.KillTimer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @NotNull
    @Override
    public List<String> getPlaceholders() {
        return List.of(
            "remaining_minutes_<world>",
            "remaining_seconds_<world>",
            "remaining_seconds_left_<world>");
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("remaining_minutes_")) {
            String worldName = params.replace("remaining_minutes_", "");

            String returnReason = returnReason(worldName);
            if (returnReason != null) return returnReason;

            return String.valueOf((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 60000);
        }

        if (params.startsWith("remaining_seconds_")) {
            String worldName = params.replace("remaining_seconds_", "");

            String returnReason = returnReason(worldName);
            if (returnReason != null) return returnReason;

            return String.valueOf((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 1000);
        }

        if (params.startsWith("remaining_seconds_left_")) {
            String worldName = params.replace("remaining_seconds_left_", "");

            String returnReason = returnReason(worldName);
            if (returnReason != null) return returnReason;

            int seconds = (int) ((KillTimer.nextKillTask.get(worldName) - System.currentTimeMillis()) / 1000);
            seconds %= 60;
            return String.valueOf(seconds);
        }

        return null; // Placeholder is unknown by the Expansion
    }

    @Nullable
    private String returnReason(String worldName) {
        if (Bukkit.getWorld(worldName) == null) return "UNKNOWN WORLD";
        if (!KillTimer.nextKillTask.containsKey(worldName))
            return "NO TASK FOR WORLD (UNDEFINED OR DIFFERING CAPS)";
        if (KillTimer.nextKillTask.get(worldName) == -1) return "DISABLED (INTERVAL SET TO -1)";
        return null;
    }
}
