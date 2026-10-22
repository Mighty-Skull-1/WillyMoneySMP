package me.moneysmp;

import me.moneysmp.commands.*;
import me.moneysmp.listeners.BannedItemListener;
import me.moneysmp.listeners.BingoEventListener;
import me.moneysmp.listeners.KillEventListener;
import me.moneysmp.managers.*;
import me.moneysmp.models.SMPPhase;
import me.moneysmp.tabcompleters.MoneySMPTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public class MoneySMP extends JavaPlugin {
    private TeamManager teamManager;
    private CreditManager creditManager;
    private TierManager tierManager;
    private DraftManager draftManager;
    private EventManager eventManager;
    private PhaseManager phaseManager;
    private DisplayManager displayManager;
    private EliminationManager eliminationManager;
    private DataManager dataManager;
    private BannedItemManager bannedItemManager;
    private SMPGuiManager smpGuiManager;
    private StatsManager statsManager;
    private me.moneysmp.web.WebServerManager webServerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();

        int defaultGameCredits = getConfig().getInt("starting-game-credits", 100);

        this.teamManager = new TeamManager();
        this.creditManager = new CreditManager(defaultGameCredits);
        this.tierManager = new TierManager();
        this.phaseManager = new PhaseManager();
        this.eliminationManager = new EliminationManager(this);
        this.bannedItemManager = new BannedItemManager(this);
        this.statsManager = new StatsManager();
        this.dataManager = new DataManager(this, teamManager, creditManager, tierManager, phaseManager, eliminationManager, statsManager);

        this.draftManager = new DraftManager(this, teamManager, creditManager, tierManager);
        this.eventManager = new EventManager(this, teamManager, creditManager);
        this.displayManager = new DisplayManager(this, teamManager, creditManager, tierManager, phaseManager);
        this.smpGuiManager = new SMPGuiManager(this, phaseManager, eventManager, teamManager, eliminationManager, dataManager, bannedItemManager);
        this.webServerManager = new me.moneysmp.web.WebServerManager(this, teamManager, creditManager, tierManager,
                phaseManager, eliminationManager, eventManager, draftManager, statsManager);
        this.webServerManager.start();
        this.draftManager.setOnChange(() -> webServerManager.requestPush());

        MoneySMPTabCompleter tabCompleter = new MoneySMPTabCompleter(tierManager, teamManager, eventManager, bannedItemManager);
        tabCompleter.setDraftManager(draftManager);

        // --- Commands Registration ---
        getCommand("smp").setExecutor(new SMPCommand(this, phaseManager, eventManager, dataManager, smpGuiManager));
        getCommand("smp").setTabCompleter(tabCompleter);

        getCommand("team").setExecutor(new TeamCommand(this, teamManager, creditManager, tierManager, eliminationManager));
        getCommand("team").setTabCompleter(tabCompleter);

        getCommand("tier").setExecutor(new TierCommand(tierManager));
        getCommand("tier").setTabCompleter(tabCompleter);

        getCommand("bid").setExecutor(new BidCommand(teamManager, creditManager, tierManager, draftManager));
        getCommand("bid").setTabCompleter(tabCompleter);

        getCommand("tc").setExecutor(new TeamChatCommand(teamManager));
        getCommand("tracker").setExecutor(new TrackerCommand(teamManager));

        AnnounceCommand announceCommand = new AnnounceCommand();
        getCommand("announce").setExecutor(announceCommand);
        getCommand("announce").setTabCompleter(tabCompleter);
        getCommand("titleannounce").setExecutor(announceCommand);
        getCommand("titleannounce").setTabCompleter(tabCompleter);

        getCommand("koth").setExecutor(new KothCommand(eventManager));
        getCommand("koth").setTabCompleter(tabCompleter);

        getCommand("airdrop").setExecutor(new AirdropCommand(eventManager));
        getCommand("airdrop").setTabCompleter(tabCompleter);

        getCommand("freefall").setExecutor(new FreeFallCommand(this, phaseManager));
        getCommand("freefall").setTabCompleter(tabCompleter);

        getCommand("bingo").setExecutor(new BingoCommand(eventManager));
        getCommand("bingo").setTabCompleter(tabCompleter);

        getCommand("credits").setExecutor(new CreditsCommand(creditManager));
        getCommand("credits").setTabCompleter(tabCompleter);

        getCommand("leaderboard").setExecutor(new LeaderboardCommand(teamManager, creditManager));

        getCommand("website").setExecutor(new WebsiteCommand(this));

        BannedItemsCommand bannedItemsCommand = new BannedItemsCommand(bannedItemManager);
        getCommand("banneditems").setExecutor(bannedItemsCommand);
        getCommand("banneditems").setTabCompleter(tabCompleter);

        // --- Listeners Registration ---
        getServer().getPluginManager().registerEvents(new KillEventListener(
                this, teamManager, creditManager, tierManager, phaseManager, eventManager, eliminationManager, smpGuiManager), this);
        getServer().getPluginManager().registerEvents(new BingoEventListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new BannedItemListener(bannedItemManager), this);

        // --- Schedulers ---
        Bukkit.getScheduler().runTaskTimer(this, () -> displayManager.updateAllDisplays(), 0L, 20L);

        int autoSaveMinutes = getConfig().getInt("auto-save-interval-minutes", 5);
        long autoSaveTicks = Math.max(1, autoSaveMinutes) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> dataManager.saveData(), autoSaveTicks, autoSaveTicks);

        getLogger().info("Willy Money SMP v3.3-SNAPSHOT loaded successfully with full persistence and tournament systems!");
    }

    @Override
    public void onDisable() {
        if (webServerManager != null) {
            webServerManager.stop();
        }
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (eventManager != null) {
            eventManager.stopKoth();
            eventManager.stopBingo(false);
        }
        if (draftManager != null) {
            draftManager.cancelDraft();
        }
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("Willy Money SMP v3.3-SNAPSHOT disabled successfully and data saved!");
    }

    public DataManager getDataManager() { return dataManager; }
    public EliminationManager getEliminationManager() { return eliminationManager; }
    public SMPGuiManager getSmpGuiManager() { return smpGuiManager; }
    public BannedItemManager getBannedItemManager() { return bannedItemManager; }
    public StatsManager getStatsManager() { return statsManager; }

    public void resetTournament(CommandSender sender) {
        // 1. Stop any ongoing draft auction or events
        if (draftManager != null) {
            draftManager.cancelDraft();
        }
        if (eventManager != null) {
            eventManager.stopKoth();
            eventManager.stopBingo(false);
        }

        // 2. Reset player gamemodes, health, hunger, inventories, and teleport to world spawn
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            org.bukkit.attribute.AttributeInstance maxHealth = maxHealthAttribute(player);
            player.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0.0f);
            player.setFireTicks(0);
            for (PotionEffect pe : player.getActivePotionEffects()) {
                player.removePotionEffect(pe.getType());
            }
            player.getInventory().clear();
            player.getEnderChest().clear();
            player.setExp(0.0f);
            player.setLevel(0);

            Location spawn = player.getWorld().getSpawnLocation();
            player.teleport(spawn);

            player.sendTitle("§c§lTOURNAMENT RESET", "§eTournament reset to the beginning!", 10, 80, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        }

        // 3. Clear eliminations and PvP stats
        if (eliminationManager != null) {
            eliminationManager.clearAll();
        }
        if (statsManager != null) {
            statsManager.clearAll();
        }
        if (draftManager != null) {
            draftManager.clearHistory();
        }

        // 4. Disband all teams
        if (teamManager != null) {
            teamManager.clearAllTeams();
        }

        // 5. Reset all player tiers
        if (tierManager != null) {
            tierManager.clearAll();
        }

        // 6. Reset all credits
        if (creditManager != null) {
            creditManager.resetAll();
        }

        // 7. Reset phase to DRAFT
        if (phaseManager != null) {
            phaseManager.setPhaseSilently(SMPPhase.DRAFT);
        }

        // 8. Wipe data.yml and save fresh state
        if (dataManager != null) {
            dataManager.resetData();
        }

        // 9. Update displays
        if (displayManager != null) {
            displayManager.updateAllDisplays();
        }

        // 10. Broadcast
        Bukkit.broadcastMessage("§c§l========================================");
        Bukkit.broadcastMessage("§c§l[Willy Money SMP] FULL TOURNAMENT RESET!");
        Bukkit.broadcastMessage("§e• All players restored to Survival mode at spawn.");
        Bukkit.broadcastMessage("§e• Eliminations cleared & all players revived.");
        Bukkit.broadcastMessage("§e• Tournament phase set to: " + SMPPhase.DRAFT.getDisplayName());
        Bukkit.broadcastMessage("§e• All teams disbanded & player tiers cleared.");
        Bukkit.broadcastMessage("§e• Credits reset to starting defaults.");
        Bukkit.broadcastMessage("§e• All events stopped & saved data wiped clean.");
        Bukkit.broadcastMessage("§c§l========================================");

        if (sender != null) {
            sender.sendMessage("§a[Willy Money SMP] Tournament successfully reset from the beginning!");
        }

        // 11. Update the website right away
        if (webServerManager != null) {
            webServerManager.requestPush();
        }
    }

    /** Max-health attribute that works on Paper 1.21.1 ("generic.max_health") and 1.21.3+ ("max_health"). */
    private static org.bukkit.attribute.AttributeInstance maxHealthAttribute(Player player) {
        for (String key : new String[]{"max_health", "generic.max_health"}) {
            try {
                Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft(key));
                if (attr != null) return player.getAttribute(attr);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /** Updates settings in older config.yml files to the current tournament rules. */
    private void migrateConfig() {
        int version = getConfig().getInt("config-version", 1);
        if (version < 2) {
            getConfig().set("draft.auction-seconds", 30);
            getConfig().set("draft.reset-timer-on-bid", false);
            getConfig().set("config-version", 2);
            saveConfig();
            getLogger().info("Updated config.yml: draft auctions are now 30 seconds with no timer reset.");
        }
    }

    public me.moneysmp.web.WebServerManager getWebServerManager() {
        return webServerManager;
    }
}
