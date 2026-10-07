package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TierManager {
    private final Map<UUID, PlayerTier> playerTiers = new HashMap<>();

    public void setTier(UUID uuid, PlayerTier tier) { playerTiers.put(uuid, tier); }
    public void removeTier(UUID uuid) { playerTiers.remove(uuid); }
    public PlayerTier getTier(UUID uuid) { return playerTiers.getOrDefault(uuid, PlayerTier.NONE); }
    public Map<UUID, PlayerTier> getAllTiers() { return java.util.Collections.unmodifiableMap(playerTiers); }
    public void clearAll() { playerTiers.clear(); }
}
