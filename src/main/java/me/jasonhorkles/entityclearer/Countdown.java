package me.jasonhorkles.entityclearer;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class Countdown {
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public void countdown() {
        new Utils().logDebug(Level.INFO, "╔══════════════════════════════════════╗");
        new Utils().logDebug(Level.INFO, "║        COUNTDOWN TASK STARTED        ║");
        new Utils().logDebug(Level.INFO, "╚══════════════════════════════════════╝");

        int initialTime;
        List<Integer> times = plugin.getConfig().getIntegerList("warning-messages");
        times.sort(Comparator.reverseOrder());
        initialTime = times.get(0);

        new Utils().logDebug(Level.INFO,
            "Starting countdown at " + initialTime + " seconds (" + initialTime / 60 + " minutes)...");
        new Utils().logDebug(Level.INFO, "Sending messages at " + times + " seconds remaining...");

        final int[] timeLeft = {initialTime};
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft[0] <= 0) {
                    new ClearTask().removeEntities(false);
                    this.cancel();
                    return;
                }

                if (!times.isEmpty()) if (timeLeft[0] <= times.get(0)) {
                    new Messages().message(timeLeft[0]);
                    times.remove(0);
                }

                timeLeft[0] -= 1;
            }
        };
        task.runTaskTimer(plugin, 0, 20);
    }
}
