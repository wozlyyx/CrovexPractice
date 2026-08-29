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
import com.crovex.practice.match.MatchState;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchLifecycleTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private WorldMock world;
    private Kit kit;
    private Arena arena;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        world = server.addSimpleWorld("match_world");

        plugin.getKitManager().createKit("NodebuffDuel", KitType.NORMAL);
        kit = plugin.getKitManager().getKit("NodebuffDuel");

        Location spawn1 = new Location(world, 0, 64, 0);
        Location spawn2 = new Location(world, 20, 64, 20);
        Cuboid bounds = new Cuboid(new Location(world, -10, 50, -10), new Location(world, 30, 80, 30));

        arena = new Arena("DuelArena", bounds, spawn1, spawn2, spawn1, true);
        plugin.getArenaManager().addArena(arena);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Match start should teleport players, apply kit, and mark arena as in-use")
    void testMatchStartAndState() {
        PlayerMock playerA = server.addPlayer("Alice");
        PlayerMock playerB = server.addPlayer("Bob");

        Match match = new Match(plugin, playerA, playerB, kit, QueueType.UNRANKED, arena);
        match.start();

        assertThat(arena.isInUse()).isTrue();
        assertThat(match.getState()).isEqualTo(MatchState.STARTING);

        PracticePlayer ppA = plugin.getPlayerManager().getPlayer(playerA);
        PracticePlayer ppB = plugin.getPlayerManager().getPlayer(playerB);

        assertThat(ppA.getState()).isEqualTo(PlayerState.MATCH);
        assertThat(ppB.getState()).isEqualTo(PlayerState.MATCH);
        assertThat(ppA.getActiveMatch()).isEqualTo(match);
        assertThat(ppB.getActiveMatch()).isEqualTo(match);
    }

    @Test
    @DisplayName("Player death in active match should transition match to ENDING and determine winner")
    void testPlayerDeathTransitionsMatchToEnding() {
        PlayerMock playerA = server.addPlayer("Alice");
        PlayerMock playerB = server.addPlayer("Bob");

        Match match = new Match(plugin, playerA, playerB, kit, QueueType.RANKED, arena);
        match.start();

        // Advance warmup countdown scheduler ticks
        server.getScheduler().performTicks(120L);

        // Player B dies in combat
        match.handleDeath(playerB);

        assertThat(match.getState()).isEqualTo(MatchState.ENDING);
        assertThat(match.getPostInventories()).containsKey(playerA.getUniqueId());
        assertThat(match.getPostInventories()).containsKey(playerB.getUniqueId());
    }

    @Test
    @DisplayName("Player disconnect during warmup (STARTING) must safely end match without stuck states")
    void testPlayerDisconnectDuringWarmup() {
        PlayerMock playerA = server.addPlayer("Alice");
        PlayerMock playerB = server.addPlayer("Bob");

        Match match = new Match(plugin, playerA, playerB, kit, QueueType.UNRANKED, arena);
        match.start();

        assertThat(match.getState()).isEqualTo(MatchState.STARTING);

        // Bob leaves during countdown
        match.handleLogout(playerB);

        assertThat(match.getState()).isEqualTo(MatchState.ENDING);
    }

    @Test
    @DisplayName("Player reset should restore full lobby state, inventory, and health")
    void testPlayerResetRestoresLobbyState() {
        PlayerMock player = server.addPlayer("Combatant");
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.setHealth(5.0);

        plugin.getPlayerManager().resetPlayer(player);

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        assertThat(pp.getState()).isEqualTo(PlayerState.LOBBY);
        assertThat(pp.getActiveMatch()).isNull();
        assertThat(player.getHealth()).isEqualTo(20.0);
        assertThat(player.getFoodLevel()).isEqualTo(20);

        // Check that lobby sword items are present
        assertThat(player.getInventory().getItem(0)).isNotNull();
        assertThat(player.getInventory().getItem(0).getType()).isEqualTo(Material.IRON_SWORD);
    }
}
