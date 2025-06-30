package net.silverstonemc.entityclearer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.silverstonemc.entityclearer.utils.ConfigUtils;
import net.silverstonemc.entityclearer.utils.LogDebug;
import net.silverstonemc.entityclearer.utils.OnlinePlayers;
import org.bukkit.Bukkit;
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

    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void countdown(World world) {
        String worldName = world.getName();

        List<Integer> times = getCountdownSorted();
        int initialTime = times.getFirst();

        int[] timeLeft = {initialTime};
        savedCountingDowns.put(
            worldName, new BukkitRunnable() {
                @Override
                public void run() {
                    if (timeLeft[0] <= 0) {
                        new ClearTask().removeEntitiesPreTask(
                            new ArrayList<>(Collections.singletonList(world)),
                            false,
                            false);
                        cancel();
                        savedCountingDowns.remove(worldName);
                        return;
                    }

                    if (!times.isEmpty()) if (timeLeft[0] <= times.getFirst()) {
                        message(timeLeft[0], world);
                        times.removeFirst();
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

            TagResolver[] placeholders = {
                Placeholder.unparsed("timeleft", actualTimeLeft),
                Placeholder.unparsed("time", String.valueOf(time))
            };

            // For each player in the world
            for (Player player : world.getPlayers()) {
                sendActionBar(player, placeholders);
                sendChat(player, placeholders);
                sendTitle(player, placeholders);
                playSound(world, player);
            }
            sendLog(world, placeholders);

        } catch (NullPointerException e) {
            LogDebug debug = new LogDebug();
            debug.error(world.getName(), "Something went wrong sending messages! Is your config outdated?");
            debug.error(
                world.getName(),
                "Please see https://github.com/SilverstoneMC/EntityClearer/blob/main/src/main/resources/config.yml for the most recent config.");

            e.printStackTrace();
        }
    }

    private void sendActionBar(Player player, TagResolver[] placeholders) {
        if (!player.hasPermission("entityclearer.removalnotifs.actionbar")) return;
        if (plugin.getConfig().getString("messages.actionbar-message").isBlank()) return;

        player.sendActionBar(MiniMessage.miniMessage()
            .deserialize(plugin.getConfig().getString("messages.actionbar-message"), placeholders));
    }

    private void sendChat(Player player, TagResolver[] placeholders) {
        if (!player.hasPermission("entityclearer.removalnotifs.chat")) return;
        if (plugin.getConfig().getString("messages.chat-message").isBlank()) return;

        player.sendMessage(MiniMessage.miniMessage()
            .deserialize(plugin.getConfig().getString("messages.chat-message"), placeholders));
    }

    private void sendTitle(Player player, TagResolver[] placeholders) {
        if (!player.hasPermission("entityclearer.removalnotifs.title")) return;
        if (plugin.getConfig().getString("messages.title-message").isBlank() && plugin.getConfig().getString(
            "messages.subtitle-message").isBlank()) return;

        Component title = MiniMessage.miniMessage().deserialize(
            plugin.getConfig()
                .getString("messages.title-message"), placeholders);

        Component subtitle = MiniMessage.miniMessage().deserialize(
            plugin.getConfig()
                .getString("messages.subtitle-message"), placeholders);

        Title fullTitle = Title.title(title, subtitle);
        player.showTitle(fullTitle);
    }

    private void playSound(World world, Player player) {
        if (!player.hasPermission("entityclearer.removalnotifs.sound")) return;

        try {
            player.playSound(
                player.getLocation(),
                "minecraft:" + plugin.getConfig().getString("sound"),
                SoundCategory.MASTER,
                1,
                Float.parseFloat(plugin.getConfig().getString("countdown-pitch")));

        } catch (NumberFormatException e) {
            new LogDebug().error(
                world.getName(),
                "Countdown pitch '" + plugin.getConfig()
                    .getString("countdown-pitch") + "' is not a valid number!");

            e.printStackTrace();
        }
    }

    private void sendLog(World world, TagResolver[] placeholders) {
        if (plugin.getConfig().getString("messages.log-message").isBlank()) return;

        String worldName = world.getName().toUpperCase() + ": ";
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage()
            .deserialize(worldName + plugin.getConfig().getString("messages.log-message"), placeholders));
    }
}
