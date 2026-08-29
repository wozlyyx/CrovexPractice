package com.crovex.practice.queue;

import com.crovex.practice.CrovexPractice;
import java.util.UUID;

public class QueueEntry {

    private final UUID uuid;
    private final String playerName;
    private final String kitName;
    private final QueueType type;
    private final long joinTime;
    private final int initialElo;

    public QueueEntry(UUID uuid, String playerName, String kitName, QueueType type, int initialElo) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.kitName = kitName;
        this.type = type;
        this.joinTime = System.currentTimeMillis();
        this.initialElo = initialElo;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getKitName() {
        return kitName;
    }

    public QueueType getType() {
        return type;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public int getInitialElo() {
        return initialElo;
    }

    public int getElapsedSeconds() {
        return (int) ((System.currentTimeMillis() - joinTime) / 1000);
    }

    public int getEloRange() {
        int starting = CrovexPractice.getInstance().getConfig().getInt("queue.elo.range-starting", 50);
        int expansion = CrovexPractice.getInstance().getConfig().getInt("queue.elo.range-expansion-per-second", 25);
        return starting + (getElapsedSeconds() * expansion);
    }
}
