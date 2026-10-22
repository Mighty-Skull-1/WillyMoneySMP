package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.SMPPhase;
import me.moneysmp.models.Team;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final PhaseManager phaseManager;
    private final EliminationManager eliminationManager;
    private final StatsManager statsManager;

    private final File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager,
                       TierManager tierManager, PhaseManager phaseManager, EliminationManager eliminationManager,
                       StatsManager statsManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.phaseManager = phaseManager;
        this.eliminationManager = eliminationManager;
        this.statsManager = statsManager;

        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load Phase
        String phaseStr = dataConfig.getString("phase");
        if (phaseStr != null) {
            try {
                phaseManager.setPhaseSilently(SMPPhase.valueOf(phaseStr));
            } catch (IllegalArgumentException ignored) {}
        }

        // Load Tiers
        ConfigurationSection tiersSec = dataConfig.getConfigurationSection("tiers");
        if (tiersSec != null) {
            for (String key : tiersSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    PlayerTier tier = PlayerTier.valueOf(tiersSec.getString(key));
                    tierManager.setTier(uuid, tier);
                } catch (Exception ignored) {}
            }
        }

        // Load Draft Credits
        ConfigurationSection draftSec = dataConfig.getConfigurationSection("draft-credits");
        if (draftSec != null) {
            for (String key : draftSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    creditManager.setDraftCredits(uuid, draftSec.getInt(key));
                } catch (Exception ignored) {}
            }
        }

        // Load Game Credits
        ConfigurationSection gameSec = dataConfig.getConfigurationSection("game-credits");
        if (gameSec != null) {
            for (String key : gameSec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    creditManager.setGameCredits(uuid, gameSec.getInt(key));
                } catch (Exception ignored) {}
            }
        }

        // Load Teams
        ConfigurationSection teamsSec = dataConfig.getConfigurationSection("teams");
        if (teamsSec != null) {
            for (String key : teamsSec.getKeys(false)) {
                String teamName = teamsSec.getString(key + ".name");
                String captainStr = teamsSec.getString(key + ".captain");
                if (teamName != null && captainStr != null) {
                    try {
                        UUID captainUuid = UUID.fromString(captainStr);
                        Team team = teamManager.createTeam(teamName, captainUuid);

                        List<String> memberList = teamsSec.getStringList(key + ".members");
                        for (String mStr : memberList) {
                            try {
                                UUID mUuid = UUID.fromString(mStr);
                                teamManager.addPlayerToTeam(team, mUuid);
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Load Eliminated Players
        List<String> elimList = dataConfig.getStringList("eliminated");
        for (String eStr : elimList) {
            try {
                eliminationManager.setEliminated(UUID.fromString(eStr), true);
            } catch (Exception ignored) {}
        }

        // Load PvP stats (kills / deaths)
        loadIntMap("kills", (uuid, value) -> statsManager.setKills(uuid, value));
        loadIntMap("deaths", (uuid, value) -> statsManager.setDeaths(uuid, value));

        plugin.getLogger().info("Tournament data loaded successfully from data.yml");
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();

        // Save Phase
        dataConfig.set("phase", phaseManager.getCurrentPhase().name());

        // Save Tiers
        for (Map.Entry<UUID, PlayerTier> entry : tierManager.getAllTiers().entrySet()) {
            dataConfig.set("tiers." + entry.getKey().toString(), entry.getValue().name());
        }

        // Save Draft Credits
        for (Map.Entry<UUID, Integer> entry : creditManager.getAllDraftCredits().entrySet()) {
            dataConfig.set("draft-credits." + entry.getKey().toString(), entry.getValue());
        }

        // Save Game Credits
        for (Map.Entry<UUID, Integer> entry : creditManager.getAllGameCredits().entrySet()) {
            dataConfig.set("game-credits." + entry.getKey().toString(), entry.getValue());
        }

        // Save Teams
        for (Team team : teamManager.getTeams()) {
            String key = "teams." + team.getName().toLowerCase();
            dataConfig.set(key + ".name", team.getName());
            dataConfig.set(key + ".captain", team.getCaptain().toString());

            List<String> memberUuids = new ArrayList<>();
            for (UUID mUuid : team.getMembers()) {
                memberUuids.add(mUuid.toString());
            }
            dataConfig.set(key + ".members", memberUuids);
        }

        // Save Eliminated Players
        List<String> elimList = new ArrayList<>();
        for (UUID uuid : eliminationManager.getEliminatedPlayers()) {
            elimList.add(uuid.toString());
        }
        dataConfig.set("eliminated", elimList);

        // Save PvP stats
        for (Map.Entry<UUID, Integer> entry : statsManager.getAllKills().entrySet()) {
            dataConfig.set("kills." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : statsManager.getAllDeaths().entrySet()) {
            dataConfig.set("deaths." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save tournament data to data.yml: " + e.getMessage());
        }
    }

    private void loadIntMap(String path, java.util.function.BiConsumer<UUID, Integer> setter) {
        ConfigurationSection sec = dataConfig.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                setter.accept(UUID.fromString(key), sec.getInt(key));
            } catch (Exception ignored) {}
        }
    }

    public void resetData() {
        if (dataFile.exists()) {
            dataFile.delete();
        }
        dataConfig = new YamlConfiguration();
        saveData();
    }
}
