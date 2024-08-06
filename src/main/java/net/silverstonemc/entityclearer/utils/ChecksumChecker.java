package net.silverstonemc.entityclearer.utils;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumChecker implements Listener {
    public ChecksumChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private final JavaPlugin plugin;
    private static boolean checksumDiffers;

    private final String msg1 = "The checksum of this plugin's jar file does not match the official one!";
    private final String msg2 = "Unless you're modifying the plugin, chances are a jar you downloaded is malware!";
    private final String msg3 = "Please re-download the plugin from the official sources: https://github.com/SilverstoneMC/";
    private final String msg4 = "If you need additional help, please reach out using the Discord server on the linked GitHub page ^";
    private final String msg5 = "If this message appears even after re-downloading, some other plugin or your server jar may be compromised, injecting its code into this one!";

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        String pluginName = plugin.getDescription().getName();

        if (checksumDiffers)
            if (event.getPlayer().hasPermission(pluginName.toLowerCase() + ".notify")) new BukkitRunnable() {
                @Override
                public void run() {
                    LogDebug debug = new LogDebug();
                    debug.error("SERVER", msg1);
                    debug.error("SERVER", msg2);
                    debug.error("SERVER", msg3 + pluginName);
                    debug.error("SERVER", msg4);
                    debug.error("SERVER", msg5);
                }
            }.runTaskLater(plugin, 20L);
    }

    public void scan(File pluginJar) {
        // Grab checksums asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                String version = plugin.getDescription().getVersion();
                String realChecksum = getOnlineChecksum(version);
                if (realChecksum == null) plugin.getLogger()
                    .severe("Failed to get checksum for version " + version + " - custom version?");

                try {
                    String fileName = Paths.get(pluginJar.getPath()).getFileName().toString();
                    byte[] data = Files.readAllBytes(plugin.getServer().getPluginsFolder().toPath()
                        .resolve(fileName));
                    byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                    StringBuilder ownChecksum = new StringBuilder();
                    for (byte b : hash) ownChecksum.append(String.format("%02x", b));

                    if (!ownChecksum.toString().equalsIgnoreCase(realChecksum)) {
                        checksumDiffers = true;
                        plugin.getLogger().severe(msg1);
                        plugin.getLogger().severe(msg2);
                        plugin.getLogger().severe(msg3 + plugin.getDescription().getName());
                        plugin.getLogger().severe(msg4);
                        plugin.getLogger().severe(msg5);
                    }
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

            @Nullable
            private String getOnlineChecksum(String version) {
                String pluginName = plugin.getDescription().getName();

                try {
                    // Send the request
                    InputStream url = new URI("https://raw.githubusercontent.com/SilverstoneMC/" + pluginName + "/main/checksums.json")
                        .toURL().openStream();

                    // Read the response
                    JSONObject response = new JSONObject(new String(url.readAllBytes(),
                        StandardCharsets.UTF_8));
                    url.close();

                    if (!response.has(version)) return null;

                    return response.getString(version);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.runTaskAsynchronously(plugin);
    }
}
