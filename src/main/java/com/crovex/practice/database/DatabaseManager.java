package com.crovex.practice.database;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PracticePlayer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final CrovexPractice plugin;
    private Connection connection;

    public DatabaseManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    private void connect() throws SQLException, ClassNotFoundException {
        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        if (type.equals("mysql") || type.equals("mariadb")) {
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String dbName = plugin.getConfig().getString("database.database", "practice");
            String user = plugin.getConfig().getString("database.username", "root");
            String password = plugin.getConfig().getString("database.password", "");
            boolean useSSL = plugin.getConfig().getBoolean("database.use-ssl", false);

            String driverClass = type.equals("mysql") ? "com.mysql.cj.jdbc.Driver" : "org.mariadb.jdbc.Driver";
            String url = "jdbc:" + type + "://" + host + ":" + port + "/" + dbName + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
            
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                if (type.equals("mysql")) {
                    // Fallback to legacy MySQL driver
                    Class.forName("com.mysql.jdbc.Driver");
                } else {
                    throw e;
                }
            }
            connection = DriverManager.getConnection(url, user, password);
        } else {
            // SQLite
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String filename = plugin.getConfig().getString("database.filename", "practice.db");
            File dbFile = new File(dataFolder, filename);
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                connect();
            } catch (Exception e) {
                throw new SQLException("Veritabani baglantisi kurulamadi!", e);
            }
        }
        return connection;
    }

    public boolean initialize() {
        try {
            getConnection();

            // Create table
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16) NOT NULL, " +
                        "unranked_wins INT DEFAULT 0, " +
                        "unranked_losses INT DEFAULT 0, " +
                        "ranked_wins INT DEFAULT 0, " +
                        "ranked_losses INT DEFAULT 0, " +
                        "elo INT DEFAULT 1000, " +
                        "winstreak INT DEFAULT 0, " +
                        "best_winstreak INT DEFAULT 0, " +
                        "kit_layouts TEXT" +
                        ");");
                
                // Add new FFA stats columns if they do not exist
                try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_kills INT DEFAULT 0;"); } catch (Exception ignored) {}
                try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_deaths INT DEFAULT 0;"); } catch (Exception ignored) {}
                try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_best_streak INT DEFAULT 0;"); } catch (Exception ignored) {}
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabani baslatilamadi!", e);
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabani baglantisi kapatilirken hata olustu!", e);
        }
    }

    public CompletableFuture<PracticePlayer> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PracticePlayer pp = new PracticePlayer(uuid, name);
                    pp.setUnrankedWins(rs.getInt("unranked_wins"));
                    pp.setUnrankedLosses(rs.getInt("unranked_losses"));
                    pp.setRankedWins(rs.getInt("ranked_wins"));
                    pp.setRankedLosses(rs.getInt("ranked_losses"));
                    pp.setElo(rs.getInt("elo"));
                    pp.setWinstreak(rs.getInt("winstreak"));
                    pp.setBestWinstreak(rs.getInt("best_winstreak"));
                    
                    String layouts = rs.getString("kit_layouts");
                    pp.deserializeKitLayouts(layouts != null ? layouts : "{}");
                    
                    pp.setFfaKills(rs.getInt("ffa_kills"));
                    pp.setFfaDeaths(rs.getInt("ffa_deaths"));
                    pp.setFfaBestStreak(rs.getInt("ffa_best_streak"));
                    return pp;
                } else {
                    // Create new player entry in db
                    int startingElo = plugin.getConfig().getInt("queue.elo.starting", 1000);
                    PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO players (uuid, name, elo) VALUES (?, ?, ?)");
                    insert.setString(1, uuid.toString());
                    insert.setString(2, name);
                    insert.setInt(3, startingElo);
                    insert.executeUpdate();
                    
                    PracticePlayer pp = new PracticePlayer(uuid, name);
                    pp.setElo(startingElo);
                    return pp;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu verisi yuklenirken hata olustu: " + name, e);
                return new PracticePlayer(uuid, name); // return default on error to avoid breaking things
            }
        });
    }

    public void savePlayer(PracticePlayer player) {
        CompletableFuture.runAsync(() -> savePlayerSync(player));
    }

    public void savePlayerSync(PracticePlayer player) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE players SET name = ?, unranked_wins = ?, unranked_losses = ?, " +
                            "ranked_wins = ?, ranked_losses = ?, elo = ?, winstreak = ?, " +
                            "best_winstreak = ?, kit_layouts = ?, ffa_kills = ?, ffa_deaths = ?, ffa_best_streak = ? WHERE uuid = ?");
            ps.setString(1, player.getName());
            ps.setInt(2, player.getUnrankedWins());
            ps.setInt(3, player.getUnrankedLosses());
            ps.setInt(4, player.getRankedWins());
            ps.setInt(5, player.getRankedLosses());
            ps.setInt(6, player.getElo());
            ps.setInt(7, player.getWinstreak());
            ps.setInt(8, player.getBestWinstreak());
            ps.setString(9, player.serializeKitLayouts());
            ps.setInt(10, player.getFfaKills());
            ps.setInt(11, player.getFfaDeaths());
            ps.setInt(12, player.getFfaBestStreak());
            ps.setString(13, player.getUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Oyuncu verisi kaydedilirken hata olustu: " + player.getName(), e);
        }
    }

    public CompletableFuture<List<PracticePlayer>> getTopElo(int limit) {
        return getTopPlayers("elo", limit);
    }

    /**
     * Fetches top players ordered by any valid column (elo, ffa_kills, ffa_best_streak, etc.).
     * Each returned PracticePlayer has all stats populated for leaderboard display.
     */
    public CompletableFuture<List<PracticePlayer>> getTopPlayers(String orderByColumn, int limit) {
        // Whitelist to prevent SQL injection
        java.util.Set<String> allowedColumns = java.util.Set.of("elo", "ffa_kills", "ffa_best_streak", "ffa_deaths", "ranked_wins", "unranked_wins");
        if (!allowedColumns.contains(orderByColumn)) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return CompletableFuture.supplyAsync(() -> {
            List<PracticePlayer> top = new ArrayList<>();
            try {
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM players ORDER BY " + orderByColumn + " DESC LIMIT ?");
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    PracticePlayer pp = new PracticePlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    pp.setElo(rs.getInt("elo"));
                    pp.setRankedWins(rs.getInt("ranked_wins"));
                    pp.setRankedLosses(rs.getInt("ranked_losses"));
                    pp.setUnrankedWins(rs.getInt("unranked_wins"));
                    pp.setUnrankedLosses(rs.getInt("unranked_losses"));
                    pp.setWinstreak(rs.getInt("winstreak"));
                    pp.setBestWinstreak(rs.getInt("best_winstreak"));
                    pp.setFfaKills(rs.getInt("ffa_kills"));
                    pp.setFfaDeaths(rs.getInt("ffa_deaths"));
                    pp.setFfaBestStreak(rs.getInt("ffa_best_streak"));
                    top.add(pp);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Siralama yuklenirken hata olustu! (Sutun: " + orderByColumn + ")", e);
            }
            return top;
        });
    }
}
