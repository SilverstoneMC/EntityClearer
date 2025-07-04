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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UpdateChecker implements Listener {
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private final JavaPlugin plugin;
    private final String pluginId = "SjDWdFjp";

    private String getUrl() {
        return "https://modrinth.com/plugin/" + pluginId + "/versions";
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        String pluginName = plugin.getPluginMeta().getName();

        if (event.getPlayer().hasPermission(pluginName.toLowerCase() + ".updatenotifs"))
            // Check for updates asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    String current = plugin.getPluginMeta().getVersion();
                    String latest = getLatestVersion();

                    if (latest == null) return;
                    if (!current.equals(latest)) event.getPlayer().sendMessage(Component.text("An update is available for " + pluginName + "! ",
                            NamedTextColor.YELLOW).append(Component.text(
                            "(" + current + " → " + latest + ")",
                            NamedTextColor.GOLD)).appendNewline()
                        .append(Component.text(getUrl(), NamedTextColor.DARK_AQUA)
                            .clickEvent(ClickEvent.openUrl(getUrl()))));
                }
            }.runTaskAsynchronously(plugin);
    }

    @Nullable
    public String getLatestVersion() {
        try {
            // Send the request
            InputStream url = new URI("https://api.modrinth.com/v2/project/" + pluginId + "/version").toURL()
                .openStream();

            // Read the response
            JSONObject response = new JSONArray(new String(
                url.readAllBytes(),
                StandardCharsets.UTF_8)).getJSONObject(0);
            url.close();

            return response.getString("version_number");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void logUpdate(String current, String latest) {
        String pluginName = plugin.getPluginMeta().getName();

        plugin.getLogger()
            .warning("An update is available for " + pluginName + "! (" + current + " → " + latest + ")");
        plugin.getLogger().warning(getUrl());
    }
}
