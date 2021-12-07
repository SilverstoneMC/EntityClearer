package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

@SuppressWarnings({"ConstantConditions"})
public class ClearTask implements CommandExecutor {

    private static final JavaPlugin plugin = EntityClearer.getInstance();
    private static final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().adventure();

    private static int removedEntities;

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Clearing entities...");
        // If it should count down first
        // Otherwise just go
        if (plugin.getConfig().getBoolean("countdown-on-command")) countdown();
        else removeEntitiesTask(false);
        return true;
    }

    public static void countdown() {
        boolean debug = plugin.getConfig().getBoolean("debug");
        if (debug) {
            plugin.getLogger().info("╔══════════════════════════════════════╗");
            plugin.getLogger().info("║        COUNTDOWN TASK STARTED        ║");
            plugin.getLogger().info("╚══════════════════════════════════════╝");
        }

        int time = 0;
        // Get the lowest time to count down from
        if (plugin.getConfig().getBoolean("warning-messages.60-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 60 seconds...");
            time = 60;
        } else if (plugin.getConfig().getBoolean("warning-messages.45-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 45 seconds...");
            time = 45;
        } else if (plugin.getConfig().getBoolean("warning-messages.30-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 30 seconds...");
            time = 30;
        } else if (plugin.getConfig().getBoolean("warning-messages.15-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 15 seconds...");
            time = 15;
        } else if (plugin.getConfig().getBoolean("warning-messages.5-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 5 seconds...");
            time = 5;
        } else if (plugin.getConfig().getBoolean("warning-messages.4-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 4 seconds...");
            time = 4;
        } else if (plugin.getConfig().getBoolean("warning-messages.3-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 3 seconds...");
            time = 3;
        } else if (plugin.getConfig().getBoolean("warning-messages.2-seconds")) {
            if (debug) plugin.getLogger().info("Starting at 3 seconds...");
            time = 2;
        } else if (plugin.getConfig().getBoolean("warning-messages.1-second")) {
            if (debug) plugin.getLogger().info("Starting at 1 second...");
            time = 1;
        }
        final int[] timeLeft = {time};
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                switch (timeLeft[0]) {
                    case 60:
                        if (plugin.getConfig().getBoolean("warning-messages.60-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 45:
                        if (plugin.getConfig().getBoolean("warning-messages.45-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 30:
                        if (plugin.getConfig().getBoolean("warning-messages.30-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 15:
                        if (plugin.getConfig().getBoolean("warning-messages.15-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 5:
                        if (plugin.getConfig().getBoolean("warning-messages.5-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 4:
                        if (plugin.getConfig().getBoolean("warning-messages.4-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 3:
                        if (plugin.getConfig().getBoolean("warning-messages.3-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 2:
                        if (plugin.getConfig().getBoolean("warning-messages.2-seconds"))
                            message(timeLeft[0], true);
                        break;
                    case 1:
                        if (plugin.getConfig().getBoolean("warning-messages.1-second"))
                            message(timeLeft[0], false);
                        break;
                    case 0:
                        removeEntitiesTask(false);
                        this.cancel();
                }
                timeLeft[0] = timeLeft[0] - 1;
            }
        };
        task.runTaskTimer(plugin, 0, 20);
    }

    public static void message(int timeLeft, boolean addS) {
        boolean debug = plugin.getConfig().getBoolean("debug");

        try {
            String s = "";
            if (addS) s = "s";

            ArrayList<World> worlds = new ArrayList<>();
            ArrayList<String> keys = new ArrayList<>(
                plugin.getConfig().getConfigurationSection("worlds").getKeys(false));

            if (keys.contains("ALL")) {
                if (debug) plugin.getLogger().info("'ALL' found! Adding all worlds to message list...");
                worlds.addAll(Bukkit.getWorlds());
            } else {
                if (debug) plugin.getLogger().info("Adding all worlds defined in config to message list...");
                for (String world : keys) worlds.add(Bukkit.getWorld(world));
            }

            // For each world in the config
            for (World world : worlds) {
                // If that world doesn't exist, complain
                if (world == null) {
                    plugin.getLogger()
                        .severe("Couldn't find world \"" + world.getName() + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // If actionbar is true
                    if (plugin.getConfig().getBoolean("messages.actionbar")) {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");
                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin
                                .getConfig()
                                .getString("messages.actionbar-message")
                                .replace("{SECONDS}", String.valueOf(timeLeft))
                                .replace("{S}", s))));
                    }

                    // If chat is true
                    if (plugin.getConfig().getBoolean("messages.chat")) {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending message to player " + player.getName() + " in world " + world.getName() + ".");
                        player.sendMessage(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin.getConfig()
                                    .getString("messages.chat-message"))
                                .replace("{SECONDS}", String.valueOf(timeLeft))
                                .replace("{S}", s)));
                    }

                    // Play the sound
                    if (debug) plugin.getLogger()
                        .info("Playing sound " + plugin.getConfig()
                            .getString(
                                "sound") + " at player " + player.getName() + " in world " + world.getName() + ".");
                    player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig()
                        .getString("sound"), SoundCategory.MASTER, 1, 0.9F);
                }
            }
        } catch (NullPointerException e) {
            plugin.getLogger()
                .severe("Something went wrong sending messages! Is your config outdated?");
            plugin.getLogger()
                .warning(
                    "Please see https://github.com/JasonHorkles/EntityClearer/blob/main/config.yml for the most recent config.");
            if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }

    public static void removeEntitiesTask(boolean tpsLow) {
        boolean debug = plugin.getConfig().getBoolean("debug");
        if (debug) {
            plugin.getLogger().info("╔══════════════════════════════════════╗");
            plugin.getLogger().info("║     REMOVE ENTITIES TASK STARTED     ║");
            plugin.getLogger().info("╚══════════════════════════════════════╝");
        }

        removedEntities = 0;

        String path = "worlds";
        if (tpsLow)
            if (plugin.getConfig().getBoolean("low-tps.separate-entity-list")) {
                plugin.getLogger().info("Separate entity list enabled!");
                path = "low-tps.worlds";
            }

        ArrayList<World> worlds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(plugin.getConfig().getConfigurationSection(path).getKeys(false));

        if (keys.contains("ALL")) {
            if (debug) plugin.getLogger().info("'ALL' found! Adding all worlds to removal list...");
            worlds.addAll(Bukkit.getWorlds());
        } else {
            if (debug) plugin.getLogger().info("Adding all worlds defined in config to removal list...");
            for (String world : keys) worlds.add(Bukkit.getWorld(world));
        }

        try {
            // For each world in the config
            for (World world : worlds) {
                // If that world doesn't exist, complain
                if (world == null) {
                    plugin.getLogger()
                        .severe("Couldn't find world \"" + world.getName() + "\"! Please double check your config.");
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
                            if (debug)
                                plugin.getLogger().info("Entity " + entities.getType() + " matches the config's!");
                            // If only entities with a specific reason should be removed
                            if (plugin.getConfig().getBoolean(path + "." + world.getName() + ".spawn-reason.enabled")) {
                                if (debug)
                                    plugin.getLogger().info("Only removing entities with a specific spawn reason...");
                                // For each spawn reason in the config
                                // If the entity's spawn reason matches the config's
                                for (String spawnReason : plugin.getConfig()
                                    .getStringList(path + "." + world.getName() + ".spawn-reason.reasons"))
                                    if (entities.getEntitySpawnReason().name().equalsIgnoreCase(spawnReason)) {
                                        if (debug) plugin.getLogger()
                                            .info(
                                                entities.getType() + "'s spawn reason " + entities.getEntitySpawnReason() + " matches the config's!");
                                        checkNearby(entities);
                                    }
                                // If any entity should be removed, regardless of the spawn reason
                            } else {
                                if (debug)
                                    plugin.getLogger().info("Removing entities regardless of their spawn reason...");
                                checkNearby(entities);
                            }
                        }
            }

            for (String command : plugin.getConfig().getStringList("commands"))
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            // Submit stats
            int finalRemovedEntities = removedEntities;
            if (removedEntities > 0) EntityClearer.getInstance()
                .getMetrics()
                .addCustomChart(new Metrics.SingleLineChart("entities_removed", () -> finalRemovedEntities));

            // For each world in the config
            for (World world : worlds) {
                // If that world doesn't exist, complain
                if (world == null) {
                    plugin.getLogger()
                        .severe("Couldn't find world \"" + world.getName() + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // If actionbar is true
                    if (plugin.getConfig().getBoolean("messages.actionbar")) if (tpsLow) {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending low TPS action bar to player " + player.getName() + " in world " + world.getName() + ".");
                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin
                                .getConfig()
                                .getString("messages.actionbar-completed-low-tps-message")
                                .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                    } else {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");
                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin
                                .getConfig()
                                .getString("messages.actionbar-completed-message")
                                .replace("{ENTITIES}", String.valueOf(removedEntities)))));
                    }

                    // If chat is true
                    if (plugin.getConfig().getBoolean("messages.chat")) if (tpsLow) {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending low TPS message to player " + player.getName() + " in world " + world.getName() + ".");
                        player.sendMessage(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin.getConfig()
                                    .getString("messages.chat-completed-low-tps-message"))
                                .replace("{ENTITIES}", String.valueOf(removedEntities))));
                    } else {
                        if (debug) plugin.getLogger()
                            .info(
                                "Sending message to player " + player.getName() + " in world " + world.getName() + ".");
                        player.sendMessage(MiniMessage.miniMessage()
                            .parse(EntityClearer.parseMessage(plugin.getConfig()
                                    .getString("messages.chat-completed-message"))
                                .replace("{ENTITIES}", String.valueOf(removedEntities))));
                    }

                    // Play the sound
                    if (debug) plugin.getLogger()
                        .info("Playing sound " + plugin.getConfig()
                            .getString(
                                "sound") + " at player " + player.getName() + " in world " + world.getName() + ".");
                    player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig()
                        .getString("sound"), SoundCategory.MASTER, 1, 1);
                }
            }
        } catch (NullPointerException e) {
            plugin.getLogger().severe("Something went wrong clearing entities! Is your config outdated?");
            plugin.getLogger()
                .warning(
                    "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");
            if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }

        if (debug) {
            plugin.getLogger().info("╔══════════════════════════════════════╗");
            plugin.getLogger().info("║           TASKS COMPLETED            ║");
            plugin.getLogger().info("║          PLEASE UPLOAD THIS          ║");
            plugin.getLogger().info("║   DEBUG REPORT TO https://paste.gg   ║");
            plugin.getLogger().info("║     & SEND IT TO US FOR SUPPORT      ║");
            plugin.getLogger().info("║                                      ║");
            plugin.getLogger().info("║     https://discord.gg/qcTzC9nMQD    ║");
            plugin.getLogger().info("╚══════════════════════════════════════╝");
        }
    }

    private static void checkNearby(Entity entity) {
        boolean debug = plugin.getConfig().getBoolean("debug");

        boolean nearby = plugin.getConfig().getBoolean("nearby-entities.enabled");
        boolean gamerule = plugin.getConfig().getBoolean("nearby-entities.gamerule-enabled-only");
        double x = plugin.getConfig().getDouble("nearby-entities.x");
        double y = plugin.getConfig().getDouble("nearby-entities.y");
        double z = plugin.getConfig().getDouble("nearby-entities.z");
        int count = plugin.getConfig().getInt("nearby-entities.count");

        // If the config option is enabled
        if (nearby) {
            if (debug) plugin.getLogger().info("Checking nearby entity count...");
            if (gamerule) {
                if (debug) plugin.getLogger().info("Only removing entities in worlds with mob spawning...");
                // If mobspawning is enabled
                if (entity.getWorld().getGameRuleValue(GameRule.DO_MOB_SPAWNING)) {
                    if (debug)
                        plugin.getLogger().info("Mob spawning enabled in world " + entity.getWorld().getName() + ".");
                    // If there are other entities nearby
                    if (debug) plugin.getLogger()
                        .info("Found " + entity.getNearbyEntities(x, y, z).size() + " nearby entities.");
                    if (entity.getNearbyEntities(x, y, z).size() > count) checkNamed(entity);
                    // If mobspawning is disabled, don't check for nearby
                } else {
                    if (debug)
                        plugin.getLogger().info("Mob spawning disabled in world " + entity.getWorld().getName() + ".");
                    checkNamed(entity);
                }
                // If check is disabled, just remove the nearby entities
            } else {
                if (debug)
                    plugin.getLogger().info("Found " + entity.getNearbyEntities(x, y, z).size() + " nearby entities.");
                if (entity.getNearbyEntities(x, y, z).size() > count) checkNamed(entity);
                else if (debug) {
                    plugin.getLogger().info("Checking next entity if available...");
                    plugin.getLogger().info("");
                }
            }
            // If nearby check is disabled, just remove the entities
        } else {
            if (debug) plugin.getLogger().info("Check nearby entities option disabled.");
            checkNamed(entity);
        }

    }

    private static void checkNamed(Entity entity) {
        boolean debug = plugin.getConfig().getBoolean("debug");

        // Should remove named
        if (plugin.getConfig().getBoolean("remove-named")) {
            if (debug) plugin.getLogger().info("Removing entities regardless of a name...");
            // Remove it!
            if (debug) plugin.getLogger().info("Removing entity " + entity.getType() + "...");
            entity.remove();
            removedEntities++;
        } else {
            if (debug) plugin.getLogger().info("Removing entities without a name only...");
            // Don't remove named
            // And it doesn't have a name
            if (entity.getCustomName() == null) {
                if (debug) plugin.getLogger().info("Entity " + entity.getType() + " didn't have a custom name!");
                // Remove it!
                if (debug) plugin.getLogger().info("Removing entity " + entity.getType() + "...");
                entity.remove();
                removedEntities++;
            } else if (debug) {
                plugin.getLogger()
                    .info(entity.getType() + " was skipped becuase it has a name: " + entity.getCustomName());
                plugin.getLogger().info("");
            }
        }

        if (entity.getCustomName() != null) {
            if (debug) plugin.getLogger()
                .info(
                    entity.getType() + " with name " + entity.getCustomName() + " removed! Total removed is " + removedEntities);
        } else if (debug) {
            plugin.getLogger().info(entity.getType() + " removed! Total removed is " + removedEntities + ".");
            plugin.getLogger().info("");
        }
    }
}
