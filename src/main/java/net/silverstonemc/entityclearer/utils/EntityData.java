package net.silverstonemc.entityclearer.utils;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class EntityData {
    private EntityType entityType;
    private String mythicMobType;
    private boolean includeNamed;
    private boolean includeOccupied;

    public EntityData(String entityType, String worldName) {
        LogDebug debug = new LogDebug();

        entityType = entityType.toUpperCase();

        // If the entityType includes named
        if (entityType.contains("-NAMED")) {
            includeNamed = true;
            entityType = entityType.replace("-NAMED", "");
        }

        // If the entityType includes occupied
        if (entityType.contains("-OCCUPIED")) {
            includeOccupied = true;
            entityType = entityType.replace("-OCCUPIED", "");
        }

        // If the entityType includes MythicMob
        if (entityType.startsWith("MYTHICMOB:")) {
            mythicMobType = entityType.replace("MYTHICMOB:", "");
            entityType = null;
        }

        // Validate mob type, or skip if MythicMob (will check later)
        if (entityType != null) try {
            this.entityType = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            debug.error(
                worldName,
                "Couldn't find the entity type \"" + entityType + "\"! Please double check your config.");

            if (LogDebug.debugActive) {
                debug.debug(Level.SEVERE, worldName, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    debug.debug(Level.SEVERE, worldName, ste.toString());
            } else e.printStackTrace();
        }

        debug.debug(
            Level.INFO,
            worldName,
            "Entity " + (this.entityType != null ? this.entityType : mythicMobType) + " is specified with the following properties:");
        debug.debug(Level.INFO, worldName, " Include named: " + includeNamed);
        debug.debug(Level.INFO, worldName, " Include occupied: " + includeOccupied);
        debug.debug(Level.INFO, worldName, " MythicMob: " + (mythicMobType != null));
    }

    @Nullable
    public EntityType getType() {
        return entityType;
    }

    @Nullable
    public String getMythicMobType() {
        return mythicMobType;
    }

    public boolean includeNamed() {
        return includeNamed;
    }

    public boolean includeOccupied() {
        return includeOccupied;
    }
}
