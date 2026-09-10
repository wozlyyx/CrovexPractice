package com.crovex.practice.crossserver;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CrossServerManager {

    private final CrovexPractice plugin;

    private boolean enabled = false;
    private String serverId = "practice-1";
    private String syncMode = "REDIS";
    private String channelName = "crovexpractice:sync";

    private boolean syncElo = true;
    private boolean syncStats = true;
    private boolean crossServerBroadcasts = true;

    private JedisPool jedisPool;
    private JedisPubSub pubSubListener;
    private Thread subscriberThread;
    private ExecutorService publishExecutor;

    private volatile boolean running = false;

    public CrossServerManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.enabled = plugin.getConfig().getBoolean("cross-server.enabled", false);
        this.serverId = plugin.getConfig().getString("cross-server.server-id", "practice-1");
        this.syncMode = plugin.getConfig().getString("cross-server.sync-mode", "REDIS").toUpperCase();

        String channelPrefix = plugin.getConfig().getString("cross-server.redis.channel-prefix", "crovexpractice");
        this.channelName = channelPrefix + ":sync";

        this.syncElo = plugin.getConfig().getBoolean("cross-server.features.sync-elo", true);
        this.syncStats = plugin.getConfig().getBoolean("cross-server.features.sync-stats", true);
        this.crossServerBroadcasts = plugin.getConfig().getBoolean("cross-server.features.cross-server-broadcasts", true);

        if (!enabled) {
            plugin.getLogger().info("Cross-Server senkronizasyonu devre disi (Tek sunucu modu aktif).");
            return;
        }

        this.publishExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CrovexPractice-CrossServer-Publisher");
            t.setDaemon(true);
            return t;
        });

        if (syncMode.equals("REDIS")) {
            setupRedis();
        } else if (syncMode.equals("DATABASE")) {
            setupDatabasePolling();
        } else {
            plugin.getLogger().info("Cross-Server modu 'NONE' olarak secildi.");
        }
    }

    private void setupRedis() {
        String host = plugin.getConfig().getString("cross-server.redis.host", "localhost");
        int port = plugin.getConfig().getInt("cross-server.redis.port", 6379);
        String password = plugin.getConfig().getString("cross-server.redis.password", "");
        int database = plugin.getConfig().getInt("cross-server.redis.database", 0);
        int timeoutMs = plugin.getConfig().getInt("cross-server.redis.timeout-ms", 2000);
        boolean ssl = plugin.getConfig().getBoolean("cross-server.redis.ssl", false);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        try {
            if (password != null && !password.trim().isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeoutMs, password, database, ssl);
            } else {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeoutMs, null, database, ssl);
            }

            // Quick connectivity test
            try (Jedis testJedis = jedisPool.getResource()) {
                testJedis.ping();
            }

            this.running = true;
            startSubscriberThread();
            plugin.getLogger().info("Cross-Server Redis baglantisi basariyla kuruldu (Sunucu ID: " + serverId + ", Kanal: " + channelName + ").");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Cross-Server Redis baglantisi saglanamadi! Eklenti yerel veritabani modunda calismaya devam edecek: " + e.getMessage());
            this.running = false;
        }
    }

    private void startSubscriberThread() {
        this.pubSubListener = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equalsIgnoreCase(channelName)) {
                    handleIncomingMessage(message);
                }
            }
        };

        this.subscriberThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(pubSubListener, channelName);
                } catch (Exception e) {
                    if (running) {
                        plugin.getLogger().warning("Redis pub/sub baglantisi koptu, 5 saniye icinde yeniden baglanilacak...");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "CrovexPractice-CrossServer-Subscriber");

        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    private void setupDatabasePolling() {
        int intervalSeconds = plugin.getConfig().getInt("cross-server.database-sync.poll-interval-seconds", 30);
        if (intervalSeconds > 0) {
            long ticks = intervalSeconds * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    refreshPlayerFromDatabase(onlinePlayer.getUniqueId(), onlinePlayer.getName());
                }
            }, ticks, ticks);
            plugin.getLogger().info("Cross-Server veritabani periyodik sorgusu aktif (" + intervalSeconds + " saniyede bir).");
        }
    }

    public void handleIncomingMessage(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) return;
        try {
            EloSyncMessage msg = EloSyncMessage.fromJson(rawJson);
            if (msg == null) return;

            // Ignore messages sent by this same server
            if (msg.getOriginServerId() != null && msg.getOriginServerId().equalsIgnoreCase(this.serverId)) {
                return;
            }

            if (msg.getType() == SyncMessageType.ELO_UPDATE) {
                handleEloUpdate(msg);
            } else if (msg.getType() == SyncMessageType.GLOBAL_BROADCAST) {
                handleBroadcast(msg);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Cross-Server mesaji islenirken hata:", e);
        }
    }

    private void handleEloUpdate(EloSyncMessage msg) {
        if (!syncElo && !syncStats) return;

        UUID uuid = msg.getPlayerUuid();
        if (uuid == null) return;

        PracticePlayer player = plugin.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            // Apply update to cached player object
            if (syncElo) {
                player.setElo(msg.getElo());
            }
            if (syncStats) {
                player.setRankedWins(msg.getRankedWins());
                player.setRankedLosses(msg.getRankedLosses());
                player.setWinstreak(msg.getWinstreak());
                player.setBestWinstreak(Math.max(player.getBestWinstreak(), msg.getBestWinstreak()));
            }
        }
    }

    private void handleBroadcast(EloSyncMessage msg) {
        if (!crossServerBroadcasts || msg.getBroadcastMessage() == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(msg.getBroadcastMessage()));
        });
    }

    public void publishEloUpdate(PracticePlayer player, int eloChange) {
        if (!enabled || !running || jedisPool == null || jedisPool.isClosed()) return;

        EloSyncMessage msg = EloSyncMessage.createEloUpdate(
                this.serverId,
                player.getUuid(),
                player.getName(),
                player.getElo(),
                eloChange,
                player.getRankedWins(),
                player.getRankedLosses(),
                player.getWinstreak(),
                player.getBestWinstreak()
        );

        publishMessage(msg);
    }

    public void publishLeaderboardInvalidate() {
        if (!enabled || !running || jedisPool == null || jedisPool.isClosed()) return;

        EloSyncMessage msg = EloSyncMessage.createLeaderboardInvalidate(this.serverId);
        publishMessage(msg);
    }

    public void publishBroadcast(String broadcastMessage) {
        if (!enabled || !running || jedisPool == null || jedisPool.isClosed()) return;

        EloSyncMessage msg = EloSyncMessage.createBroadcast(this.serverId, broadcastMessage);
        publishMessage(msg);
    }

    private void publishMessage(EloSyncMessage msg) {
        if (publishExecutor == null || publishExecutor.isShutdown()) return;

        publishExecutor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channelName, msg.toJson());
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "Cross-Server mesaji gonderilemedi: " + e.getMessage());
            }
        });
    }

    public void refreshPlayerFromDatabase(UUID uuid, String name) {
        plugin.getDatabaseManager().loadPlayer(uuid, name).thenAccept(loaded -> {
            PracticePlayer cached = plugin.getPlayerManager().getPlayer(uuid);
            if (cached != null) {
                cached.setElo(loaded.getElo());
                cached.setRankedWins(loaded.getRankedWins());
                cached.setRankedLosses(loaded.getRankedLosses());
                cached.setUnrankedWins(loaded.getUnrankedWins());
                cached.setUnrankedLosses(loaded.getUnrankedLosses());
                cached.setWinstreak(loaded.getWinstreak());
                cached.setBestWinstreak(loaded.getBestWinstreak());
                cached.setFfaKills(loaded.getFfaKills());
                cached.setFfaDeaths(loaded.getFfaDeaths());
                cached.setFfaBestStreak(loaded.getFfaBestStreak());
            }
        });
    }

    public void shutdown() {
        this.running = false;

        if (pubSubListener != null && pubSubListener.isSubscribed()) {
            try {
                pubSubListener.unsubscribe();
            } catch (Exception ignored) {}
        }

        if (subscriberThread != null && subscriberThread.isAlive()) {
            subscriberThread.interrupt();
        }

        if (publishExecutor != null && !publishExecutor.isShutdown()) {
            publishExecutor.shutdown();
            try {
                if (!publishExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    publishExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                publishExecutor.shutdownNow();
            }
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRunning() {
        return running;
    }

    public String getServerId() {
        return serverId;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public String getChannelName() {
        return channelName;
    }
}
