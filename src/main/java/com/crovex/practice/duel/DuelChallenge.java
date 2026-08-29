package com.crovex.practice.duel;

import com.crovex.practice.kit.Kit;
import java.util.UUID;

public class DuelChallenge {
    private final UUID challenger;
    private final UUID challenged;
    private final Kit kit;
    private final long timestamp;

    public DuelChallenge(UUID challenger, UUID challenged, Kit kit) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.kit = kit;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getChallenger() {
        return challenger;
    }

    public UUID getChallenged() {
        return challenged;
    }

    public Kit getKit() {
        return kit;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > 60000; // 60 seconds
    }
}
