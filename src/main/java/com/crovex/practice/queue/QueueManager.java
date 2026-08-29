package com.crovex.practice.queue;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private final CrovexPractice plugin;
    private final Map<UUID, QueueEntry> queues = new ConcurrentHashMap<>();

    public QueueManager(CrovexPractice plugin) {
        this.plugin = plugin;
        startMatchmakingTask();
        startActionBarTask();
    }

    public void addToQueue(Player player, Kit kit, QueueType type) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        if (pp.getState() != PlayerState.LOBBY) {
            player.sendMessage(plugin.getMessageManager().getMessage("queue.cannot-join"));
            return;
        }

        // Set state
        pp.setState(PlayerState.QUEUE);
        
        QueueEntry entry = new QueueEntry(player.getUniqueId(), player.getName(), kit.getName(), type, pp.getElo());
        queues.put(player.getUniqueId(), entry);

        // Give leave item
        player.getInventory().clear();
        ItemStack leaveItem = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.displayName(plugin.getMessageManager().getMessage("queue.leave-item"));
        leaveItem.setItemMeta(meta);
        player.getInventory().setItem(4, leaveItem);
        player.updateInventory();

        // Sound & Msg
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        String typeStr = type == QueueType.RANKED ? 
                plugin.getMessageManager().getRawMessage("queue.type-ranked") : 
                plugin.getMessageManager().getRawMessage("queue.type-unranked");
        String eloInfo = type == QueueType.RANKED ? " <gray>(Elo: " + pp.getElo() + ")" : "";
        player.sendMessage(plugin.getMessageManager().getMessage("queue.joined",
                "%type%", typeStr,
                "%kit%", kit.getDisplayName(),
                "%elo_info%", eloInfo
        ));
    }

    public void removeFromQueue(Player player, boolean silent) {
        QueueEntry entry = queues.remove(player.getUniqueId());
        if (entry == null) return;

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            pp.setState(PlayerState.LOBBY);
        }

        plugin.getPlayerManager().resetPlayer(player);

        if (!silent) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            player.sendMessage(plugin.getMessageManager().getMessage("queue.left"));
        }
    }

    public boolean isInQueue(Player player) {
        return queues.containsKey(player.getUniqueId());
    }

    public QueueEntry getQueueEntry(Player player) {
        return queues.get(player.getUniqueId());
    }

    public int getQueuedCount(String kitName, QueueType type) {
        int count = 0;
        for (QueueEntry entry : queues.values()) {
            if (entry.getKitName().equalsIgnoreCase(kitName) && entry.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public int getTotalQueuedCount() {
        return queues.size();
    }

    private void startMatchmakingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (queues.isEmpty()) return;

                List<QueueEntry> entries = new ArrayList<>(queues.values());
                Set<UUID> matched = new HashSet<>();

                for (int i = 0; i < entries.size(); i++) {
                    QueueEntry entryA = entries.get(i);
                    if (matched.contains(entryA.getUuid())) continue;

                    Player playerA = Bukkit.getPlayer(entryA.getUuid());
                    if (playerA == null) {
                        queues.remove(entryA.getUuid());
                        continue;
                    }

                    for (int j = i + 1; j < entries.size(); j++) {
                        QueueEntry entryB = entries.get(j);
                        if (matched.contains(entryB.getUuid())) continue;
                        if (!entryA.getKitName().equalsIgnoreCase(entryB.getKitName())) continue;
                        if (entryA.getType() != entryB.getType()) continue;

                        Player playerB = Bukkit.getPlayer(entryB.getUuid());
                        if (playerB == null) {
                            queues.remove(entryB.getUuid());
                            continue;
                        }

                        // Matchmaking Logic
                        boolean canMatch = false;
                        if (entryA.getType() == QueueType.UNRANKED) {
                            canMatch = true; // Unranked matches instantly
                        } else {
                            // Ranked - Elo range matching
                            int eloDiff = Math.abs(entryA.getInitialElo() - entryB.getInitialElo());
                            int rangeA = entryA.getEloRange();
                            int rangeB = entryB.getEloRange();
                            
                            if (eloDiff <= rangeA && eloDiff <= rangeB) {
                                canMatch = true;
                            }
                        }

                        if (canMatch) {
                            Kit kit = plugin.getKitManager().getKit(entryA.getKitName());
                            // Find an available arena
                            Arena arena = plugin.getArenaManager().findAvailableArena(kit);
                            if (arena == null) {
                                // Inform them they are waiting for an arena
                                Component msg = plugin.getMessageManager().getMessage("queue.waiting-arena");
                                playerA.sendMessage(msg);
                                playerB.sendMessage(msg);
                                continue;
                            }

                            // Match found!
                            matched.add(entryA.getUuid());
                            matched.add(entryB.getUuid());
                            
                            queues.remove(entryA.getUuid());
                            queues.remove(entryB.getUuid());

                            // Start match on the main thread
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getMatchManager().startMatch(playerA, playerB, kit, entryA.getType(), arena);
                            });
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Every 1 second
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, QueueEntry> entry : queues.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null) continue;

                    QueueEntry qe = entry.getValue();
                    int seconds = qe.getElapsedSeconds();
                    String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);

                    Component text;
                    if (qe.getType() == QueueType.UNRANKED) {
                        text = plugin.getMessageManager().getMessage("queue.actionbar-unranked", "%time%", timeStr);
                    } else {
                        text = plugin.getMessageManager().getMessage("queue.actionbar-ranked", "%time%", timeStr, "%range%", String.valueOf(qe.getEloRange()));
                    }
                    player.sendActionBar(text);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // Every 0.5 seconds
    }
}
