package me.jasonhorkles.entityclearer;

import io.lumine.mythic.api.MythicPlugin;
import me.jasonhorkles.entityclearer.utils.KillTimer;
import me.jasonhorkles.entityclearer.utils.MetricsUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

@SuppressWarnings("DataFlowIssue")
public class EntityClearer extends JavaPlugin implements Listener {
    private BukkitAudiences adventure;
    private Metrics metrics;
    private MythicPlugin mythicPlugin;
    private Plugin placeholderAPI;
    private static EntityClearer instance;

    @Override
    public void onEnable() {
        instance = this;

        adventure = BukkitAudiences.create(this);

        mythicPlugin = (MythicPlugin) getServer().getPluginManager().getPlugin("MythicMobs");
        if (mythicPlugin != null) getLogger().log(Level.INFO, "Enabled MythicMobs hook!");

        placeholderAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null) {
            new PapiHook().register();
            getLogger().log(Level.INFO, "Enabled PlaceholderAPI hook!");
        }

        metrics = new Metrics(this, 10915);
        new MetricsUtils().send();

        saveDefaultConfig();

        getCommand("entityclearer").setExecutor(new Commands());
        getCommand("entityclearer").setTabCompleter(new TabComplete());

        getServer().getPluginManager().registerEvents(new ReloadEvent(this), this);

        new KillTimer().start();
        if (getConfig().getBoolean("low-tps.enabled")) new TpsMonitoring().tpsTimer(600);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public static EntityClearer getInstance() {
        return instance;
    }

    public BukkitAudiences getAdventure() {
        if (this.adventure == null) throw new IllegalStateException(
            "Tried to access Adventure when the plugin was disabled!");
        return this.adventure;
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