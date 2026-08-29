package com.crovex.practice.duel;

import com.crovex.practice.CrovexPractice;
import com.crovex.practice.arena.Arena;
import com.crovex.practice.kit.Kit;
import com.crovex.practice.player.PlayerState;
import com.crovex.practice.player.PracticePlayer;
import com.crovex.practice.queue.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.*;

public class DuelManager {

    private final CrovexPractice plugin;
    private final Map<UUID, List<DuelChallenge>> activeChallenges = new HashMap<>(); // Challenged UUID -> List of Challenges

    public DuelManager(CrovexPractice plugin) {
        this.plugin = plugin;
    }

    public void sendChallenge(Player challenger, Player challenged, Kit kit) {
        if (challenger.getUniqueId().equals(challenged.getUniqueId())) {
            challenger.sendMessage(plugin.getMessageManager().getMessage("duel.self-challenge"));
            return;
        }

        // Check if both are in lobby
        PracticePlayer ppChallenger = plugin.getPlayerManager().getPlayer(challenger);
        PracticePlayer ppChallenged = plugin.getPlayerManager().getPlayer(challenged);

        if (ppChallenger == null || ppChallenger.getState() != PlayerState.LOBBY ||
            ppChallenged == null || ppChallenged.getState() != PlayerState.LOBBY) {
            challenger.sendMessage(plugin.getMessageManager().getMessage("duel.already-in-match"));
            return;
        }

        DuelChallenge challenge = new DuelChallenge(challenger.getUniqueId(), challenged.getUniqueId(), kit);
        
        List<DuelChallenge> list = activeChallenges.computeIfAbsent(challenged.getUniqueId(), k -> new ArrayList<>());
        // Prevent duplicates
        list.removeIf(c -> c.getChallenger().equals(challenger.getUniqueId()));
        list.add(challenge);

        challenger.sendMessage(plugin.getMessageManager().getMessage("duel.challenge-sent", "%target%", challenged.getName(), "%kit%", kit.getDisplayName()));

        Component acceptComponent = plugin.getMessageManager().getMessage("duel.accept-btn")
                .clickEvent(ClickEvent.runCommand("/duel accept " + challenger.getName()))
                .hoverEvent(HoverEvent.showText(plugin.getMessageManager().getMessage("duel.accept-hover")));

        Component declineComponent = plugin.getMessageManager().getMessage("duel.decline-btn")
                .clickEvent(ClickEvent.runCommand("/duel decline " + challenger.getName()))
                .hoverEvent(HoverEvent.showText(plugin.getMessageManager().getMessage("duel.decline-hover")));

        Component inviteMsg = plugin.getMessageManager().getMessage("duel.challenge-received",
                "%challenger%", challenger.getName(),
                "%kit%", kit.getDisplayName())
                .append(Component.text(" "))
                .append(acceptComponent)
                .append(Component.text(" "))
                .append(declineComponent);

        challenged.sendMessage(inviteMsg);
        challenged.playSound(challenged.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public void acceptChallenge(Player challenged, Player challenger) {
        List<DuelChallenge> challenges = activeChallenges.get(challenged.getUniqueId());
        DuelChallenge challenge = null;
        if (challenges != null) {
            for (DuelChallenge c : challenges) {
                if (c.getChallenger().equals(challenger.getUniqueId())) {
                    challenge = c;
                    break;
                }
            }
        }

        if (challenge == null || challenge.isExpired()) {
            if (challenges != null && challenge != null) {
                challenges.remove(challenge);
            }
            challenged.sendMessage(plugin.getMessageManager().getMessage("duel.expired"));
            return;
        }

        challenges.remove(challenge);

        // Verify players are online and in LOBBY state
        PracticePlayer ppChallenger = plugin.getPlayerManager().getPlayer(challenger);
        PracticePlayer ppChallenged = plugin.getPlayerManager().getPlayer(challenged);

        if (ppChallenger == null || ppChallenger.getState() != PlayerState.LOBBY ||
            ppChallenged == null || ppChallenged.getState() != PlayerState.LOBBY) {
            challenged.sendMessage(plugin.getMessageManager().getMessage("duel.already-in-match"));
            return;
        }

        Arena arena = plugin.getArenaManager().findAvailableArena(challenge.getKit());
        if (arena == null) {
            Component waitMsg = plugin.getMessageManager().getMessage("queue.waiting-arena");
            challenged.sendMessage(waitMsg);
            challenger.sendMessage(waitMsg);
            return;
        }

        // Start 1v1 Unranked Match
        plugin.getMatchManager().startMatch(challenger, challenged, challenge.getKit(), QueueType.UNRANKED, arena);
    }

    public void declineChallenge(Player challenged, Player challenger) {
        List<DuelChallenge> challenges = activeChallenges.get(challenged.getUniqueId());
        DuelChallenge challenge = null;
        if (challenges != null) {
            for (DuelChallenge c : challenges) {
                if (c.getChallenger().equals(challenger.getUniqueId())) {
                    challenge = c;
                    break;
                }
            }
        }

        if (challenge == null || challenge.isExpired()) {
            if (challenges != null && challenge != null) {
                challenges.remove(challenge);
            }
            challenged.sendMessage(plugin.getMessageManager().getMessage("duel.expired"));
            return;
        }

        challenges.remove(challenge);

        challenged.sendMessage(plugin.getMessageManager().getMessage("duel.declined-success", "%challenger%", challenger.getName()));

        Player challengerPlayer = Bukkit.getPlayer(challenger.getUniqueId());
        if (challengerPlayer != null && challengerPlayer.isOnline()) {
            challengerPlayer.sendMessage(plugin.getMessageManager().getMessage("duel.declined-challenger", "%target%", challenged.getName()));
            challengerPlayer.playSound(challengerPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    public List<DuelChallenge> getActiveChallenges(UUID challengedUuid) {
        List<DuelChallenge> challenges = activeChallenges.get(challengedUuid);
        if (challenges == null) {
            return Collections.emptyList();
        }
        // Clean up expired ones on the fly
        challenges.removeIf(DuelChallenge::isExpired);
        return challenges;
    }
}
