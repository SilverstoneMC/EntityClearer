package net.silverstonemc.entityclearer.utils;

import net.silverstonemc.entityclearer.EntityClearer;
import org.bukkit.configuration.file.FileConfiguration;

public class TestConfigUtils {
    private final EntityClearer plugin = EntityClearer.getInstance();

    public enum TestType {
        DEFAULT_CONFIG, DROPPED_ITEM, ENTITY_FLAGS, GLOBAL_INTERVAL, GLOBAL_MIN_PLAYERS, INVALID_WORLD, MESSAGES, NEARBY_ENTITIES, REMOVAL_COMMANDS, SPAWN_REASON, WORLD_INTERVAL, WORLD_MIN_PLAYERS
    }

    public void initConfig(TestType testType) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        config.set("worlds.mock_world", config.getConfigurationSection("worlds.replace_me_with_world"));
        config.set("worlds.replace_me_with_world", null);

        config.set(
            "worlds.mock_world_nether",
            config.getConfigurationSection("worlds.replace_me_with_world_nether"));
        config.set("worlds.replace_me_with_world_nether", null);

        switch (testType) {
        }

        plugin.saveConfig();
        System.out.println("Test configuration initialized.");
    }
}
