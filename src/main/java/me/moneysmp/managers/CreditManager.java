package me.moneysmp.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreditManager {
    private final Map<UUID, Integer> draftCredits = new HashMap<>();
    private final Map<UUID, Integer> gameCredits = new HashMap<>();
    private final int defaultGameCredits;

    public CreditManager() {
        this(100);
    }

    public CreditManager(int defaultGameCredits) {
        this.defaultGameCredits = defaultGameCredits;
    }

    public void setDraftCredits(UUID uuid, int amount) { draftCredits.put(uuid, amount); }
    public int getDraftCredits(UUID uuid) { return draftCredits.getOrDefault(uuid, 0); }

    public boolean removeDraftCredits(UUID uuid, int amount) {
        int current = getDraftCredits(uuid);
        if (current < amount) return false;
        draftCredits.put(uuid, current - amount);
        return true;
    }

    public void setGameCredits(UUID uuid, int amount) { gameCredits.put(uuid, amount); }
    public int getGameCredits(UUID uuid) { return gameCredits.getOrDefault(uuid, defaultGameCredits); }

    public void addGameCredits(UUID uuid, int amount) {
        setGameCredits(uuid, getGameCredits(uuid) + amount);
    }

    public boolean removeGameCredits(UUID uuid, int amount) {
        int current = getGameCredits(uuid);
        if (current < amount) return false;
        gameCredits.put(uuid, current - amount);
        return true;
    }

    public Map<UUID, Integer> getAllDraftCredits() { return java.util.Collections.unmodifiableMap(draftCredits); }
    public Map<UUID, Integer> getAllGameCredits() { return java.util.Collections.unmodifiableMap(gameCredits); }

    public void resetAll() {
        draftCredits.clear();
        gameCredits.clear();
    }
}
