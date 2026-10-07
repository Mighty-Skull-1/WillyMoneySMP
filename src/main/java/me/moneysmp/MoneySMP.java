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
    private ShopManager shopManager;
    private DataManager dataManager;
    private BannedItemManager bannedItemManager;
    private SMPGuiManager smpGuiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int defaultGameCredits = getConfig().getInt("starting-game-credits", 100);

        this.teamManager = new TeamManager();
        this.creditManager = new CreditManager(defaultGameCredits);
        this.tierManager = new TierManager();
        this.phaseManager = new PhaseManager();
        this.eliminationManager = new EliminationManager(this);
        this.shopManager = new ShopManager(creditManager);
        this.bannedItemManager = new BannedItemManager(this);
        this.dataManager = new DataManager(this, teamManager, creditManager, tierManager, phaseManager, eliminationManager);

        this.draftManager = new DraftManager(this, teamManager, creditManager, tierManager);
        this.eventManager = new EventManager(this, teamManager, creditManager);
        this.displayManager = new DisplayManager(this, teamManager, creditManager, tierManager, phaseManager);
        this.smpGuiManager = new SMPGuiManager(this, phaseManager, eventManager, teamManager, eliminationManager, dataManager, bannedItemManager);

        MoneySMPTabCompleter tabCompleter = new MoneySMPTabCompleter(tierManager, teamManager, eventManager, bannedItemManager);

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
        getCommand("tracker").setExecutor(new TrackerCommand());

        AnnounceCommand announceCommand = new AnnounceCommand();
        getCommand("announce").setExecutor(announceCommand);
        getCommand("announce").setTabCompleter(tabCompleter);
        getCommand("titleannounce").setExecutor(announceCommand);
        getCommand("titleannounce").setTabCompleter(tabCompleter);

        getCommand("koth").setExecutor(new KothCommand(eventManager));
        getCommand("koth").setTabCompleter(tabCompleter);

        getCommand("airdrop").setExecutor(new AirdropCommand(eventManager));
        getCommand("airdrop").setTabCompleter(tabCompleter);

        getCommand("gladiator").setExecutor(new GladiatorCommand(teamManager, creditManager));
        getCommand("gladiator").setTabCompleter(tabCompleter);

        getCommand("bingo").setExecutor(new BingoCommand(eventManager));
        getCommand("bingo").setTabCompleter(tabCompleter);

        getCommand("shop").setExecutor(new ShopCommand(shopManager));

        getCommand("credits").setExecutor(new CreditsCommand(creditManager));
        getCommand("credits").setTabCompleter(tabCompleter);

        getCommand("leaderboard").setExecutor(new LeaderboardCommand(teamManager, creditManager));

        BannedItemsCommand bannedItemsCommand = new BannedItemsCommand(bannedItemManager);
        getCommand("banneditems").setExecutor(bannedItemsCommand);
        getCommand("banneditems").setTabCompleter(tabCompleter);

        // --- Listeners Registration ---
        getServer().getPluginManager().registerEvents(new KillEventListener(
                this, teamManager, creditManager, tierManager, phaseManager, eventManager, eliminationManager, shopManager, smpGuiManager), this);
        getServer().getPluginManager().registerEvents(new BingoEventListener(this, eventManager), this);
        getServer().getPluginManager().registerEvents(new BannedItemListener(bannedItemManager), this);

        // --- Schedulers ---
        Bukkit.getScheduler().runTaskTimer(this, () -> displayManager.updateAllDisplays(), 0L, 20L);

        int autoSaveMinutes = getConfig().getInt("auto-save-interval-minutes", 5);
        long autoSaveTicks = Math.max(1, autoSaveMinutes) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> dataManager.saveData(), autoSaveTicks, autoSaveTicks);

        getLogger().info("Willy Money SMP v2.5-SNAPSHOT loaded successfully with full persistence, shop, and tournament systems!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (eventManager != null) {
            eventManager.stopKoth();
            eventManager.stopBingo();
        }
        if (draftManager != null) {
            draftManager.cancelDraft();
        }
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("Willy Money SMP v2.5-SNAPSHOT disabled successfully and data saved!");
    }

    public DataManager getDataManager() { return dataManager; }
    public EliminationManager getEliminationManager() { return eliminationManager; }
    public ShopManager getShopManager() { return shopManager; }
    public SMPGuiManager getSmpGuiManager() { return smpGuiManager; }
    public BannedItemManager getBannedItemManager() { return bannedItemManager; }

    public void resetTournament(CommandSender sender) {
        // 1. Stop any ongoing draft auction or events
        if (draftManager != null) {
            draftManager.cancelDraft();
        }
        if (eventManager != null) {
            eventManager.stopKoth();
            eventManager.stopBingo();
        }

        // 2. Reset player gamemodes, health, hunger, inventories, and teleport to world spawn
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            } else {
                player.setHealth(20.0);
            }
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

        // 3. Clear eliminations
        if (eliminationManager != null) {
            eliminationManager.clearAll();
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
    }
}
