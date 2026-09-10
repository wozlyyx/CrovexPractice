package com.crovex.practice;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.crovex.practice.crossserver.CrossServerManager;
import com.crovex.practice.crossserver.EloSyncMessage;
import com.crovex.practice.crossserver.SyncMessageType;
import com.crovex.practice.player.PracticePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CrossServerSyncTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private CrossServerManager crossServerManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        crossServerManager = plugin.getCrossServerManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("EloSyncMessage should serialize to JSON and deserialize accurately")
    void testMessageSerialization() {
        UUID uuid = UUID.randomUUID();
        EloSyncMessage msg = EloSyncMessage.createEloUpdate(
                "node-1",
                uuid,
                "TestPlayer",
                1450,
                25,
                15,
                3,
                6,
                10
        );

        String json = msg.toJson();
        assertThat(json).isNotNull();
        assertThat(json).contains("node-1");
        assertThat(json).contains("TestPlayer");
        assertThat(json).contains("1450");

        EloSyncMessage deserialized = EloSyncMessage.fromJson(json);
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getType()).isEqualTo(SyncMessageType.ELO_UPDATE);
        assertThat(deserialized.getOriginServerId()).isEqualTo("node-1");
        assertThat(deserialized.getPlayerUuid()).isEqualTo(uuid);
        assertThat(deserialized.getPlayerName()).isEqualTo("TestPlayer");
        assertThat(deserialized.getElo()).isEqualTo(1450);
        assertThat(deserialized.getEloChange()).isEqualTo(25);
        assertThat(deserialized.getRankedWins()).isEqualTo(15);
        assertThat(deserialized.getRankedLosses()).isEqualTo(3);
        assertThat(deserialized.getWinstreak()).isEqualTo(6);
        assertThat(deserialized.getBestWinstreak()).isEqualTo(10);
    }

    @Test
    @DisplayName("CrossServerManager should update cached player when receiving ELO_UPDATE from another node")
    void testHandleIncomingEloUpdateFromRemoteNode() {
        PlayerMock player = server.addPlayer("SyncFighter");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        assertThat(pp).isNotNull();
        pp.setElo(1000);
        pp.setRankedWins(0);
        pp.setRankedLosses(0);
        pp.setWinstreak(0);

        // Simulate incoming message from "node-2"
        EloSyncMessage remoteUpdate = EloSyncMessage.createEloUpdate(
                "node-2",
                player.getUniqueId(),
                player.getName(),
                1120,
                15,
                5,
                1,
                3,
                5
        );

        crossServerManager.handleIncomingMessage(remoteUpdate.toJson());

        // Verify player's memory state was instantly updated without database latency
        assertThat(pp.getElo()).isEqualTo(1120);
        assertThat(pp.getRankedWins()).isEqualTo(5);
        assertThat(pp.getRankedLosses()).isEqualTo(1);
        assertThat(pp.getWinstreak()).isEqualTo(3);
        assertThat(pp.getBestWinstreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("CrossServerManager must ignore messages originating from the same server")
    void testIgnoreMessagesFromSelf() {
        PlayerMock player = server.addPlayer("SelfPlayer");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        assertThat(pp).isNotNull();
        pp.setElo(1000);

        // Same server id as configured ("practice-1")
        EloSyncMessage selfMsg = EloSyncMessage.createEloUpdate(
                crossServerManager.getServerId(),
                player.getUniqueId(),
                player.getName(),
                2000,
                50,
                10,
                0,
                5,
                5
        );

        crossServerManager.handleIncomingMessage(selfMsg.toJson());

        // ELO should remain 1000 because message was discarded
        assertThat(pp.getElo()).isEqualTo(1000);
    }

    @Test
    @DisplayName("CrossServerManager should gracefully handle invalid or corrupted JSON messages")
    void testHandleCorruptedJsonGracefully() {
        assertDoesNotThrow(() -> {
            crossServerManager.handleIncomingMessage(null);
            crossServerManager.handleIncomingMessage("");
            crossServerManager.handleIncomingMessage("   ");
            crossServerManager.handleIncomingMessage("invalid json content {}}");
        });
    }

    @Test
    @DisplayName("CrossServerManager should operate safely when Redis is offline or disabled")
    void testOfflineOrDisabledBehavior() {
        assertThat(crossServerManager.isEnabled()).isFalse(); // disabled by default in test config
        assertThat(crossServerManager.isRunning()).isFalse();

        PlayerMock player = server.addPlayer("DummyPlayer");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);

        // Publishing when disabled/offline must never throw or crash
        assertDoesNotThrow(() -> {
            crossServerManager.publishEloUpdate(pp, 10);
            crossServerManager.publishLeaderboardInvalidate();
            crossServerManager.publishBroadcast("Hello network!");
        });
    }
}
