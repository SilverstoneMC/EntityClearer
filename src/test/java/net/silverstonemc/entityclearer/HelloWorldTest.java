package net.silverstonemc.entityclearer;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public class HelloWorldTest {
    private ServerMock server;
    private EntityClearer plugin;
    private World world;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("mock_world");

        EntityClearer.testing = true;
        plugin = MockBukkit.load(EntityClearer.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void test() {
        Assertions.assertTrue(plugin.getConfig().contains("worlds.mock_world"), "No mock_world in config");
    }
}
