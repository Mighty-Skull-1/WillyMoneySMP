package me.moneysmp.models;

public enum PlayerTier {
    CAPTAIN("§c§l[S-CAPTAIN]"),
    A("§e§l[TIER-A]"),
    B("§a§l[TIER-B]"),
    C("§b§l[TIER-C]"),
    NONE("§7[NO TIER]");

    private final String prefix;

    PlayerTier(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
