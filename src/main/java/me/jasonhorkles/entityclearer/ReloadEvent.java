package me.jasonhorkles.entityclearer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public record ReloadEvent(JavaPlugin plugin) implements Listener {

    @EventHandler
    public void onReload(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) plugin.getLogger()
            .severe(
                "Server reload detected - things may break! Please restart your server as you should never use /reload on a production server!");
    }
}
