package lol.notender.ignore;

import lol.notender.ignore.commands.IgnoreCommand;
import lol.notender.ignore.database.DatabaseManager;
import lol.notender.ignore.listeners.ChatListener;
import lol.notender.ignore.listeners.WhisperListener;
import lol.notender.ignore.managers.ConfigManager;
import lol.notender.ignore.managers.IgnoreManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main plugin class for the Ignore system
 */
public final class Ignore extends JavaPlugin {

    private DatabaseManager databaseManager;
    private IgnoreManager ignoreManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        try {
            // Initialize configuration
            this.configManager = new ConfigManager(this);

            // Initialize database
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.initialize();

            // Initialize ignore manager
            this.ignoreManager = new IgnoreManager(databaseManager);

            // Register command
            Objects.requireNonNull(getCommand("ignore"))
                    .setExecutor(new IgnoreCommand(ignoreManager));

            // Register event listeners
            getServer().getPluginManager().registerEvents(new ChatListener(ignoreManager), this);
            getServer().getPluginManager().registerEvents(new WhisperListener(ignoreManager, configManager), this);

            getLogger().info("Ignore Plugin đã được bật!");
        } catch (Exception e) {
            getLogger().severe("Lỗi khi khởi động Ignore Plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Ignore Plugin đã được tắt!");
    }

    /**
     * Get the database manager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Get the ignore manager instance
     */
    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    /**
     * Get the config manager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}