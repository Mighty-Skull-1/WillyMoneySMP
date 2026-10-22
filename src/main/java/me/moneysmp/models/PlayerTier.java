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

    /**
     * Tier tag in the player's team colour (e.g. a TeamBlue captain shows a blue [S-CAPTAIN]).
     * Falls back to the normal tier colours when the player has no team.
     */
    public String getPrefix(String teamColorCode) {
        if (teamColorCode == null || teamColorCode.isEmpty() || this == NONE) return prefix;
        String label = prefix.replaceAll("§.", "");
        return teamColorCode + "§l" + label;
    }
}
