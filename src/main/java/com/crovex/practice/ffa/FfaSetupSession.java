package com.crovex.practice.ffa;

import org.bukkit.Location;

public class FfaSetupSession {
    private final String arenaName;
    private Location pos1;
    private Location pos2;

    public FfaSetupSession(String arenaName) {
        this.arenaName = arenaName;
    }

    public String getArenaName() {
        return arenaName;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }
}
