package net.silverstonemc.entityclearer.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UpdateChecker implements Listener {
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private final JavaPlugin plugin;

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        String pluginName = plugin.getPluginMeta().getName();

        if (event.getPlayer().hasPermission(pluginName.toLowerCase() + ".updatenotifs"))
            // Check for updates asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    String latest = getLatestVersion();
                    String current = plugin.getPluginMeta().getVersion();

                    if (latest == null) return;
                    if (!current.equals(latest)) event.getPlayer().sendMessage(Component.text("An update is available for " + pluginName + "! ",
                        NamedTextColor.YELLOW).append(Component.text(
                        "(" + current + " → " + latest + ")",
                        NamedTextColor.GOLD)).appendNewline().append(Component.text(
                            "https://github.com/SilverstoneMC/" + pluginName + "/releases/latest",
                            NamedTextColor.DARK_AQUA)
                        .clickEvent(ClickEvent.openUrl("https://github.com/SilverstoneMC/" + pluginName + "/releases/latest"))));
                }
            }.runTaskAsynchronously(plugin);
    }

    @Nullable
    public String getLatestVersion() {
        String pluginName = plugin.getPluginMeta().getName();

        try {
            // Send the request
            InputStream url = new URI("https://api.github.com/repos/SilverstoneMC/" + pluginName + "/releases/latest")
                .toURL().openStream();

            // Read the response
            JSONObject response = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
            url.close();

            return response.getString("tag_name");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void logUpdate(String current, String latest) {
        String pluginName = plugin.getPluginMeta().getName();

        plugin.getLogger()
            .warning("An update is available for " + pluginName + "! (" + current + " → " + latest + ")");
        plugin.getLogger().warning("https://github.com/SilverstoneMC/" + pluginName + "/releases/latest");
    }
}
