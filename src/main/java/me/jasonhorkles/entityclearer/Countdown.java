package me.jasonhorkles.entityclearer;

import me.jasonhorkles.entityclearer.utils.LogDebug;
import me.jasonhorkles.entityclearer.utils.ParseMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

@SuppressWarnings("DataFlowIssue")
public class Countdown {
    public static BukkitTask savedCountingDown;

    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void countdown() {
        new LogDebug().debug(Level.INFO, "");
        new LogDebug().debug(Level.INFO, "╔══════════════════════════════════════╗");
        new LogDebug().debug(Level.INFO, "║        COUNTDOWN TASK STARTED        ║");
        new LogDebug().debug(Level.INFO, "╚══════════════════════════════════════╝");

        List<Integer> times = getCountdownSorted();
        int initialTime = times.get(0);

        new LogDebug().debug(Level.INFO,
            "Starting countdown at " + initialTime + " seconds (" + initialTime / 60 + " minutes)...");
        new LogDebug().debug(Level.INFO, "Sending messages at " + times + " seconds remaining...");

        // Add all the worlds to a list
        ArrayList<World> worlds = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(
            plugin.getConfig().getConfigurationSection("worlds").getKeys(false));

        if (keys.contains("ALL")) {
            new LogDebug().debug(Level.INFO, "'ALL' found! Adding all worlds to message list...");
            worlds.addAll(Bukkit.getWorlds());
        } else {
            new LogDebug().debug(Level.INFO, "Adding all worlds defined in config to message list...");
            for (String world : keys) worlds.add(Bukkit.getWorld(world));
        }
        new LogDebug().debug(Level.INFO, "");

        final int[] timeLeft = {initialTime};
        savedCountingDown = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft[0] <= 0) {
                    new ClearTask().removeEntitiesPreTask(false);
                    this.cancel();
                    return;
                }

                if (!times.isEmpty()) if (timeLeft[0] <= times.get(0)) {
                    message(timeLeft[0], worlds, keys);
                    times.remove(0);
                }

                timeLeft[0] -= 1;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public List<Integer> getCountdownSorted() {
        List<Integer> times = plugin.getConfig().getIntegerList("warning-messages");
        times.sort(Comparator.reverseOrder());
        return times;
    }

    public void message(int timeLeft, ArrayList<World> worlds, ArrayList<String> keys) {
        try {
            StringBuilder time = new StringBuilder();
            int divideBy = 1;
            if (timeLeft > 60) {
                divideBy = 60;
                time.append("minute");
            } else time.append("second");
            if (timeLeft != 1) time.append("s");

            // For each world in the config
            int index = -1;
            for (World world : worlds) {
                index++;
                // If that world doesn't exist, complain
                if (world == null) {
                    new LogDebug().error("Couldn't find the world \"" + keys.get(
                        index) + "\"! Please double check your config.");
                    continue;
                }

                // For each player in said world
                for (Player player : world.getPlayers()) {
                    sendActionBar(timeLeft, world, player, divideBy, time);
                    sendChat(timeLeft, world, player, divideBy, time);
                    playSound(world, player);
                }
            }
            new LogDebug().debug(Level.INFO, "");

        } catch (NullPointerException e) {
            new LogDebug().error("Something went wrong sending messages! Is your config outdated?");
            new LogDebug().error(
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, ste.toString());
            } else e.printStackTrace();
        }
    }

    private void sendActionBar(int timeLeft, World world, Player player, int divideBy, StringBuilder time) {
        if (!plugin.getConfig().getString("messages.actionbar-message").isBlank()) {
            new LogDebug().debug(Level.INFO,
                "Sending action bar to player " + player.getName() + " in world " + world.getName() + ".");

            bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage().deserialize(
                new ParseMessage().parse(plugin.getConfig().getString("messages.actionbar-message")
                    .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time))));
        }
    }

    private void sendChat(int timeLeft, World world, Player player, int divideBy, StringBuilder time) {
        if (!plugin.getConfig().getString("messages.chat-message").isBlank()) {
            new LogDebug().debug(Level.INFO,
                "Sending message to player " + player.getName() + " in world " + world.getName() + ".");

            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(
                new ParseMessage().parse(plugin.getConfig().getString("messages.chat-message"))
                    .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time)));
        }
    }

    private void playSound(World world, Player player) {
        new LogDebug().debug(Level.INFO, "Playing sound " + plugin.getConfig()
            .getString("sound") + " at player " + player.getName() + " in world " + world.getName() + ".");

        try {
            player.playSound(player.getLocation(), "minecraft:" + plugin.getConfig().getString("sound"),
                SoundCategory.MASTER, 1, Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

        } catch (NumberFormatException e) {
            new LogDebug().error("Countdown pitch '" + plugin.getConfig()
                .getString("countdown-pitch") + "' is not a valid number!");

            if (LogDebug.debugActive) {
                new LogDebug().debug(Level.SEVERE, e.toString());
                for (StackTraceElement ste : e.getStackTrace())
                    new LogDebug().debug(Level.SEVERE, ste.toString());
            } else e.printStackTrace();
        }
    }
}
