package me.jasonhorkles.entityclearer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

public class TpsMonitoring {
    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();
    private final JavaPlugin plugin = EntityClearer.getInstance();

    public static boolean tpsTimerRan = false;
    public static BukkitTask savedTpsTask;
    public static final ArrayList<Integer> tickList = new ArrayList<>();

    public void tpsTimer(int delay) {
        plugin.getLogger().info("TPS monitoring activated.");
        BukkitRunnable taskTimer = new BukkitRunnable() {
            @Override
            public void run() {
                // If the timer was already run
                if (tpsTimerRan) return;

                long now = System.currentTimeMillis();
                BukkitRunnable countTicks = new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        ticks++;
                        if (now + 1000 <= System.currentTimeMillis()) {
                            this.cancel();
                            averageTPS(ticks);
                        }
                    }
                };
                countTicks.runTaskTimer(plugin, 0, 1);
            }
        };
        savedTpsTask = taskTimer.runTaskTimerAsynchronously(plugin, delay, 20);
    }

    private void averageTPS(int tps) {
        tickList.add(tps);
        if (tickList.size() > 10) tickList.remove(0);
        else return;

        int sum = 0;
        double average;
        for (int x : tickList) sum += x;
        average = (double) sum / tickList.size();
        tpsLow(average);
    }

    @SuppressWarnings("DataFlowIssue")
    private void tpsLow(double tps) {
        // If TPS is not below the threshold, return
        if (!(tps < plugin.getConfig().getInt("low-tps.threshold"))) return;

        // If a chat message should be sent
        if (plugin.getConfig().getBoolean("low-tps.chat")) for (Player player : Bukkit.getOnlinePlayers())
            if (player.hasPermission("entityclearer.lowtps")) bukkitAudiences.player(player).sendMessage(
                MiniMessage.miniMessage().deserialize(
                    new Utils().parseMessage(plugin.getConfig().getString("low-tps.chat-message"))
                        .replace("{TPS}", String.valueOf(tps))));

        // If the entities should be removed instantly
        new ClearTask().removeEntities(true);

        // Cooldown
        tpsTimerRan = true;
        tickList.clear();
        BukkitRunnable resetTimerRan = new BukkitRunnable() {
            @Override
            public void run() {
                tpsTimerRan = false;
            }
        };
        resetTimerRan.runTaskLaterAsynchronously(plugin, 1800);
    }
}
