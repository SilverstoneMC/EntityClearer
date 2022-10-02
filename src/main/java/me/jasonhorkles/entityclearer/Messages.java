package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.logging.Level;

public class Messages {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    @SuppressWarnings("ConstantConditions")
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
                new Utils().logDebug(Level.INFO, "'ALL' found! Adding all worlds to message list...");
                worlds.addAll(Bukkit.getWorlds());
            } else {
                new Utils().logDebug(Level.INFO, "Adding all worlds defined in config to message list...");
                for (String world : keys) worlds.add(Bukkit.getWorld(world));
            }

            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    new Utils().sendError(
                        "Couldn't find the world \"" + keys.get(index) + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    // Action bar
                    if (!plugin.getConfig().getString("messages.actionbar-message").isBlank()) {
                        new Utils().logDebug(Level.INFO,
                            "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                            new Utils().parseMessage(plugin.getConfig().getString("messages.actionbar-message")
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time))));
                    }

                    // Chat
                    if (!plugin.getConfig().getString("messages.chat-message").isBlank()) {
                        new Utils().logDebug(Level.INFO,
                            "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

                        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                            new Utils().parseMessage(plugin.getConfig().getString("messages.chat-message"))
                                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time)));
                    }

                    // Play the sound
                    new Utils().logDebug(Level.INFO, "Playing sound " + plugin.getConfig()
                        .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

                    try {
                        player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                            SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

                    } catch (NumberFormatException e) {
                        new Utils().sendError("Countdown pitch '" + plugin.getConfig()
                            .getString("countdown-pitch") + "' is not a valid number!");

                        if (Utils.debug) {
                            new Utils().logDebug(Level.SEVERE, e.toString());
                            for (StackTraceElement ste : e.getStackTrace())
                                new Utils().logDebug(Level.SEVERE, ste.toString());
                        } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
                    }
                }
            }

        } catch (NullPointerException e) {
            new Utils().sendError("Something went wrong sending messages! Is your config outdated?");
            new Utils().logDebug(Level.WARNING,
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            if (Utils.debug) {
                new Utils().logDebug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace()) new Utils().logDebug(Level.SEVERE, ste.toString());
            } else if (plugin.getConfig().getBoolean("print-stack-traces")) e.printStackTrace();
        }
    }
}
