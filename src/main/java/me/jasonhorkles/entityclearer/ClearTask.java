package me.jasonhorkles.entityclearer;

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

@SuppressWarnings("ConstantConditions")
public class ClearTask implements CommandExecutor {

    private final JavaPlugin plugin = EntityClearer.getInstance();
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().adventure();
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
        logDebug("╔══════════════════════════════════════╗");
        logDebug("║        COUNTDOWN TASK STARTED        ║");
        logDebug("╚══════════════════════════════════════╝");

        int initialTime;
        List<Integer> times = plugin.getConfig().getIntegerList("warning-messages");
        times.sort(Comparator.reverseOrder());
        initialTime = times.get(0);

        logDebug("Starting countdown at " + initialTime + " seconds (" + initialTime / 60 + " minutes)...");
        logDebug("Sending messages at " + times + " seconds remaining...");

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
                logDebug("'ALL' found! Adding all worlds to message list...");
                worlds.addAll(Bukkit.getWorlds());
            } else {
                logDebug("Adding all worlds defined in config to message list...");
                for (String world : keys) worlds.add(Bukkit.getWorld(world));
            }

            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    plugin.getLogger().severe(
                        "Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");

                    for (Player players : Bukkit.getOnlinePlayers())
                        if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                            Component.text("[EntityClearer] Couldn't find the world \"" + keys.get(
                                index) + "\"! Please double check your config.").color(NamedTextColor.RED));

                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (!plugin.getConfig().getString("messages.actionbar-message").isBlank()) {
                        logDebug(
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.actionbar-message")
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time))));
                    }

                    // Chat
                    if (!plugin.getConfig().getString("messages.chat-message").isBlank()) {
                        logDebug(
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.chat-message"))
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time)));
                    }

                    // Play the sound
                    logDebug("Playing sound " + plugin.getConfig()
                        .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                            SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

                    } catch (NumberFormatException e) {
                        for (Player players : Bukkit.getOnlinePlayers())
                            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players)
                                .sendMessage(Component.text("[EntityClearer] Countdown pitch \"" + plugin.getConfig()
                                    .getString("countdown-pitch") + "\" is not a number!").color(NamedTextColor.RED));
                        plugin.getLogger().severe("Countdown pitch \"" + plugin.getConfig()
                            .getString("countdown-pitch") + "\" is not a valid number!");
                        if (debug) {
                            logDebug(e.toString());
                            for (StackTraceElement ste : e.getStackTrace()) logDebug(ste.toString());
                        } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (NullPointerException e) {
            plugin.getLogger().severe("Something went wrong sending messages! Is your config outdated?");
            plugin.getLogger().warning(
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            for (Player players : Bukkit.getOnlinePlayers())
                if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                    Component.text("[EntityClearer] Something went wrong sending messages! Is your config outdated?")
                        .color(NamedTextColor.RED));

            if (debug) {
                logDebug(e.toString());
                for (StackTraceElement ste : e.getStackTrace()) logDebug(ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }

    public void removeEntitiesTask(boolean tpsLow) {
        {
            logDebug("╔══════════════════════════════════════╗");
            logDebug("║     REMOVE ENTITIES TASK STARTED     ║");
            logDebug("╚══════════════════════════════════════╝");
        }

        removedEntities = 0;

        String path = "worlds";
        if (tpsLow) if (plugin.getConfig().getBoolean("low-tps.separate-entity-list")) {
            logDebug("Separate entity list enabled!");
            path = "low-tps.worlds";
        }

        ArrayList<World> worlds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(plugin.getConfig().getConfigurationSection(path).getKeys(false));

        if (keys.contains("ALL")) {
            logDebug("'ALL' found! Adding all worlds to removal list...");
            worlds.addAll(Bukkit.getWorlds());
        } else {
            logDebug("Adding all worlds defined in config to removal list...");
            for (String world : keys) worlds.add(Bukkit.getWorld(world));
        }

        try {
            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    plugin.getLogger().severe(
                        "Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    for (Player players : Bukkit.getOnlinePlayers())
                        if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                            Component.text("[EntityClearer] Couldn't find the world \"" + keys.get(
                                index) + "\"! Please double check your config.").color(NamedTextColor.RED));
                    continue;
                }

                String worldName = world.getName();
                if (keys.contains("ALL")) worldName = "ALL";

                // Get the loaded entities
                // For each entity type in the config
                // If the entity is actually in the config
                for (Entity entities : world.getEntities())
                    for (String entityTypes : plugin.getConfig().getStringList(path + "." + worldName + ".entities"))
                        if (entities.getType().toString().equalsIgnoreCase(entityTypes)) {
                            logDebug("Entity " + entities.getType() + " matches the config's!");

                            if (entities.getType() == EntityType.DROPPED_ITEM) {
                                logDebug("Skipping detection of spawn reasons and nearby entities...");
                                checkNamed(entities);
                                continue;
                            }

                            // If only entities with a specific reason should be removed
                            if (plugin.getConfig().getBoolean(path + "." + worldName + ".spawn-reason.enabled")) {
                                logDebug("Only removing entities with a specific spawn reason...");

                                try {
                                    // For each spawn reason in the config
                                    // If the entity's spawn reason matches the config's
                                    for (String spawnReason : plugin.getConfig()
                                        .getStringList(path + "." + worldName + ".spawn-reason.reasons"))
                                        if (entities.getEntitySpawnReason().name().equalsIgnoreCase(spawnReason)) {
                                            logDebug(
                                                entities.getType() + "'s spawn reason " + entities.getEntitySpawnReason() + " matches the config's!");
                                            checkNearby(entities, path, worldName);
                                        }

                                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                                    if (logCooldown) continue;

                                    plugin.getLogger()
                                        .severe("Unable to check for entity spawn reason! Are you not running Paper?");
                                    plugin.getLogger()
                                        .warning("Please use Paper or its forks for this feature to work.");

                                    for (Player players : Bukkit.getOnlinePlayers())
                                        if (players.hasPermission("entityclearer.notify"))
                                            bukkitAudiences.player(players).sendMessage(Component.text(
                                                    "[EntityClearer] Unable to check for entity spawn reason! Are you not running Paper?")
                                                .color(NamedTextColor.RED));

                                    if (debug) {
                                        logDebug(e.toString());
                                        for (StackTraceElement ste : e.getStackTrace()) logDebug(ste.toString());
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
                                logDebug("Removing entities regardless of their spawn reason...");
                                checkNearby(entities, path, worldName);
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
                    plugin.getLogger().severe(
                        "Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    for (Player players : Bukkit.getOnlinePlayers())
                        if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                            Component.text("[EntityClearer] Couldn't find the world \"" + keys.get(
                                index) + "\"! Please double check your config.").color(NamedTextColor.RED));
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.actionbar-completed-low-tps-message").isBlank()) {
                            logDebug(
                                "Sending low TPS action bar to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                                EntityClearer.parseMessage(
                                    plugin.getConfig().getString("messages.actionbar-completed-low-tps-message")
                                        .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                        }
                    } else if (!plugin.getConfig().getString("messages.actionbar-completed-message").isBlank()) {
                        logDebug(
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(
                                plugin.getConfig().getString("messages.actionbar-completed-message")
                                    .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                    }

                    // Chat
                    if (tpsLow) {
                        if (!plugin.getConfig().getString("messages.chat-completed-low-tps-message").isBlank()) {
                            logDebug(
                                "Sending low TPS message to player " + player.getName() + " in world " + world.getName() + ".");

                            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                                EntityClearer.parseMessage(
                                        plugin.getConfig().getString("messages.chat-completed-low-tps-message"))
                                    .replace("{ENTITIES}", String.valueOf(removedEntities))));
                        }
                    } else if (!plugin.getConfig().getString("messages.chat-completed-message").isBlank()) {
                        logDebug(
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            EntityClearer.parseMessage(plugin.getConfig().getString("messages.chat-completed-message"))
                                .replace("{ENTITIES}", String.valueOf(removedEntities))));
                    }

                    // Play the sound
                    logDebug("Playing sound " + plugin.getConfig()
                        .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                            SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("cleared-pitch")));
                    } catch (NumberFormatException e) {
                        for (Player players : Bukkit.getOnlinePlayers())
                            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players)
                                .sendMessage(Component.text("[EntityClearer] Cleared pitch \"" + plugin.getConfig()
                                    .getString("cleared-pitch") + "\" is not a number!").color(NamedTextColor.RED));

                        plugin.getLogger().severe(
                            "\"" + plugin.getConfig().getString("cleared-pitch") + "Cleared pitch \" is not a number!");

                        for (StackTraceElement ste : e.getStackTrace())
                            if (debug) logDebug(ste.toString());
                            else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (Error | Exception e) {
            plugin.getLogger().severe("Something went wrong clearing entities!");

            for (Player players : Bukkit.getOnlinePlayers())
                if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                    Component.text("[EntityClearer] Something went wrong clearing entities! Is your config outdated?")
                        .color(NamedTextColor.RED));

            if (debug) {
                logDebug(e.toString());
                for (StackTraceElement ste : e.getStackTrace()) logDebug(ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
            else plugin.getLogger().warning("Enable 'print-stack-traces' in your config to see the whole error.");
        }

        {
            logDebug("╔══════════════════════════════════════╗");
            logDebug("║           TASKS COMPLETED            ║");
            logDebug("║      IF SUPPORT IS NEEDED, FIND      ║");
            logDebug("║     THE DUMP FILE LOCATED IN THE     ║");
            logDebug("║         ENTITYCLEARER FOLDER         ║");
            logDebug("║         AND SEND IT TO US AT         ║");
            logDebug("║     https://discord.gg/qcTzC9nMQD    ║");
            logDebug("╚══════════════════════════════════════╝");
            if (debug) {
                try {
                    debugFile.close();
                } catch (IOException e) {
                    logDebug(e.toString());
                    for (StackTraceElement ste : e.getStackTrace()) logDebug(ste.toString());
                }
                debug = false;
            }
        }
    }

    private void checkNearby(Entity entity, String path, String worldName) {
        boolean nearby = plugin.getConfig().getBoolean("nearby-entities.enabled");
        boolean onlyCountFromList = plugin.getConfig().getBoolean("nearby-entities.only-count-from-list");
        double x = plugin.getConfig().getDouble("nearby-entities.x");
        double y = plugin.getConfig().getDouble("nearby-entities.y");
        double z = plugin.getConfig().getDouble("nearby-entities.z");
        int count = plugin.getConfig().getInt("nearby-entities.count");

        // If the config option is enabled
        if (nearby) {
            logDebug("Checking nearby entity count...");

            ArrayList<Entity> nearbyEntities = new ArrayList<>(entity.getNearbyEntities(x, y, z));

            logDebug("Found " + nearbyEntities.size() + " nearby entities.");

            if (onlyCountFromList) {
                logDebug("However, only entities on the list should be counted...");

                for (Entity nearbyEntity : new ArrayList<>(nearbyEntities)) {
                    boolean isInList = false;
                    for (String entityType : plugin.getConfig().getStringList(path + "." + worldName + ".entities"))
                        if (nearbyEntity.getType().toString().equals(entityType)) {
                            isInList = true;
                            break;
                        }

                    if (!isInList) {
                        nearbyEntities.remove(nearbyEntity);

                        logDebug("Entity " + nearbyEntity.getType() + " was removed from the nearby entity list.");
                    }
                }

                logDebug("Found " + nearbyEntities.size() + " nearby entities that were on the list.");
            }

            if (nearbyEntities.size() > count) checkNamed(entity);
            else {
                logDebug("Checking next entity if available...");
                logDebug("");
            }

            // If nearby check is disabled, just remove the entities
        } else {
            logDebug("Check nearby entities option disabled.");
            checkNamed(entity);
        }

    }

    private void checkNamed(Entity entity) {
        // Should remove named
        if (plugin.getConfig().getBoolean("remove-named")) {
            logDebug("Removing entities regardless of a name...");
            // Remove it!
            logDebug("Removing entity " + entity.getType() + "...");
            entity.remove();
            removedEntities++;
        } else {
            logDebug("Removing entities without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                logDebug("Entity " + entity.getType() + " didn't have a custom name!");
                // Remove it!
                logDebug("Removing entity " + entity.getType() + "...");
                entity.remove();
                removedEntities++;
            } else {
                logDebug(entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                logDebug("");
                return;
            }
        }

        if (entity.getCustomName() != null) logDebug(
            entity.getType() + " with name " + entity.getCustomName() + " removed! Total removed is " + removedEntities);
        else {
            logDebug(entity.getType() + " removed! Total removed is " + removedEntities + ".");
            logDebug("");
        }
    }

    private void logDebug(String text) {
        if (!debug) return;

        plugin.getLogger().info(text);
        try {
            debugFile.write(text + "\n");
        } catch (IOException e) {
            if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }
}
