package net.silverstonemc.entityclearer.utils;

import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class EntityData {
    private EntityType entityType;
    private String mythicMobType = null;
    private boolean includeNamed = false;
    private boolean includeOccupied = false;

    public EntityData(String entityType, String worldName) {
        entityType = entityType.toUpperCase();

        // If the entityType includes named
        if (entityType.contains("-NAMED")) {
            this.includeNamed = true;
            entityType = entityType.replace("-NAMED", "");
        }

        // If the entityType includes occupied
        if (entityType.contains("-OCCUPIED")) {
            this.includeOccupied = true;
            entityType = entityType.replace("-OCCUPIED", "");
        }

        // If the entityType includes MythicMob
        if (entityType.startsWith("MYTHICMOB:")) {
            this.mythicMobType = entityType.replace("MYTHICMOB:", "");
            entityType = null;
        }

        // Validate mob type, or skip if MythicMob (will check later)
        if (entityType != null) try {
            this.entityType = EntityType.valueOf(entityType);
        } catch (IllegalArgumentException e) {
            new LogDebug().error(
                worldName,
                "Couldn't find the entity type \"" + entityType + "\"! Please double check your config.");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, worldName, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, worldName, ste.toString());
            } else e.printStackTrace();
        }

        new LogDebug().debug(
            Level.INFO,
            worldName,
            "Entity " + (this.entityType != null ? this.entityType : this.mythicMobType) + " is specified with the following properties:");
        new LogDebug().debug(Level.INFO, worldName, " Include named: " + this.includeNamed);
        new LogDebug().debug(Level.INFO, worldName, " Include occupied: " + this.includeOccupied);
        new LogDebug().debug(Level.INFO, worldName, " MythicMob: " + (this.mythicMobType != null));
    }

    @Nullable
    public EntityType getType() {
        return entityType;
    }

    @Nullable
    public String getMythicMobType() {
        return mythicMobType;
    }

    @NonNull
    public boolean includeNamed() {
        return includeNamed;
    }

    @NonNull
    public boolean includeOccupied() {
        return includeOccupied;
    }
}
