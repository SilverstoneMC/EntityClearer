package me.jasonhorkles.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

@SuppressWarnings("ConstantConditions")
public class ClearTask implements CommandExecutor {

    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final MythicPlugin mythicPlugin = EntityClearer.getInstance().getMythicPlugin();
    private int removedEntities;
    private static boolean logCooldown = false;

    public static boolean debug = false;
    public static FileWriter debugFile;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Clearing entities...");
        // If it should count down first
        // Otherwise just go
        if (plugin.getConfig().getBoolean("countdown-on-command")) countdown();
        else removeEntitiesTask(false);
        return true;
    }

    public void countdown() {
        logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        logDebug(Level.INFO, "║        COUNTDOWN TASK STARTED        ║");
        logDebug(Level.INFO, "╚══════════════════════════════════════╝");

        int initialTime;
        List<Integer> times = plugin.getConfig().getIntegerList("warning-messages");
        times.sort(Comparator.reverseOrder());
        initialTime = times.get(0);

        logDebug(Level.INFO, "Starting countdown at " + initialTime + " seconds (" + initialTime / 60 + " minutes)...");
        logDebug(Level.INFO, "Sending messages at " + times + " seconds remaining...");

        final int[] timeLeft = {initialTime};
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft[0] <= 0) {
                    removeEntitiesTask(false);
                    this.cancel();
                    return;
                }

                if (timeLeft[0] <= times.get(0)) {
                    message(timeLeft[0]);
                    times.remove(0);
                }

                timeLeft[0] -= 1;
            }
        };
        task.runTaskTimer(plugin, 0, 20);
    }

    public void message(int timeLeft) {
        try {
            StringBuilder time = new StringBuilder();
            int divideBy = 1;
            if (timeLeft > 60) {
                divideBy = 60;
                time.append("minute");
            } else time.append("second");
            if (timeLeft != 1) time.append("s");

            ArrayList<World> worlds = new ArrayList<>();
            ArrayList<String> keys = new ArrayList<>(
                plugin.getConfig().getConfigurationSection("worlds").getKeys(false));

            if (keys.contains("ALL")) {
                logDebug(Level.INFO, "'ALL' found! Adding all worlds to message list...");
                worlds.addAll(Bukkit.getWorlds());
            } else {
                logDebug(Level.INFO, "Adding all worlds defined in config to message list...");
                for (String world : keys) worlds.add(Bukkit.getWorld(world));
            }

            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    sendError("Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (!plugin.getConfig().getString("messages.actionbar-message").isBlank()) {
                        logDebug(Level.INFO,
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.actionbar-message")
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time))));
                    }

                    // Chat
                    if (!plugin.getConfig().getString("messages.chat-message").isBlank()) {
                        logDebug(Level.INFO,
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.chat-message"))
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time)));
                    }

                    // Play the sound
                    logDebug(Level.INFO, "Playing sound " + plugin.getConfig()
                        .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                            SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

                    } catch (NumberFormatException e) {
                        sendError("Countdown pitch '" + plugin.getConfig()
                            .getString("countdown-pitch") + "' is not a valid number!");

                        if (debug) {
                            logDebug(Level.SEVERE, e.toString());
                            for (StackTraceElement ste : e.getStackTrace()) logDebug(Level.SEVERE, ste.toString());
                        } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (NullPointerException e) {
            sendError("Something went wrong sending messages! Is your config outdated?");
            logDebug(Level.WARNING,
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            if (debug) {
                logDebug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace()) logDebug(Level.SEVERE, ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }

    public void removeEntitiesTask(boolean tpsLow) {
        logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        logDebug(Level.INFO, "║     REMOVE ENTITIES TASK STARTED     ║");
        logDebug(Level.INFO, "╚══════════════════════════════════════╝");

        if (mythicPlugin != null) logDebug(Level.INFO, "MythicMobs plugin found!");

        removedEntities = 0;

        String path = "worlds";
        if (tpsLow) if (plugin.getConfig().getBoolean("low-tps.separate-entity-list")) {
            logDebug(Level.INFO, "Separate entity list enabled!");
            path = "low-tps.worlds";
        }

        ArrayList<World> worlds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(plugin.getConfig().getConfigurationSection(path).getKeys(false));

        if (keys.contains("ALL")) {
            logDebug(Level.INFO, "'ALL' found! Adding all worlds to removal list...");
            worlds.addAll(Bukkit.getWorlds());
        } else {
            logDebug(Level.INFO, "Adding all worlds defined in config to removal list...");
            for (String world : keys) worlds.add(Bukkit.getWorld(world));
        }

        logDebug(Level.INFO, "");

        try {
            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;

                // If that world doesn't exist, complain
                if (world == null) {
                    sendError("Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    continue;
                }

                String worldName = world.getName();
                if (keys.contains("ALL")) worldName = "ALL";

                // Get the loaded entities
                for (Entity entity : world.getEntities())
                    // For each entity type in the config
                    for (String entityType : plugin.getConfig().getStringList(path + "." + worldName + ".entities")) {
                        // If the entity is a MythicMob
                        boolean isValidMythicMob = false;

                        if (mythicPlugin != null) {
                            ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                                .orElse(null);

                            // Check if mob is vanilla
                            if (mythicMob != null && !entity.getType().toString()
                                .equalsIgnoreCase(mythicMob.getMobType())) if (entityType.startsWith("MythicMob:")) {
                                if (mythicMob.getMobType()
                                    .equalsIgnoreCase(entityType.replaceFirst("MythicMob:", ""))) {
                                    logDebug(Level.INFO, "Entity is a MythicMob!");
                                    isValidMythicMob = true;
                                    entityType = entityType.replaceFirst("MythicMob:", "");
                                }
                            } else continue;
                        }


                        // If the entity is actually in the config
                        if (entity.getType().toString().equalsIgnoreCase(entityType) || isValidMythicMob) {
                            if (isValidMythicMob) logDebug(Level.INFO,
                                "MythicMob '" + MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                                    .orElse(null).getMobType() + "' matches the config's!");
                            else logDebug(Level.INFO, "Entity " + entity.getType() + " matches the config's!");

                            if (entity.getType() == EntityType.DROPPED_ITEM) {
                                logDebug(Level.INFO, "Skipping detection of spawn reasons and nearby entities...");
                                checkNamed(entity, isValidMythicMob);
                                continue;
                            }

                            // If only entities with a specific reason should be removed
                            if (plugin.getConfig().getBoolean(path + "." + worldName + ".spawn-reason.enabled")) {
                                logDebug(Level.INFO, "Only removing entities with a specific spawn reason...");

                                try {
                                    // For each spawn reason in the config
                                    // If the entity's spawn reason matches the config's
                                    for (String spawnReason : plugin.getConfig()
                                        .getStringList(path + "." + worldName + ".spawn-reason.reasons"))
                                        if (entity.getEntitySpawnReason().name().equalsIgnoreCase(spawnReason)) {
                                            logDebug(Level.INFO,
                                                entity.getType() + "'s spawn reason " + entity.getEntitySpawnReason() + " matches the config's!");
                                            checkNearby(entity, path, worldName, isValidMythicMob);
                                        }

                                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                                    if (logCooldown) continue;

                                    sendError("Unable to check for entity spawn reason! Are you not running Paper?");
                                    logDebug(Level.WARNING, "Please use Paper or its forks for this feature to work.");

                                    if (debug) {
                                        logDebug(Level.SEVERE, e.toString());
                                        for (StackTraceElement ste : e.getStackTrace())
                                            logDebug(Level.SEVERE, ste.toString());
                                    } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();

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
                                logDebug(Level.INFO, "Removing entities regardless of their spawn reason...");
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
                    sendError("Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.actionbar-completed-low-tps-message").isBlank()) {
                            logDebug(Level.INFO,
                                "Sending low TPS action bar to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                                EntityClearer.parseMessage(
                                    plugin.getConfig().getString("messages.actionbar-completed-low-tps-message")
                                        .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                        }
                    } else if (!plugin.getConfig().getString("messages.actionbar-completed-message").isBlank()) {
                        logDebug(Level.INFO,
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(
                                plugin.getConfig().getString("messages.actionbar-completed-message")
                                    .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                    }

                    // Chat
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.chat-completed-low-tps-message").isBlank()) {
                            logDebug(Level.INFO,
                                "Sending low TPS message to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                                EntityClearer.parseMessage(
                                        plugin.getConfig().getString("messages.chat-completed-low-tps-message"))
                                    .replace("{ENTITIES}", String.valueOf(removedEntities))));
                        }
                    } else if (!plugin.getConfig().getString("messages.chat-completed-message").isBlank()) {
                        logDebug(Level.INFO,
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.chat-completed-message"))
                                .replace("{ENTITIES}", String.valueOf(removedEntities))));
                    }

                    // Play the sound
                    logDebug(Level.INFO, "Playing sound " + plugin.getConfig()
                        .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                            SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("cleared-pitch")));
                    } catch (NumberFormatException e) {
                        sendError(
                            "Cleared pitch \"" + plugin.getConfig().getString("cleared-pitch") + "\" is not a number!");

                        if (debug) {
                            logDebug(Level.SEVERE, e.toString());
                            for (StackTraceElement ste : e.getStackTrace()) logDebug(Level.SEVERE, ste.toString());
                        } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (Error | Exception e) {
            sendError("Something went wrong clearing entities! Is your config outdated?");

            if (debug) {
                logDebug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace()) logDebug(Level.SEVERE, ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
            else logDebug(Level.WARNING, "Enable 'print-stack-traces' in your config to see the whole error.");
        }

        logDebug(Level.INFO, "");
        logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        logDebug(Level.INFO, "║           TASKS COMPLETED            ║");
        logDebug(Level.INFO, "║      IF SUPPORT IS NEEDED, FIND      ║");
        logDebug(Level.INFO, "║     THE DUMP FILE LOCATED IN THE     ║");
        logDebug(Level.INFO, "║         ENTITYCLEARER FOLDER         ║");
        logDebug(Level.INFO, "║         AND SEND IT TO US AT         ║");
        logDebug(Level.INFO, "║     https://discord.gg/p6FuXyx6wA    ║");
        logDebug(Level.INFO, "╚══════════════════════════════════════╝");
        if (debug) {
            try {
                debugFile.close();
            } catch (IOException e) {
                logDebug(Level.INFO, e.toString());
                for (StackTraceElement ste : e.getStackTrace()) logDebug(Level.INFO, ste.toString());
            }
            debug = false;
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
            logDebug(Level.INFO, "Checking nearby entity count...");

            ArrayList<Entity> nearbyEntities = new ArrayList<>(entity.getNearbyEntities(x, y, z));

            logDebug(Level.INFO, "Found " + nearbyEntities.size() + " nearby entities.");

            if (onlyCountFromList) {
                logDebug(Level.INFO, "However, only entities on the list should be counted...");

                for (Entity nearbyEntity : new ArrayList<>(nearbyEntities)) {
                    boolean isInList = false;

                    for (String entityType : plugin.getConfig().getStringList(path + "." + worldName + ".entities")) {
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
                                    logDebug(Level.INFO,
                                        "Found MythicMob '" + mythicMob.getMobType() + "' from the config nearby!");
                                    isInList = true;
                                    break;
                                }
                            }
                        }

                        if (!nearbyIsMythicMob) if (nearbyEntity.getType().toString().equals(entityType)) {
                            logDebug(Level.INFO, "Found entity " + nearbyEntity.getType() + " from the config nearby!");
                            isInList = true;
                            break;
                        }
                    }

                    if (!isInList) {
                        nearbyEntities.remove(nearbyEntity);

                        logDebug(Level.INFO,
                            "Nearby entity " + nearbyEntity.getType() + " was removed from the nearby entity list.");
                    }
                }

                logDebug(Level.INFO, "Found " + nearbyEntities.size() + " nearby entities that were on the list.");
            }

            if (nearbyEntities.size() > count) checkNamed(entity, isMythicMob);
            else {
                logDebug(Level.INFO, "Checking next entity if available...");
                logDebug(Level.INFO, "");
            }

            // If nearby check is disabled, just remove the entities
        } else {
            logDebug(Level.INFO, "Check nearby entities option disabled.");
            checkNamed(entity, isMythicMob);
        }

    }

    private void checkNamed(Entity entity, boolean isMythicMob) {
        // MythicMobs
        if (isMythicMob) if (plugin.getConfig().getBoolean("remove-named-mythicmobs")) {
            logDebug(Level.INFO, "Removing MythicMob regardless of a name...");
            // Remove it!
            logDebug(Level.INFO, "Removing MythicMob " + entity.getType() + "...");
            entity.remove();
            removedEntities++;

        } else {
            logDebug(Level.INFO, "Removing MythicMob without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                logDebug(Level.INFO, "MythicMob " + entity.getType() + " doesn't have a custom name!");
                // Remove it!
                logDebug(Level.INFO, "Removing MythicMob " + entity.getType() + "...");
                entity.remove();
                removedEntities++;

            } else {
                logDebug(Level.INFO,
                    entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                logDebug(Level.INFO, "");
                return;
            }
        }

            // Vanilla
        else if (plugin.getConfig().getBoolean("remove-named")) {
            logDebug(Level.INFO, "Removing entity regardless of a name...");
            // Remove it!
            logDebug(Level.INFO, "Removing entity " + entity.getType() + "...");
            entity.remove();
            removedEntities++;

        } else {
            logDebug(Level.INFO, "Removing entity without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                logDebug(Level.INFO, "Entity " + entity.getType() + " doesn't have a custom name!");
                // Remove it!
                logDebug(Level.INFO, "Removing entity " + entity.getType() + "...");
                entity.remove();
                removedEntities++;

            } else {
                logDebug(Level.INFO,
                    entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                logDebug(Level.INFO, "");
                return;
            }
        }

        if (entity.getCustomName() != null) {
            logDebug(Level.INFO,
                entity.getType() + " with name " + entity.getCustomName() + " removed! Total removed is " + removedEntities);
            logDebug(Level.INFO, "");
        } else {
            logDebug(Level.INFO, entity.getType() + " removed! Total removed is " + removedEntities + ".");
            logDebug(Level.INFO, "");
        }
    }

    private void logDebug(Level level, String message) {
        if (!debug) return;

        plugin.getLogger().log(level, message);
        try {
            debugFile.write(message + "\n");
        } catch (IOException e) {
            if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }

    private void sendError(String message) {
        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players)
                .sendMessage(Component.text("[EntityClearer] " + message).color(NamedTextColor.RED));

        logDebug(Level.SEVERE, message);
    }
}
