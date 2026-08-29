package com.crovex.practice;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.arena.BlockRestoreManager;
import com.crovex.practice.arena.Cuboid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArenaRollbackTest {

    private ServerMock server;
    private CrovexPractice plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(CrovexPractice.class);
        world = server.addSimpleWorld("arena_world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Arena in-memory rollback should restore original block state accurately")
    void testArenaInMemoryRollback() {
        Location loc1 = new Location(world, 0, 64, 0);
        Location loc2 = new Location(world, 10, 70, 10);
        Cuboid cuboid = new Cuboid(loc1, loc2);

        Arena arena = new Arena("TestArena", cuboid, loc1, loc2, loc1, true);
        arena.setSupportsBlockPlace(true);
        arena.setSupportsBlockBreak(true);

        Location testBlockLoc = new Location(world, 5, 65, 5);
        Block block = testBlockLoc.getBlock();
        block.setType(Material.STONE);

        // Record initial state
        arena.recordBlockChange(testBlockLoc);
        assertThat(arena.getOriginalBlocks()).containsKey(testBlockLoc);

        // Simulate player placing Cobblestone over Stone
        block.setType(Material.COBBLESTONE);
        assertThat(block.getType()).isEqualTo(Material.COBBLESTONE);

        // Execute rollback
        arena.rollbackBlocks();

        // Verify block restored to Stone and tracking map cleared
        assertThat(block.getType()).isEqualTo(Material.STONE);
        assertThat(arena.getOriginalBlocks()).isEmpty();
    }

    @Test
    @DisplayName("BlockRestoreManager should record changes and successfully restore and cleanup")
    void testBlockRestoreManagerLifecycle() {
        BlockRestoreManager restoreManager = plugin.getBlockRestoreManager();
        UUID matchId = UUID.randomUUID();

        Location loc1 = new Location(world, 0, 64, 0);
        Location loc2 = new Location(world, 10, 70, 10);
        Cuboid cuboid = new Cuboid(loc1, loc2);
        Arena arena = new Arena("RestoreArena", cuboid, loc1, loc2, loc1, true);

        restoreManager.startTracking(matchId, arena);

        Location targetLoc = new Location(world, 3, 64, 3);
        Block block = targetLoc.getBlock();
        block.setType(Material.DIRT);

        BlockData originalData = block.getBlockData().clone();

        // Player places Obsidian
        restoreManager.recordChange(matchId, targetLoc, originalData);
        block.setType(Material.OBSIDIAN);
        assertThat(block.getType()).isEqualTo(Material.OBSIDIAN);

        // Restore and cleanup match
        restoreManager.restoreAndCleanup(matchId);

        assertThat(block.getType()).isEqualTo(Material.DIRT);
    }
}
