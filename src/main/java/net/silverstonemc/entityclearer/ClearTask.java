package net.silverstonemc.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.silverstonemc.entityclearer.utils.ConfigUtils;
import net.silverstonemc.entityclearer.utils.EntityData;
import net.silverstonemc.entityclearer.utils.LogDebug;
import net.silverstonemc.entityclearer.utils.OnlinePlayers;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@SuppressWarnings({"DataFlowIssue", "resource"})
public class ClearTask {
    private final List<EntityData> entityDataList = new ArrayList<>();
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final MythicPlugin mythicPlugin = EntityClearer.getInstance().getMythicPlugin();
    private int removedEntities;

    public void removeEntitiesPreTask(Iterable<World> worlds, boolean useTpsList, boolean tpsLow) {
        LogDebug debug = new LogDebug();
        debug.debug(Level.INFO, "", "");
        debug.debug(Level.INFO, "", "╔══════════════════════════════════════╗");
        debug.debug(Level.INFO, "", "║     REMOVE ENTITIES TASK STARTED     ║");
        debug.debug(Level.INFO, "", "╚══════════════════════════════════════╝");

        if (mythicPlugin != null) debug.debug(Level.INFO, "", "MythicMobs plugin found!");
        removedEntities = 0;

        try {
            removeEntities(worlds, useTpsList, tpsLow);
        } catch (Error | Exception e) {
            debug.error("SERVER", "Something went wrong clearing entities! Is your config outdated?");

            if (LogDebug.debugActive) {
                debug.debug(Level.SEVERE, "", e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    debug.debug(Level.SEVERE, "", ste.toString());
            } else e.printStackTrace();
        }
    }

    private void removeEntities(Iterable<World> worlds, boolean useTpsList, boolean tpsLow) {
        LogDebug debug = new LogDebug();

        String path = "worlds";
        if (useTpsList) path = "low-tps.worlds";

        // For each world in the config
        for (World world : worlds) {
            String worldName = world.getName();

            debug.debug(Level.INFO, "", "");

            String worldConfigName = ConfigUtils.isAll ? "ALL" : worldName;

            Object[] notEnoughPlayers = new OnlinePlayers().isNotEnough(world, worldConfigName);
            // Skip the world if there aren't enough players online
            if ((boolean) notEnoughPlayers[0]) {
                debug.debug(
                    Level.WARNING,
                    worldName,
                    "Not enough players in the " + notEnoughPlayers[1] + "! Skipping...");
                continue;
            }

            // Save the entity data from the config
            for (String entityType : plugin.getConfig()
                .getStringList(path + "." + worldConfigName + ".entities"))
                entityDataList.add(new EntityData(entityType, worldName));
            if (entityDataList.isEmpty()) debug.error(
                worldName,
                "No entities are set to be removed in the config!");
            debug.debug(Level.INFO, "", "");

            debug.debug(Level.INFO, worldName, "Scanning world...");

            // Get the loaded entities
            for (Entity entity : world.getEntities()) {
                EntityData entityData = matchEntityFromConfig(entity, worldName);

                if (entityData == null) continue;

                // If the entity is a MythicMob matched in the config
                if (entityData.getMythicMobType() != null) {
                    ActiveMob mythicMob = MythicBukkit.inst().getMobManager()
                        .getActiveMob(entity.getUniqueId()).orElse(null);

                    if (mythicMob.getMobType().equalsIgnoreCase(entityData.getMythicMobType())) {
                        checkOccupied(entity, entityData, worldName);
                        continue;
                    }
                }

                // Skip all the checks if a dropped item
                if (entity.getType() == EntityType.ITEM) removeEntity(entity, worldName);
                else checkOccupied(entity, entityData, worldName);
            }

            // For each player in said world
            for (Player player : world.getPlayers()) {
                sendActionBar(world, player, tpsLow);
                sendChat(world, player, tpsLow);
                sendTitle(world, player, tpsLow);
                playSound(world, player);
            }
            sendLog(world, tpsLow);
        }

        for (String command : plugin.getConfig().getStringList("commands"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        removeEntitiesPostTask();
    }

    @Nullable
    private EntityData matchEntityFromConfig(Entity entity, String worldName) {
        LogDebug debug = new LogDebug();

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
                    debug.debug(
                        Level.INFO,
                        worldName,
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
                    debug.debug(
                        Level.WARNING,
                        worldName,
                        "Entity " + mythicMob.getMobType() + " is a MythicMob not in the config! Skipping...");
                    return null;
                }
            }

            debug.debug(Level.INFO, worldName, "Entity " + entity.getType() + " matches the config's!");
            return entityData;
        }

        debug.debug(Level.WARNING, worldName, "Entity " + entity.getType() + " is not listed in the config!");
        return null;
    }

    private void checkOccupied(Entity entity, EntityData entityData, String worldName) {
        // Skip entity if it is occupied and the config doesn't allow it
        if (!entityData.includeOccupied()) for (Entity passenger : entity.getPassengers())
            if (passenger.getType() == EntityType.PLAYER) {
                new LogDebug().debug(
                    Level.INFO,
                    worldName,
                    "Skipping entity " + entity.getType() + " because it is occupied!");
                return;
            }

        checkNamed(entity, entityData, worldName);
    }

    private void checkNamed(Entity entity, EntityData entityData, String worldName) {
        LogDebug debug = new LogDebug();

        String entityType = (entityData.getMythicMobType() != null) ? "MythicMob" : "entity";

        if (entityData.includeNamed()) {
            // We don't care if it has a name
            checkNearby(entity, worldName);
            return;
        }

        // Don't remove named
        debug.debug(Level.INFO, worldName, "Removing " + entityType + " without a name only...");
        // And it doesn't have a name
        if (entity.getCustomName() == null) {
            debug.debug(
                Level.INFO,
                worldName,
                "The " + entityType + " " + entity.getType() + " doesn't have a custom name!");
            checkNearby(entity, worldName);
        }
        // And it does have a name
        else {
            debug.debug(
                Level.INFO,
                worldName,
                entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
            debug.debug(Level.INFO, "", "");
        }
    }

    private void checkNearby(Entity entity, String worldName) {
        LogDebug debug = new LogDebug();

        // If nearby check is disabled, just move on
        if (!plugin.getConfig().getBoolean("nearby-entities.enabled")) {
            debug.debug(Level.INFO, worldName, "Check nearby entities option disabled.");
            removeEntity(entity, worldName);
            return;
        }

        // Get nearby entities
        double x = plugin.getConfig().getDouble("nearby-entities.x");
        double y = plugin.getConfig().getDouble("nearby-entities.y");
        double z = plugin.getConfig().getDouble("nearby-entities.z");
        ArrayList<Entity> nearbyEntities = new ArrayList<>(entity.getNearbyEntities(x, y, z));
        debug.debug(Level.INFO, worldName, "Found " + nearbyEntities.size() + " nearby entities!");

        // If only entities on the list should be counted
        if (plugin.getConfig().getBoolean("nearby-entities.only-count-from-list")) {
            debug.debug(Level.INFO, worldName, "However, only entities on the list should be counted...");

            for (Entity nearbyEntity : new ArrayList<>(nearbyEntities)) {
                EntityData nearbyEntityData = matchEntityFromConfig(nearbyEntity, worldName);

                if (nearbyEntityData == null) {
                    nearbyEntities.remove(nearbyEntity);
                    debug.debug(
                        Level.INFO,
                        worldName,
                        "Nearby entity " + nearbyEntity.getType() + " was removed from the nearby entity list.");
                }
            }

            debug.debug(
                Level.INFO,
                worldName,
                "Found " + nearbyEntities.size() + " nearby entities that were on the list.");
        }

        if (nearbyEntities.size() > plugin.getConfig().getInt("nearby-entities.count")) removeEntity(
            entity,
            worldName);
        else {
            debug.debug(Level.INFO, worldName, "Not enough entities nearby! Skipping...");
            debug.debug(Level.INFO, "", "");
        }
    }

    private void removeEntity(Entity entity, String worldName) {
        entity.remove();
        removedEntities++;

        LogDebug debug = new LogDebug();
        debug.debug(
            Level.INFO,
            worldName,
            entity.getType() + " removed! Total removed is " + removedEntities + ".");
        debug.debug(Level.INFO, "", "");
    }

    private void removeEntitiesPostTask() {
        LogDebug debug = new LogDebug();
        debug.debug(Level.INFO, "", "");
        debug.debug(Level.INFO, "", "╔══════════════════════════════════════╗");
        debug.debug(Level.INFO, "", "║           TASKS COMPLETED.           ║");
        debug.debug(Level.INFO, "", "║      IF SUPPORT IS NEEDED, SEND      ║");
        debug.debug(Level.INFO, "", "║     US THE LINK GIVEN IN CHAT AT     ║");
        debug.debug(Level.INFO, "", "║     https://discord.gg/5XFBx8uZVN    ║");
        debug.debug(Level.INFO, "", "╚══════════════════════════════════════╝");
        if (LogDebug.debugActive) {
            try {
                LogDebug.debugFile.close();

                Path path = Path.of(plugin.getDataFolder().getPath(), "debug");
                File file = new File(path.toFile(), "debug-" + LogDebug.fileId + ".yml");

                if (!plugin.getConfig().getBoolean("disable-paste-upload")) debug.upload(file);
            } catch (IOException e) {
                debug.debug(Level.INFO, "", e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    debug.debug(Level.INFO, "", ste.toString());
            }
            LogDebug.debugActive = false;
            LogDebug.fileId = null;
        }
    }

    private void sendActionBar(World world, Player player, boolean tpsLow) {
        if (!player.hasPermission("entityclearer.removalnotifs.actionbar")) return;

        String path = tpsLow ? "messages.actionbar-completed-low-tps-message" : "messages.actionbar-completed-message";

        if (plugin.getConfig().getString(path).isBlank()) return;

        new LogDebug().debug(
            Level.INFO,
            world.getName(),
            "Sending action bar to player " + player.getName() + " about " + removedEntities + " entities");

        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
            .deserialize(plugin.getConfig().getString(path)
                .replace("{ENTITIES}", String.valueOf(removedEntities))));
    }

    private void sendChat(World world, Player player, boolean tpsLow) {
        if (!player.hasPermission("entityclearer.removalnotifs.chat")) return;

        String path = tpsLow ? "messages.chat-completed-low-tps-message" : "messages.chat-completed-message";

        if (plugin.getConfig().getString(path).isBlank()) return;

        new LogDebug().debug(
            Level.INFO,
            world.getName(),
            "Sending message to player " + player.getName() + " about " + removedEntities + " entities");

        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage()
            .deserialize(plugin.getConfig().getString(path)
                .replace("{ENTITIES}", String.valueOf(removedEntities))));
    }

    private void sendTitle(World world, Player player, boolean tpsLow) {
        if (!player.hasPermission("entityclearer.removalnotifs.title")) return;

        String titlePath = tpsLow ? "messages.title-completed-low-tps-message" : "messages.title-completed-message";
        String subtitlePath = tpsLow ? "messages.subtitle-completed-low-tps-message" : "messages.subtitle-completed-message";

        if (plugin.getConfig().getString(titlePath).isBlank() && plugin.getConfig().getString(subtitlePath)
            .isBlank()) return;

        new LogDebug().debug(
            Level.INFO,
            world.getName(),
            "Sending action bar to player " + player.getName() + " about " + removedEntities + " entities");

        Title title = Title.title(
            MiniMessage.miniMessage().deserialize(plugin.getConfig().getString(titlePath)
                .replace("{ENTITIES}", String.valueOf(removedEntities))),
            MiniMessage.miniMessage().deserialize(plugin.getConfig().getString(subtitlePath)
                .replace("{ENTITIES}", String.valueOf(removedEntities))));
        bukkitAudiences.player(player).showTitle(title);
    }

    private void playSound(World world, Player player) {
        if (!player.hasPermission("entityclearer.removalnotifs.sound")) return;

        LogDebug debug = new LogDebug();
        String worldName = world.getName();

        debug.debug(
            Level.INFO,
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
            debug.error(
                world.getName(),
                "Cleared pitch \"" + plugin.getConfig().getString("cleared-pitch") + "\" is not a number!");

            if (LogDebug.debugActive) {
                debug.debug(Level.SEVERE, worldName, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    debug.debug(Level.SEVERE, worldName, ste.toString());
            } else e.printStackTrace();
        }
    }

    private void sendLog(World world, boolean tpsLow) {
        String worldName = world.getName().toUpperCase() + ": ";

        if (tpsLow) {
            if (plugin.getConfig().getString("messages.log-completed-low-tps-message").isBlank()) return;

            bukkitAudiences.console().sendMessage(MiniMessage.miniMessage()
                .deserialize(worldName + plugin.getConfig()
                    .getString("messages.log-completed-low-tps-message")
                    .replace("{ENTITIES}", String.valueOf(removedEntities))));

        } else if (!plugin.getConfig().getString("messages.log-completed-message").isBlank())

            bukkitAudiences.console().sendMessage(MiniMessage.miniMessage()
                .deserialize(worldName + plugin.getConfig().getString("messages.log-completed-message")
                    .replace("{ENTITIES}", String.valueOf(removedEntities))));
    }
}
