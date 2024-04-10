package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.EntityClearer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;

public class LogDebug {
    public static boolean debugActive = false;
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

    private void dumpLink(String link) {
        for (Player players : Bukkit.getOnlinePlayers())
            if (players.hasPermission("entityclearer.notify")) bukkitAudiences.player(players).sendMessage(
                Component.text("[EntityClearer] SERVER: The debug dump can be found at ", NamedTextColor.GRAY)
                    .append(Component.text(link, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(link))));

        EntityClearer.getInstance().getLogger().log(Level.INFO, "The debug dump can be found at " + link);
    }

    public void upload(File file) {
        // Run async
        new BukkitRunnable() {
            @Override
            public void run() {

                // Build the json
                try {
                    JSONObject json = new JSONObject();
                    json.put("name", "EntityClearer Dump");
                    json.put("visibility", "unlisted");
                    json.put("expires", Instant.now().plus(7, ChronoUnit.DAYS));

                    JSONObject fileJson = new JSONObject();
                    fileJson.put("name", file.getName());

                    JSONObject content = new JSONObject();
                    content.put("format", "text");
                    content.put("value", Files.readString(file.toPath()));
                    fileJson.put("content", content);

                    json.put("files", new JSONArray().put(fileJson));

                    // Send the request
                    URL url = new URL("https://api.paste.gg/v1/pastes");
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection) con;
                    http.setRequestMethod("POST");
                    http.setDoInput(true);
                    http.setDoOutput(true);

                    byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
                    int length = out.length;

                    http.setFixedLengthStreamingMode(length);
                    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    http.connect();
                    try (OutputStream os = http.getOutputStream()) {
                        os.write(out);
                    }

                    // Read the response
                    InputStream input = http.getInputStream();
                    JSONObject returnedText = new JSONObject(new String(input.readAllBytes(),
                        StandardCharsets.UTF_8));

                    if (returnedText.getString("status").equals("success")) {
                        String id = returnedText.getJSONObject("result").getString("id");
                        String link = "https://paste.gg/p/anonymous/" + id;
                        dumpLink(link);

                    } else if (returnedText.getString("status").equals("error")) error("SERVER",
                        returnedText.getString("error") + "\n" + returnedText.getString("message"));

                } catch (Exception e) {
                    error("SERVER",
                        "An error occurred while uploading the debug dump! (" + e.getMessage() + ")");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(EntityClearer.getInstance());
    }
}
