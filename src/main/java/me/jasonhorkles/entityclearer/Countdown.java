package me.jasonhorkles.entityclearer;

import me.jasonhorkles.entityclearer.utils.LogDebug;
import me.jasonhorkles.entityclearer.utils.ParseMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@SuppressWarnings("DataFlowIssue")
public class Countdown {
    public static final HashMap<String, BukkitTask> savedCountingDowns = new HashMap<>();

    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void countdown(World world) {
        String worldName = world.getName();

        List<Integer> times = getCountdownSorted();
        int initialTime = times.get(0);

        final int[] timeLeft = {initialTime};
        savedCountingDowns.put(worldName, new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft[0] <= 0) {
                    new ClearTask().removeEntitiesPreTask(new ArrayList<>(Collections.singletonList(world)),
                        false,
                        false);
                    this.cancel();
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
        try {
            StringBuilder time = new StringBuilder();
            int divideBy = 1;
            if (timeLeft > 60) {
                divideBy = 60;
                time.append("minute");
            } else time.append("second");
            if (timeLeft != 1) time.append("s");


            // For each player in the world
            for (Player player : world.getPlayers()) {
                sendActionBar(timeLeft, player, divideBy, time);
                sendChat(timeLeft, player, divideBy, time);
                playSound(world, player);
            }

        } catch (NullPointerException e) {
            new LogDebug().error(world.getName(),
                "Something went wrong sending messages! Is your config outdated?");
            new LogDebug().error(
                world.getName(),
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            e.printStackTrace();
        }
    }

    private void sendActionBar(int timeLeft, Player player, int divideBy, StringBuilder time) {
        if (!plugin.getConfig().getString("messages.actionbar-message").isBlank()) bukkitAudiences.player(
            player).sendActionBar(MiniMessage.miniMessage()
            .deserialize(new ParseMessage().parse(plugin.getConfig().getString("messages.actionbar-message")
                .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time))));
    }

    private void sendChat(int timeLeft, Player player, int divideBy, StringBuilder time) {
        if (!plugin.getConfig().getString("messages.chat-message").isBlank())
            bukkitAudiences.player(player).sendMessage(MiniMessage.miniMessage()
                .deserialize(new ParseMessage().parse(plugin.getConfig().getString("messages.chat-message"))
                    .replace("{TIMELEFT}", String.valueOf(timeLeft / divideBy)).replace("{TIME}", time)));
    }

    private void playSound(World world, Player player) {
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
}
