package com.crovex.practice;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.arena.Cuboid;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.kit.KitType;
import com.crovex.practice.match.Match;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import com.crovex.practice.visibility.VisibilityManager;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityManagerTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private VisibilityManager visibilityManager;
    private WorldMock world;
    private Kit kit;
    private Arena arena;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        visibilityManager = plugin.getVisibilityManager();
        world = server.addSimpleWorld("vis_world");

        plugin.getKitManager().createKit("Nodebuff", KitType.NORMAL);
        kit = plugin.getKitManager().getKit("Nodebuff");

        Location loc1 = new Location(world, 0, 64, 0);
        Location loc2 = new Location(world, 10, 70, 10);
        Cuboid cuboid = new Cuboid(loc1, loc2);
        arena = new Arena("VisArena", cuboid, loc1, loc2, loc1, true);
        plugin.getArenaManager().addArena(arena);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Lobby players should see each other when hide-players-in-lobby is false")
    void testLobbyPlayersVisibleToEachOther() {
        PlayerMock p1 = server.addPlayer("Lobby1");
        PlayerMock p2 = server.addPlayer("Lobby2");

        PracticePlayer pp1 = plugin.getPlayerManager().getPlayer(p1);
        PracticePlayer pp2 = plugin.getPlayerManager().getPlayer(p2);
        pp1.setState(PlayerState.LOBBY);
        pp2.setState(PlayerState.LOBBY);

        assertThat(visibilityManager.shouldSee(p1, p2)).isTrue();
        assertThat(visibilityManager.shouldSee(p2, p1)).isTrue();
    }

    @Test
    @DisplayName("Match combatants must see each other in the same match")
    void testMatchCombatantsSeeEachOther() {
        PlayerMock p1 = server.addPlayer("Fighter1");
        PlayerMock p2 = server.addPlayer("Fighter2");

        Match match = new Match(plugin, p1, p2, kit, QueueType.UNRANKED, arena);
        match.start();

        assertThat(visibilityManager.shouldSee(p1, p2)).isTrue();
        assertThat(visibilityManager.shouldSee(p2, p1)).isTrue();
    }

    @Test
    @DisplayName("Match combatants must NEVER see spectators, but spectator must see combatants")
    void testSpectatorVisibilityRules() {
        PlayerMock fighter1 = server.addPlayer("Fighter1");
        PlayerMock fighter2 = server.addPlayer("Fighter2");
        PlayerMock spectator = server.addPlayer("Spectator1");

        Match match = new Match(plugin, fighter1, fighter2, kit, QueueType.UNRANKED, arena);
        match.start();
        match.addSpectator(spectator);

        // Spectator can see the fighter
        assertThat(visibilityManager.shouldSee(spectator, fighter1)).isTrue();
        // Fighter cannot see the spectator
        assertThat(visibilityManager.shouldSee(fighter1, spectator)).isFalse();
    }

    @Test
    @DisplayName("Player in match must NOT see player in lobby and vice versa")
    void testMatchAndLobbyIsolation() {
        PlayerMock fighter1 = server.addPlayer("Fighter1");
        PlayerMock fighter2 = server.addPlayer("Fighter2");
        PlayerMock lobbyPlayer = server.addPlayer("LobbyPlayer");

        Match match = new Match(plugin, fighter1, fighter2, kit, QueueType.UNRANKED, arena);
        match.start();

        PracticePlayer ppLobby = plugin.getPlayerManager().getPlayer(lobbyPlayer);
        ppLobby.setState(PlayerState.LOBBY);

        assertThat(visibilityManager.shouldSee(fighter1, lobbyPlayer)).isFalse();
        assertThat(visibilityManager.shouldSee(lobbyPlayer, fighter1)).isFalse();
    }

    @Test
    @DisplayName("updateVisibility should execute smoothly across all players")
    void testUpdateVisibilityExecution() {
        PlayerMock p1 = server.addPlayer("Player1");
        PlayerMock p2 = server.addPlayer("Player2");

        visibilityManager.updateVisibility(p1);
        visibilityManager.updateVisibility(p1, p2);
        visibilityManager.updateAllVisibility();
    }
}
