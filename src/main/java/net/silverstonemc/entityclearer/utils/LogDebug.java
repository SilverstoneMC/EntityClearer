package net.silverstonemc.entityclearer.utils;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.silverstonemc.entityclearer.EntityClearer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class LogDebug {
    public static boolean debugActive;
    public static FileWriter debugFile;
    public static Long fileId;

    private final BukkitAudiences bukkitAudiences = EntityClearer.getInstance().getAdventure();

    public void debug(Level level, String worldName, String message) {
        if (!debugActive) return;

        worldName = worldName.toUpperCase() + ": ";
        if (worldName.equals((": "))) worldName = "";

        EntityClearer.getInstance().getLogger().log(level, worldName + message);
        try {
            debugFile.write(worldName + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void error(String worldName, String message) {
        worldName = worldName.toUpperCase();

        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                Component.text("[EntityClearer] " + worldName + ": " + message, NamedTextColor.RED));

        debug(Level.SEVERE, worldName, message);
    }

    public void upload(File file) {
        EntityClearer.getInstance().getLogger().log(Level.INFO, "Uploading debug dump...");

        // Run async
        new BukkitRunnable() {
            @Override
            public void run() {
                // Build the json
                try {
                    // Send the request
                    URL url = new URL("https://api.pastes.dev/post");
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection) con;
                    http.setRequestMethod("POST");
                    http.setDoInput(true);
                    http.setDoOutput(true);

                    // Read file content and compress it
                    String fileContent = Files.readString(file.toPath());
                    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteOutputStream)) {
                        gzipOutputStream.write(fileContent.getBytes(StandardCharsets.UTF_8));
                    }
                    byte[] out = byteOutputStream.toByteArray();
                    int length = out.length;

                    http.setFixedLengthStreamingMode(length);
                    http.setRequestProperty("Content-Type", "text/yaml");
                    http.setRequestProperty("Content-Encoding", "gzip");
                    http.setRequestProperty("User-Agent", "EntityClearer Debug Dump");
                    http.connect();
                    try (OutputStream os = http.getOutputStream()) {
                        os.write(out);
                    }

                    // Read the response
                    InputStream input = http.getInputStream();
                    JSONObject returnedText = new JSONObject(new String(input.readAllBytes(),
                        StandardCharsets.UTF_8));
                    input.close();

                    if (http.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        String key = returnedText.getString("key");
                        String link = "https://pastes.dev/" + key;
                        dumpLink(link);

                    } else if (returnedText.getString("status").equals("error")) error("SERVER",
                        returnedText.getString("error") + "\n" + returnedText.getString("message"));

                } catch (Exception e) {
                    error("SERVER",
                        "An error occurred while uploading the debug dump! (" + e.getMessage() + ")");
                    e.printStackTrace();
                }
            }

            private void dumpLink(String link) {
                for (Player players : Bukkit.getOnlinePlayers())
                    if (players.hasPermission("entityclearer.notify"))
                        bukkitAudiences.player(players).sendMessage(Component.text(
                            "[EntityClearer] SERVER: The debug dump can be found at ",
                            NamedTextColor.GRAY).append(Component.text(link, NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl(link))));

                EntityClearer.getInstance().getLogger().log(Level.INFO,
                    "The debug dump can be found at " + link);
            }
        }.runTaskAsynchronously(EntityClearer.getInstance());
    }
}
