package net.silverstonemc.entityclearer;

import net.silverstonemc.entityclearer.utils.PrepPlugin;
import net.silverstonemc.entityclearer.utils.TestConfigUtils.TestType;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Ensure the mock default config is set properly.
 */
public class ConfigTest {
    private static EntityClearer plugin;

    @BeforeAll
    public static void setUp() {
        MockBukkit.mock();

        PrepPlugin.prep(TestType.DEFAULT_CONFIG);
        plugin = MockBukkit.load(EntityClearer.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Proper worlds exist in the config")
    public void worldsExist() {
        Assertions.assertTrue(plugin.getConfig().contains("worlds.mock_world"), "No mock_world in config");
        Assertions.assertTrue(
            plugin.getConfig().contains("worlds.mock_world_nether"),
            "No mock_world_nether in config");
    }

    @Test
    @DisplayName("Debug uploading is disabled")
    public void debugDisabled() {
        Assertions.assertTrue(
            plugin.getConfig().getBoolean("disable-paste-upload"),
            "Debug upload is not disabled");
    }
}
