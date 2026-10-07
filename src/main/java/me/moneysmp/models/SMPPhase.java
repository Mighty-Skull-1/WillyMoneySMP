package me.moneysmp.models;

public enum SMPPhase {
    DRAFT("§eDraft Phase"),
    GRACE("§aGrace Period (No PvP)"),
    PVP("§cPvP Enabled"),
    FINALE("§4Finale Showdown");

    private final String displayName;

    SMPPhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
