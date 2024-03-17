package me.jasonhorkles.entityclearer;

import me.jasonhorkles.entityclearer.utils.LogDebug;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ReloadEvent implements Listener {
    public ReloadEvent(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private final JavaPlugin plugin;

    @EventHandler
    public void onReload(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            String message = "Server reload detected - things may break! Please restart your server as you should never use /reload on a production server!";

            new LogDebug().error("SERVER", message);
            plugin.getLogger().log(Level.SEVERE, message);
        }
    }
}
