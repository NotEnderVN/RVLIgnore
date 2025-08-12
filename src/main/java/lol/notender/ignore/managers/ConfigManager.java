package lol.notender.ignore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Manages plugin configuration settings
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    // Default values
    private static final List<String> DEFAULT_BLOCKED_COMMANDS = Arrays.asList(
            "/w", "/whisper", "/msg", "/message", "/tell", "/pm", "/t", "/m"
    );

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load and create default configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Set default values if not present
        if (!config.contains("blocked-commands")) {
            config.set("blocked-commands", DEFAULT_BLOCKED_COMMANDS);
        }

        if (!config.contains("block-private-messages")) {
            config.set("block-private-messages", true);
        }

        if (!config.contains("ignore-message")) {
            config.set("ignore-message", "Bạn không thể gửi tin nhắn riêng cho {player} vì họ đã ignore bạn.");
        }

        plugin.saveConfig();
    }

    /**
     * Get list of blocked commands
     */
    public List<String> getBlockedCommands() {
        return config.getStringList("blocked-commands");
    }

    /**
     * Check if private message blocking is enabled
     */
    public boolean isBlockPrivateMessages() {
        return config.getBoolean("block-private-messages", true);
    }

    /**
     * Get the ignore message with placeholder replacement
     */
    public String getIgnoreMessage(String playerName) {
        String message = config.getString("ignore-message",
                "Bạn không thể gửi tin nhắn riêng cho {player} vì họ đã ignore bạn.");
        return message.replace("{player}", playerName);
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        loadConfig();
    }
}