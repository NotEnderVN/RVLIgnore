package lol.notender.ignore.managers;

import lol.notender.ignore.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ignore operations and caching for better performance
 */
public class IgnoreManager {

    private final DatabaseManager databaseManager;

    // Cache for better performance - stores ignore relationships in memory
    // Key: Player UUID, Value: Set of ignored player UUIDs
    private final Map<UUID, Set<UUID>> ignoreCache = new ConcurrentHashMap<>();

    public IgnoreManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        loadIgnoreCache();
    }

    /**
     * Load all ignore data from database into cache
     */
    private void loadIgnoreCache() {
        // Load ignore data for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerIgnoreData(player.getUniqueId());
        }
    }

    /**
     * Load ignore data for a specific player
     */
    public void loadPlayerIgnoreData(UUID playerUUID) {
        Set<UUID> ignoredPlayers = databaseManager.getIgnoredPlayers(playerUUID);
        if (!ignoredPlayers.isEmpty()) {
            ignoreCache.put(playerUUID, new HashSet<>(ignoredPlayers));
        }
    }

    /**
     * Toggle ignore status between two players
     * @param playerUUID The player who wants to ignore/unignore
     * @param targetUUID The player to be ignored/unignored
     * @return true if now ignoring, false if unignored
     */
    public boolean toggleIgnore(UUID playerUUID, UUID targetUUID) {
        if (isIgnoring(playerUUID, targetUUID)) {
            return removeIgnore(playerUUID, targetUUID);
        } else {
            return addIgnore(playerUUID, targetUUID);
        }
    }

    /**
     * Add a player to ignore list
     */
    public boolean addIgnore(UUID playerUUID, UUID targetUUID) {
        // Update database
        if (databaseManager.addIgnore(playerUUID, targetUUID)) {
            // Update cache
            ignoreCache.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(targetUUID);
            return true;
        }
        return false;
    }

    /**
     * Remove a player from ignore list
     */
    public boolean removeIgnore(UUID playerUUID, UUID targetUUID) {
        // Update database
        if (databaseManager.removeIgnore(playerUUID, targetUUID)) {
            // Update cache
            Set<UUID> playerIgnoreSet = ignoreCache.get(playerUUID);
            if (playerIgnoreSet != null) {
                playerIgnoreSet.remove(targetUUID);
                // Remove empty sets to save memory
                if (playerIgnoreSet.isEmpty()) {
                    ignoreCache.remove(playerUUID);
                }
            }
            return false; // Return false to indicate "not ignoring anymore"
        }
        return true; // Return true to indicate "still ignoring" (operation failed)
    }

    /**
     * Check if a player is ignoring another player
     */
    public boolean isIgnoring(UUID playerUUID, UUID targetUUID) {
        // First check cache for better performance
        Set<UUID> playerIgnoreSet = ignoreCache.get(playerUUID);
        if (playerIgnoreSet != null) {
            return playerIgnoreSet.contains(targetUUID);
        }

        // If not in cache, check database and update cache
        boolean isIgnoring = databaseManager.isIgnoring(playerUUID, targetUUID);
        if (isIgnoring) {
            // Load full ignore data for this player into cache
            loadPlayerIgnoreData(playerUUID);
        }

        return isIgnoring;
    }

    /**
     * Get all players that a specific player is ignoring
     */
    public Set<UUID> getIgnoredPlayers(UUID playerUUID) {
        Set<UUID> ignoredPlayers = ignoreCache.get(playerUUID);
        if (ignoredPlayers != null) {
            return new HashSet<>(ignoredPlayers); // Return copy to prevent modification
        }

        // If not in cache, load from database
        ignoredPlayers = databaseManager.getIgnoredPlayers(playerUUID);
        if (!ignoredPlayers.isEmpty()) {
            ignoreCache.put(playerUUID, new HashSet<>(ignoredPlayers));
        }

        return ignoredPlayers;
    }

    /**
     * Get formatted ignore list for display
     */
    public List<String> getFormattedIgnoreList(UUID playerUUID) {
        Set<UUID> ignoredPlayers = getIgnoredPlayers(playerUUID);
        List<String> formattedList = new ArrayList<>();

        for (UUID ignoredUUID : ignoredPlayers) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ignoredUUID);
            String name = offlinePlayer.getName();
            if (name != null) {
                // Add online/offline status
                Player onlinePlayer = Bukkit.getPlayer(ignoredUUID);
                String status = onlinePlayer != null ? "§a[Online]" : "§7[Offline]";
                formattedList.add("§7- " + name + " " + status);
            }
        }

        // Sort alphabetically
        formattedList.sort(String.CASE_INSENSITIVE_ORDER);
        return formattedList;
    }

    /**
     * Get ignore count for a player
     */
    public int getIgnoreCount(UUID playerUUID) {
        Set<UUID> ignoredPlayers = ignoreCache.get(playerUUID);
        if (ignoredPlayers != null) {
            return ignoredPlayers.size();
        }
        return databaseManager.getIgnoreCount(playerUUID);
    }

    /**
     * Remove player from cache when they disconnect (memory optimization)
     */
    public void removeFromCache(UUID playerUUID) {
        ignoreCache.remove(playerUUID);
    }

    /**
     * Get cache size for debugging
     */
    public int getCacheSize() {
        return ignoreCache.size();
    }

    /**
     * Clear all cache (for debugging or reload purposes)
     */
    public void clearCache() {
        ignoreCache.clear();
    }
}