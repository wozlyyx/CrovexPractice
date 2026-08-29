package com.crovex.practice.command;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.arena.ArenaSetupSession;
import com.crovex.practice.arena.Cuboid;
import com.crovex.practice.gui.AdminArenaMenu;
import com.crovex.practice.gui.KitEditMenu;
import com.crovex.practice.gui.LeaderboardMenu;
import com.crovex.practice.gui.PostMatchInventoryGUI;
import com.crovex.practice.gui.QueueMenu;
import com.crovex.practice.gui.PartyMenu;
import com.crovex.practice.gui.PartyKitSelectMenu;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.match.Match;
import com.crovex.practice.match.MatchState;
import com.crovex.practice.party.Party;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PracticeCommand implements CommandExecutor, TabCompleter {

    private final CrovexPractice plugin;

    public PracticeCommand(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return handleInternalCommand(sender, command.getName().toLowerCase(), args);
    }

    public boolean handleInternalCommand(CommandSender sender, String internalKey, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getRawMessage("general.only-players"));
            return true;
        }

        Player player = (Player) sender;

        switch (internalKey.toLowerCase()) {
            case "cpractice":
                return handleAdminCommand(player, args);
            case "queue":
                return handleQueueCommand(player, args);
            case "party":
                return handlePartyCommand(player, args);
            case "spectate":
                return handleSpectateCommand(player, args);
            case "stats":
                return handleStatsCommand(player, args);
            case "duel":
                return handleDuelCommand(player, args);
            case "ffa":
                if (args.length > 0) {
                    plugin.getFfaManager().joinFfa(player, args[0]);
                } else {
                    java.util.List<com.crovex.practice.ffa.FfaArena> activeArenas = new java.util.ArrayList<>();
                    for (com.crovex.practice.ffa.FfaArena ffa : plugin.getFfaManager().getFfaArenas()) {
                        if (ffa.isEnabled() && ffa.getSpawnLocation() != null && ffa.getBounds() != null) {
                            activeArenas.add(ffa);
                        }
                    }
                    if (activeArenas.size() == 1) {
                        plugin.getFfaManager().joinFfa(player, activeArenas.get(0).getName());
                    } else {
                        new com.crovex.practice.gui.FfaSelectorMenu(plugin).open(player);
                    }
                }
                return true;
            case "leaveffa":
                plugin.getFfaManager().leaveFfa(player, true);
                return true;
            case "leaderboard":
                return handleLeaderboardCommand(player);
        }

        return true;
    }

    // 1. Admin Commands (/cpractice)
    private boolean handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("crovexpractice.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            for (Component line : plugin.getMessageManager().getMessageList("commands.admin-help")) {
                player.sendMessage(line);
            }
            return true;
        }

        String sub = getInternalSubcommand("cpractice", args[0]);
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.getKitManager().loadKits();
            plugin.getArenaManager().loadArenas();
            plugin.getFfaManager().loadConfig();
            plugin.getMessageManager().reloadMessages();
            plugin.registerAllCommands();
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Configuration and commands have been successfully reloaded!"));
            return true;
        }

        if (sub.equals("lang") || sub.equals("language")) {
            if (args.length < 2) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Kullanım / Usage: /cpractice lang <tr|en|es|fr> (Mevcut / Current: <yellow>" + plugin.getMessageManager().getLanguage() + "<red>)"));
                return true;
            }
            String targetLang = args[1].toLowerCase();
            if (!targetLang.equals("tr") && !targetLang.equals("en") && !targetLang.equals("es") && !targetLang.equals("fr")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Geçersiz dil / Invalid language! Seçenekler / Options: <yellow>tr, en, es, fr"));
                return true;
            }
            plugin.getMessageManager().setLanguage(targetLang);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Dil başarıyla değiştirildi / Language successfully set to: <yellow>" + targetLang.toUpperCase()));
            return true;
        }

        if (sub.equals("admin")) {
            new AdminArenaMenu(plugin).open(player);
            return true;
        }

        if (sub.equals("createarena")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createarena.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getArenaManager().getArena(name) != null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createarena.exists"));
                return true;
            }
            plugin.getArenaManager().createArena(name);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.createarena.success", "%arena%", name));
            plugin.getWebhookManager().sendAdminActionWebhook(player.getName(), "New arena created: **" + name + "**");
            return true;
        }

        if (sub.equals("deletearena")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deletearena.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getArenaManager().getArena(name) == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deletearena.not-found"));
                return true;
            }
            plugin.getArenaManager().deleteArena(name);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.deletearena.success", "%arena%", name));
            plugin.getWebhookManager().sendAdminActionWebhook(player.getName(), "Arena silindi: **" + name + "**");
            return true;
        }

        if (sub.equals("setup")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.setup.usage"));
                return true;
            }
            String arenaName = args[1];
            if (plugin.getArenaManager().getArena(arenaName) == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.setup.not-found"));
                return true;
            }

            plugin.getArenaManager().startSetupSession(player.getUniqueId(), arenaName);
            
            ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            meta.displayName(plugin.getMessageManager().getMessage("setup.wand-name"));
            meta.lore(plugin.getMessageManager().getMessageList("setup.wand-lore"));
            wand.setItemMeta(meta);

            player.getInventory().addItem(wand);
            player.sendMessage(plugin.getMessageManager().getMessage("setup.wand-give", "%arena%", arenaName));
            return true;
        }

        if (sub.equals("savebounds")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.savebounds.usage"));
                return true;
            }
            String arenaName = args[1];
            ArenaSetupSession session = plugin.getArenaManager().getSetupSession(player.getUniqueId());

            if (session == null || !session.getArenaName().equalsIgnoreCase(arenaName)) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.savebounds.no-session"));
                return true;
            }

            if (!session.isComplete()) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.savebounds.incomplete"));
                return true;
            }

            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.savebounds.not-found"));
                return true;
            }

            Cuboid bounds = new Cuboid(session.getPos1(), session.getPos2());
            arena.setBounds(bounds);
            plugin.getArenaManager().saveArenas();
            plugin.getArenaManager().removeSetupSession(player.getUniqueId());

            // Remove the golden axe wand from inventory
            player.getInventory().remove(Material.GOLDEN_AXE);
            player.updateInventory();

            player.sendMessage(plugin.getMessageManager().getMessage("commands.savebounds.success", "%arena%", arenaName));
            // Re-open the arena editor menu for easy continuation
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                new com.crovex.practice.gui.ArenaEditorMenu(plugin, arena).open(player), 1L);
            return true;
        }

        if (sub.equals("editlayout")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.editlayout.usage"));
                return true;
            }
            String typeStr = args[1].toLowerCase();
            if (!typeStr.equals("unranked") && !typeStr.equals("ranked")) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.editlayout.usage"));
                return true;
            }
            QueueType qType = typeStr.equals("ranked") ? QueueType.RANKED : QueueType.UNRANKED;
            new com.crovex.practice.gui.QueueLayoutEditor(plugin, qType).open(player);
            return true;
        }

        if (sub.equals("kits")) {
            new com.crovex.practice.gui.AdminKitMenu(plugin).open(player);
            return true;
        }

        if (sub.equals("createkit")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createkit.usage"));
                return true;
            }
            String name = args[1];
            String typeStr = args[2].toUpperCase();
            com.crovex.practice.kit.KitType type;
            try {
                type = com.crovex.practice.kit.KitType.valueOf(typeStr);
            } catch (Exception e) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createkit.invalid-type"));
                return true;
            }
            if (plugin.getKitManager().getKit(name) != null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createkit.exists"));
                return true;
            }
            plugin.getKitManager().createKit(name, type);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.createkit.success", "%kit%", name));
            plugin.getWebhookManager().sendAdminActionWebhook(player.getName(), "New kit created: **" + name + "** (Type: " + type.name() + ")");
            return true;
        }

        if (sub.equals("deletekit")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deletekit.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getKitManager().getKit(name) == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deletekit.not-found"));
                return true;
            }
            plugin.getKitManager().deleteKit(name);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.deletekit.success", "%kit%", name));
            plugin.getWebhookManager().sendAdminActionWebhook(player.getName(), "Kit silindi: **" + name + "**");
            return true;
        }

        if (sub.equals("setinv")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.setinv.usage"));
                return true;
            }
            String name = args[1];
            com.crovex.practice.kit.Kit kit = plugin.getKitManager().getKit(name);
            if (kit == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.setinv.not-found"));
                return true;
            }
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] invContents = new ItemStack[36];
            System.arraycopy(contents, 0, invContents, 0, 36);
            ItemStack[] armorContents = player.getInventory().getArmorContents();
            kit.setInventoryContents(invContents);
            kit.setArmorContents(armorContents);
            plugin.getKitManager().saveKits();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-inv-set", "%kit%", kit.getDisplayName()));
            return true;
        }

        if (sub.equals("seticon")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.seticon.usage"));
                return true;
            }
            String name = args[1];
            com.crovex.practice.kit.Kit kit = plugin.getKitManager().getKit(name);
            if (kit == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.seticon.not-found"));
                return true;
            }
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-hold-item"));
                return true;
            }
            ItemStack newIcon = inHand.clone();
            newIcon.setAmount(1);
            kit.setIcon(newIcon);
            plugin.getKitManager().saveKits();
            player.sendMessage(plugin.getMessageManager().getMessage("gui.admin.kit-editor.msg-icon-set", "%kit%", kit.getDisplayName()));
            return true;
        }

        if (sub.equals("editinv")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.editinv.usage"));
                return true;
            }
            String name = args[1];
            com.crovex.practice.kit.Kit kit = plugin.getKitManager().getKit(name);
            if (kit == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.editinv.not-found"));
                return true;
            }
            new com.crovex.practice.gui.KitInventoryEditorMenu(plugin, kit).open(player);
            return true;
        }

        if (sub.equals("createffa")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createffa.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getFfaManager().getFfaArena(name) != null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.createffa.exists"));
                return true;
            }
            plugin.getFfaManager().createFfaArena(name);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.createffa.success", "%arena%", name));
            return true;
        }

        if (sub.equals("deleteffa")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deleteffa.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getFfaManager().getFfaArena(name) == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deleteffa.not-found"));
                return true;
            }
            plugin.getFfaManager().deleteFfaArena(name);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.deleteffa.success", "%arena%", name));
            return true;
        }

        if (sub.equals("ffa")) {
            if (args.length >= 2) {
                String name = args[1];
                com.crovex.practice.ffa.FfaArena ffa = plugin.getFfaManager().getFfaArena(name);
                if (ffa != null) {
                    new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player);
                    return true;
                }
            }
            new com.crovex.practice.gui.AdminFfaMenu(plugin).open(player);
            return true;
        }

        if (sub.equals("setupffa")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.setupffa.usage"));
                return true;
            }
            String name = args[1];
            if (plugin.getFfaManager().getFfaArena(name) == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deleteffa.not-found"));
                return true;
            }
            plugin.getFfaManager().startSetupSession(player.getUniqueId(), name);
            
            ItemStack wand = new ItemStack(Material.IRON_AXE);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.getMessageManager().getMessage("ffa.setup.wand-name"));
                meta.lore(plugin.getMessageManager().getMessageList("ffa.setup.wand-lore"));
                wand.setItemMeta(meta);
            }

            player.getInventory().addItem(wand);
            player.sendMessage(plugin.getMessageManager().getMessage("ffa.setup.wand-give"));
            return true;
        }

        if (sub.equals("saveffabounds")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.saveffabounds.usage"));
                return true;
            }
            String name = args[1];
            com.crovex.practice.ffa.FfaSetupSession session = plugin.getFfaManager().getSetupSession(player.getUniqueId());
            if (session == null || !session.getArenaName().equalsIgnoreCase(name)) {
                player.sendMessage(plugin.getMessageManager().getMessage("ffa.setup.no-session"));
                return true;
            }
            if (!session.isComplete()) {
                player.sendMessage(plugin.getMessageManager().getMessage("ffa.setup.incomplete"));
                return true;
            }

            com.crovex.practice.ffa.FfaArena ffa = plugin.getFfaManager().getFfaArena(name);
            if (ffa == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.deleteffa.not-found"));
                return true;
            }

            Cuboid bounds = new Cuboid(session.getPos1(), session.getPos2());
            ffa.setBounds(bounds);
            plugin.getFfaManager().saveConfig();
            plugin.getFfaManager().removeSetupSession(player.getUniqueId());

            // Remove the iron axe wand from inventory
            player.getInventory().remove(Material.IRON_AXE);
            player.updateInventory();

            player.sendMessage(plugin.getMessageManager().getMessage("ffa.setup.success"));
            // Re-open the FFA editor menu for easy continuation
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                new com.crovex.practice.gui.FfaEditorMenu(plugin, ffa).open(player), 1L);
            return true;
        }

        if (sub.equals("setlobbyspawn")) {
            plugin.getConfig().set("lobby-spawn", player.getLocation());
            plugin.saveConfig();
            player.sendMessage(plugin.getMessageManager().getMessage("commands.setlobbyspawn.success"));
            return true;
        }

        if (sub.equals("resetitems")) {
            if (!player.hasPermission("crovexpractice.admin")) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.no-permission"));
                return true;
            }
            plugin.getPlayerManager().giveLobbyItems(player);
            player.sendMessage(plugin.getMessageManager().getMessage("commands.resetitems.success"));
            return true;
        }

        // Invisible command for inventory view links click
        if (sub.equals("viewinv")) {
            if (args.length < 3) return true;
            try {
                UUID matchId = UUID.fromString(args[1]);
                UUID targetUuid = UUID.fromString(args[2]);

                Match match = plugin.getMatchManager().getMatch(matchId);
                if (match == null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("match.ended.inventories-expired"));
                    return true;
                }

                String name = match.getInitialPlayers().contains(targetUuid) ? match.getPostInventories().containsKey(targetUuid) ? Bukkit.getOfflinePlayer(targetUuid).getName() : "" : "";
                ItemStack[] contents = match.getPostInventories().get(targetUuid);
                ItemStack[] armor = match.getPostArmor().get(targetUuid);
                double health = match.getPostHealth().getOrDefault(targetUuid, 20.0);
                int food = match.getPostFood().getOrDefault(targetUuid, 20);
                int hits = match.getHits(targetUuid);

                new PostMatchInventoryGUI(name, contents, armor, health, food, hits).open(player);
            } catch (Exception e) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>An error occurred, inventory could not be loaded."));
            }
            return true;
        }

        return true;
    }

    // 6. Leaderboard Command (/leaderboard)
    private boolean handleLeaderboardCommand(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("gui.leaderboard.loading"));
        plugin.getDatabaseManager()
                .getTopPlayers("elo", 10)
                .thenAccept(topPlayers -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    new LeaderboardMenu(plugin, LeaderboardMenu.LeaderboardCategory.ELO, topPlayers).open(player);
                }));
        return true;
    }

    // 2. Queue Command (/queue)
    private boolean handleQueueCommand(Player player, String[] args) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getState() != PlayerState.LOBBY) {
            player.sendMessage(plugin.getMessageManager().getMessage("queue.cannot-join"));
            return true;
        }
        
        QueueType type = QueueType.UNRANKED;
        if (args.length > 0) {
            String typeStr = args[0].toLowerCase();
            if (typeStr.startsWith("rank") || typeStr.equals("r")) {
                type = QueueType.RANKED;
            }
        }

        if (type == QueueType.UNRANKED) {
            if (!plugin.getConfig().getBoolean("queue.unranked-enabled", true)) {
                player.sendMessage(plugin.getMessageManager().getMessage("queue.unranked-disabled"));
                return true;
            }
        } else {
            if (!plugin.getConfig().getBoolean("queue.ranked-enabled", true)) {
                player.sendMessage(plugin.getMessageManager().getMessage("queue.ranked-disabled"));
                return true;
            }
        }

        new QueueMenu(plugin, type).open(player);
        return true;
    }

    // 3. Party Command (/party)
    private boolean handlePartyCommand(Player player, String[] args) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return true;

        if (args.length == 0) {
            if (pp.getActiveParty() != null) {
                // If already in a party, open the GUI!
                new PartyMenu(plugin).open(player);
            } else {
                // Show commands help
                for (Component line : plugin.getMessageManager().getMessageList("commands.party-help")) {
                    player.sendMessage(line);
                }
            }
            return true;
        }

        String sub = getInternalSubcommand("party", args[0]);
        if (sub.equals("create")) {
            plugin.getPartyManager().createParty(player);
            return true;
        }
        
        if (sub.equals("invite")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-invite.usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            plugin.getPartyManager().invitePlayer(player, target);
            return true;
        }

        if (sub.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-accept.usage"));
                return true;
            }
            plugin.getPartyManager().acceptInvite(player, args[1]);
            return true;
        }

        if (sub.equals("leave")) {
            plugin.getPartyManager().leaveParty(player);
            return true;
        }

        if (sub.equals("kick")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-kick.usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            plugin.getPartyManager().kickPlayer(player, target);
            return true;
        }

        if (sub.equals("chat") || sub.equals("c")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-chat.usage"));
                return true;
            }
            if (pp.getActiveParty() == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String chatFormat = plugin.getMessageManager().getRawMessage("party.chat-format")
                    .replace("%player%", player.getName())
                    .replace("%message%", msg);
            
            plugin.getPartyManager().sendMessageToParty(pp.getActiveParty(), chatFormat);
            return true;
        }

        // New Party Subcommands: ffa, split, challenge, acceptchallenge
        if (sub.equals("ffa")) {
            Party party = pp.getActiveParty();
            if (party == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return true;
            }
            new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.FFA, null).open(player);
            return true;
        }

        if (sub.equals("split")) {
            Party party = pp.getActiveParty();
            if (party == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return true;
            }
            new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.SPLIT, null).open(player);
            return true;
        }

        if (sub.equals("challenge") || sub.equals("duel")) {
            Party party = pp.getActiveParty();
            if (party == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-challenge.usage"));
                return true;
            }
            Player targetLeader = Bukkit.getPlayer(args[1]);
            if (targetLeader == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            
            Party targetParty = plugin.getPartyManager().getParty(targetLeader);
            if (targetParty == null || !targetParty.isLeader(targetLeader.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.challenge-not-leader"));
                return true;
            }

            if (targetLeader.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.challenge-self"));
                return true;
            }

            new PartyKitSelectMenu(plugin, PartyKitSelectMenu.PartyAction.CHALLENGE, targetLeader).open(player);
            return true;
        }

        if (sub.equals("acceptchallenge")) {
            Party party = pp.getActiveParty();
            if (party == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-acceptchallenge.usage"));
                return true;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            
            plugin.getPartyManager().acceptChallenge(player, challenger);
            return true;
        }

        if (sub.equals("declinechallenge") || sub.equals("rejectchallenge")) {
            Party party = pp.getActiveParty();
            if (party == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
                return true;
            }
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("commands.party-declinechallenge.usage"));
                return true;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            
            plugin.getPartyManager().declineChallenge(player, challenger);
            return true;
        }

        return true;
    }

    // 4. Spectate Command (/spectate)
    private boolean handleSpectateCommand(Player player, String[] args) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return true;

        if (pp.getState() != PlayerState.LOBBY) {
            player.sendMessage(plugin.getMessageManager().getMessage("match.spectating.only-lobby"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getMessage("commands.spectate.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
            return true;
        }

        Match targetMatch = plugin.getMatchManager().getMatch(target);
        if (targetMatch == null || targetMatch.getState() != MatchState.ACTIVE) {
            player.sendMessage(plugin.getMessageManager().getMessage("match.spectating.not-active"));
            return true;
        }

        targetMatch.addSpectator(player);
        return true;
    }

    // 5. Stats Command (/stats)
    private boolean handleStatsCommand(Player player, String[] args) {
        Player target = player;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
        }

        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-not-found"));
            return true;
        }

        PracticePlayer pp = plugin.getPlayerManager().getPlayer(target);
        if (pp == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.stats-not-found"));
            return true;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.header"));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.title", "%player%", target.getName()));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.elo", "%elo%", String.valueOf(pp.getElo())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.ranked-wins", "%wins%", String.valueOf(pp.getRankedWins())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.ranked-losses", "%losses%", String.valueOf(pp.getRankedLosses())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.unranked-wins", "%wins%", String.valueOf(pp.getUnrankedWins())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.unranked-losses", "%losses%", String.valueOf(pp.getUnrankedLosses())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.winstreak", "%winstreak%", String.valueOf(pp.getWinstreak())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.best-winstreak", "%best%", String.valueOf(pp.getBestWinstreak())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.ffa-kills", "%kills%", String.valueOf(pp.getFfaKills())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.ffa-deaths", "%deaths%", String.valueOf(pp.getFfaDeaths())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.ffa-best-streak", "%streak%", String.valueOf(pp.getFfaBestStreak())));
        player.sendMessage(plugin.getMessageManager().getMessage("commands.stats.footer"));

        return true;
    }

    // 6. Duel Command (/duel)
    private boolean handleDuelCommand(Player player, String[] args) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return true;

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("duel.usage"));
            return true;
        }

        String sub = getInternalSubcommand("duel", args[0]);
        if (sub.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("duel.accept-usage"));
                return true;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            plugin.getDuelManager().acceptChallenge(player, challenger);
            return true;
        }

        if (sub.equals("decline") || sub.equals("reject")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("duel.decline-usage"));
                return true;
            }
            Player challenger = Bukkit.getPlayer(args[1]);
            if (challenger == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
                return true;
            }
            plugin.getDuelManager().declineChallenge(player, challenger);
            return true;
        }

        // Otherwise, args[0] is target player name we want to challenge
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessage("duel.self-challenge"));
            return true;
        }

        // Verify target's state
        PracticePlayer ppTarget = plugin.getPlayerManager().getPlayer(target);
        if (ppTarget == null || ppTarget.getState() != PlayerState.LOBBY) {
            player.sendMessage(plugin.getMessageManager().getMessage("duel.already-in-match"));
            return true;
        }

        // Open Kit Selection Menu
        new com.crovex.practice.gui.DuelKitSelectMenu(plugin, target).open(player);
        return true;
    }

    public String getInternalSubcommand(String commandKey, String customSub) {
        if (customSub == null || customSub.isEmpty()) return "";
        java.util.Map<String, String> mappings = plugin.getSubcommandMappings().get(commandKey.toLowerCase());
        if (mappings != null) {
            String internal = mappings.get(customSub.toLowerCase());
            if (internal != null) {
                return internal;
            }
        }
        return customSub.toLowerCase();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return handleInternalTabComplete(sender, command.getName().toLowerCase(), args);
    }

    public List<String> handleInternalTabComplete(CommandSender sender, String internalKey, String[] args) {
        List<String> list = new ArrayList<>();

        if (internalKey.equals("cpractice") && sender.hasPermission("crovexpractice.admin")) {
            String resolvedSub = getInternalSubcommand("cpractice", args[0]);
            if (args.length == 1) {
                return plugin.getCommandSubcommands().getOrDefault("cpractice", java.util.Collections.emptyList()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("deletearena") || resolvedSub.equalsIgnoreCase("setup") || resolvedSub.equalsIgnoreCase("savebounds"))) {
                return plugin.getArenaManager().getArenas().stream()
                        .map(Arena::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("deleteffa") || resolvedSub.equalsIgnoreCase("setupffa") || resolvedSub.equalsIgnoreCase("saveffabounds") || resolvedSub.equalsIgnoreCase("ffa"))) {
                return plugin.getFfaManager().getFfaArenas().stream()
                        .map(com.crovex.practice.ffa.FfaArena::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("deletekit") || resolvedSub.equalsIgnoreCase("setinv") || resolvedSub.equalsIgnoreCase("seticon") || resolvedSub.equalsIgnoreCase("editinv"))) {
                return plugin.getKitManager().getKits().stream()
                        .map(com.crovex.practice.kit.Kit::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("lang") || resolvedSub.equalsIgnoreCase("language"))) {
                return Arrays.asList("tr", "en", "es", "fr").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && resolvedSub.equalsIgnoreCase("editlayout")) {
                return Arrays.asList("unranked", "ranked").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3 && resolvedSub.equalsIgnoreCase("createkit")) {
                return Arrays.stream(com.crovex.practice.kit.KitType.values())
                        .map(Enum::name)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (internalKey.equals("party")) {
            String resolvedSub = getInternalSubcommand("party", args[0]);
            if (args.length == 1) {
                return plugin.getCommandSubcommands().getOrDefault("party", java.util.Collections.emptyList()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("invite") || resolvedSub.equalsIgnoreCase("kick") || resolvedSub.equalsIgnoreCase("accept") || resolvedSub.equalsIgnoreCase("challenge") || resolvedSub.equalsIgnoreCase("acceptchallenge"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (internalKey.equals("ffa")) {
            if (args.length == 1) {
                return plugin.getFfaManager().getFfaArenas().stream()
                        .filter(com.crovex.practice.ffa.FfaArena::isEnabled)
                        .map(com.crovex.practice.ffa.FfaArena::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (internalKey.equals("spectate")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (internalKey.equals("duel")) {
            String resolvedSub = getInternalSubcommand("duel", args[0]);
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>(plugin.getCommandSubcommands().getOrDefault("duel", java.util.Collections.emptyList()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(sender)) {
                        suggestions.add(p.getName());
                    }
                }
                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && (resolvedSub.equalsIgnoreCase("accept") || resolvedSub.equalsIgnoreCase("decline") || resolvedSub.equalsIgnoreCase("reject"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> !s.equalsIgnoreCase(sender.getName()))
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return list;
    }
}
