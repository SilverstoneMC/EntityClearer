package me.jasonhorkles.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

public class ClearTask implements CommandExecutor {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final MythicPlugin mythicPlugin = EntityClearer.getInstance().getMythicPlugin();
    private int removedEntities;
    private static boolean logCooldown = false;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Clearing entities...");
        // If it should count down first
        // Otherwise just go
        if (plugin.getConfig().getBoolean("countdown-on-command")) new Countdown().countdown();
        else removeEntities(false);
        return true;
    }

    @SuppressWarnings("DataFlowIssue")
    public void removeEntities(boolean tpsLow) {
        new Utils().logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        new Utils().logDebug(Level.INFO, "║     REMOVE ENTITIES TASK STARTED     ║");
        new Utils().logDebug(Level.INFO, "╚══════════════════════════════════════╝");

        if (mythicPlugin != null) new Utils().logDebug(Level.INFO, "MythicMobs plugin found!");

        removedEntities = 0;

        String path = "worlds";
        if (tpsLow) if (plugin.getConfig().getBoolean("low-tps.separate-entity-list")) {
            new Utils().logDebug(Level.INFO, "Separate entity list enabled!");
            path = "low-tps.worlds";
        }

        ArrayList<World> worlds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(
            plugin.getConfig().getConfigurationSection(path).getKeys(false));

        if (keys.contains("ALL")) {
            new Utils().logDebug(Level.INFO, "'ALL' found! Adding all worlds to removal list...");
            worlds.addAll(Bukkit.getWorlds());
        } else {
            new Utils().logDebug(Level.INFO, "Adding all worlds defined in config to removal list...");
            for (String world : keys) worlds.add(Bukkit.getWorld(world));
        }

        new Utils().logDebug(Level.INFO, "World list: ");
        for (World world : worlds) new Utils().logDebug(Level.INFO, world.getName());
        new Utils().logDebug(Level.INFO, "");

        try {
            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;

                // If that world doesn't exist, complain
                if (world == null) {
                    new Utils().sendError("Couldn't find the world \"" + keys.get(
                        index) + "\"! Please double check your config.");
                    continue;
                }

                String worldName = world.getName();
                if (keys.contains("ALL")) worldName = "ALL";

                // Get the loaded entities
                for (Entity entity : world.getEntities())
                    // For each entity type in the config
                    for (String entityType : plugin.getConfig()
                        .getStringList(path + "." + worldName + ".entities")) {
                        // If the entity is a MythicMob
                        boolean isValidMythicMob = false;

                        if (mythicPlugin != null) {
                            ActiveMob mythicMob = MythicBukkit.inst().getMobManager()
                                .getActiveMob(entity.getUniqueId()).orElse(null);

                            // Check if mob is vanilla
                            if (mythicMob != null && !entity.getType().toString()
                                .equalsIgnoreCase(mythicMob.getMobType()))
                                if (entityType.startsWith("MythicMob:")) {
                                    if (mythicMob.getMobType()
                                        .equalsIgnoreCase(entityType.replaceFirst("MythicMob:", ""))) {
                                        new Utils().logDebug(Level.INFO, "Entity is a MythicMob!");
                                        isValidMythicMob = true;
                                        entityType = entityType.replaceFirst("MythicMob:", "");
                                    }
                                } else continue;
                        }


                        // If the entity is actually in the config
                        if (entity.getType().toString().equalsIgnoreCase(entityType) || isValidMythicMob) {
                            if (isValidMythicMob) new Utils().logDebug(Level.INFO,
                                "MythicMob '" + MythicBukkit.inst().getMobManager()
                                    .getActiveMob(entity.getUniqueId()).orElse(null)
                                    .getMobType() + "' matches the config's!");
                            else new Utils().logDebug(Level.INFO,
                                "Entity " + entity.getType() + " matches the config's!");

                            if (entity.getType() == EntityType.DROPPED_ITEM) {
                                new Utils().logDebug(Level.INFO,
                                    "Skipping detection of spawn reasons and nearby entities...");
                                checkNamed(entity, isValidMythicMob);
                                continue;
                            }

                            // If only entities with a specific reason should be removed
                            if (plugin.getConfig()
                                .getBoolean(path + "." + worldName + ".spawn-reason.enabled")) {
                                new Utils().logDebug(Level.INFO,
                                    "Only removing entities with a specific spawn reason...");

                                try {
                                    // For each spawn reason in the config
                                    // If the entity's spawn reason matches the config's
                                    for (String spawnReason : plugin.getConfig()
                                        .getStringList(path + "." + worldName + ".spawn-reason.reasons"))
                                        if (entity.getEntitySpawnReason().name()
                                            .equalsIgnoreCase(spawnReason)) {
                                            new Utils().logDebug(Level.INFO,
                                                entity.getType() + "'s spawn reason " + entity.getEntitySpawnReason() + " matches the config's!");
                                            checkNearby(entity, path, worldName, isValidMythicMob);
                                        }

                                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                                    if (logCooldown) continue;

                                    new Utils().sendError(
                                        "Unable to check for entity spawn reason! Are you not running Paper?");
                                    new Utils().logDebug(Level.WARNING,
                                        "Please use Paper or its forks for this feature to work.");

                                    if (Utils.debug) {
                                        new Utils().logDebug(Level.SEVERE, e.toString());
                                        for (StackTraceElement ste : e.getStackTrace())
                                            new Utils().logDebug(Level.SEVERE, ste.toString());
                                    } else if (plugin.getConfig().getBoolean("print-stack-traces"))
                                        e.printStackTrace();

                                    logCooldown = true;
                                    BukkitRunnable cooldown = new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            logCooldown = false;
                                        }
                                    };
                                    cooldown.runTaskLater(plugin, 200);
                                }

                                // If any entity should be removed, regardless of the spawn reason
                            } else {
                                new Utils().logDebug(Level.INFO,
                                    "Removing entities regardless of their spawn reason...");
                                checkNearby(entity, path, worldName, isValidMythicMob);
                            }
                        }
                    }
            }

            for (String command : plugin.getConfig().getStringList("commands"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            // Submit stats
            int finalRemovedEntities = removedEntities;
            if (removedEntities > 0) EntityClearer.getInstance().getMetrics()
                .addCustomChart(new SingleLineChart("entities_removed", () -> finalRemovedEntities));

            // For each world in the config
            index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    new Utils().sendError("Couldn't find the world \"" + keys.get(
                        index) + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.actionbar-completed-low-tps-message")
                            .isBlank()) {
                            new Utils().logDebug(Level.INFO,
                                "Sending low TPS action bar to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                                .deserialize(new Utils().parseMessage(plugin.getConfig()
                                    .getString("messages.actionbar-completed-low-tps-message")
                                    .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                        }
                    } else if (!plugin.getConfig().getString("messages.actionbar-completed-message")
                        .isBlank()) {
                        new Utils().logDebug(Level.INFO,
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            new Utils().parseMessage(
                                plugin.getConfig().getString("messages.actionbar-completed-message")
                                    .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                    }

                    // Chat
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.chat-completed-low-tps-message")
                            .isBlank()) {
                            new Utils().logDebug(Level.INFO,
                                "Sending low TPS message to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                                new Utils().parseMessage(
                                        plugin.getConfig().getString("messages.chat-completed-low-tps-message"))
                                    .replace("{ENTITIES}", String.valueOf(removedEntities))));
                        }
                    } else if (!plugin.getConfig().getString("messages.chat-completed-message").isBlank()) {
                        new Utils().logDebug(Level.INFO,
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            new Utils().parseMessage(
                                    plugin.getConfig().getString("messages.chat-completed-message"))
                                .replace("{ENTITIES}", String.valueOf(removedEntities))));
                    }

                    // Play the sound
                    new Utils().logDebug(Level.INFO, "Playing sound " + plugin.getConfig().getString(
                        "sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(),
                            "minecraft:" + plugin.getConfig().getString("sound"), SoundCategory.MASTER, 1,
                            Float.parseFloat(plugin.getConfig().getString("cleared-pitch")));
                    } catch (NumberFormatException e) {
                        new Utils().sendError("Cleared pitch \"" + plugin.getConfig()
                            .getString("cleared-pitch") + "\" is not a number!");

                        if (Utils.debug) {
                            new Utils().logDebug(Level.SEVERE, e.toString());
                            for (StackTraceElement ste : e.getStackTrace())
                                new Utils().logDebug(Level.SEVERE, ste.toString());
                        } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (Error | Exception e) {
            new Utils().sendError("Something went wrong clearing entities! Is your config outdated?");

            if (Utils.debug) {
                new Utils().logDebug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new Utils().logDebug(Level.SEVERE, ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
            else new Utils().logDebug(Level.WARNING,
                    "Enable 'print-stack-traces' in your config to see the whole error.");
        }

        new Utils().logDebug(Level.INFO, "");
        new Utils().logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        new Utils().logDebug(Level.INFO, "║           TASKS COMPLETED            ║");
        new Utils().logDebug(Level.INFO, "║      IF SUPPORT IS NEEDED, FIND      ║");
        new Utils().logDebug(Level.INFO, "║     THE DUMP FILE LOCATED IN THE     ║");
        new Utils().logDebug(Level.INFO, "║         ENTITYCLEARER FOLDER         ║");
        new Utils().logDebug(Level.INFO, "║         AND SEND IT TO US AT         ║");
        new Utils().logDebug(Level.INFO, "║     https://discord.gg/p6FuXyx6wA    ║");
        new Utils().logDebug(Level.INFO, "╚══════════════════════════════════════╝");
        if (Utils.debug) {
            try {
                Utils.debugFile.close();
            } catch (IOException e) {
                new Utils().logDebug(Level.INFO, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new Utils().logDebug(Level.INFO, ste.toString());
            }
            Utils.debug = false;
        }
    }

    private void checkNearby(Entity entity, String path, String worldName, boolean isMythicMob) {
        boolean nearby = plugin.getConfig().getBoolean("nearby-entities.enabled");
        boolean onlyCountFromList = plugin.getConfig().getBoolean("nearby-entities.only-count-from-list");
        double x = plugin.getConfig().getDouble("nearby-entities.x");
        double y = plugin.getConfig().getDouble("nearby-entities.y");
        double z = plugin.getConfig().getDouble("nearby-entities.z");
        int count = plugin.getConfig().getInt("nearby-entities.count");

        // If the config option is enabled
        if (nearby) {
            new Utils().logDebug(Level.INFO, "Checking nearby entity count...");

            ArrayList<Entity> nearbyEntities = new ArrayList<>(entity.getNearbyEntities(x, y, z));

            new Utils().logDebug(Level.INFO, "Found " + nearbyEntities.size() + " nearby entities.");

            if (onlyCountFromList) {
                new Utils().logDebug(Level.INFO, "However, only entities on the list should be counted...");

                for (Entity nearbyEntity : new ArrayList<>(nearbyEntities)) {
                    boolean isInList = false;

                    for (String entityType : plugin.getConfig()
                        .getStringList(path + "." + worldName + ".entities")) {
                        boolean nearbyIsMythicMob = false;

                        // If the nearby entity is a MythicMob
                        if (mythicPlugin != null) {
                            ActiveMob mythicMob = MythicBukkit.inst().getMobManager()
                                .getActiveMob(nearbyEntity.getUniqueId()).orElse(null);

                            // Check if mob is vanilla
                            if (mythicMob != null && !nearbyEntity.getType().toString()
                                .equalsIgnoreCase(mythicMob.getMobType())) {
                                nearbyIsMythicMob = true;

                                if (entityType.startsWith("MythicMob:")) if (mythicMob.getMobType()
                                    .equalsIgnoreCase(entityType.replaceFirst("MythicMob:", ""))) {
                                    new Utils().logDebug(Level.INFO,
                                        "Found MythicMob '" + mythicMob.getMobType() + "' from the config nearby!");
                                    isInList = true;
                                    break;
                                }
                            }
                        }

                        if (!nearbyIsMythicMob) if (nearbyEntity.getType().toString().equals(entityType)) {
                            new Utils().logDebug(Level.INFO,
                                "Found entity " + nearbyEntity.getType() + " from the config nearby!");
                            isInList = true;
                            break;
                        }
                    }

                    if (!isInList) {
                        nearbyEntities.remove(nearbyEntity);

                        new Utils().logDebug(Level.INFO,
                            "Nearby entity " + nearbyEntity.getType() + " was removed from the nearby entity list.");
                    }
                }

                new Utils().logDebug(Level.INFO,
                    "Found " + nearbyEntities.size() + " nearby entities that were on the list.");
            }

            if (nearbyEntities.size() > count) checkNamed(entity, isMythicMob);
            else {
                new Utils().logDebug(Level.INFO, "Checking next entity if available...");
                new Utils().logDebug(Level.INFO, "");
            }

            // If nearby check is disabled, just remove the entities
        } else {
            new Utils().logDebug(Level.INFO, "Check nearby entities option disabled.");
            checkNamed(entity, isMythicMob);
        }

    }

    private void checkNamed(Entity entity, boolean isMythicMob) {
        // MythicMobs
        if (isMythicMob) if (plugin.getConfig().getBoolean("remove-named-mythicmobs")) {
            new Utils().logDebug(Level.INFO, "Removing MythicMob regardless of a name...");
            // Remove it!
            new Utils().logDebug(Level.INFO, "Removing MythicMob " + entity.getType() + "...");
            entity.remove();
            removedEntities++;

        } else {
            new Utils().logDebug(Level.INFO, "Removing MythicMob without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                new Utils().logDebug(Level.INFO,
                    "MythicMob " + entity.getType() + " doesn't have a custom name!");
                // Remove it!
                new Utils().logDebug(Level.INFO, "Removing MythicMob " + entity.getType() + "...");
                entity.remove();
                removedEntities++;

            } else {
                new Utils().logDebug(Level.INFO,
                    entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                new Utils().logDebug(Level.INFO, "");
                return;
            }
        }

            // Vanilla
        else if (plugin.getConfig().getBoolean("remove-named")) {
            new Utils().logDebug(Level.INFO, "Removing entity regardless of a name...");
            // Remove it!
            new Utils().logDebug(Level.INFO, "Removing entity " + entity.getType() + "...");
            entity.remove();
            removedEntities++;

        } else {
            new Utils().logDebug(Level.INFO, "Removing entity without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                new Utils().logDebug(Level.INFO,
                    "Entity " + entity.getType() + " doesn't have a custom name!");
                // Remove it!
                new Utils().logDebug(Level.INFO, "Removing entity " + entity.getType() + "...");
                entity.remove();
                removedEntities++;

            } else {
                new Utils().logDebug(Level.INFO,
                    entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                new Utils().logDebug(Level.INFO, "");
                return;
            }
        }

        if (entity.getCustomName() != null) {
            new Utils().logDebug(Level.INFO,
                entity.getType() + " with name " + entity.getCustomName() + " removed! Total removed is " + removedEntities);
            new Utils().logDebug(Level.INFO, "");
        } else {
            new Utils().logDebug(Level.INFO,
                entity.getType() + " removed! Total removed is " + removedEntities + ".");
            new Utils().logDebug(Level.INFO, "");
        }
    }
}
