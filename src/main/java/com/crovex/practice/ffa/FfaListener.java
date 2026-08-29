package com.crovex.practice.ffa;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class FfaListener implements Listener {

    private final CrovexPractice plugin;

    public FfaListener(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        FfaManager ffaManager = plugin.getFfaManager();

        // Check if player crossed block boundaries to reduce calculations
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        FfaArena inside = ffaManager.getInsideFfaZone(to);

        if (inside != null) {
            if (pp.getState() == PlayerState.LOBBY) {
                ffaManager.joinFfa(player, inside.getName(), false);
            }
        } else {
            if (pp.getState() == PlayerState.FFA) {
                // Check if they left their active FFA arena boundaries
                String activeFfaName = ffaManager.getPlayerFfaArenaName(player);
                if (activeFfaName != null) {
                    FfaArena activeFfa = ffaManager.getFfaArena(activeFfaName);
                    if (activeFfa != null && !activeFfa.getBounds().contains(to)) {
                        ffaManager.handleKill(null, player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(victim);
        if (pp == null || pp.getState() != PlayerState.FFA) return;

        // Intercept fatal damage to perform fast respawn
        if (victim.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);

            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) event;
                if (edbee.getDamager() instanceof Player) {
                    killer = (Player) edbee.getDamager();
                } else if (edbee.getDamager() instanceof Projectile) {
                    Projectile proj = (Projectile) edbee.getDamager();
                    if (proj.getShooter() instanceof Player) {
                        killer = (Player) proj.getShooter();
                    }
                }
            }

            plugin.getFfaManager().handleKill(killer, victim);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getFfaManager().handleLogout(player);
        plugin.getFfaManager().leaveFfa(player, false);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() == PlayerState.FFA) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.no-building"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() == PlayerState.FFA) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.no-building"));
        }
    }


    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() == PlayerState.FFA) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.no-item-drop"));
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        if (attacker == null || attacker.equals(victim)) return;

        PracticePlayer ppVictim = plugin.getPlayerManager().getPlayer(victim);
        PracticePlayer ppAttacker = plugin.getPlayerManager().getPlayer(attacker);

        if (ppVictim != null && ppVictim.getState() == PlayerState.FFA &&
            ppAttacker != null && ppAttacker.getState() == PlayerState.FFA) {
            
            String victimFfa = plugin.getFfaManager().getPlayerFfaArenaName(victim);
            String attackerFfa = plugin.getFfaManager().getPlayerFfaArenaName(attacker);
            
            if (victimFfa != null && victimFfa.equalsIgnoreCase(attackerFfa)) {
                plugin.getFfaManager().tagPlayer(victim, attacker);
                plugin.getFfaManager().tagPlayer(attacker, victim);
            }
        }
    }
}
