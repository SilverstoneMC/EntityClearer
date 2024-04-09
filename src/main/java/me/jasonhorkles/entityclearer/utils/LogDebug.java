package me.jasonhorkles.entityclearer.utils;

import me.jasonhorkles.entityclearer.EntityClearer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;

public class LogDebug {
    public static boolean debugActive = false;
    public static FileWriter debugFile;

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

    public void upload() {
        // Build the json
        try {
            JSONObject json = new JSONObject();
            json.put("name", "Silverstone");
            json.put("visibility", "unlisted");
            json.put("expires", Instant.now().plus(7, ChronoUnit.DAYS));

            JSONArray files = new JSONArray();
            for (Message.Attachment attachment : attachments) {
                JSONObject file = new JSONObject();

                boolean isLog = attachment.getFileExtension().equalsIgnoreCase("log");
                if (isLog) file.put("name", attachment.getFileName().replace(".log", ".accesslog"));
                else file.put("name", attachment.getFileName());

                JSONObject content = new JSONObject();
                content.put("format", "text");
                try (InputStream bytes = attachment.getProxy().download().join()) {
                    content.put("value", new String(bytes.readAllBytes(), StandardCharsets.UTF_8));
                }
                file.put("content", content);

                files.put(file);
            }
            json.put("files", files);

            // Send the request
            URL url = new URL("https://api.paste.gg/v1/pastes");
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.setRequestProperty(
                "Authorization",
                "Key " + new me.jasonhorkles.silverstone.Secrets().getPasteKey());
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
                event.getHook().editOriginal("<https://paste.gg/p/JasonHorkles/" + id + ">").queue();

            } else if (returnedText.getString("status").equals("error")) event.getHook().editOriginal(
                    "## Error: " + returnedText.getString("error") + "\n" + returnedText.getString("message"))
                .queue();

        } catch (Exception e) {
            System.out.print(new me.jasonhorkles.silverstone.Utils().getTime(me.jasonhorkles.silverstone.Utils.LogColor.RED));
            e.printStackTrace();
            event.getHook()
                .editOriginal("An error occurred while uploading the file(s)! (" + e.getMessage() + ")")
                .queue();
        }
    }
}
