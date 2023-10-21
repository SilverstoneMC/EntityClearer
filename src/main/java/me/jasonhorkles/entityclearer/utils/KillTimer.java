package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.Countdown;
import me.jasonhorkles.entityclearer.EntityClearer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class KillTimer {
    public static BukkitTask savedTimeTillKillTask;
    public static BukkitTask savedStartCountdown;
    public static long nextKillTask = -1;

    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void start() {
        new LogDebug().debug(Level.INFO, "");
        new LogDebug().debug(Level.INFO, "╔══════════════════════════════════════╗");
        new LogDebug().debug(Level.INFO, "║        STARTING REMOVAL TIMER        ║");
        new LogDebug().debug(Level.INFO, "╚══════════════════════════════════════╝");

        if (plugin.getConfig().getInt("interval") < 1) {
            new LogDebug().debug(Level.WARNING,
                "The interval is set to a value less than 1, so it's been disabled!");
            nextKillTask = -1;
            return;
        }

        // interval - countdown time = time to wait (in secs)
        long interval = plugin.getConfig().getInt("interval") * 60L;
        long countdownLength = new Countdown().getCountdownSorted().get(0);
        long delay = interval - countdownLength;

        if (delay < 0) {
            new LogDebug().error("The interval is set to a value less than the highest countdown time!");
            nextKillTask = -1;
            return;
        }

        // Papi countdown
        if (EntityClearer.getInstance().getPlaceholderAPI() != null)
            savedTimeTillKillTask = new BukkitRunnable() {
                @Override
                public void run() {
                    nextKillTask = System.currentTimeMillis() + ((interval + 1) * 1000);
                }
            }.runTaskTimer(plugin, 0, interval * 20);

        savedStartCountdown = new BukkitRunnable() {
            @Override
            public void run() {
                new Countdown().countdown();
            }
        }.runTaskTimer(plugin, delay * 20, interval * 20);
    }
}
