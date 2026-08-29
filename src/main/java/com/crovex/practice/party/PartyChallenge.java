package com.crovex.practice.party;

import com.crovex.practice.kit.Kit;
import java.util.UUID;

public class PartyChallenge {
    private final UUID challengerLeader;
    private final UUID challengedLeader;
    private final Kit kit;
    private final long timestamp;

    public PartyChallenge(UUID challengerLeader, UUID challengedLeader, Kit kit) {
        this.challengerLeader = challengerLeader;
        this.challengedLeader = challengedLeader;
        this.kit = kit;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getChallengerLeader() {
        return challengerLeader;
    }

    public UUID getChallengedLeader() {
        return challengedLeader;
    }

    public Kit getKit() {
        return kit;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > 60000; // 60 seconds
    }
}
