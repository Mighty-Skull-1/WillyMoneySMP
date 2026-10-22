package me.moneysmp.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player PvP kills and deaths so they can be shown on the website.
 */
public class StatsManager {
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    public int getKills(UUID uuid) { return kills.getOrDefault(uuid, 0); }
    public int getDeaths(UUID uuid) { return deaths.getOrDefault(uuid, 0); }

    public void addKill(UUID uuid) { kills.merge(uuid, 1, Integer::sum); }
    public void addDeath(UUID uuid) { deaths.merge(uuid, 1, Integer::sum); }

    public void setKills(UUID uuid, int amount) { kills.put(uuid, amount); }
    public void setDeaths(UUID uuid, int amount) { deaths.put(uuid, amount); }

    public Map<UUID, Integer> getAllKills() { return Collections.unmodifiableMap(kills); }
    public Map<UUID, Integer> getAllDeaths() { return Collections.unmodifiableMap(deaths); }

    public void clearAll() {
        kills.clear();
        deaths.clear();
    }
}
