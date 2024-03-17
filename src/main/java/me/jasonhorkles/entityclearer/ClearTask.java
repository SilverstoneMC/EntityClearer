package me.jasonhorkles.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.jasonhorkles.entityclearer.utils.ConfigUtils;
import me.jasonhorkles.entityclearer.utils.EntityData;
import me.jasonhorkles.entityclearer.utils.LogDebug;
import me.jasonhorkles.entityclearer.utils.ParseMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

@SuppressWarnings({"DataFlowIssue", "resource"})
public class ClearTask {
    private final ArrayList<EntityData> entityDataList = new ArrayList<>();
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final MythicPlugin mythicPlugin = EntityClearer.getInstance().getMythicPlugin();
    private int removedEntities;
    private static boolean logCooldown = false;

    public void removeEntitiesPreTask(ArrayList<World> worlds, boolean useTpsList, boolean tpsLow) {
        new LogDebug().debug(Level.INFO, "", "");
        new LogDebug().debug(Level.INFO, "", "╔══════════════════════════════════════╗");
        new LogDebug().debug(Level.INFO, "", "║     REMOVE ENTITIES TASK STARTED     ║");
        new LogDebug().debug(Level.INFO, "", "╚══════════════════════════════════════╝");

        if (mythicPlugin != null) new LogDebug().debug(Level.INFO, "", "MythicMobs plugin found!");
        removedEntities = 0;

        try {
            removeEntities(worlds, useTpsList, tpsLow);
        } catch (Error | Exception e) {
            new LogDebug().error("SERVER",
                "Something went wrong clearing entities! Is your config outdated?");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, "", e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, "", ste.toString());
            } else e.printStackTrace();
        }
    }

    private void removeEntities(ArrayList<World> worlds, boolean useTpsList, boolean tpsLow) {
        String path = "worlds";
        if (useTpsList) path = "low-tps.worlds";

        // For each world in the config
        for (World world : worlds) {
            String worldName = world.getName();

            new LogDebug().debug(Level.INFO, "", "");
            new LogDebug().debug(Level.INFO, worldName, "Scanning world...");

            String worldConfigName = worldName;
            if (ConfigUtils.isAll) worldConfigName = "ALL";

            // Save the entity data from the config
            for (String entityType : plugin.getConfig()
                .getStringList(path + "." + worldConfigName + ".entities"))
                entityDataList.add(new EntityData(entityType, worldName));
            new LogDebug().debug(Level.INFO, "", "");

            // Get the loaded entities
            for (Entity entity : world.getEntities()) {
                EntityData entityData = matchEntityFromConfig(entity, worldName);

                if (entityData == null) continue;

                // If the entity is a MythicMob matched in the config
                if (entityData.getMythicMobType() != null) {
                    ActiveMob mythicMob = MythicBukkit.inst().getMobManager()
                        .getActiveMob(entity.getUniqueId()).orElse(null);

                    if (mythicMob.getMobType().equalsIgnoreCase(entityData.getMythicMobType())) {
                        checkOccupied(entity, entityData, path, worldName);
                        continue;
                    }
                }

                // Skip all the checks if a dropped item
                if (entity.getType() == EntityType.DROPPED_ITEM) removeEntity(entity, worldName);
                else checkOccupied(entity, entityData, path, worldName);
            }

            // For each player in said world
            for (Player player : world.getPlayers()) {
                sendActionBar(world, player, tpsLow);
                sendChat(world, player, tpsLow);
                playSound(world, player);
            }
        }

        for (String command : plugin.getConfig().getStringList("commands"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        removeEntitiesPostTask();
    }

    @Nullable
    private EntityData matchEntityFromConfig(Entity entity, String worldName) {
        // If the entity is a MythicMob
        if (mythicPlugin != null) {
            ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                .orElse(null);

            if (mythicMob != null) {
                // Try to match mythic entity with one from the config
                EntityData mythicEntityData = entityDataList.stream().filter(entityData1 -> {
                    if (entityData1.getMythicMobType() == null) return false;
                    return entityData1.getMythicMobType().equalsIgnoreCase(mythicMob.getMobType());
                }).findFirst().orElse(null);

                if (mythicEntityData != null) {
                    new LogDebug().debug(Level.INFO, worldName,
                        "Entity " + mythicMob.getMobType() + " is a MythicMob that matches the config's!");
                    return mythicEntityData;
                }
            }
        }

        // Try to match entity with one from the config
        EntityData entityData = entityDataList.stream()
            .filter(entityData1 -> entityData1.getType() == entity.getType()).findFirst().orElse(null);

        // If the entity isn't a MythicMob and is found in the config
        if (entityData != null) {
            // Check if it's a MythicMob again so it doesn't get removed as a regular entity
            if (mythicPlugin != null) {
                ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                    .orElse(null);

                if (mythicMob != null) {
                    new LogDebug().debug(Level.WARNING, worldName,
                        "Entity " + mythicMob.getMobType() + " is a MythicMob not in the config! Skipping...");
                    return null;
                }
            }

            new LogDebug().debug(Level.INFO,
                worldName,
                "Entity " + entity.getType() + " matches the config's!");
            return entityData;
        }

        new LogDebug().debug(Level.WARNING, worldName,
            "Entity " + entity.getType() + " is not a valid type from the config!");
        return null;
    }

    private void checkOccupied(Entity entity, EntityData entityData, String path, String worldName) {
        // Skip entity if it is occupied and the config doesn't allow it
        if (!entityData.includeOccupied()) for (Entity passenger : entity.getPassengers())
            if (passenger.getType() == EntityType.PLAYER) {
                new LogDebug().debug(Level.INFO, worldName,
                    "Skipping entity " + entity.getType() + " because it is occupied!");
                return;
            }

        checkNamed(entity, entityData, path, worldName);
    }

    @SuppressWarnings("deprecation")
    private void checkNamed(Entity entity, EntityData entityData, String path, String worldName) {
        String entityType = (entityData.getMythicMobType() != null) ? "MythicMob" : "entity";

        if (entityData.includeNamed()) {
            // We don't care if it has a name
            checkSpawnReason(entity, path, worldName);
            return;
        }

        // Don't remove named
        new LogDebug().debug(Level.INFO, worldName, "Removing " + entityType + " without a name only...");
        // And it doesn't have a name
        if (entity.getCustomName() == null) {
            new LogDebug().debug(Level.INFO, worldName,
                "The " + entityType + " " + entity.getType() + " doesn't have a custom name!");
            checkSpawnReason(entity, path, worldName);
        }
        // And it does have a name
        else {
            new LogDebug().debug(Level.INFO, worldName,
                entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
            new LogDebug().debug(Level.INFO, "", "");
        }
    }

    private void checkSpawnReason(Entity entity, String path, String worldName) {
        // If any entity should be removed, regardless of the spawn reason
        if (!plugin.getConfig().getBoolean(path + "." + worldName + ".spawn-reason.enabled")) {
            new LogDebug().debug(Level.INFO,
                worldName,
                "Removing entities regardless of their spawn reason...");
            checkNearby(entity, worldName);
            return;
        }

        new LogDebug().debug(Level.INFO, worldName, "Only removing entities with a specific spawn reason...");

        try {
            ArrayList<String> spawnReasons = new ArrayList<>();
            for (String spawnReason : plugin.getConfig()
                .getStringList(path + "." + worldName + ".spawn-reason.reasons"))
                spawnReasons.add(spawnReason.toUpperCase());

            // If the entity's spawn reason matches the config's
            if (spawnReasons.contains(entity.getEntitySpawnReason().name())) {
                new LogDebug().debug(Level.INFO,
                    worldName,
                    entity.getType() + "'s spawn reason " + entity.getEntitySpawnReason() + " matches the config's!");
                checkNearby(entity, worldName);

            } else new LogDebug().debug(Level.INFO, worldName,
                entity.getType() + "'s spawn reason " + entity.getEntitySpawnReason()
                    .name() + " doesn't match the config's! (" + spawnReasons + ")");

        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            if (logCooldown) return;

            new LogDebug().error(worldName,
                "Unable to check for entity spawn reason! Are you not running Paper?");
            plugin.getLogger().warning("Please use Paper or its forks for this feature to work.");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, worldName, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, worldName, ste.toString());
            } else e.printStackTrace();

            logCooldown = true;
            BukkitRunnable cooldown = new BukkitRunnable() {
                @Override
                public void run() {
                    logCooldown = false;
                }
            };
            cooldown.runTaskLater(plugin, 200);
        }
    }

    private void checkNearby(Entity entity, String worldName) {
        // If nearby check is disabled, just move on
        if (!plugin.getConfig().getBoolean("nearby-entities.enabled")) {
            new LogDebug().debug(Level.INFO, worldName, "Check nearby entities option disabled.");
            removeEntity(entity, worldName);
            return;
        }

        // Get nearby entities
        double x = plugin.getConfig().getDouble("nearby-entities.x");
        double y = plugin.getConfig().getDouble("nearby-entities.y");
        double z = plugin.getConfig().getDouble("nearby-entities.z");
        ArrayList<Entity> nearbyEntities = new ArrayList<>(entity.getNearbyEntities(x, y, z));
        new LogDebug().debug(Level.INFO, worldName, "Found " + nearbyEntities.size() + " nearby entities!");

        // If only entities on the list should be counted
        if (plugin.getConfig().getBoolean("nearby-entities.only-count-from-list")) {
            new LogDebug().debug(Level.INFO,
                worldName,
                "However, only entities on the list should be counted...");

            for (Entity nearbyEntity : new ArrayList<>(nearbyEntities)) {
                EntityData nearbyEntityData = matchEntityFromConfig(nearbyEntity, worldName);

                if (nearbyEntityData == null) {
                    nearbyEntities.remove(nearbyEntity);
                    new LogDebug().debug(Level.INFO,
                        worldName,
                        "Nearby entity " + nearbyEntity.getType() + " was removed from the nearby entity list.");
                }
            }

            new LogDebug().debug(Level.INFO, worldName,
                "Found " + nearbyEntities.size() + " nearby entities that were on the list.");
        }

        if (nearbyEntities.size() > plugin.getConfig().getInt("nearby-entities.count")) removeEntity(entity,
            worldName);
        else {
            new LogDebug().debug(Level.INFO, worldName, "Not enough entities nearby! Skipping...");
            new LogDebug().debug(Level.INFO, "", "");
        }
    }

    private void removeEntity(Entity entity, String worldName) {
        entity.remove();
        removedEntities++;

        new LogDebug().debug(Level.INFO, worldName,
            entity.getType() + " removed! Total removed is " + removedEntities + ".");
        new LogDebug().debug(Level.INFO, "", "");
    }

    private void removeEntitiesPostTask() {
        new LogDebug().debug(Level.INFO, "", "");
        new LogDebug().debug(Level.INFO, "", "╔══════════════════════════════════════╗");
        new LogDebug().debug(Level.INFO, "", "║           TASKS COMPLETED            ║");
        new LogDebug().debug(Level.INFO, "", "║      IF SUPPORT IS NEEDED, FIND      ║");
        new LogDebug().debug(Level.INFO, "", "║     THE DEBUG FILE LOCATED IN THE    ║");
        new LogDebug().debug(Level.INFO, "", "║         ENTITYCLEARER FOLDER         ║");
        new LogDebug().debug(Level.INFO, "", "║         AND SEND IT TO US AT         ║");
        new LogDebug().debug(Level.INFO, "", "║     https://discord.gg/p6FuXyx6wA    ║");
        new LogDebug().debug(Level.INFO, "", "╚══════════════════════════════════════╝");
        new LogDebug().debug(Level.INFO, "", "");
        if (LogDebug.debugActive) {
            try {
                LogDebug.debugFile.close();
            } catch (IOException e) {
                new LogDebug().debug(Level.INFO, "", e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.INFO, "", ste.toString());
            }
            LogDebug.debugActive = false;
        }
    }

    private void sendActionBar(World world, Player player, boolean tpsLow) {
        if (tpsLow) {
            if (!plugin.getConfig().getString("messages.actionbar-completed-low-tps-message").isBlank())
                bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                    .deserialize(new ParseMessage().parse(plugin.getConfig()
                        .getString("messages.actionbar-completed-low-tps-message")
                        .replace("{ENTITIES}", String.valueOf(removedEntities)))));

        } else if (!plugin.getConfig().getString("messages.actionbar-completed-message").isBlank()) {
            new LogDebug().debug(Level.INFO,
                world.getName(),
                "Sending action bar to player " + player.getName());

            bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                .deserialize(new ParseMessage().parse(plugin.getConfig()
                    .getString("messages.actionbar-completed-message")
                    .replace("{ENTITIES}", String.valueOf(removedEntities)))));
        }
    }

    private void sendChat(World world, Player player, boolean tpsLow) {
        if (tpsLow) {
            if (!plugin.getConfig().getString("messages.chat-completed-low-tps-message").isBlank())
                bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage()
                    .deserialize(new ParseMessage()
                        .parse(plugin.getConfig().getString("messages.chat-completed-low-tps-message"))
                        .replace("{ENTITIES}", String.valueOf(removedEntities))));

        } else if (!plugin.getConfig().getString("messages.chat-completed-message").isBlank()) {
            new LogDebug().debug(Level.INFO,
                world.getName(),
                "Sending message to player " + player.getName());

            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage()
                .deserialize(new ParseMessage()
                    .parse(plugin.getConfig().getString("messages.chat-completed-message"))
                    .replace("{ENTITIES}", String.valueOf(removedEntities))));
        }
    }

    private void playSound(World world, Player player) {
        String worldName = world.getName();

        new LogDebug().debug(Level.INFO,
            worldName,
            "Playing sound " + plugin.getConfig().getString("sound") + " at player " + player.getName());

        try {
            player.playSound(
                player.getLocation(),
                "minecraft:" + plugin.getConfig().getString("sound"),
                SoundCategory.MASTER,
                1,
                Float.parseFloat(plugin.getConfig().getString("cleared-pitch")));

        } catch (NumberFormatException e) {
            new LogDebug().error(world.getName(),
                "Cleared pitch \"" + plugin.getConfig().getString("cleared-pitch") + "\" is not a number!");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, worldName, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, worldName, ste.toString());
            } else e.printStackTrace();
        }
    }
}
