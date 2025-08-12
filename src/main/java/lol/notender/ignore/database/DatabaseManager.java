package lol.notender.ignore.database;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages SQLite database operations for the ignore system
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // lưu file DB với đuôi .db cho sqlite
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "ignoredata.db";
    }

    /**
     * Initialize database connection and create tables
     */
    public void initialize() throws SQLException {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }

        // Establish SQLite connection
        String jdbcUrl = "jdbc:sqlite:" + databasePath;
        connection = DriverManager.getConnection(jdbcUrl);

        // Tuning pragmas for better concurrency / behavior in plugin environment
        try (Statement pragma = connection.createStatement()) {
            // Use WAL for better concurrent read/write
            pragma.executeUpdate("PRAGMA journal_mode=WAL;");
            // Wait up to 5000ms when DB is busy
            pragma.executeUpdate("PRAGMA busy_timeout=5000;");
        } catch (SQLException e) {
            plugin.getLogger().warning("Không thể thiết lập PRAGMA cho SQLite: " + e.getMessage());
        }

        // Create ignore_list table if it doesn't exist
        createTables();

        plugin.getLogger().info("Cơ sở dữ liệu SQLite đã được khởi tạo thành công!");
    }

    /**
     * Create necessary database tables
     */
    private void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS ignore_list (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                ignored_uuid TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(player_uuid, ignored_uuid)
            )
        """;

        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.execute();
        }

        // Create index for better performance
        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_player_uuid ON ignore_list(player_uuid)";
        try (PreparedStatement stmt = connection.prepareStatement(createIndexSQL)) {
            stmt.execute();
        }
    }

    /**
     * Add a player to ignore list
     */
    public boolean addIgnore(UUID playerUUID, UUID ignoredUUID) {
        // SQLite: use ON CONFLICT DO UPDATE to refresh created_at if pair exists
        String sql = "INSERT INTO ignore_list (player_uuid, ignored_uuid) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid, ignored_uuid) DO UPDATE SET created_at = CURRENT_TIMESTAMP";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, ignoredUUID.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi thêm bản ghi ignore: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a player from ignore list
     */
    public boolean removeIgnore(UUID playerUUID, UUID ignoredUUID) {
        String sql = "DELETE FROM ignore_list WHERE player_uuid = ? AND ignored_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, ignoredUUID.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi xóa bản ghi ignore: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a player is ignoring another player
     */
    public boolean isIgnoring(UUID playerUUID, UUID ignoredUUID) {
        String sql = "SELECT 1 FROM ignore_list WHERE player_uuid = ? AND ignored_uuid = ? LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, ignoredUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi kiểm tra trạng thái ignore: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all players that a specific player is ignoring
     */
    public Set<UUID> getIgnoredPlayers(UUID playerUUID) {
        Set<UUID> ignoredPlayers = new HashSet<>();
        String sql = "SELECT ignored_uuid FROM ignore_list WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        ignoredPlayers.add(UUID.fromString(rs.getString("ignored_uuid")));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("UUID không hợp lệ trong cơ sở dữ liệu: " + rs.getString("ignored_uuid"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi lấy danh sách người chơi bị ignore: " + e.getMessage());
        }

        return ignoredPlayers;
    }

    /**
     * Get total count of ignore records for a player
     */
    public int getIgnoreCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) as count FROM ignore_list WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Lỗi khi lấy số lượng ignore: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Close database connection
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Kết nối cơ sở dữ liệu đã được đóng thành công!");
            } catch (SQLException e) {
                plugin.getLogger().severe("Lỗi khi đóng kết nối cơ sở dữ liệu: " + e.getMessage());
            }
        }
    }

    /**
     * Check if database connection is valid
     */
    public boolean isConnected() {
        try {
            if (connection == null || connection.isClosed()) return false;
            // try isValid, but fall back to a simple query if unsupported
            try {
                return connection.isValid(5);
            } catch (AbstractMethodError | SQLException ex) {
                try (Statement s = connection.createStatement()) {
                    try (ResultSet rs = s.executeQuery("SELECT 1")) {
                        return rs.next();
                    }
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
