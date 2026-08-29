package com.crovex.practice.listener;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.KitType;
import com.crovex.practice.match.Match;
import com.crovex.practice.match.MatchState;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MatchListener implements Listener {

    private final CrovexPractice plugin;

    public MatchListener(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null) return;

        // Freeze player during STARTING state (warmup)
        if (match.getState() == MatchState.STARTING) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                // Allow looking around (yaw, pitch) but reset movement
                event.setTo(new Location(
                        from.getWorld(), 
                        from.getX(), 
                        to.getY(), // allow falling back to ground if spawned in air
                        from.getZ(), 
                        to.getYaw(), 
                        to.getPitch()
                ));
            }
            return;
        }

        // Check if player left the arena bounds during active match
        if (match.getState() == MatchState.ACTIVE && match.getAlivePlayers().contains(player.getUniqueId())) {
            Location to = event.getTo();
            Location from = event.getFrom();
            if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
                return;
            }

            Arena arena = match.getArena();
            if (arena != null && arena.getBounds() != null) {
                if (!arena.getBounds().contains(to)) {
                    // Left match arena bounds -> eliminate / die
                    player.setHealth(20.0);
                    match.handleDeath(player);
                }
            }
        }
    }

    @EventHandler
    public void onMatchDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = getAttackerPlayer(event);
        if (attacker == null) {
            return;
        }

        PracticePlayer ppVictim = plugin.getPlayerManager().getPlayer(victim);
        PracticePlayer ppAttacker = plugin.getPlayerManager().getPlayer(attacker);

        if (ppVictim == null || ppAttacker == null) return;

        // --- OUTSIDER ANTI-GRIEF RULES ---
        // Rule 1: Victim is in MATCH, but Attacker is not in a match (or in a different match)
        if (ppVictim.getState() == PlayerState.MATCH) {
            Match victimMatch = ppVictim.getActiveMatch();
            if (victimMatch == null || ppAttacker.getState() != PlayerState.MATCH || 
                ppAttacker.getActiveMatch() == null || !victimMatch.getId().equals(ppAttacker.getActiveMatch().getId())) {
                event.setCancelled(true);
                return;
            }
        }

        // Rule 2: Attacker is in MATCH, but Victim is NOT in a match
        // We want to ALLOW this (so match players can hit outsiders to push them out).
        if (ppAttacker.getState() == PlayerState.MATCH && ppVictim.getState() != PlayerState.MATCH && ppVictim.getState() != PlayerState.FFA) {
            return; // returns without cancelling (GeneralListener handles allowing this damage)
        }
        // ----------------------------------

        // Check if both are in matches
        if (ppVictim.getState() != PlayerState.MATCH || ppAttacker.getState() != PlayerState.MATCH) {
            return;
        }

        Match match = ppVictim.getActiveMatch();
        // Verify they are in the same match
        if (match == null || !match.getId().equals(ppAttacker.getActiveMatch().getId())) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if match is not active yet (starting or ending)
        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Friendly fire protection inside split or VS duels
        if (match.getMatchType() == com.crovex.practice.match.MatchType.PARTY_SPLIT || 
            match.getMatchType() == com.crovex.practice.match.MatchType.PARTY_VS) {
            if (match.isSameTeam(attacker, victim)) {
                event.setCancelled(true);
                return;
            }
        }

        // Add hit
        match.addHit(attacker);
    }

    private Player getAttackerPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }
        return null;
    }

    @EventHandler
    public void onMatchDeathCheck(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null || match.getState() != MatchState.ACTIVE) return;

        // Prevent actual death and handle match termination
        if (player.getHealth() - event.getFinalDamage() <= 0.0) {
            event.setCancelled(true);
            player.setHealth(20.0);
            
            // Trigger match end
            match.handleDeath(player);
        }
    }

    private Match getMatchAtLocation(Location loc) {
        for (Match match : plugin.getMatchManager().getMatches()) {
            if (match.getState() == MatchState.ACTIVE && match.getArena() != null && match.getArena().getBounds() != null) {
                if (match.getArena().getBounds().contains(loc)) {
                    return match;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onMatchBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Allow block placing if the kit permits it or if the kit is BUILDUHC
        if (!match.getKit().isAllowBlockPlace() && match.getKit().getType() != KitType.BUILDUHC) {
            event.setCancelled(true);
            return;
        }

        Arena arena = match.getArena();
        if (arena == null || !arena.isSupportsBlockPlace()) {
            event.setCancelled(true);
            return;
        }
        Location blockLoc = event.getBlock().getLocation();

        // Check bounds
        if (arena.getBounds() == null || !arena.getBounds().contains(blockLoc)) {
            player.sendMessage(plugin.getMessageManager().getMessage("match.uhc-out-of-bounds"));
            event.setCancelled(true);
            return;
        }

        // Record for rollback
        arena.recordBlockChange(blockLoc);
        plugin.getBlockRestoreManager().recordChange(match.getId(), blockLoc, event.getBlockReplacedState().getBlockData());
    }

    @EventHandler
    public void onMatchBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Allow block breaking if the kit permits it or if the kit is BUILDUHC
        if (!match.getKit().isAllowBlockBreak() && match.getKit().getType() != KitType.BUILDUHC) {
            event.setCancelled(true);
            return;
        }

        Arena arena = match.getArena();
        if (arena == null || !arena.isSupportsBlockBreak()) {
            event.setCancelled(true);
            return;
        }
        Location blockLoc = event.getBlock().getLocation();

        // Only allow breaking placed blocks in UHC
        if (match.getKit().getType() == KitType.BUILDUHC) {
            if (!arena.getOriginalBlocks().containsKey(blockLoc)) {
                player.sendMessage(plugin.getMessageManager().getMessage("match.uhc-break-placed-only"));
                event.setCancelled(true);
                return;
            }
        }

        // Record block state before break
        arena.recordBlockChange(blockLoc);
        plugin.getBlockRestoreManager().recordChange(match.getId(), blockLoc, event.getBlock().getBlockData());
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        if (!match.getKit().isAllowBlockPlace() && match.getKit().getType() != KitType.BUILDUHC) {
            event.setCancelled(true);
            return;
        }

        Arena arena = match.getArena();
        if (arena == null || !arena.isSupportsBlockPlace()) {
            event.setCancelled(true);
            return;
        }
        org.bukkit.block.Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        if (arena.getBounds() == null || !arena.getBounds().contains(blockLoc)) {
            player.sendMessage(plugin.getMessageManager().getMessage("match.uhc-out-of-bounds"));
            event.setCancelled(true);
            return;
        }

        // Record for rollback
        arena.recordBlockChange(blockLoc);
        plugin.getBlockRestoreManager().recordChange(match.getId(), blockLoc, block.getBlockData());
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        Match match = pp.getActiveMatch();
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        if (!match.getKit().isAllowBlockBreak() && match.getKit().getType() != KitType.BUILDUHC) {
            event.setCancelled(true);
            return;
        }

        Arena arena = match.getArena();
        if (arena == null || !arena.isSupportsBlockBreak()) {
            event.setCancelled(true);
            return;
        }
        org.bukkit.block.Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        if (match.getKit().getType() == KitType.BUILDUHC) {
            if (!arena.getOriginalBlocks().containsKey(blockLoc)) {
                player.sendMessage(plugin.getMessageManager().getMessage("match.uhc-break-placed-only"));
                event.setCancelled(true);
                return;
            }
        }

        // Record for rollback
        arena.recordBlockChange(blockLoc);
        plugin.getBlockRestoreManager().recordChange(match.getId(), blockLoc, block.getBlockData());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Match match = getMatchAtLocation(event.getLocation());
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE || !match.getKit().isAllowExplosions()
                || match.getArena() == null || !match.getArena().isSupportsExplosions()) {
            event.blockList().clear();
            return;
        }

        Arena arena = match.getArena();
        Iterator<org.bukkit.block.Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (arena.getBounds() == null || !arena.getBounds().contains(block.getLocation())) {
                iterator.remove();
                continue;
            }

            // Record block state before explosion
            arena.recordBlockChange(block.getLocation());
            plugin.getBlockRestoreManager().recordChange(match.getId(), block.getLocation(), block.getBlockData());
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Match match = getMatchAtLocation(event.getBlock().getLocation());
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE || !match.getKit().isAllowExplosions()
                || match.getArena() == null || !match.getArena().isSupportsExplosions()) {
            event.blockList().clear();
            return;
        }

        Arena arena = match.getArena();
        Iterator<org.bukkit.block.Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (arena.getBounds() == null || !arena.getBounds().contains(block.getLocation())) {
                iterator.remove();
                continue;
            }

            // Record block state before explosion
            arena.recordBlockChange(block.getLocation());
            plugin.getBlockRestoreManager().recordChange(match.getId(), block.getLocation(), block.getBlockData());
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Match match = getMatchAtLocation(event.getToBlock().getLocation());
        if (match == null) return;

        Arena arena = match.getArena();
        Location toLoc = event.getToBlock().getLocation();

        if (arena.getBounds() != null && arena.getBounds().contains(toLoc)) {
            arena.recordBlockChange(toLoc);
            plugin.getBlockRestoreManager().recordChange(match.getId(), toLoc, event.getToBlock().getBlockData());
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Match match = getMatchAtLocation(event.getBlock().getLocation());
        if (match == null) return;

        Arena arena = match.getArena();
        Location loc = event.getBlock().getLocation();

        if (arena.getBounds() != null && arena.getBounds().contains(loc)) {
            arena.recordBlockChange(loc);
            plugin.getBlockRestoreManager().recordChange(match.getId(), loc, event.getBlock().getBlockData());
        }
    }

    @EventHandler
    public void onGoldenHeadConsume(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.MATCH) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GOLDEN_APPLE) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("Golden Head")) {
                // It is a golden head!
                event.setCancelled(true);
                
                // Consume 1 head
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                // Apply effects (Regen II for 5 seconds, Absorption for 2 mins)
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getMessage("match.golden-head-use"));
            }
        }
    }
}
