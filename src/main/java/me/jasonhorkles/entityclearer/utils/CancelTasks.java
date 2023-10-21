package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.Countdown;

public class CancelTasks {
    public void all() {
        // Papi
        if (KillTimer.savedTimeTillKillTask != null && !KillTimer.savedTimeTillKillTask.isCancelled())
            KillTimer.savedTimeTillKillTask.cancel();

        if (KillTimer.savedStartCountdown != null && !KillTimer.savedStartCountdown.isCancelled())
            KillTimer.savedStartCountdown.cancel();

        if (Countdown.savedCountingDown != null && !Countdown.savedCountingDown.isCancelled())
            Countdown.savedCountingDown.cancel();
    }
}
