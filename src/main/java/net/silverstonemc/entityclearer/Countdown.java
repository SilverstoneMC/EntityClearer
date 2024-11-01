package net.silverstonemc.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.silverstonemc.entityclearer.utils.ConfigUtils;
import net.silverstonemc.entityclearer.utils.LogDebug;
import net.silverstonemc.entityclearer.utils.OnlinePlayers;
import net.silverstonemc.entityclearer.utils.ParseMessage;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@SuppressWarnings("DataFlowIssue")
public class Countdown {
    public static final Map<String, BukkitTask> savedCountingDowns = new HashMap<>();

    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void countdown(World world) {
        String worldName = world.getName();

        List<Integer> times = getCountdownSorted();
        int initialTime = times.get(0);

        int[] timeLeft = {initialTime};
        savedCountingDowns.put(worldName, new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft[0] <= 0) {
                    new ClearTask().removeEntitiesPreTask(new ArrayList<>(Collections.singletonList(world)),
                        false,
                        false);
                    cancel();
                    savedCountingDowns.remove(worldName);
                    return;
                }

                if (!times.isEmpty()) if (timeLeft[0] <= times.get(0)) {
                    message(timeLeft[0], world);
                    times.remove(0);
                }

                timeLeft[0] -= 1;
            }
        }.runTaskTimer(plugin, 0, 20));
    }

    public List<Integer> getCountdownSorted() {
        List<Integer> times = plugin.getConfig().getIntegerList("warning-messages");
        times.sort(Comparator.reverseOrder());
        return times;
    }

    public void message(int timeLeft, World world) {
        // Don't send messages if there aren't enough players
        String worldConfigName = ConfigUtils.isAll ? "ALL" : world.getName();
        boolean notEnoughPlayers = (boolean) new OnlinePlayers().isNotEnough(world, worldConfigName)[0];
        if (notEnoughPlayers) return;
        
        //noinspection ProhibitedExceptionCaught
        try {
            StringBuilder time = new StringBuilder(7);
            int divideBy = 1;
            // If timeLeft is greater than 60, divide by 60 to get minutes
            if (timeLeft >= 60) {
                divideBy = 60;
                time.append(plugin.getConfig().getString("messages.timeleft-minute"));
            } else time.append(plugin.getConfig().getString("messages.timeleft-second"));
            if (timeLeft / divideBy != 1) time.append(plugin.getConfig().getString("messages.append-s-text"));

            String actualTimeLeft = String.valueOf(timeLeft / divideBy);

            // For each player in the world
            for (Player player : world.getPlayers()) {
                sendActionBar(actualTimeLeft, player, time);
                sendChat(actualTimeLeft, player, time);
                playSound(world, player);
            }
            sendLog(world, actualTimeLeft, time);

        } catch (NullPointerException e) {
            LogDebug debug = new LogDebug();
            debug.error(world.getName(), "Something went wrong sending messages! Is your config outdated?");
            debug.error(
                world.getName(),
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            e.printStackTrace();
        }
    }

    private void sendActionBar(String timeLeft, Player player, StringBuilder time) {
        if (plugin.getConfig().getString("messages.actionbar-message").isBlank()) return;
        if (!player.hasPermission("entityclearer.removalnotifs.actionbar")) return;

        bukkitAudiences.player(player).sendActionBar(MiniMessage.miniMessage()
            .deserialize(new ParseMessage().parse(plugin.getConfig().getString("messages.actionbar-message")
                .replace("{TIMELEFT}", timeLeft).replace("{TIME}", time))));
    }

    private void sendChat(String timeLeft, Player player, StringBuilder time) {
        if (plugin.getConfig().getString("messages.chat-message").isBlank()) return;
        if (!player.hasPermission("entityclearer.removalnotifs.chat")) return;

        bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage()
            .deserialize(new ParseMessage().parse(plugin.getConfig().getString("messages.chat-message"))
                .replace("{TIMELEFT}", timeLeft).replace("{TIME}", time)));
    }

    private void playSound(World world, Player player) {
        if (!player.hasPermission("entityclearer.removalnotifs.sound")) return;

        try {
            player.playSound(player.getLocation(),
                "minecraft:" + plugin.getConfig().getString("sound"),
                SoundCategory.MASTER,
                1,
                Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

        } catch (NumberFormatException e) {
            new LogDebug().error(world.getName(),
                "Countdown pitch '" + plugin.getConfig()
                    .getString("countdown-pitch") + "' is not a valid number!");

            e.printStackTrace();
        }
    }

    private void sendLog(World world, String timeLeft, StringBuilder time) {
        if (plugin.getConfig().getString("messages.log-message").isBlank()) return;

        String worldName = world.getName().toUpperCase() + ": ";
        bukkitAudiences.console().sendMessage(MiniMessage.miniMessage().deserialize(new ParseMessage().parse(
                worldName + plugin.getConfig().getString("messages.log-message")).replace("{TIMELEFT}", timeLeft)
            .replace("{TIME}", time)));
    }
}
