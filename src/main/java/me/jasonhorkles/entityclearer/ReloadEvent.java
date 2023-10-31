package me.jasonhorkles.entityclearer;

import me.jasonhorkles.entityclearer.utils.LogDebug;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public record ReloadEvent(JavaPlugin plugin) implements Listener {
    @EventHandler
    public void onReload(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            new LogDebug().error(
                "Server reload detected - things may break! Please restart your server as you should never use /reload on a production server!");
            plugin.getLogger().log(Level.SEVERE,
                "Server reload detected - things may break! Please restart your server as you should never use /reload on a production server!");
        }
    }
}
