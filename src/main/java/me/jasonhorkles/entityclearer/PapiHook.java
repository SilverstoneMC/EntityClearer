package me.jasonhorkles.entityclearer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jasonhorkles.entityclearer.utils.KillTimer;
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
        return "1";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("remaining_minutes")) {
            if (KillTimer.nextKillTask == -1) return "DISABLED";

            return String.valueOf((KillTimer.nextKillTask - System.currentTimeMillis()) / 60000);
        }

        if (params.equalsIgnoreCase("remaining_seconds")) {
            if (KillTimer.nextKillTask == -1) return "DISABLED";

            return String.valueOf((KillTimer.nextKillTask - System.currentTimeMillis()) / 1000);
        }

        if (params.equalsIgnoreCase("remaining_seconds_left")) {
            if (KillTimer.nextKillTask == -1) return "DISABLED";

            int seconds = (int) ((KillTimer.nextKillTask - System.currentTimeMillis()) / 1000);
            seconds %= 60;
            return String.valueOf(seconds);
        }

        return null; // Placeholder is unknown by the Expansion
    }
}
