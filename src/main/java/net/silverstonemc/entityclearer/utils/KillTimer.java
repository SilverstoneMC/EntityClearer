package net.silverstonemc.entityclearer.utils;

import net.silverstonemc.entityclearer.Countdown;
import net.silverstonemc.entityclearer.EntityClearer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class KillTimer {
    public static final Map<String, BukkitTask> savedTimeTillKillTasks = new HashMap<>();
    public static final Map<String, BukkitTask> savedStartCountdowns = new HashMap<>();
    public static final HashMap<String, Long> nextKillTask = new HashMap<>();

    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void start() {
        // For each world in the config, start a timer
        for (World world : new ConfigUtils().getWorlds("worlds")) {
            String worldName = world.getName();
            String worldConfigName = ConfigUtils.isAll ? "ALL" : worldName;

            int interval = plugin.getConfig().getInt("worlds." + worldConfigName + ".interval");
            if (interval <= -1) interval = plugin.getConfig().getInt("global-interval");

            if (interval == 0) {
                nextKillTask.put(worldName, -1L);
                continue;
            }

            // interval - countdown time = time to wait/delay (in secs)
            interval = interval * 60;
            long countdownLength = new Countdown().getCountdownSorted().getFirst();
            long delay = interval - countdownLength;

            if (delay < 0) {
                new LogDebug().error(
                    worldName,
                    "The interval is set to a value less than the highest countdown time!");
                nextKillTask.put(worldName, -1L);
                continue;
            }

            // Papi countdown
            if (EntityClearer.getInstance().getPlaceholderAPI() != null) {
                int finalInterval = interval;

                savedTimeTillKillTasks.put(
                    worldName, new BukkitRunnable() {
                        @Override
                        public void run() {
                            nextKillTask.put(
                                worldName,
                                System.currentTimeMillis() + ((finalInterval + 1) * 1000L));
                        }
                    }.runTaskTimer(plugin, 0, interval * 20L));
            }

            savedStartCountdowns.put(
                worldName, new BukkitRunnable() {
                    @Override
                    public void run() {
                        new Countdown().countdown(world);
                    }
                }.runTaskTimer(plugin, (delay * 20) + 10, (interval * 20L) + 20));
        }
    }
}
