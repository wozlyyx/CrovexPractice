package com.crovex.practice;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.kit.KitType;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueManager;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueueManagerTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private QueueManager queueManager;
    private Kit nodebuffKit;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        queueManager = plugin.getQueueManager();

        plugin.getKitManager().createKit("NoDebuff", KitType.NORMAL);
        nodebuffKit = plugin.getKitManager().getKit("NoDebuff");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Player in LOBBY should successfully join queue and receive leave item")
    void testAddToQueueSuccess() {
        PlayerMock player = server.addPlayer("TestPlayer");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        assertThat(pp).isNotNull();
        pp.setState(PlayerState.LOBBY);

        queueManager.addToQueue(player, nodebuffKit, QueueType.UNRANKED);

        assertThat(queueManager.isInQueue(player)).isTrue();
        assertThat(pp.getState()).isEqualTo(PlayerState.QUEUE);
        assertThat(queueManager.getQueuedCount("NoDebuff", QueueType.UNRANKED)).isEqualTo(1);
        assertThat(queueManager.getTotalQueuedCount()).isEqualTo(1);

        // Verify leave item at slot 4 (Red Dye)
        assertThat(player.getInventory().getItem(4)).isNotNull();
        assertThat(player.getInventory().getItem(4).getType()).isEqualTo(Material.RED_DYE);
    }

    @Test
    @DisplayName("Player not in LOBBY state must be rejected from joining queue")
    void testAddToQueueRejectedWhenNotInLobby() {
        PlayerMock player = server.addPlayer("InMatchPlayer");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        pp.setState(PlayerState.MATCH);

        queueManager.addToQueue(player, nodebuffKit, QueueType.UNRANKED);

        assertThat(queueManager.isInQueue(player)).isFalse();
        assertThat(pp.getState()).isEqualTo(PlayerState.MATCH);
        assertThat(queueManager.getTotalQueuedCount()).isZero();
    }

    @Test
    @DisplayName("Player should be able to leave queue and return to LOBBY state")
    void testRemoveFromQueue() {
        PlayerMock player = server.addPlayer("QueueLeaver");
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        pp.setState(PlayerState.LOBBY);

        queueManager.addToQueue(player, nodebuffKit, QueueType.RANKED);
        assertThat(queueManager.isInQueue(player)).isTrue();

        queueManager.removeFromQueue(player, false);

        assertThat(queueManager.isInQueue(player)).isFalse();
        assertThat(pp.getState()).isEqualTo(PlayerState.LOBBY);
        assertThat(queueManager.getTotalQueuedCount()).isZero();
    }

    @Test
    @DisplayName("Queue counts should accurately reflect multiple kits and types")
    void testQueueCountsPerKitAndType() {
        plugin.getKitManager().createKit("Boxing", KitType.BOXING);
        Kit boxingKit = plugin.getKitManager().getKit("Boxing");

        PlayerMock p1 = server.addPlayer("Player1");
        PlayerMock p2 = server.addPlayer("Player2");
        PlayerMock p3 = server.addPlayer("Player3");

        plugin.getPlayerManager().getPlayer(p1).setState(PlayerState.LOBBY);
        plugin.getPlayerManager().getPlayer(p2).setState(PlayerState.LOBBY);
        plugin.getPlayerManager().getPlayer(p3).setState(PlayerState.LOBBY);

        queueManager.addToQueue(p1, nodebuffKit, QueueType.UNRANKED);
        queueManager.addToQueue(p2, nodebuffKit, QueueType.RANKED);
        queueManager.addToQueue(p3, boxingKit, QueueType.UNRANKED);

        assertThat(queueManager.getTotalQueuedCount()).isEqualTo(3);
        assertThat(queueManager.getQueuedCount("NoDebuff", QueueType.UNRANKED)).isEqualTo(1);
        assertThat(queueManager.getQueuedCount("NoDebuff", QueueType.RANKED)).isEqualTo(1);
        assertThat(queueManager.getQueuedCount("Boxing", QueueType.UNRANKED)).isEqualTo(1);
        assertThat(queueManager.getQueuedCount("Boxing", QueueType.RANKED)).isZero();
    }
}
