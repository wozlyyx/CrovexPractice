package com.crovex.practice.ffa;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Cuboid;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FfaManager {

    private final CrovexPractice plugin;
    private final Map<String, FfaArena> ffaArenas = new LinkedHashMap<>();
    private final Map<UUID, String> playerFfaArena = new HashMap<>();
    private final Map<UUID, Integer> currentStreaks = new HashMap<>();
    private final Map<UUID, FfaSetupSession> ffaSetupSessions = new HashMap<>();
    private final Map<UUID, Long> combatTags = new HashMap<>();
    private final Map<UUID, UUID> combatOpponents = new HashMap<>();

    public FfaManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        ffaArenas.clear();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();

        if (config.contains("ffa-arenas")) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("ffa-arenas");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection arenaSec = section.getConfigurationSection(key);
                    if (arenaSec != null) {
                        FfaArena arena = FfaArena.deserialize(key, arenaSec);
                        ffaArenas.put(key.toLowerCase(), arena);
                        if (arena.isEnabled() && arena.getCommandAlias() != null && !arena.getCommandAlias().isEmpty()) {
                            registerFfaCommand(arena.getCommandAlias(), arena.getName());
                        }
                    }
                }
            }
        } else {
            // Migrate old single FFA configuration
            if (config.contains("ffa.enabled")) {
                FfaArena defaultArena = new FfaArena("DefaultFFA");
                defaultArena.setEnabled(config.getBoolean("ffa.enabled", false));
                defaultArena.setKitName(config.getString("ffa.kit", "NoDebuff"));
                defaultArena.setSpawnLocation(config.getLocation("ffa.spawn"));
                if (config.contains("ffa.bounds")) {
                    Map<String, Object> boundsMap = config.getConfigurationSection("ffa.bounds").getValues(false);
                    defaultArena.setBounds(Cuboid.deserialize(boundsMap));
                }
                ffaArenas.put("defaultffa", defaultArena);
                if (defaultArena.isEnabled() && defaultArena.getCommandAlias() != null && !defaultArena.getCommandAlias().isEmpty()) {
                    registerFfaCommand(defaultArena.getCommandAlias(), defaultArena.getName());
                }

                config.set("ffa.enabled", null);
                config.set("ffa.kit", null);
                config.set("ffa.spawn", null);
                config.set("ffa.bounds", null);
                saveConfig();
            }
        }
    }

    public void saveConfig() {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        config.set("ffa-arenas", null);
        for (FfaArena arena : ffaArenas.values()) {
            String path = "ffa-arenas." + arena.getName();
            config.set(path + ".enabled", arena.isEnabled());
            config.set(path + ".kit", arena.getKitName());
            config.set(path + ".spawn", arena.getSpawnLocation());
            if (arena.getBounds() != null) {
                config.createSection(path + ".bounds", arena.getBounds().serialize());
            }
            if (arena.getCommandAlias() != null && !arena.getCommandAlias().isEmpty()) {
                config.set(path + ".command-alias", arena.getCommandAlias());
            }
        }
        plugin.saveConfig();
    }

    public Collection<FfaArena> getFfaArenas() {
        return ffaArenas.values();
    }

    public FfaArena getFfaArena(String name) {
        if (name == null) return null;
        return ffaArenas.get(name.toLowerCase());
    }

    public void createFfaArena(String name) {
        if (name == null || ffaArenas.containsKey(name.toLowerCase())) return;
        FfaArena arena = new FfaArena(name);
        ffaArenas.put(name.toLowerCase(), arena);
        saveConfig();
    }

    public void deleteFfaArena(String name) {
        if (name == null) return;
        FfaArena arena = ffaArenas.remove(name.toLowerCase());
        if (arena != null && arena.getCommandAlias() != null && !arena.getCommandAlias().isEmpty()) {
            unregisterFfaCommand(arena.getCommandAlias());
        }
        saveConfig();
    }

    public FfaArena getInsideFfaZone(Location loc) {
        for (FfaArena arena : ffaArenas.values()) {
            if (arena.isEnabled() && arena.getBounds() != null && arena.getBounds().contains(loc)) {
                return arena;
            }
        }
        return null;
    }

    public String getPlayerFfaArenaName(Player player) {
        return playerFfaArena.get(player.getUniqueId());
    }

    public void startSetupSession(UUID uuid, String arenaName) {
        ffaSetupSessions.put(uuid, new FfaSetupSession(arenaName));
    }

    public FfaSetupSession getSetupSession(UUID uuid) {
        return ffaSetupSessions.get(uuid);
    }

    public void removeSetupSession(UUID uuid) {
        ffaSetupSessions.remove(uuid);
    }

    public int getPlayerCount(String arenaName) {
        int count = 0;
        for (String activeArena : playerFfaArena.values()) {
            if (activeArena.equalsIgnoreCase(arenaName)) {
                count++;
            }
        }
        return count;
    }

    public int getPlayerCount() {
        return playerFfaArena.size();
    }

    public void joinFfa(Player player, String arenaName) {
        joinFfa(player, arenaName, true);
    }

    public void joinFfa(Player player, String arenaName, boolean teleport) {
        FfaArena arena = getFfaArena(arenaName);
        if (arena == null || !arena.isEnabled() || arena.getSpawnLocation() == null || arena.getBounds() == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.disabled"));
            return;
        }

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        if (pp.getState() != PlayerState.LOBBY) {
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.only-from-lobby"));
            return;
        }

        Kit kit = plugin.getKitManager().getKit(arena.getKitName());
        if (kit == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.kit-not-found"));
            return;
        }

        pp.setState(PlayerState.FFA);
        playerFfaArena.put(player.getUniqueId(), arena.getName());
        currentStreaks.put(player.getUniqueId(), 0);

        if (teleport) {
            player.teleport(arena.getSpawnLocation());
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.getActivePotionEffects().clear();

        // Apply kit
        ItemStack[] customLayout = pp.getKitLayout(kit.getName());
        kit.applyToPlayer(player, customLayout);

        player.updateInventory();

        player.sendMessage(plugin.getMessageManager().getMessage("ffa.joined"));
        broadcastToFfaArena(arena.getName(), plugin.getMessageManager().getMessage("ffa.join-broadcast", "%player%", player.getName()));
    }

    public boolean leaveFfa(Player player, boolean teleport) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.FFA) return false;

        if (isInCombat(player)) {
            int seconds = getCombatSecondsLeft(player);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.combat-tag", "%seconds%", String.valueOf(seconds)));
            return false;
        }

        String arenaName = playerFfaArena.remove(player.getUniqueId());
        currentStreaks.remove(player.getUniqueId());
        clearCombatTag(player.getUniqueId());

        if (teleport) {
            Location lobbySpawn = null;
            if (plugin.getConfig().contains("lobby-spawn")) {
                lobbySpawn = plugin.getConfig().getLocation("lobby-spawn");
            }
            if (lobbySpawn == null) {
                lobbySpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            player.teleport(lobbySpawn);
        }

        plugin.getPlayerManager().resetPlayer(player);
        player.sendMessage(plugin.getMessageManager().getMessage("ffa.left"));
        if (arenaName != null) {
            broadcastToFfaArena(arenaName, plugin.getMessageManager().getMessage("ffa.leave-broadcast", "%player%", player.getName()));
        }
        return true;
    }

    public void handleKill(Player killer, Player victim) {
        PracticePlayer ppVictim = plugin.getPlayerManager().getPlayer(victim);
        if (ppVictim == null) return;

        // Resolve killer if none provided but victim is in combat
        if (killer == null && isInCombat(victim)) {
            UUID oppUuid = combatOpponents.get(victim.getUniqueId());
            if (oppUuid != null) {
                Player oppPlayer = Bukkit.getPlayer(oppUuid);
                if (oppPlayer != null && oppPlayer.isOnline()) {
                    PracticePlayer ppOpp = plugin.getPlayerManager().getPlayer(oppPlayer);
                    if (ppOpp != null && ppOpp.getState() == PlayerState.FFA) {
                        killer = oppPlayer;
                    }
                }
            }
        }

        String arenaName = playerFfaArena.get(victim.getUniqueId());
        FfaArena arena = getFfaArena(arenaName);
        if (arena == null) return;

        PracticePlayer ppKiller = killer != null ? plugin.getPlayerManager().getPlayer(killer) : null;

        // Victim increments death & resets streak
        ppVictim.setFfaDeaths(ppVictim.getFfaDeaths() + 1);
        currentStreaks.put(victim.getUniqueId(), 0);
        clearCombatTag(victim.getUniqueId());
        plugin.getDatabaseManager().savePlayer(ppVictim);

        if (ppKiller != null && killer != null) {
            // Killer increments kills & streak
            ppKiller.setFfaKills(ppKiller.getFfaKills() + 1);
            int newStreak = currentStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
            currentStreaks.put(killer.getUniqueId(), newStreak);

            if (newStreak > ppKiller.getFfaBestStreak()) {
                ppKiller.setFfaBestStreak(newStreak);
            }
            plugin.getDatabaseManager().savePlayer(ppKiller);

            // Restore health of killer
            killer.setHealth(20.0);
            killer.setFoodLevel(20);
            killer.setFireTicks(0);

            // Refill inventory of killer
            Kit kit = plugin.getKitManager().getKit(arena.getKitName());
            if (kit != null) {
                ItemStack[] customLayout = ppKiller.getKitLayout(kit.getName());
                kit.applyToPlayer(killer, customLayout);
                
                killer.updateInventory();
            }

            // Send messages
            broadcastToFfaArena(arena.getName(), plugin.getMessageManager().getMessage("ffa.kill-broadcast",
                    "%killer%", killer.getName(),
                    "%victim%", victim.getName(),
                    "%killer_streak%", String.valueOf(newStreak)
            ));

            // Streak milestones announcements (5, 10, 15, 20, 25, 50, 100)
            if (newStreak % 5 == 0 || newStreak == 100) {
                Bukkit.broadcast(plugin.getMessageManager().getMessage("ffa.streak-milestone",
                        "%player%", killer.getName(),
                        "%streak%", String.valueOf(newStreak)
                ));
                plugin.getWebhookManager().sendFfaStreakWebhook(killer.getName(), newStreak);
            }
        } else {
            // Died of non-player causes
            broadcastToFfaArena(arena.getName(), plugin.getMessageManager().getMessage("ffa.death-suicide",
                    "%victim%", victim.getName()
            ));
        }

        // Respawn victim immediately at spawn
        victim.teleport(arena.getSpawnLocation());
        victim.setHealth(20.0);
        victim.setFoodLevel(20);
        victim.setFireTicks(0);
        victim.getActivePotionEffects().clear();

        Kit kit = plugin.getKitManager().getKit(arena.getKitName());
        if (kit != null) {
            ItemStack[] customLayout = ppVictim.getKitLayout(kit.getName());
            kit.applyToPlayer(victim, customLayout);
            
            victim.updateInventory();
        }
    }

    public void handleLogout(Player player) {
        UUID uuid = player.getUniqueId();
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null && pp.getState() == PlayerState.FFA) {
            String arenaName = playerFfaArena.get(uuid);
            FfaArena arena = getFfaArena(arenaName);

            if (arena != null && isInCombat(player)) {
                // Combat logged!
                UUID oppUuid = combatOpponents.get(uuid);
                Player killer = null;
                if (oppUuid != null) {
                    Player opp = Bukkit.getPlayer(oppUuid);
                    if (opp != null && opp.isOnline()) {
                        PracticePlayer ppOpp = plugin.getPlayerManager().getPlayer(opp);
                        if (ppOpp != null && ppOpp.getState() == PlayerState.FFA) {
                            killer = opp;
                        }
                    }
                }

                // Increment deaths for victim
                pp.setFfaDeaths(pp.getFfaDeaths() + 1);
                plugin.getDatabaseManager().savePlayerSync(pp);

                if (killer != null) {
                    PracticePlayer ppKiller = plugin.getPlayerManager().getPlayer(killer);
                    if (ppKiller != null) {
                        ppKiller.setFfaKills(ppKiller.getFfaKills() + 1);
                        int newStreak = currentStreaks.getOrDefault(killer.getUniqueId(), 0) + 1;
                        currentStreaks.put(killer.getUniqueId(), newStreak);
                        if (newStreak > ppKiller.getFfaBestStreak()) {
                            ppKiller.setFfaBestStreak(newStreak);
                        }
                        plugin.getDatabaseManager().savePlayer(ppKiller);

                        // Heal killer
                        killer.setHealth(20.0);
                        killer.setFoodLevel(20);
                        killer.setFireTicks(0);

                        // Refill inventory
                        Kit kit = plugin.getKitManager().getKit(arena.getKitName());
                        if (kit != null) {
                            ItemStack[] customLayout = ppKiller.getKitLayout(kit.getName());
                            kit.applyToPlayer(killer, customLayout);

                            killer.updateInventory();
                        }

                        broadcastToFfaArena(arena.getName(), plugin.getMessageManager().getMessage("ffa.kill-broadcast",
                                "%killer%", killer.getName(),
                                "%victim%", player.getName(),
                                "%killer_streak%", String.valueOf(newStreak)
                        ));

                        if (newStreak % 5 == 0 || newStreak == 100) {
                            Bukkit.broadcast(plugin.getMessageManager().getMessage("ffa.streak-milestone",
                                    "%player%", killer.getName(),
                                    "%streak%", String.valueOf(newStreak)
                            ));
                            plugin.getWebhookManager().sendFfaStreakWebhook(killer.getName(), newStreak);
                        }
                    }
                } else {
                    broadcastToFfaArena(arena.getName(), plugin.getMessageManager().getMessage("ffa.death-suicide",
                            "%victim%", player.getName()
                    ));
                }
            }

            // Set state to LOBBY so they rejoin at lobby
            pp.setState(PlayerState.LOBBY);
            plugin.getDatabaseManager().savePlayerSync(pp);
        }

        playerFfaArena.remove(uuid);
        currentStreaks.remove(uuid);
        ffaSetupSessions.remove(uuid);
        clearCombatTag(uuid);
    }

    public int getCurrentStreak(Player player) {
        return currentStreaks.getOrDefault(player.getUniqueId(), 0);
    }

    public void broadcastToFfaArena(String arenaName, Component msg) {
        if (arenaName == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PracticePlayer pp = plugin.getPlayerManager().getPlayer(p);
            if (pp != null && pp.getState() == PlayerState.FFA) {
                String pArena = playerFfaArena.get(p.getUniqueId());
                if (arenaName.equalsIgnoreCase(pArena)) {
                    p.sendMessage(msg);
                }
            }
        }
    }

    public boolean isInCombat(Player player) {
        Long expire = combatTags.get(player.getUniqueId());
        return expire != null && System.currentTimeMillis() < expire;
    }

    public int getCombatSecondsLeft(Player player) {
        Long expire = combatTags.get(player.getUniqueId());
        if (expire == null) return 0;
        long diff = expire - System.currentTimeMillis();
        return diff <= 0 ? 0 : (int) Math.ceil(diff / 1000.0);
    }

    public void tagPlayer(Player player, Player opponent) {
        int seconds = plugin.getConfig().getInt("ffa.combat-tag-seconds", 0);
        if (seconds <= 0) return;

        combatTags.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
        if (opponent != null) {
            combatOpponents.put(player.getUniqueId(), opponent.getUniqueId());
        }
    }

    public void clearCombatTag(UUID uuid) {
        combatTags.remove(uuid);
        combatOpponents.remove(uuid);
    }

    public void registerFfaCommand(String alias, String arenaName) {
        if (alias == null || alias.isEmpty()) return;
        try {
            java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(Bukkit.getServer());

            DynamicFfaCommand cmd = new DynamicFfaCommand(plugin, alias.toLowerCase(), arenaName);
            commandMap.register(plugin.getName().toLowerCase(), cmd);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Dinamik FFA komutu kaydedilemedi: " + alias, e);
        }
    }

    public void unregisterFfaCommand(String alias) {
        if (alias == null || alias.isEmpty()) return;
        try {
            java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(Bukkit.getServer());

            java.lang.reflect.Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            knownCommands.remove(alias.toLowerCase());
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + alias.toLowerCase());

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Dinamik FFA komutu kaldirilirken hata olustu: " + alias, e);
        }
    }
}
