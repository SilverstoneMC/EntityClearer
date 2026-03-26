/*
 * EntityClearer - a Minecraft plugin
 * Copyright (C) 2026 JasonHorkles and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.silverstonemc.entityclearer;

import static net.silverstonemc.entityclearer.utils.TestConfigUtils.TestType;

import net.silverstonemc.entityclearer.utils.*;

import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

import io.lumine.mythic.api.MythicPlugin;

@SuppressWarnings("DataFlowIssue")
public class EntityClearer extends JavaPlugin implements Listener {
    public static boolean testing;
    public static TestType testType;

    private Metrics metrics;
    private MythicPlugin mythicPlugin;
    private Plugin placeholderAPI;
    private static EntityClearer instance;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize test configurations
        if (testing) new TestConfigUtils().initConfig(testType);

        // Validate the checksum of the plugin's jar file to the one on GitHub
        if (!testing) new ChecksumChecker(this).scan(getFile());

        mythicPlugin = (MythicPlugin) getServer().getPluginManager().getPlugin("MythicMobs");
        if (mythicPlugin != null) getLogger().log(Level.INFO, "Enabled MythicMobs hook!");

        placeholderAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null) {
            new PapiHook().register();
            getLogger().log(Level.INFO, "Enabled PlaceholderAPI hook!");
        }

        if (!testing) {
            metrics = new Metrics(this, 10915);
            new CustomMetrics().send();
        }

        saveDefaultConfig();

        getCommand("entityclearer").setExecutor(new Commands());
        getCommand("entityclearer").setTabCompleter(new TabComplete());

        getServer().getPluginManager().registerEvents(new ChecksumChecker(this), this);
        getServer().getPluginManager().registerEvents(new ReloadEvent(this), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                new KillTimer().start();
                if (getConfig().getBoolean("low-tps.enabled")) new TpsMonitoring().tpsTimer(200);
            }
        }.runTaskLater(this, 3L);

        // Log version update
        if (!testing) new BukkitRunnable() {
            @Override
            public void run() {
                String latest = new UpdateChecker(instance).getLatestVersion();
                String current = instance.getPluginMeta().getVersion();

                if (latest == null) return;
                if (!current.equals(latest)) new UpdateChecker(instance).logUpdate(current, latest);
            }
        }.runTaskAsynchronously(this);
    }

    public static EntityClearer getInstance() {
        return instance;
    }

    public MythicPlugin getMythicPlugin() {
        return mythicPlugin;
    }

    public Plugin getPlaceholderAPI() {
        return placeholderAPI;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}