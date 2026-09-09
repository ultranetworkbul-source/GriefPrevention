package com.griefprevention.protection.temporary;

import java.util.UUID;

public class TemporaryProtection {

    private final UUID ownerUUID;
    private final ProtectionType type;
    private final long startTime;
    private final long durationMillis;

    public TemporaryProtection(UUID ownerUUID, ProtectionType type, long startTime, long durationMillis) {
        this.ownerUUID = ownerUUID;
        this.type = type;
        this.startTime = startTime;
        this.durationMillis = durationMillis;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public ProtectionType getType() {
        return type;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long getEndTime() {
        return startTime + durationMillis;
    }

    public long getRemainingMillis() {
        return Math.max(0, getEndTime() - System.currentTimeMillis());
    }

    public boolean isActive() {
        return System.currentTimeMillis() < getEndTime();
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getFormattedRemaining() {
        long remaining = getRemainingMillis();
        if (remaining <= 0) {
            return "Expired";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
