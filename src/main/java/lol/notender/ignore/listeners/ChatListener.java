package lol.notender.ignore.listeners;

import lol.notender.ignore.managers.IgnoreManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles chat events and player join/quit events for the ignore system
 */
public class ChatListener implements Listener {

    private final IgnoreManager ignoreManager;

    public ChatListener(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    /**
     * Filter chat messages based on ignore list
     * Uses HIGHEST priority to ensure we get the final recipient list
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Skip if event is already cancelled
        if (event.isCancelled()) {
            return;
        }

        Player sender = event.getPlayer();
        UUID senderUUID = sender.getUniqueId();

        // Create new recipient set without ignored players
        Set<Player> filteredRecipients = new HashSet<>();

        // Check each recipient to see if they're ignoring the sender
        for (Player recipient : event.getRecipients()) {
            UUID recipientUUID = recipient.getUniqueId();

            // If recipient is not ignoring the sender, add them to filtered list
            if (!ignoreManager.isIgnoring(recipientUUID, senderUUID)) {
                filteredRecipients.add(recipient);
            }
        }

        // Update the recipient list
        event.getRecipients().clear();
        event.getRecipients().addAll(filteredRecipients);
    }

    /**
     * Load player ignore data when they join
     * This ensures their ignore list is cached for better performance
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Load players' ignore data into cache
        // This is done async to avoid blocking the main thread
        player.getServer().getScheduler().runTaskAsynchronously(
                player.getServer().getPluginManager().getPlugin("Ignore"),
                () -> ignoreManager.loadPlayerIgnoreData(playerUUID)
        );
    }

    /**
     * Clean up player data when they quit to save memory
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Remove player from cache to save memory
        // Their data will be loaded again when they rejoin
        ignoreManager.removeFromCache(playerUUID);
    }
}