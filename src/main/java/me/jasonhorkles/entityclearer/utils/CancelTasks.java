package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.Countdown;

public class CancelTasks {
    public void all() {
        // Papi
        KillTimer.savedTimeTillKillTasks.values().forEach(task -> {
            if (task != null) if (!task.isCancelled()) task.cancel();
        });
        KillTimer.savedTimeTillKillTasks.clear();

        KillTimer.savedStartCountdowns.values().forEach(task -> {
            if (task != null) if (!task.isCancelled()) task.cancel();
        });
        KillTimer.savedStartCountdowns.clear();

        Countdown.savedCountingDowns.values().forEach(task -> {
            if (task != null) if (!task.isCancelled()) task.cancel();
        });
        Countdown.savedCountingDowns.clear();
    }
}
