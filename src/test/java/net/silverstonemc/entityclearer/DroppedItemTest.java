package net.silverstonemc.entityclearer;

import net.silverstonemc.entityclearer.utils.LogDebug;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * 
 */
public class DroppedItemTest {
    private static EntityClearer plugin;

    @BeforeAll
    public static void setUp() {
        MockBukkit.mock();

        EntityClearer.testing = true;
        LogDebug.debugActive = true;
        plugin = MockBukkit.load(EntityClearer.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unmock();
    }

    
}
