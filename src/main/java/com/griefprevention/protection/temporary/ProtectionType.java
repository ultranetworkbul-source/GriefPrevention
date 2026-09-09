package com.griefprevention.protection.temporary;

public enum ProtectionType {

    CHEST("chest", "Chest Protection"),
    PVP("pvp", "PVP Protection"),
    MOB("mob", "Mob Protection");

    private final String id;
    private final String displayName;

    ProtectionType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProtectionType fromId(String id) {
        for (ProtectionType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
