package com.crovex.practice.listener;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.ArenaSetupSession;
import com.crovex.practice.gui.KitEditMenu;
import com.crovex.practice.gui.Menu;
import com.crovex.practice.gui.QueueMenu;
import com.crovex.practice.match.Match;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class GeneralListener implements Listener {

    private final CrovexPractice plugin;
    private final java.util.Set<java.util.UUID> pendingKitCreation = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Map<java.util.UUID, com.crovex.practice.ffa.FfaArena> pendingFfaAlias = new java.util.concurrent.ConcurrentHashMap<>();

    public GeneralListener(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void registerPendingFfaAlias(Player player, com.crovex.practice.ffa.FfaArena arena) {
        pendingFfaAlias.put(player.getUniqueId(), arena);
    }

    public void registerPendingKitCreation(Player player) {
        pendingKitCreation.add(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load data async
        plugin.getPlayerManager().loadPlayer(player).thenAccept(pp -> {
            // Setup player items & state in main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getPlayerManager().resetPlayer(player);
                plugin.getVisibilityManager().updateVisibility(player);
                player.sendMessage(plugin.getMessageManager().getMessage("general.welcome-join", "%player%", player.getName()));
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            // Remove from queue
            if (pp.getState() == PlayerState.QUEUE) {
                plugin.getQueueManager().removeFromQueue(player, true);
            }
            // Remove from match
            else if (pp.getState() == PlayerState.MATCH && pp.getActiveMatch() != null) {
                pp.getActiveMatch().handleLogout(player);
            }
            // Remove from party
            else if (pp.getActiveParty() != null) {
                plugin.getPartyManager().leaveParty(player);
            }
            // Remove from spectators
            else if (pp.getState() == PlayerState.SPECTATING && pp.getActiveMatch() != null) {
                pp.getActiveMatch().removeSpectator(player);
            }
        }
        plugin.getPlayerManager().unloadPlayer(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) {
            Menu menu = (Menu) event.getInventory().getHolder();
            menu.handleMenu(event);
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            // Prevent inventory manipulation when in lobby, queue or spectating
            // Admins are exempt — they can freely manage their inventory
            if (pp.getState() == PlayerState.LOBBY || pp.getState() == PlayerState.QUEUE || pp.getState() == PlayerState.SPECTATING) {
                if (!player.hasPermission("crovexpractice.admin")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Custom selector tool checks
        if (item.getType() == Material.GOLDEN_AXE) {
            ArenaSetupSession session = plugin.getArenaManager().getSetupSession(player.getUniqueId());
            if (session != null) {
                event.setCancelled(true);
                Block block = event.getClickedBlock();
                if (block != null) {
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        session.setPos1(block.getLocation());
                        player.sendMessage(plugin.getMessageManager().getMessage("setup.pos1-set",
                                "%x%", String.valueOf(block.getX()),
                                "%y%", String.valueOf(block.getY()),
                                "%z%", String.valueOf(block.getZ())
                        ));
                    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        session.setPos2(block.getLocation());
                        player.sendMessage(plugin.getMessageManager().getMessage("setup.pos2-set",
                                "%x%", String.valueOf(block.getX()),
                                "%y%", String.valueOf(block.getY()),
                                "%z%", String.valueOf(block.getZ())
                        ));
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                    // If both positions are now selected, show a clickable save hint
                    if (session.isComplete()) {
                        String saveCmd = "cpractice savebounds " + session.getArenaName();
                        net.kyori.adventure.text.Component hint = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            plugin.getMessageManager().getRawMessage("setup.both-set-hint")
                                .replace("%command%", "/" + saveCmd)
                        );
                        net.kyori.adventure.text.Component clickable = hint.clickEvent(
                            net.kyori.adventure.text.event.ClickEvent.runCommand("/" + saveCmd)
                        ).hoverEvent(
                            net.kyori.adventure.text.event.HoverEvent.showText(
                                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                                    plugin.getMessageManager().getRawMessage("setup.both-set-hint-hover")
                                )
                            )
                        );
                        player.sendMessage(clickable);
                    }
                }
                return;
            }
        } else if (item.getType() == Material.IRON_AXE) {
            com.crovex.practice.ffa.FfaSetupSession ffaSession = plugin.getFfaManager().getSetupSession(player.getUniqueId());
            if (ffaSession != null) {
                event.setCancelled(true);
                Block block = event.getClickedBlock();
                if (block != null) {
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        ffaSession.setPos1(block.getLocation());
                        player.sendMessage(plugin.getMessageManager().getMessage("setup.pos1-set",
                                "%x%", String.valueOf(block.getX()),
                                "%y%", String.valueOf(block.getY()),
                                "%z%", String.valueOf(block.getZ())
                        ));
                    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        ffaSession.setPos2(block.getLocation());
                        player.sendMessage(plugin.getMessageManager().getMessage("setup.pos2-set",
                                "%x%", String.valueOf(block.getX()),
                                "%y%", String.valueOf(block.getY()),
                                "%z%", String.valueOf(block.getZ())
                        ));
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                    // If both positions are now selected, show a clickable save hint
                    if (ffaSession.isComplete()) {
                        String saveCmd = "cpractice saveffabounds " + ffaSession.getArenaName();
                        net.kyori.adventure.text.Component hint = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            plugin.getMessageManager().getRawMessage("setup.both-set-hint")
                                .replace("%command%", "/" + saveCmd)
                        );
                        net.kyori.adventure.text.Component clickable = hint.clickEvent(
                            net.kyori.adventure.text.event.ClickEvent.runCommand("/" + saveCmd)
                        ).hoverEvent(
                            net.kyori.adventure.text.event.HoverEvent.showText(
                                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                                    plugin.getMessageManager().getRawMessage("setup.both-set-hint-hover")
                                )
                            )
                        );
                        player.sendMessage(clickable);
                    }
                }
                return;
            }
        }

        // Lobby / Queue right-click utilities
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (pp.getState() == PlayerState.LOBBY) {
                if (item.getType() == Material.IRON_SWORD) {
                    if (!plugin.getConfig().getBoolean("queue.unranked-enabled", true)) {
                        player.sendMessage(plugin.getMessageManager().getMessage("queue.unranked-disabled"));
                        return;
                    }
                    new QueueMenu(plugin, QueueType.UNRANKED).open(player);
                } else if (item.getType() == Material.DIAMOND_SWORD) {
                    if (!plugin.getConfig().getBoolean("queue.ranked-enabled", true)) {
                        player.sendMessage(plugin.getMessageManager().getMessage("queue.ranked-disabled"));
                        return;
                    }
                    new QueueMenu(plugin, QueueType.RANKED).open(player);
                } else if (item.getType() == Material.BOOK) {
                    new KitEditMenu(plugin).open(player);
                } else if (item.getType() == Material.NETHER_STAR) {
                    new com.crovex.practice.gui.PartyMenu(plugin).open(player);
                } else if (item.getType() == Material.PAPER) {
                    player.performCommand("stats");
                } else if (item.getType() == Material.BOOKSHELF) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.leaderboard.loading"));
                    plugin.getDatabaseManager()
                            .getTopPlayers("elo", 10)
                            .thenAccept(topPlayers -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                                new com.crovex.practice.gui.LeaderboardMenu(plugin, com.crovex.practice.gui.LeaderboardMenu.LeaderboardCategory.ELO, topPlayers).open(player)));
                }
            } 
            else if (pp.getState() == PlayerState.QUEUE) {
                if (item.getType() == Material.RED_DYE) {
                    plugin.getQueueManager().removeFromQueue(player, false);
                }
            } 
            else if (pp.getState() == PlayerState.SPECTATING) {
                if (item.getType() == Material.RED_DYE) {
                    Match match = plugin.getMatchManager().getMatchBySpectator(player);
                    if (match != null) {
                        match.removeSpectator(player);
                    } else {
                        plugin.getPlayerManager().resetPlayer(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() != PlayerState.MATCH) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() != PlayerState.MATCH) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
            if (pp != null && pp.getState() != PlayerState.MATCH && pp.getState() != PlayerState.FFA) {
                // Allow match/ffa players to hit non-match/non-ffa players (outsiders)
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) event;
                    Player attacker = null;
                    if (edbee.getDamager() instanceof Player) {
                        attacker = (Player) edbee.getDamager();
                    } else if (edbee.getDamager() instanceof org.bukkit.entity.Projectile) {
                        org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) edbee.getDamager();
                        if (proj.getShooter() instanceof Player) {
                            attacker = (Player) proj.getShooter();
                        }
                    }
                    if (attacker != null) {
                        PracticePlayer ppAttacker = plugin.getPlayerManager().getPlayer(attacker);
                        if (ppAttacker != null && (ppAttacker.getState() == PlayerState.MATCH || ppAttacker.getState() == PlayerState.FFA)) {
                            return; // Allow the damage & knockback
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
            if (pp != null && pp.getState() != PlayerState.MATCH) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() == PlayerState.MATCH) return;

        // Admins can "drop" items — they are silently deleted from the world
        if (player.hasPermission("crovexpractice.admin")) {
            event.setCancelled(false);
            // Remove the dropped entity from the world on the next tick
            Bukkit.getScheduler().runTask(plugin, () -> event.getItemDrop().remove());
        } else {
            // Regular players cannot drop items outside of matches
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (pendingFfaAlias.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            com.crovex.practice.ffa.FfaArena ffa = pendingFfaAlias.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("iptal")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-creation-cancelled"));
                    new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player);
                    return;
                }

                String oldAlias = ffa.getCommandAlias();

                if (message.equalsIgnoreCase("none") || message.equalsIgnoreCase("yok") || message.equalsIgnoreCase("sil")) {
                    if (oldAlias != null && !oldAlias.isEmpty()) {
                        plugin.getFfaManager().unregisterFfaCommand(oldAlias);
                    }
                    ffa.setCommandAlias(null);
                    plugin.getFfaManager().saveConfig();
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-alias-set", "%alias%", "Yok"));
                    new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player);
                    return;
                }

                if (!message.matches("^[a-zA-Z0-9_]{1,16}$")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-alias-invalid"));
                    new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player);
                    return;
                }

                if (oldAlias != null && !oldAlias.isEmpty()) {
                    plugin.getFfaManager().unregisterFfaCommand(oldAlias);
                }

                ffa.setCommandAlias(message.toLowerCase());
                plugin.getFfaManager().saveConfig();

                if (ffa.isEnabled()) {
                    plugin.getFfaManager().registerFfaCommand(ffa.getCommandAlias(), ffa.getName());
                }

                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.ffa-editor.msg-alias-set", "%alias%", "/" + ffa.getCommandAlias()));
                new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player);
            });
            return;
        }

        if (pendingKitCreation.contains(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            pendingKitCreation.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("iptal")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-creation-cancelled"));
                    new com.crovex.practice.gui.AdminKitMenu(plugin).open(player);
                    return;
                }

                if (!message.matches("^[a-zA-Z0-9_]{3,16}$")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-invalid-name"));
                    new com.crovex.practice.gui.AdminKitMenu(plugin).open(player);
                    return;
                }

                if (plugin.getKitManager().getKit(message) != null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-already-exists"));
                    new com.crovex.practice.gui.AdminKitMenu(plugin).open(player);
                    return;
                }

                plugin.getKitManager().createKit(message, com.crovex.practice.kit.KitType.NORMAL);
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kits.msg-created-success", "%kit%", message));
                
                com.crovex.practice.kit.Kit created = plugin.getKitManager().getKit(message);
                if (created != null) {
                    new com.crovex.practice.gui.KitSettingsMenu(plugin, created).open(player);
                } else {
                    new com.crovex.practice.gui.AdminKitMenu(plugin).open(player);
                }
            });
        }
    }
}
