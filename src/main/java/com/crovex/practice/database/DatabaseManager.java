package com.crovex.practice.database;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PracticePlayer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DatabaseManager {

    private final CrovexPractice plugin;
    private HikariDataSource dataSource;
    private ExecutorService databaseExecutor;

    public DatabaseManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            setupDataSource();
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "HikariCP veritabani baglantisi baslatilamadi!", e);
            return false;
        }
    }

    private void setupDataSource() {
        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        HikariConfig config = new HikariConfig();
        config.setPoolName("CrovexPractice-HikariPool");

        int maxPoolSize = plugin.getConfig().getInt("database.pool.maximum-pool-size", 10);
        int minIdle = plugin.getConfig().getInt("database.pool.minimum-idle", 2);
        long connTimeout = plugin.getConfig().getLong("database.pool.connection-timeout-ms", 30000);
        long idleTimeout = plugin.getConfig().getLong("database.pool.idle-timeout-ms", 600000);
        long maxLifetime = plugin.getConfig().getLong("database.pool.max-lifetime-ms", 1800000);
        long leakThreshold = plugin.getConfig().getLong("database.pool.leak-detection-threshold-ms", 10000);

        config.setConnectionTimeout(connTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakThreshold);

        if (type.equals("mysql") || type.equals("mariadb")) {
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String dbName = plugin.getConfig().getString("database.database", "practice");
            String user = plugin.getConfig().getString("database.username", "root");
            String password = plugin.getConfig().getString("database.password", "");
            boolean useSSL = plugin.getConfig().getBoolean("database.use-ssl", false);

            config.setJdbcUrl("jdbc:" + type + "://" + host + ":" + port + "/" + dbName +
                    "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);

            // MySQL / MariaDB High-Performance Hikari Properties
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
        } else if (type.equals("postgresql") || type.equals("postgres")) {
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 5432);
            String dbName = plugin.getConfig().getString("database.database", "practice");
            String user = plugin.getConfig().getString("database.username", "postgres");
            String password = plugin.getConfig().getString("database.password", "");
            boolean useSSL = plugin.getConfig().getBoolean("database.use-ssl", false);

            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName + (useSSL ? "?ssl=true" : ""));
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
        } else {
            // SQLite
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String filename = plugin.getConfig().getString("database.filename", "practice.db");
            File dbFile = new File(dataFolder, filename);

            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            // SQLite is file-locked; pool size of 1-4 with WAL mode is standard and prevents lock contention
            config.setMaximumPoolSize(Math.min(maxPoolSize, 4));
            config.setMinimumIdle(1);
            config.setConnectionTestQuery("SELECT 1");

            // SQLite PRAGMAs for performance
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("busy_timeout", "5000");
        }

        this.dataSource = new HikariDataSource(config);

        int threadPoolThreads = Math.max(2, Math.min(maxPoolSize, 8));
        this.databaseExecutor = Executors.newFixedThreadPool(threadPoolThreads, r -> {
            Thread t = new Thread(r, "CrovexPractice-DB-Worker");
            t.setDaemon(true);
            return t;
        });

        plugin.getLogger().info("HikariCP baglanti havuzu baslatildi (" + type.toUpperCase() + ", Maks: " + config.getMaximumPoolSize() + ").");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource erisilebilir degil veya kapali!");
        }
        return dataSource.getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
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

            // FFA stats columns
            try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_kills INT DEFAULT 0;"); } catch (Exception ignored) {}
            try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_deaths INT DEFAULT 0;"); } catch (Exception ignored) {}
            try { statement.executeUpdate("ALTER TABLE players ADD COLUMN ffa_best_streak INT DEFAULT 0;"); } catch (Exception ignored) {}
        }
    }

    public void closeConnection() {
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP baglanti havuzu kapatildi.");
        }
    }

    public CompletableFuture<PracticePlayer> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
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
                        try (PreparedStatement insert = conn.prepareStatement(
                                "INSERT INTO players (uuid, name, elo) VALUES (?, ?, ?)")) {
                            insert.setString(1, uuid.toString());
                            insert.setString(2, name);
                            insert.setInt(3, startingElo);
                            insert.executeUpdate();
                        }

                        PracticePlayer pp = new PracticePlayer(uuid, name);
                        pp.setElo(startingElo);
                        return pp;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu verisi yuklenirken hata olustu: " + name, e);
                return new PracticePlayer(uuid, name);
            }
        }, databaseExecutor != null ? databaseExecutor : Runnable::run);
    }

    public void savePlayer(PracticePlayer player) {
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.submit(() -> savePlayerSync(player));
        } else {
            CompletableFuture.runAsync(() -> savePlayerSync(player));
        }
    }

    public void savePlayerSync(PracticePlayer player) {
        if (dataSource == null || dataSource.isClosed()) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET name = ?, unranked_wins = ?, unranked_losses = ?, " +
                             "ranked_wins = ?, ranked_losses = ?, elo = ?, winstreak = ?, " +
                             "best_winstreak = ?, kit_layouts = ?, ffa_kills = ?, ffa_deaths = ?, ffa_best_streak = ? WHERE uuid = ?")) {
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
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT * FROM players ORDER BY " + orderByColumn + " DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
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
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Siralama yuklenirken hata olustu! (Sutun: " + orderByColumn + ")", e);
            }
            return top;
        }, databaseExecutor != null ? databaseExecutor : Runnable::run);
    }
}

