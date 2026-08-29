package com.crovex.practice.party;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class PartyManager {

    private final CrovexPractice plugin;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> invites = new HashMap<>(); // Invitee UUID -> Party Leader UUID
    private final Map<UUID, List<PartyChallenge>> activeChallenges = new HashMap<>(); // Challenged Leader UUID -> List of Challenges

    public PartyManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void createParty(Player player) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null) return;

        if (pp.getActiveParty() != null) {
            player.sendMessage(plugin.getMessageManager().getMessage("party.already-in-party"));
            return;
        }

        Party party = new Party(player.getUniqueId());
        parties.put(player.getUniqueId(), party);
        pp.setActiveParty(party);

        player.sendMessage(plugin.getMessageManager().getMessage("party.created"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    public void disbandParty(Party party) {
        parties.remove(party.getLeader());

        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) {
                PracticePlayer pp = plugin.getPlayerManager().getPlayer(p);
                if (pp != null) {
                    pp.setActiveParty(null);
                }
                p.sendMessage(plugin.getMessageManager().getMessage("party.disbanded"));
                plugin.getPlayerManager().giveLobbyItems(p);
            }
        }
    }

    public void invitePlayer(Player inviter, Player target) {
        PracticePlayer ppInviter = plugin.getPlayerManager().getPlayer(inviter);
        if (ppInviter == null || ppInviter.getActiveParty() == null) {
            inviter.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
            return;
        }

        Party party = ppInviter.getActiveParty();
        if (!party.isLeader(inviter.getUniqueId())) {
            inviter.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
            return;
        }

        PracticePlayer ppTarget = plugin.getPlayerManager().getPlayer(target);
        if (ppTarget == null || ppTarget.getActiveParty() != null) {
            inviter.sendMessage(plugin.getMessageManager().getMessage("party.target-already-in-party"));
            return;
        }

        invites.put(target.getUniqueId(), inviter.getUniqueId());
        inviter.sendMessage(plugin.getMessageManager().getMessage("party.invite-sent", "%player%", target.getName()));

        // Clickable accept text
        Component acceptComponent = plugin.getMessageManager().getMessage("party.invite-accept-btn")
                .clickEvent(ClickEvent.runCommand("/party accept " + inviter.getName()))
                .hoverEvent(HoverEvent.showText(plugin.getMessageManager().getMessage("party.invite-accept-hover")));

        target.sendMessage(plugin.getMessageManager().getMessage("party.invite-received", "%player%", inviter.getName()).append(acceptComponent));
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public void acceptInvite(Player player, String leaderName) {
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("general.player-offline"));
            return;
        }

        UUID leaderUuid = invites.get(player.getUniqueId());
        if (leaderUuid == null || !leaderUuid.equals(leader.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getMessage("party.invite-expired"));
            return;
        }

        Party party = parties.get(leaderUuid);
        if (party == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("party.not-exist"));
            invites.remove(player.getUniqueId());
            return;
        }

        invites.remove(player.getUniqueId());
        party.addMember(player.getUniqueId());
        
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp != null) {
            pp.setActiveParty(party);
        }

        player.sendMessage(plugin.getMessageManager().getMessage("party.joined"));
        sendMessageToParty(party, plugin.getMessageManager().getRawMessage("party.join-broadcast").replace("%player%", player.getName()));
        
        // Update lobby items
        plugin.getPlayerManager().giveLobbyItems(player);
    }

    public void leaveParty(Player player) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        if (pp == null || pp.getActiveParty() == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
            return;
        }

        Party party = pp.getActiveParty();
        pp.setActiveParty(null);

        if (party.isLeader(player.getUniqueId())) {
            // Disband
            disbandParty(party);
        } else {
            party.removeMember(player.getUniqueId());
            sendMessageToParty(party, plugin.getMessageManager().getRawMessage("party.leave-broadcast").replace("%player%", player.getName()));
            player.sendMessage(plugin.getMessageManager().getMessage("party.left"));
            plugin.getPlayerManager().giveLobbyItems(player);
        }
    }

    public void kickPlayer(Player leader, Player target) {
        PracticePlayer ppLeader = plugin.getPlayerManager().getPlayer(leader);
        if (ppLeader == null || ppLeader.getActiveParty() == null) {
            leader.sendMessage(plugin.getMessageManager().getMessage("party.not-in-party"));
            return;
        }

        Party party = ppLeader.getActiveParty();
        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(plugin.getMessageManager().getMessage("party.not-leader"));
            return;
        }

        if (!party.getMembers().contains(target.getUniqueId())) {
            leader.sendMessage(plugin.getMessageManager().getMessage("party.target-not-in-party"));
            return;
        }

        if (leader.getUniqueId().equals(target.getUniqueId())) {
            leader.sendMessage(plugin.getMessageManager().getMessage("party.kick-self-error"));
            return;
        }

        party.removeMember(target.getUniqueId());
        
        PracticePlayer ppTarget = plugin.getPlayerManager().getPlayer(target);
        if (ppTarget != null) {
            ppTarget.setActiveParty(null);
        }

        target.sendMessage(plugin.getMessageManager().getMessage("party.kicked"));
        sendMessageToParty(party, plugin.getMessageManager().getRawMessage("party.kick-broadcast").replace("%player%", target.getName()));
        plugin.getPlayerManager().giveLobbyItems(target);
    }

    public void sendMessageToParty(Party party, String message) {
        String prefix = plugin.getMessageManager().getRawMessage("party.prefix");
        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) {
                p.sendMessage(MiniMessage.miniMessage().deserialize(prefix + message));
            }
        }
    }

    public Party getParty(Player player) {
        PracticePlayer pp = plugin.getPlayerManager().getPlayer(player);
        return pp != null ? pp.getActiveParty() : null;
    }

    public Party getParty(UUID leaderUuid) {
        return parties.get(leaderUuid);
    }

    public Collection<Party> getParties() {
        return parties.values();
    }

    // Party FFA Execution
    public void startFFA(Party party, Kit kit) {
        if (party.getSize() < 2) {
            Player leader = Bukkit.getPlayer(party.getLeader());
            if (leader != null) {
                leader.sendMessage(plugin.getMessageManager().getMessage("party.insufficient-players", "%min%", "2"));
            }
            return;
        }

        Arena arena = plugin.getArenaManager().findAvailableArena(kit);
        if (arena == null) {
            notifyParty(party, plugin.getMessageManager().getMessage("queue.waiting-arena"));
            return;
        }

        List<Player> players = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
        }

        plugin.getMatchManager().startPartyFFAMatch(players, kit, arena);
    }

    // Party Split Execution
    public void startSplit(Party party, Kit kit) {
        if (party.getSize() < 2) {
            Player leader = Bukkit.getPlayer(party.getLeader());
            if (leader != null) {
                leader.sendMessage(plugin.getMessageManager().getMessage("party.insufficient-players", "%min%", "2"));
            }
            return;
        }

        Arena arena = plugin.getArenaManager().findAvailableArena(kit);
        if (arena == null) {
            notifyParty(party, plugin.getMessageManager().getMessage("queue.waiting-arena"));
            return;
        }

        List<Player> members = new ArrayList<>();
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                members.add(p);
            }
        }

        Collections.shuffle(members);

        List<Player> teamA = new ArrayList<>();
        List<Player> teamB = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            if (i % 2 == 0) {
                teamA.add(members.get(i));
            } else {
                teamB.add(members.get(i));
            }
        }

        plugin.getMatchManager().startPartySplitMatch(teamA, teamB, kit, arena);
    }

    // Party Duel Challenge logic
    public void sendChallenge(Player challengerLeader, Player challengedLeader, Kit kit) {
        Party challengerParty = getParty(challengerLeader);
        Party challengedParty = getParty(challengedLeader);

        if (challengerParty == null || challengedParty == null) return;

        PartyChallenge challenge = new PartyChallenge(challengerLeader.getUniqueId(), challengedLeader.getUniqueId(), kit);
        
        List<PartyChallenge> list = activeChallenges.computeIfAbsent(challengedLeader.getUniqueId(), k -> new ArrayList<>());
        // Remove any old challenge from the same challenger to prevent duplicates
        list.removeIf(c -> c.getChallengerLeader().equals(challengerLeader.getUniqueId()));
        list.add(challenge);

        challengerLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-sent", "%target%", challengedLeader.getName(), "%kit%", kit.getDisplayName()));

        Component acceptComponent = plugin.getMessageManager().getMessage("party.challenge-accept-btn")
                .clickEvent(ClickEvent.runCommand("/party acceptchallenge " + challengerLeader.getName()))
                .hoverEvent(HoverEvent.showText(plugin.getMessageManager().getMessage("party.challenge-accept-hover")));

        challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-received", 
                "%challenger%", challengerLeader.getName(), 
                "%kit%", kit.getDisplayName())
                .append(Component.text(" "))
                .append(acceptComponent));
        challengedLeader.playSound(challengedLeader.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public void acceptChallenge(Player challengedLeader, Player challengerLeader) {
        List<PartyChallenge> challenges = activeChallenges.get(challengedLeader.getUniqueId());
        PartyChallenge challenge = null;
        if (challenges != null) {
            for (PartyChallenge c : challenges) {
                if (c.getChallengerLeader().equals(challengerLeader.getUniqueId())) {
                    challenge = c;
                    break;
                }
            }
        }

        if (challenge == null || challenge.isExpired()) {
            if (challenges != null && challenge != null) {
                challenges.remove(challenge);
            }
            challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-expired"));
            return;
        }

        challenges.remove(challenge);

        Party challengerParty = parties.get(challenge.getChallengerLeader());
        Party challengedParty = parties.get(challenge.getChallengedLeader());

        if (challengerParty == null || challengedParty == null) {
            challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-expired"));
            return;
        }

        // Verify players are online and not in match
        List<Player> teamA = new ArrayList<>();
        for (UUID uuid : challengerParty.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                PracticePlayer pp = plugin.getPlayerManager().getPlayer(p);
                if (pp != null && pp.getState() != com.crovex.practice.player.PlayerState.LOBBY) {
                    challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-already-in-match"));
                    return;
                }
                teamA.add(p);
            }
        }

        List<Player> teamB = new ArrayList<>();
        for (UUID uuid : challengedParty.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                PracticePlayer pp = plugin.getPlayerManager().getPlayer(p);
                if (pp != null && pp.getState() != com.crovex.practice.player.PlayerState.LOBBY) {
                    challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-already-in-match"));
                    return;
                }
                teamB.add(p);
            }
        }

        Arena arena = plugin.getArenaManager().findAvailableArena(challenge.getKit());
        if (arena == null) {
            Component waitMsg = plugin.getMessageManager().getMessage("queue.waiting-arena");
            challengedLeader.sendMessage(waitMsg);
            challengerLeader.sendMessage(waitMsg);
            return;
        }

        plugin.getMatchManager().startPartyVSMatch(teamA, teamB, challenge.getKit(), arena);
    }

    public void declineChallenge(Player challengedLeader, Player challengerLeader) {
        List<PartyChallenge> challenges = activeChallenges.get(challengedLeader.getUniqueId());
        PartyChallenge challenge = null;
        if (challenges != null) {
            for (PartyChallenge c : challenges) {
                if (c.getChallengerLeader().equals(challengerLeader.getUniqueId())) {
                    challenge = c;
                    break;
                }
            }
        }

        if (challenge == null || challenge.isExpired()) {
            if (challenges != null && challenge != null) {
                challenges.remove(challenge);
            }
            challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-expired"));
            return;
        }

        challenges.remove(challenge);

        challengedLeader.sendMessage(plugin.getMessageManager().getMessage("party.challenge-declined-success", "%challenger%", challengerLeader.getName()));

        Player challenger = Bukkit.getPlayer(challengerLeader.getUniqueId());
        if (challenger != null && challenger.isOnline()) {
            challenger.sendMessage(plugin.getMessageManager().getMessage("party.challenge-declined", "%target%", challengedLeader.getName()));
            challenger.playSound(challenger.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public List<PartyChallenge> getActiveChallenges(UUID challengedLeaderUuid) {
        List<PartyChallenge> challenges = activeChallenges.get(challengedLeaderUuid);
        if (challenges == null) {
            return Collections.emptyList();
        }
        // Clean up expired ones on the fly
        challenges.removeIf(PartyChallenge::isExpired);
        return challenges;
    }

    private void notifyParty(Party party, Component component) {
        for (UUID memberUuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) {
                p.sendMessage(component);
            }
        }
    }
}
