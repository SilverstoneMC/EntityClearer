package me.jasonhorkles.entityclearer.utils;

public class ParseMessage {
    public String parse(String message) {
        return message.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>").replace("&6",
                "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>").replace("&9", "<blue>")
            .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>").replace("&d",
                "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>").replace("&k",
                "<obfuscated>").replace("&l", "<bold>").replace("&m", "<strikethrough>").replace("&n",
                "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
    }
}
