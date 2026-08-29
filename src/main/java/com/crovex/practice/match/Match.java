package com.crovex.practice.match;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.gui.PostMatchInventoryGUI;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.kit.KitType;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class Match {

    private final CrovexPractice plugin;
    private final UUID id;
    
    // Legacy support for 1v1 checks
    private final Player playerA;
    private final Player playerB;
    private final UUID uuidA;
    private final UUID uuidB;
    private final String nameA;
    private final String nameB;

    // Advanced multi-player and team structures
    private final MatchType matchType;
    private final List<UUID> teamA = new ArrayList<>();
    private final List<UUID> teamB = new ArrayList<>();
    private final List<UUID> initialPlayers = new ArrayList<>();
    private final List<UUID> alivePlayers = new ArrayList<>();
    private final Map<UUID, String> playerNames = new HashMap<>();

    private final Kit kit;
    private final QueueType queueType;
    private final Arena arena;
    private MatchState state;

    private long startTime;
    private int durationSeconds = 0;
    private BukkitRunnable matchTask;

    // Hit counts
    private final Map<UUID, Integer> hits = new HashMap<>();

    // Cached inventories for post-match GUI
    private final Map<UUID, ItemStack[]> postInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> postArmor = new HashMap<>();
    private final Map<UUID, Double> postHealth = new HashMap<>();
    private final Map<UUID, Integer> postFood = new HashMap<>();

    // Spectator list
    private final Set<UUID> spectators = new HashSet<>();

    // Constructor for legacy 1v1
    public Match(CrovexPractice plugin, Player playerA, Player playerB, Kit kit, QueueType queueType, Arena arena) {
        this.plugin = plugin;
        this.id = UUID.randomUUID();
        this.matchType = MatchType.DUEL;
        
        this.playerA = playerA;
        this.playerB = playerB;
        this.uuidA = playerA.getUniqueId();
        this.uuidB = playerB.getUniqueId();
        this.nameA = playerA.getName();
        this.nameB = playerB.getName();
        
        this.kit = kit;
        this.queueType = queueType;
        this.arena = arena;
        this.state = MatchState.STARTING;

        // Initialize players lists
        this.initialPlayers.add(uuidA);
        this.initialPlayers.add(uuidB);
        this.alivePlayers.addAll(initialPlayers);
        this.teamA.add(uuidA);
        this.teamB.add(uuidB);
        this.playerNames.put(uuidA, nameA);
        this.playerNames.put(uuidB, nameB);
        
        this.hits.put(uuidA, 0);
        this.hits.put(uuidB, 0);
    }

    // Constructor for Party FFA
    public Match(CrovexPractice plugin, List<Player> players, MatchType matchType, Kit kit, Arena arena) {
        this.plugin = plugin;
        this.id = UUID.randomUUID();
        this.matchType = matchType;
        
        this.playerA = players.isEmpty() ? null : players.get(0);
        this.playerB = players.size() < 2 ? null : players.get(1);
        this.uuidA = playerA != null ? playerA.getUniqueId() : null;
        this.uuidB = playerB != null ? playerB.getUniqueId() : null;
        this.nameA = playerA != null ? playerA.getName() : "";
        this.nameB = playerB != null ? playerB.getName() : "";

        this.kit = kit;
        this.queueType = QueueType.UNRANKED;
        this.arena = arena;
        this.state = MatchState.STARTING;

        for (Player p : players) {
            UUID uuid = p.getUniqueId();
            initialPlayers.add(uuid);
            alivePlayers.add(uuid);
            playerNames.put(uuid, p.getName());
            hits.put(uuid, 0);
        }
    }

    // Constructor for Party Split / Party VS
    public Match(CrovexPractice plugin, List<Player> teamAPlayers, List<Player> teamBPlayers, MatchType matchType, Kit kit, Arena arena) {
        this.plugin = plugin;
        this.id = UUID.randomUUID();
        this.matchType = matchType;

        this.playerA = teamAPlayers.isEmpty() ? null : teamAPlayers.get(0);
        this.playerB = teamBPlayers.isEmpty() ? null : teamBPlayers.get(0);
        this.uuidA = playerA != null ? playerA.getUniqueId() : null;
        this.uuidB = playerB != null ? playerB.getUniqueId() : null;
        this.nameA = playerA != null ? playerA.getName() : "";
        this.nameB = playerB != null ? playerB.getName() : "";

        this.kit = kit;
        this.queueType = QueueType.UNRANKED;
        this.arena = arena;
        this.state = MatchState.STARTING;

        for (Player p : teamAPlayers) {
            UUID uuid = p.getUniqueId();
            teamA.add(uuid);
            initialPlayers.add(uuid);
            alivePlayers.add(uuid);
            playerNames.put(uuid, p.getName());
            hits.put(uuid, 0);
        }

        for (Player p : teamBPlayers) {
            UUID uuid = p.getUniqueId();
            teamB.add(uuid);
            initialPlayers.add(uuid);
            alivePlayers.add(uuid);
            playerNames.put(uuid, p.getName());
            hits.put(uuid, 0);
        }
    }

    public void start() {
        arena.setInUse(true);
        plugin.getBlockRestoreManager().startTracking(id, arena);

        // Update player states and teleport
        int ffaIndex = 0;
        for (UUID uuid : initialPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                setPlayerMatchState(p);
                
                // Teleportation Spawns
                if (matchType == MatchType.DUEL) {
                    p.teleport(uuid.equals(uuidA) ? arena.getSpawn1() : arena.getSpawn2());
                } else if (matchType == MatchType.PARTY_SPLIT || matchType == MatchType.PARTY_VS) {
                    p.teleport(teamA.contains(uuid) ? arena.getSpawn1() : arena.getSpawn2());
                } else { // PARTY_FFA
                    p.teleport(ffaIndex % 2 == 0 ? arena.getSpawn1() : arena.getSpawn2());
                    ffaIndex++;
                }

                // Apply Kit
                PracticePlayer pp = plugin.getPlayerManager().getPlayer(p);
                kit.applyToPlayer(p, pp != null ? pp.getKitLayout(kit.getName()) : null);
            }
        }

        new BukkitRunnable() {
            int countdown = plugin.getConfig().getInt("match.countdown-seconds", 5);

            @Override
            public void run() {
                // If match ended prematurely or all players logged out
                if (state == MatchState.ENDING || alivePlayers.isEmpty()) {
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    Component subtitle = plugin.getMessageManager().getMessage("match.countdown", "%time%", String.valueOf(countdown));
                    Component title = plugin.getMessageManager().getMessage("match.countdown-title", "%time%", String.valueOf(countdown));
                    
                    for (UUID uuid : initialPlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    countdown--;
                } else {
                    Component startTitle = plugin.getMessageManager().getMessage("match.start-title");
                    
                    for (UUID uuid : initialPlayers) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.showTitle(net.kyori.adventure.title.Title.title(startTitle, Component.empty()));
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.0f);
                        }
                    }

                    state = MatchState.ACTIVE;
                    startTime = System.currentTimeMillis();
                    startMatchTimer();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void setPlayerMatchState(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getActivePotionEffects().clear();

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            pp.setState(PlayerState.MATCH);
            pp.setActiveMatch(this);
        }
    }

    private void startMatchTimer() {
        matchTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != MatchState.ACTIVE) {
                    cancel();
                    return;
                }
                durationSeconds++;

                // Max duration limit check
                int maxDuration = plugin.getConfig().getInt("match.max-duration-seconds", 600);
                if (durationSeconds >= maxDuration) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (matchType == MatchType.DUEL) {
                            endMatch(null, null);
                        } else if (matchType == MatchType.PARTY_FFA) {
                            endMatchFFA(null);
                        } else {
                            endMatchTeam(new ArrayList<>(), new ArrayList<>());
                        }
                    });
                    cancel();
                    return;
                }

                // Sumo water detection check
                if (kit.getType() == KitType.SUMO) {
                    for (UUID uuid : new ArrayList<>(alivePlayers)) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            checkSumoWater(p);
                        }
                    }
                }
            }
        };
        matchTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkSumoWater(Player player) {
        Material mat = player.getLocation().getBlock().getType();
        if (mat == Material.WATER) {
            handleDeath(player);
        }
    }

    public void addHit(Player attacker) {
        if (state != MatchState.ACTIVE) return;
        UUID attackerUuid = attacker.getUniqueId();
        int currentHits = hits.getOrDefault(attackerUuid, 0) + 1;
        hits.put(attackerUuid, currentHits);

        // Boxing logic
        if (kit.getType() == KitType.BOXING) {
            int maxHits = plugin.getConfig().getInt("match.boxing-max-hits", 100);
            attacker.sendActionBar(plugin.getMessageManager().getMessage("match.boxing-actionbar", 
                    "%hits%", String.valueOf(currentHits),
                    "%max%", String.valueOf(maxHits)));
            
            if (currentHits >= maxHits) {
                // Instantly win match or eliminate others
                if (matchType == MatchType.DUEL) {
                    Player defender = (attackerUuid.equals(uuidA)) ? playerB : playerA;
                    endMatch(attacker, defender);
                } else if (matchType == MatchType.PARTY_FFA) {
                    // Attacker wins FFA instantly
                    endMatchFFA(attackerUuid);
                } else { // Team split or VS
                    // Attacker team wins instantly
                    endMatchTeam(teamA.contains(attackerUuid) ? teamA : teamB, teamA.contains(attackerUuid) ? teamB : teamA);
                }
            }
        }
    }

    public void handleLogout(Player player) {
        if (state == MatchState.ENDING) return;
        if (state == MatchState.STARTING) {
            UUID dcUuid = player.getUniqueId();
            if (alivePlayers.contains(dcUuid)) {
                alivePlayers.remove(dcUuid);
                if (matchType == MatchType.DUEL) {
                    Player winner = dcUuid.equals(uuidA) ? playerB : playerA;
                    endMatch(winner, player);
                    return;
                } else if (matchType == MatchType.PARTY_FFA) {
                    if (alivePlayers.size() <= 1) {
                        endMatchFFA(alivePlayers.isEmpty() ? null : alivePlayers.get(0));
                        return;
                    }
                } else {
                    eliminatePlayer(player);
                    return;
                }
            }
        }
        handleDeath(player);
    }

    public void handleDeath(Player deadPlayer) {
        if (state != MatchState.ACTIVE) return;
        UUID deadUuid = deadPlayer.getUniqueId();

        if (matchType == MatchType.DUEL) {
            Player winner = deadUuid.equals(uuidA) ? playerB : playerA;
            endMatch(winner, deadPlayer);
        } else {
            // Eliminate player in party matches
            eliminatePlayer(deadPlayer);
        }
    }

    private void eliminatePlayer(Player deadPlayer) {
        UUID deadUuid = deadPlayer.getUniqueId();
        if (!alivePlayers.contains(deadUuid)) return;

        alivePlayers.remove(deadUuid);
        cachePostMatchStats(deadPlayer);

        // Notify match participants and spectators
        broadcastMessage(plugin.getMessageManager().getMessage("match.eliminated", "%player%", deadPlayer.getName()));

        // Make dead player a spectator in this match
        makePlayerSpectator(deadPlayer);

        // Check Match termination conditions
        if (matchType == MatchType.PARTY_FFA) {
            if (alivePlayers.size() <= 1) {
                UUID winnerUuid = alivePlayers.isEmpty() ? null : alivePlayers.get(0);
                endMatchFFA(winnerUuid);
            }
        } else if (matchType == MatchType.PARTY_SPLIT || matchType == MatchType.PARTY_VS) {
            boolean teamAAlive = false;
            for (UUID uuid : teamA) {
                if (alivePlayers.contains(uuid)) {
                    teamAAlive = true;
                    break;
                }
            }

            boolean teamBAlive = false;
            for (UUID uuid : teamB) {
                if (alivePlayers.contains(uuid)) {
                    teamBAlive = true;
                    break;
                }
            }

            if (!teamAAlive) {
                // Team B wins
                endMatchTeam(teamB, teamA);
            } else if (!teamBAlive) {
                // Team A wins
                endMatchTeam(teamA, teamB);
            }
        }
    }

    private void makePlayerSpectator(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Hide spectator from alive players
        for (UUID uuid : alivePlayers) {
            Player alive = Bukkit.getPlayer(uuid);
            if (alive != null && alive.isOnline()) {
                alive.hidePlayer(plugin, player);
            }
        }

        // Give spectator leave item
        ItemStack leaveItem = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.displayName(plugin.getMessageManager().getMessage("match.spectating.leave-item"));
        leaveItem.setItemMeta(meta);
        player.getInventory().setItem(8, leaveItem);
        player.updateInventory();

        player.teleport(arena.getSpectatorSpawn() != null ? arena.getSpectatorSpawn() : arena.getSpawn1());
    }

    // legacy 1v1 endMatch adapter
    public void endMatch(Player winner, Player loser) {
        if (state == MatchState.ENDING) return;
        
        List<UUID> winners = winner != null ? List.of(winner.getUniqueId()) : List.of();
        List<UUID> losers = loser != null ? List.of(loser.getUniqueId()) : List.of();

        // Cache stats for both players in 1v1
        for (UUID uuid : initialPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                cachePostMatchStats(p);
            }
        }

        int eloChange = 0;
        int oldEloW = 1000, oldEloL = 1000;
        int newEloW = 1000, newEloL = 1000;

        if (queueType == QueueType.RANKED && winner != null && loser != null) {
            PracticePlayer ppWinner = plugin.getPlayerManager().getPlayer(winner);
            PracticePlayer ppLoser = plugin.getPlayerManager().getPlayer(loser);

            if (ppWinner != null && ppLoser != null) {
                oldEloW = ppWinner.getElo();
                oldEloL = ppLoser.getElo();

                eloChange = calculateEloChange(oldEloW, oldEloL);
                newEloW = oldEloW + eloChange;
                newEloL = Math.max(plugin.getConfig().getInt("queue.elo.minimum", 100), oldEloL - eloChange);

                ppWinner.setElo(newEloW);
                ppWinner.setRankedWins(ppWinner.getRankedWins() + 1);
                ppWinner.setWinstreak(ppWinner.getWinstreak() + 1);
                if (ppWinner.getWinstreak() > ppWinner.getBestWinstreak()) {
                    ppWinner.setBestWinstreak(ppWinner.getWinstreak());
                }

                ppLoser.setElo(newEloL);
                ppLoser.setRankedLosses(ppLoser.getRankedLosses() + 1);
                ppLoser.setWinstreak(0);

                plugin.getDatabaseManager().savePlayer(ppWinner);
                plugin.getDatabaseManager().savePlayer(ppLoser);
            }
        } else if (queueType == QueueType.UNRANKED) {
            if (winner != null) {
                PracticePlayer ppWinner = plugin.getPlayerManager().getPlayer(winner);
                if (ppWinner != null) {
                    ppWinner.setUnrankedWins(ppWinner.getUnrankedWins() + 1);
                    ppWinner.setWinstreak(ppWinner.getWinstreak() + 1);
                    if (ppWinner.getWinstreak() > ppWinner.getBestWinstreak()) {
                        ppWinner.setBestWinstreak(ppWinner.getWinstreak());
                    }
                    plugin.getDatabaseManager().savePlayer(ppWinner);
                }
            }
            if (loser != null) {
                PracticePlayer ppLoser = plugin.getPlayerManager().getPlayer(loser);
                if (ppLoser != null) {
                    ppLoser.setUnrankedLosses(ppLoser.getUnrankedLosses() + 1);
                    ppLoser.setWinstreak(0);
                    plugin.getDatabaseManager().savePlayer(ppLoser);
                }
            }
        }

        List<Component> results = new ArrayList<>();
        if (winner == null && loser == null) {
            results.add(plugin.getMessageManager().getMessage("match.ended.draw-line"));
        } else {
            Component winnerComp;
            Component loserComp;
            if (queueType == QueueType.RANKED && winner != null && loser != null) {
                winnerComp = plugin.getMessageManager().getMessage("match.ended.winner-line",
                        "%winner%", winner.getName(),
                        "%elo_change%", String.valueOf(eloChange),
                        "%new_elo%", String.valueOf(newEloW));
                loserComp = plugin.getMessageManager().getMessage("match.ended.loser-line",
                        "%loser%", loser.getName(),
                        "%elo_change%", String.valueOf(eloChange),
                        "%new_elo%", String.valueOf(newEloL));
            } else {
                winnerComp = plugin.getMessageManager().getMessage("match.ended.winner-line-unranked", "%winner%", winner != null ? winner.getName() : "Draw");
                loserComp = plugin.getMessageManager().getMessage("match.ended.loser-line-unranked", "%loser%", loser != null ? loser.getName() : "Draw");
            }
            results.add(winnerComp);
            results.add(loserComp);
        }

        endMatchGeneral("DUEL", winners, losers, results);

        // Webhook Integration
        if (queueType == QueueType.RANKED && winner != null && loser != null) {
            plugin.getWebhookManager().sendMatchEndWebhook(
                winner.getName(), loser.getName(), kit.getDisplayName(), "Ranked",
                eloChange, newEloW, newEloL, durationSeconds, true
            );
        } else {
            plugin.getWebhookManager().sendMatchEndWebhook(
                winner != null ? winner.getName() : "Draw",
                loser != null ? loser.getName() : "Draw",
                kit.getDisplayName(), "Unranked",
                0, 0, 0, durationSeconds, false
            );
        }
    }

    private void endMatchFFA(UUID winnerUuid) {
        if (state == MatchState.ENDING) return;

        List<UUID> winners = new ArrayList<>();
        List<UUID> losers = new ArrayList<>();

        String winnerName = "Nobody";
        if (winnerUuid != null) {
            winners.add(winnerUuid);
            winnerName = playerNames.get(winnerUuid);
            
            Player wp = Bukkit.getPlayer(winnerUuid);
            if (wp != null) cachePostMatchStats(wp);
        }

        for (UUID uuid : initialPlayers) {
            if (!uuid.equals(winnerUuid)) {
                losers.add(uuid);
            }
        }

        Component winnerComp = plugin.getMessageManager().getMessage("match.ended.winner-line-ffa", "%winner%", winnerName);

        endMatchGeneral("PARTY_FFA", winners, losers, List.of(winnerComp));

        // Webhook Integration
        List<String> loserNames = new ArrayList<>();
        for (UUID uuid : initialPlayers) {
            if (!uuid.equals(winnerUuid)) {
                loserNames.add(playerNames.get(uuid));
            }
        }
        plugin.getWebhookManager().sendMatchEndWebhook(
            winnerName, String.join(", ", loserNames), kit.getDisplayName(), "Party FFA",
            0, 0, 0, durationSeconds, false
        );
    }

    private void endMatchTeam(List<UUID> winningTeam, List<UUID> losingTeam) {
        if (state == MatchState.ENDING) return;

        // Cache stats for remaining survivors
        for (UUID uuid : winningTeam) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && alivePlayers.contains(uuid)) {
                cachePostMatchStats(p);
            }
        }

        Component teamComp;
        String teamName;
        if (winningTeam.isEmpty() && losingTeam.isEmpty()) {
            teamName = "Draw";
            teamComp = plugin.getMessageManager().getMessage("match.ended.draw-line");
        } else {
            teamName = winningTeam.equals(teamA) ? "Red Team" : "Blue Team";
            teamComp = plugin.getMessageManager().getMessage("match.ended.winner-line-team", "%team%", teamName);
        }

        endMatchGeneral(matchType == MatchType.PARTY_SPLIT ? "PARTY_SPLIT" : "PARTY_VS", winningTeam, losingTeam, List.of(teamComp));

        // Webhook Integration
        String losingTeamName = losingTeam.equals(teamA) ? "Red Team" : "Blue Team";
        if (winningTeam.isEmpty() && losingTeam.isEmpty()) {
            losingTeamName = "Draw";
        }
        List<String> winnersList = new ArrayList<>();
        for (UUID u : winningTeam) winnersList.add(playerNames.get(u));
        List<String> losersList = new ArrayList<>();
        for (UUID u : losingTeam) losersList.add(playerNames.get(u));

        plugin.getWebhookManager().sendMatchEndWebhook(
            teamName + " (" + String.join(", ", winnersList) + ")",
            losingTeamName + " (" + String.join(", ", losersList) + ")",
            kit.getDisplayName(),
            matchType == MatchType.PARTY_SPLIT ? "Party Split" : "Party VS",
            0, 0, 0, durationSeconds, false
        );
    }

    private void endMatchGeneral(String displayType, List<UUID> winners, List<UUID> losers, List<Component> detailLines) {
        state = MatchState.ENDING;

        if (matchTask != null) {
            matchTask.cancel();
        }

        // Play sounds
        for (UUID uuid : winners) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        for (UUID uuid : losers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.8f);
        }

        // Format and send result cards
        String headerType = "Unranked";
        if (displayType.equals("DUEL")) {
            headerType = queueType == QueueType.RANKED ? "Ranked" : "Unranked";
        } else if (displayType.equals("PARTY_FFA")) {
            headerType = "Party FFA";
        } else if (displayType.equals("PARTY_SPLIT")) {
            headerType = "Party Split";
        } else if (displayType.equals("PARTY_VS")) {
            headerType = "Party VS";
        }

        Component header = plugin.getMessageManager().getMessage("match.ended.header", "%type%", headerType, "%kit%", kit.getDisplayName());
        String border = "<dark_gray><st>--------------------------------------------------</st>";

        // Build clickable inventories
        Component invLine = plugin.getMessageManager().getMessage("match.ended.inventories-format");
        Component divider = plugin.getMessageManager().getMessage("match.ended.divider");
        
        boolean first = true;
        for (UUID uuid : initialPlayers) {
            String name = playerNames.get(uuid);
            Component click = plugin.getMessageManager().getMessage("match.ended.inv-format", "%player%", name)
                    .clickEvent(ClickEvent.runCommand("/cpractice viewinv " + id.toString() + " " + uuid.toString()))
                    .hoverEvent(HoverEvent.showText(plugin.getMessageManager().getMessage("match.ended.inventories-hover", "%player%", name)));
            
            if (!first) invLine = invLine.append(divider);
            invLine = invLine.append(click);
            first = false;
        }

        // Send to players and spectators
        List<Player> recipients = new ArrayList<>();
        for (UUID uuid : initialPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) recipients.add(p);
        }
        for (UUID sUuid : spectators) {
            Player spec = Bukkit.getPlayer(sUuid);
            if (spec != null && spec.isOnline()) recipients.add(spec);
        }

        for (Player p : recipients) {
            p.sendMessage(MiniMessage.miniMessage().deserialize(border));
            p.sendMessage(header);
            for (Component line : detailLines) {
                p.sendMessage(line);
            }
            p.sendMessage(Component.empty());
            p.sendMessage(invLine);
            p.sendMessage(MiniMessage.miniMessage().deserialize(border));
        }

        // Return players to lobby after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Restore players
                for (UUID uuid : initialPlayers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        plugin.getPlayerManager().resetPlayer(p);
                    }
                }

                // Restore spectators
                for (UUID specUuid : new ArrayList<>(spectators)) {
                    Player spec = Bukkit.getPlayer(specUuid);
                    if (spec != null) {
                        removeSpectator(spec);
                    }
                }

                // Rollback blocks
                plugin.getBlockRestoreManager().restoreAndCleanup(id);
                arena.rollbackBlocks();
                arena.setInUse(false);
                plugin.getMatchManager().removeActiveMatch(id);
            }
        }.runTaskLater(plugin, plugin.getConfig().getInt("match.lobby-return-delay", 3) * 20L);
    }

    private void cachePostMatchStats(Player player) {
        UUID uuid = player.getUniqueId();
        if (postInventories.containsKey(uuid)) return; // Prevent double caching
        postInventories.put(uuid, player.getInventory().getContents().clone());
        postArmor.put(uuid, player.getInventory().getArmorContents().clone());
        postHealth.put(uuid, player.getHealth());
        postFood.put(uuid, player.getFoodLevel());
    }

    private int calculateEloChange(int eloWinner, int eloLoser) {
        int k = plugin.getConfig().getInt("queue.elo.gain-max", com.crovex.practice.util.EloCalculator.DEFAULT_K_FACTOR);
        return com.crovex.practice.util.EloCalculator.calculateEloChange(eloWinner, eloLoser, k);
    }

    // Spectator Handling
    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());
        
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            pp.setState(PlayerState.SPECTATING);
            pp.setActiveMatch(this);
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Hide spectator from alive players
        for (UUID uuid : alivePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.hidePlayer(plugin, player);
            }
        }

        // Give spectator leave item
        ItemStack leaveItem = new ItemStack(Material.RED_DYE);
        ItemMeta meta = leaveItem.getItemMeta();
        meta.displayName(plugin.getMessageManager().getMessage("match.spectating.leave-item"));
        leaveItem.setItemMeta(meta);
        player.getInventory().setItem(8, leaveItem);
        player.updateInventory();

        player.teleport(arena.getSpectatorSpawn() != null ? arena.getSpectatorSpawn() : arena.getSpawn1());
        player.sendMessage(plugin.getMessageManager().getMessage("match.spectating.joined", "%player1%", nameA, "%player2%", nameB));
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
        
        // Show player to match players
        for (UUID uuid : initialPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.showPlayer(plugin, player);
            }
        }

        plugin.getPlayerManager().resetPlayer(player);
        player.sendMessage(plugin.getMessageManager().getMessage("match.spectating.left"));
    }

    private void broadcastMessage(Component msg) {
        for (UUID uuid : initialPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
        for (UUID sUuid : spectators) {
            Player spec = Bukkit.getPlayer(sUuid);
            if (spec != null && spec.isOnline()) {
                spec.sendMessage(msg);
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public Kit getKit() {
        return kit;
    }

    public Arena getArena() {
        return arena;
    }

    public MatchState getState() {
        return state;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public int getHits(UUID uuid) {
        return hits.getOrDefault(uuid, 0);
    }

    public Map<UUID, ItemStack[]> getPostInventories() {
        return postInventories;
    }

    public Map<UUID, ItemStack[]> getPostArmor() {
        return postArmor;
    }

    public Map<UUID, Double> getPostHealth() {
        return postHealth;
    }

    public Map<UUID, Integer> getPostFood() {
        return postFood;
    }

    public String getNameA() {
        return nameA;
    }

    public String getNameB() {
        return nameB;
    }

    public UUID getUuidA() {
        return uuidA;
    }

    public UUID getUuidB() {
        return uuidB;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public List<UUID> getInitialPlayers() {
        return initialPlayers;
    }

    public List<UUID> getAlivePlayers() {
        return alivePlayers;
    }

    public boolean isSameTeam(Player p1, Player p2) {
        UUID u1 = p1.getUniqueId();
        UUID u2 = p2.getUniqueId();
        return (teamA.contains(u1) && teamA.contains(u2)) || (teamB.contains(u1) && teamB.contains(u2));
    }
}
