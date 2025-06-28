package net.silverstonemc.entityclearer.utils;

import net.silverstonemc.entityclearer.EntityClearer;

public class PrepPlugin {
    public static void prep(TestConfigUtils.TestType testType) {
        LogDebug.debugActive = true;
        EntityClearer.testing = true;
        EntityClearer.testType = testType;
    }
}
