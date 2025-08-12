package lol.notender.ignore.listeners;

import lol.notender.ignore.managers.ConfigManager;
import lol.notender.ignore.managers.IgnoreManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.UUID;

/**
 * Handles private messaging commands to prevent ignored players from sending whispers
 */
public class WhisperListener implements Listener {

    private final IgnoreManager ignoreManager;
    private final ConfigManager configManager;

    public WhisperListener(IgnoreManager ignoreManager, ConfigManager configManager) {
        this.ignoreManager = ignoreManager;
        this.configManager = configManager;
    }

    /**
     * Intercept private message commands and block them if sender is ignored by recipient
     * Uses HIGHEST priority to ensure we can cancel the command before it's processed
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Check if private message blocking is enabled
        if (!configManager.isBlockPrivateMessages()) {
            return;
        }

        String[] args = event.getMessage().split(" ");

        // Check if it's a private message command
        if (args.length < 3) { // Need at least "/cmd target message"
            return;
        }

        String command = args[0].toLowerCase();
        List<String> blockedCommands = configManager.getBlockedCommands();

        if (!blockedCommands.contains(command)) {
            return;
        }

        Player sender = event.getPlayer();
        String targetName = args[1];

        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            // Let the original command handle "player not found" message
            return;
        }

        // Don't block messages to self (though it's weird)
        if (target.equals(sender)) {
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // Check if target is ignoring sender
        if (ignoreManager.isIgnoring(targetUUID, senderUUID)) {
            // Cancel the command and notify sender
            event.setCancelled(true);
            sender.sendMessage(ChatColor.RED + configManager.getIgnoreMessage(target.getName()));

            // Optional: Log the attempt for moderation purposes
            // Bukkit.getLogger().info(sender.getName() + " attempted to send private message to " +
            //                        target.getName() + " but was blocked due to ignore");
        }
    }
}