package net.silverstonemc.entityclearer.utils;

public class VersionUtils {
    /**
     * Compares two version strings.
     *
     * @param v1 the first version string
     * @param v2 the second version string
     * @return a negative integer, zero, or a positive integer as the first version
     * is less than, equal to, or greater than the second version
     */
    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) return Integer.compare(num1, num2);
        }

        return 0;
    }
}
