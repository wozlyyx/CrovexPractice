package com.crovex.practice.crossserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.UUID;

public class EloSyncMessage {

    private static final Gson GSON = new GsonBuilder().create();

    private SyncMessageType type;
    private String originServerId;
    private UUID playerUuid;
    private String playerName;
    private int elo;
    private int eloChange;
    private int rankedWins;
    private int rankedLosses;
    private int winstreak;
    private int bestWinstreak;
    private String broadcastMessage;
    private long timestamp;

    public EloSyncMessage() {
    }

    public static EloSyncMessage createEloUpdate(String originServerId, UUID playerUuid, String playerName,
                                                int elo, int eloChange, int rankedWins, int rankedLosses,
                                                int winstreak, int bestWinstreak) {
        EloSyncMessage msg = new EloSyncMessage();
        msg.type = SyncMessageType.ELO_UPDATE;
        msg.originServerId = originServerId;
        msg.playerUuid = playerUuid;
        msg.playerName = playerName;
        msg.elo = elo;
        msg.eloChange = eloChange;
        msg.rankedWins = rankedWins;
        msg.rankedLosses = rankedLosses;
        msg.winstreak = winstreak;
        msg.bestWinstreak = bestWinstreak;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    public static EloSyncMessage createLeaderboardInvalidate(String originServerId) {
        EloSyncMessage msg = new EloSyncMessage();
        msg.type = SyncMessageType.LEADERBOARD_INVALIDATE;
        msg.originServerId = originServerId;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    public static EloSyncMessage createBroadcast(String originServerId, String broadcastMessage) {
        EloSyncMessage msg = new EloSyncMessage();
        msg.type = SyncMessageType.GLOBAL_BROADCAST;
        msg.originServerId = originServerId;
        msg.broadcastMessage = broadcastMessage;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static EloSyncMessage fromJson(String json) {
        try {
            return GSON.fromJson(json, EloSyncMessage.class);
        } catch (Exception e) {
            return null;
        }
    }

    public SyncMessageType getType() {
        return type;
    }

    public void setType(SyncMessageType type) {
        this.type = type;
    }

    public String getOriginServerId() {
        return originServerId;
    }

    public void setOriginServerId(String originServerId) {
        this.originServerId = originServerId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getEloChange() {
        return eloChange;
    }

    public void setEloChange(int eloChange) {
        this.eloChange = eloChange;
    }

    public int getRankedWins() {
        return rankedWins;
    }

    public void setRankedWins(int rankedWins) {
        this.rankedWins = rankedWins;
    }

    public int getRankedLosses() {
        return rankedLosses;
    }

    public void setRankedLosses(int rankedLosses) {
        this.rankedLosses = rankedLosses;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public void setWinstreak(int winstreak) {
        this.winstreak = winstreak;
    }

    public int getBestWinstreak() {
        return bestWinstreak;
    }

    public void setBestWinstreak(int bestWinstreak) {
        this.bestWinstreak = bestWinstreak;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
