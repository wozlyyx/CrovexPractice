package com.crovex.practice;

import com.crovex.practice.arena.ArenaManager;
import com.crovex.practice.command.PracticeCommand;
import com.crovex.practice.database.DatabaseManager;
import com.crovex.practice.kit.KitManager;
import com.crovex.practice.listener.GeneralListener;
import com.crovex.practice.listener.MatchListener;
import com.crovex.practice.match.MatchManager;
import com.crovex.practice.party.PartyManager;
import com.crovex.practice.player.PlayerManager;
import com.crovex.practice.placeholder.PracticePlaceholder;
import com.crovex.practice.queue.QueueManager;
import com.crovex.practice.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.crovex.practice.command.DynamicPracticeCommand;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class CrovexPractice extends JavaPlugin {

    private static CrovexPractice instance;

    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private QueueManager queueManager;
    private PartyManager partyManager;
    private MessageManager messageManager;
    private com.crovex.practice.queue.QueueGuiManager queueGuiManager;
    private GeneralListener generalListener;
    private com.crovex.practice.ffa.FfaManager ffaManager;
    private com.crovex.practice.webhook.WebhookManager webhookManager;
    private com.crovex.practice.duel.DuelManager duelManager;
    private com.crovex.practice.arena.BlockRestoreManager blockRestoreManager;
    private com.crovex.practice.visibility.VisibilityManager visibilityManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize MessageManager
        this.messageManager = new MessageManager(this);

        // 1. Initialize Database
        this.databaseManager = new DatabaseManager(this);
        if (!this.databaseManager.initialize()) {
            getLogger().log(Level.SEVERE, "Database connection could not be established! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Initialize Managers
        this.playerManager = new PlayerManager(this);
        this.kitManager = new KitManager(this);
        this.arenaManager = new ArenaManager(this);
        this.matchManager = new MatchManager(this);
        this.queueGuiManager = new com.crovex.practice.queue.QueueGuiManager(this);
        this.queueManager = new QueueManager(this);
        this.partyManager = new PartyManager(this);
        this.ffaManager = new com.crovex.practice.ffa.FfaManager(this);
        this.webhookManager = new com.crovex.practice.webhook.WebhookManager(this);
        this.duelManager = new com.crovex.practice.duel.DuelManager(this);
        this.blockRestoreManager = new com.crovex.practice.arena.BlockRestoreManager(this);
        this.visibilityManager = new com.crovex.practice.visibility.VisibilityManager(this);

        // Load configs
        this.kitManager.loadKits();
        this.arenaManager.loadArenas();
        this.ffaManager.loadConfig();
        this.blockRestoreManager.recoverPendingRestores();

        // 3. Register Listeners
        this.generalListener = new GeneralListener(this);
        Bukkit.getPluginManager().registerEvents(this.generalListener, this);
        Bukkit.getPluginManager().registerEvents(new MatchListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.crovex.practice.ffa.FfaListener(this), this);

        // 4. Register Commands
        registerAllCommands();

        // 5. PlaceholderAPI Integration
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PracticePlaceholder(this).register();
            getLogger().info("PlaceholderAPI integration successful!");
        }

        getLogger().info("CrovexPractice has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Stop all active matches
        if (matchManager != null) {
            matchManager.endAllMatches();
        }

        // Unregister dynamic commands
        unregisterAllCommands();

        // Save player data and close database
        if (playerManager != null) {
            playerManager.saveAllPlayersSync();
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        getLogger().info("CrovexPractice deaktif edildi.");
    }

    public static CrovexPractice getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public com.crovex.practice.queue.QueueGuiManager getQueueGuiManager() {
        return queueGuiManager;
    }

    public GeneralListener getGeneralListener() {
        return generalListener;
    }

    public com.crovex.practice.ffa.FfaManager getFfaManager() {
        return ffaManager;
    }

    public com.crovex.practice.webhook.WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public com.crovex.practice.duel.DuelManager getDuelManager() {
        return duelManager;
    }

    public com.crovex.practice.arena.BlockRestoreManager getBlockRestoreManager() {
        return blockRestoreManager;
    }

    public com.crovex.practice.visibility.VisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    private final Map<String, Map<String, String>> subcommandMappings = new HashMap<>();
    private final Map<String, List<String>> commandSubcommands = new HashMap<>();
    private final List<DynamicPracticeCommand> activeDynamicCommands = new ArrayList<>();

    public Map<String, Map<String, String>> getSubcommandMappings() {
        return subcommandMappings;
    }

    public Map<String, List<String>> getCommandSubcommands() {
        return commandSubcommands;
    }

    public void registerAllCommands() {
        // Unregister any active ones first
        unregisterAllCommands();

        subcommandMappings.clear();
        commandSubcommands.clear();

        org.bukkit.configuration.file.FileConfiguration config = getConfig();
        if (!config.contains("commands")) {
            createDefaultCommandsConfig();
            config = getConfig(); // reload configuration reference
        }

        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("commands");
        if (section == null) return;

        PracticeCommand practiceCommand = new PracticeCommand(this);

        try {
            java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(Bukkit.getServer());

            for (String internalKey : section.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection cmdSection = section.getConfigurationSection(internalKey);
                if (cmdSection == null) continue;

                String customName = cmdSection.getString("name", internalKey);
                String desc = cmdSection.getString("description", "");
                List<String> aliases = cmdSection.getStringList("aliases");
                if (aliases == null) {
                    aliases = new ArrayList<>();
                }

                // Ensure the default name is always an alias as a fallback to prevent breakages of internal/other plugin calls
                if (!customName.equalsIgnoreCase(internalKey) && !aliases.contains(internalKey)) {
                    aliases = new ArrayList<>(aliases); // make mutable
                    aliases.add(internalKey);
                }

                // Register subcommands
                Map<String, String> mappings = new HashMap<>();
                List<String> subList = new ArrayList<>();

                if (cmdSection.contains("subcommands")) {
                    org.bukkit.configuration.ConfigurationSection subSection = cmdSection.getConfigurationSection("subcommands");
                    if (subSection != null) {
                        for (String internalSub : subSection.getKeys(false)) {
                            List<String> customSubs = subSection.getStringList(internalSub);
                            if (customSubs == null || customSubs.isEmpty()) {
                                String single = subSection.getString(internalSub);
                                if (single != null && !single.isEmpty()) {
                                    customSubs = java.util.Arrays.asList(single.split(","));
                                }
                            }
                            if (customSubs != null) {
                                boolean first = true;
                                for (String customSub : customSubs) {
                                    String trimmed = customSub.trim();
                                    if (trimmed.isEmpty()) continue;
                                    mappings.put(trimmed.toLowerCase(), internalSub);
                                    if (first) {
                                        subList.add(trimmed);
                                        first = false;
                                    }
                                }
                            }
                        }
                    }
                }

                subcommandMappings.put(internalKey.toLowerCase(), mappings);
                commandSubcommands.put(internalKey.toLowerCase(), subList);

                // Create and register the dynamic command
                DynamicPracticeCommand dynamicCommand = new DynamicPracticeCommand(this, customName, desc, aliases, internalKey, practiceCommand);
                commandMap.register(getName().toLowerCase(), dynamicCommand);
                activeDynamicCommands.add(dynamicCommand);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Komutlar dinamik olarak kaydedilemedi!", e);
        }
    }

    public void unregisterAllCommands() {
        try {
            java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(Bukkit.getServer());

            java.lang.reflect.Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            for (DynamicPracticeCommand cmd : activeDynamicCommands) {
                knownCommands.remove(cmd.getName().toLowerCase());
                knownCommands.remove(getName().toLowerCase() + ":" + cmd.getName().toLowerCase());
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                    knownCommands.remove(getName().toLowerCase() + ":" + alias.toLowerCase());
                }
            }
            activeDynamicCommands.clear();
        } catch (Exception e) {
            // Ignored, safe to fail if commands weren't registered yet
        }
    }

    private void createDefaultCommandsConfig() {
        org.bukkit.configuration.file.FileConfiguration config = getConfig();
        org.bukkit.configuration.ConfigurationSection section = config.createSection("commands");

        // cpractice
        org.bukkit.configuration.ConfigurationSection cprac = section.createSection("cpractice");
        cprac.set("name", "cpractice");
        cprac.set("description", "Main admin command.");
        cprac.set("aliases", java.util.Arrays.asList("practice", "crovexpractice"));
        org.bukkit.configuration.ConfigurationSection cpracSubs = cprac.createSection("subcommands");
        cpracSubs.set("admin", java.util.Arrays.asList("admin"));
        cpracSubs.set("createarena", java.util.Arrays.asList("createarena"));
        cpracSubs.set("deletearena", java.util.Arrays.asList("deletearena"));
        cpracSubs.set("setup", java.util.Arrays.asList("setup"));
        cpracSubs.set("savebounds", java.util.Arrays.asList("savebounds"));
        cpracSubs.set("editlayout", java.util.Arrays.asList("editlayout"));
        cpracSubs.set("kits", java.util.Arrays.asList("kits"));
        cpracSubs.set("createkit", java.util.Arrays.asList("createkit"));
        cpracSubs.set("deletekit", java.util.Arrays.asList("deletekit"));
        cpracSubs.set("setinv", java.util.Arrays.asList("setinv"));
        cpracSubs.set("seticon", java.util.Arrays.asList("seticon"));
        cpracSubs.set("editinv", java.util.Arrays.asList("editinv"));
        cpracSubs.set("createffa", java.util.Arrays.asList("createffa"));
        cpracSubs.set("deleteffa", java.util.Arrays.asList("deleteffa"));
        cpracSubs.set("ffa", java.util.Arrays.asList("ffa"));
        cpracSubs.set("setupffa", java.util.Arrays.asList("setupffa"));
        cpracSubs.set("saveffabounds", java.util.Arrays.asList("saveffabounds"));
        cpracSubs.set("setlobbyspawn", java.util.Arrays.asList("setlobbyspawn"));
        cpracSubs.set("viewinv", java.util.Arrays.asList("viewinv"));
        cpracSubs.set("reload", java.util.Arrays.asList("reload"));

        // queue
        org.bukkit.configuration.ConfigurationSection queue = section.createSection("queue");
        queue.set("name", "queue");
        queue.set("description", "Join queue command.");
        queue.set("aliases", java.util.Arrays.asList("q"));

        // party
        org.bukkit.configuration.ConfigurationSection party = section.createSection("party");
        party.set("name", "party");
        party.set("description", "Party command.");
        party.set("aliases", java.util.Arrays.asList("p"));
        org.bukkit.configuration.ConfigurationSection partySubs = party.createSection("subcommands");
        partySubs.set("create", java.util.Arrays.asList("create"));
        partySubs.set("invite", java.util.Arrays.asList("invite"));
        partySubs.set("accept", java.util.Arrays.asList("accept"));
        partySubs.set("leave", java.util.Arrays.asList("leave"));
        partySubs.set("kick", java.util.Arrays.asList("kick"));
        partySubs.set("chat", java.util.Arrays.asList("chat", "c"));
        partySubs.set("ffa", java.util.Arrays.asList("ffa"));
        partySubs.set("split", java.util.Arrays.asList("split"));
        partySubs.set("challenge", java.util.Arrays.asList("challenge", "duel"));
        partySubs.set("acceptchallenge", java.util.Arrays.asList("acceptchallenge"));
        partySubs.set("declinechallenge", java.util.Arrays.asList("declinechallenge", "rejectchallenge"));

        // spectate
        org.bukkit.configuration.ConfigurationSection spectate = section.createSection("spectate");
        spectate.set("name", "spectate");
        spectate.set("description", "Spectate matches.");
        spectate.set("aliases", java.util.Arrays.asList("spec"));

        // stats
        org.bukkit.configuration.ConfigurationSection stats = section.createSection("stats");
        stats.set("name", "stats");
        stats.set("description", "View stats.");
        stats.set("aliases", java.util.Arrays.asList("stat"));

        // ffa
        org.bukkit.configuration.ConfigurationSection ffa = section.createSection("ffa");
        ffa.set("name", "ffa");
        ffa.set("description", "Join FFA.");
        ffa.set("aliases", java.util.Arrays.asList("joinffa", "ffapvp"));

        // leaveffa
        org.bukkit.configuration.ConfigurationSection leaveffa = section.createSection("leaveffa");
        leaveffa.set("name", "leaveffa");
        leaveffa.set("description", "Leave FFA.");
        leaveffa.set("aliases", java.util.Arrays.asList("quitffa", "ffaleave"));

        // leaderboard
        org.bukkit.configuration.ConfigurationSection leaderboard = section.createSection("leaderboard");
        leaderboard.set("name", "leaderboard");
        leaderboard.set("description", "View leaderboards.");
        leaderboard.set("aliases", java.util.Arrays.asList("top", "lb", "siralamalar"));

        // duel
        org.bukkit.configuration.ConfigurationSection duel = section.createSection("duel");
        duel.set("name", "duel");
        duel.set("description", "Challenge players.");
        duel.set("aliases", java.util.Arrays.asList("d", "challenge", "vs"));
        org.bukkit.configuration.ConfigurationSection duelSubs = duel.createSection("subcommands");
        duelSubs.set("accept", java.util.Arrays.asList("accept"));
        duelSubs.set("decline", java.util.Arrays.asList("decline", "reject"));

        saveConfig();
    }
}
