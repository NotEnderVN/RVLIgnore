package lol.notender.ignore.commands;

import lol.notender.ignore.managers.IgnoreManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the /ignore command and its tab completion
 */
public class IgnoreCommand implements CommandExecutor, TabCompleter {

    private final IgnoreManager ignoreManager;

    public IgnoreCommand(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Chỉ có người chơi mới có thể sử dụng lệnh này!");
            return true;
        }

        // Handle different argument cases
        if (args.length == 0) {
            sendUsageMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleListCommand(player);
            case "clear" -> handleClearCommand(player);
            case "help" -> sendHelpMessage(player);
            default -> handleIgnorePlayerCommand(player, args[0]);
        }

        return true;
    }

    /**
     * Handle ignoring/unignoring a specific player
     */
    private void handleIgnorePlayerCommand(Player player, String targetName) {
        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Không tìm thấy người chơi: " + targetName);
            return;
        }

        // Check if trying to ignore themselves
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "Bạn không thể ignore chính mình!");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // Toggle ignore status
        boolean nowIgnoring = ignoreManager.toggleIgnore(playerUUID, targetUUID);

        if (nowIgnoring) {
            player.sendMessage(ChatColor.YELLOW + "Đã ignore " + target.getName() + ". Bạn sẽ không thấy tin nhắn của họ nữa.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Đã bỏ ignore " + target.getName() + ".");
        }
    }

    /**
     * Handle /ignore list command
     */
    private void handleListCommand(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<String> ignoredList = ignoreManager.getFormattedIgnoreList(playerUUID);

        if (ignoredList.isEmpty()) {
            player.sendMessage(ChatColor.GREEN + "Bạn chưa ignore ai cả.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Danh sách người bạn đã ignore (" + ignoredList.size() + "):");
        for (String ignoredPlayer : ignoredList) {
            player.sendMessage(ignoredPlayer);
        }
    }

    /**
     * Handle /ignore clear command
     */
    private void handleClearCommand(Player player) {
        UUID playerUUID = player.getUniqueId();
        int ignoredCount = ignoreManager.getIgnoreCount(playerUUID);

        if (ignoredCount == 0) {
            player.sendMessage(ChatColor.GREEN + "Bạn chưa ignore ai cả.");
            return;
        }

        // Clear all ignores for this player
        for (UUID ignoredUUID : ignoreManager.getIgnoredPlayers(playerUUID)) {
            ignoreManager.removeIgnore(playerUUID, ignoredUUID);
        }

        player.sendMessage(ChatColor.GREEN + "Đã xóa danh sách ignore. Bạn không còn ignore " +
                ignoredCount + " người chơi nữa.");
    }

    /**
     * Send usage message to player
     */
    private void sendUsageMessage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Cách sử dụng:");
        player.sendMessage(ChatColor.GRAY + "  /ignore <người chơi> - Ignore/bỏ ignore một người chơi");
        player.sendMessage(ChatColor.GRAY + "  /ignore list - Xem danh sách ignore của bạn");
        player.sendMessage(ChatColor.GRAY + "  /ignore clear - Xóa toàn bộ danh sách ignore");
        player.sendMessage(ChatColor.GRAY + "  /ignore help - Hiển thị trợ giúp");
    }

    /**
     * Send detailed help message to player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== RVL Chat Ignore ===");
        player.sendMessage(ChatColor.YELLOW + "Hệ thống ignore cho phép bạn ẩn tin nhắn từ những người chơi cụ thể.");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "Các lệnh:");
        player.sendMessage(ChatColor.GRAY + "  • /ignore <người chơi> - Thêm hoặc xóa người chơi khỏi danh sách ignore");
        player.sendMessage(ChatColor.GRAY + "  • /ignore list - Xem tất cả người chơi bạn đang ignore");
        player.sendMessage(ChatColor.GRAY + "  • /ignore clear - Xóa tất cả người chơi khỏi danh sách ignore");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=======================");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partialArg = args[0].toLowerCase();

            // Add sub-commands
            if ("list".startsWith(partialArg)) completions.add("list");
            if ("clear".startsWith(partialArg)) completions.add("clear");
            if ("help".startsWith(partialArg)) completions.add("help");

            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialArg)) {
                    // Don't suggest the command sender
                    if (!player.equals(sender)) {
                        completions.add(player.getName());
                    }
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}