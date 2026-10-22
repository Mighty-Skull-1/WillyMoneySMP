package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class DraftManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;

    private UUID nomineeUuid = null;
    private String nomineeName = null;
    private Player highestBidder = null;
    private int highestBid = 0;
    private int timeLeft = 0;
    private int taskId = -1;
    private long endsAtMillis = 0L;
    private int auctionSeconds = 30;
    private int bidIncrement = 5;
    private boolean resetTimerOnBid = false;

    /** Most recent sales first: {playerName, teamName, price}. Shown on the website. */
    private final Deque<String[]> recentPicks = new ArrayDeque<>();
    private static final int MAX_RECENT_PICKS = 12;

    /** Called whenever the auction changes so the website updates instantly. */
    private Runnable onChange = null;

    public DraftManager(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager, TierManager tierManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
    }

    public boolean isDraftActive() { return nomineeUuid != null; }
    public UUID getCurrentNomineeUuid() { return nomineeUuid; }
    public String getCurrentNomineeName() { return nomineeName; }
    public Player getCurrentNominee() { return nomineeUuid != null ? Bukkit.getPlayer(nomineeUuid) : null; }
    public int getHighestBid() { return highestBid; }
    public String getHighestBidderName() { return highestBidder != null ? highestBidder.getName() : null; }
    public String getHighestBidderTeamName() {
        if (highestBidder == null) return null;
        Team team = teamManager.getTeamByPlayer(highestBidder.getUniqueId());
        return team != null ? team.getName() : null;
    }
    public String getNomineeTierName() {
        return nomineeUuid != null ? tierManager.getTier(nomineeUuid).name() : null;
    }
    public long getEndsAtMillis() { return endsAtMillis; }
    public int getAuctionSeconds() { return auctionSeconds; }
    public int getBidIncrement() { return bidIncrement; }
    /** The only bid amount allowed right now: current top bid + the increment (5, 10, 15, ...). */
    public int getNextBid() { return highestBid + bidIncrement; }
    public boolean isHighestBidder(Player player) {
        return player != null && highestBidder != null && highestBidder.getUniqueId().equals(player.getUniqueId());
    }

    private void loadDraftSettings() {
        auctionSeconds = Math.max(5, plugin.getConfig().getInt("draft.auction-seconds", 30));
        bidIncrement = Math.max(1, plugin.getConfig().getInt("draft.bid-increment", 5));
        resetTimerOnBid = plugin.getConfig().getBoolean("draft.reset-timer-on-bid", false);
    }
    public List<String[]> getRecentPicks() { return new ArrayList<>(recentPicks); }
    public void clearHistory() { recentPicks.clear(); notifyChange(); }
    public void setOnChange(Runnable onChange) { this.onChange = onChange; }

    private void notifyChange() {
        if (onChange != null) {
            try { onChange.run(); } catch (Exception ignored) {}
        }
    }

    public boolean startNomination(Player target) {
        if (isDraftActive() || target == null) return false;

        this.nomineeUuid = target.getUniqueId();
        this.nomineeName = target.getName();
        this.highestBidder = null;
        this.highestBid = 0;
        loadDraftSettings();
        this.timeLeft = auctionSeconds;
        this.endsAtMillis = System.currentTimeMillis() + auctionSeconds * 1000L;

        PlayerTier tier = tierManager.getTier(target.getUniqueId());
        Bukkit.broadcastMessage("§6§l[DRAFT] §e" + nomineeName + " §7(" + tier.getPrefix() + "§7) is up for auction! §a"
                + auctionSeconds + " seconds on the clock!");
        Bukkit.broadcastMessage("§7Captains: type §e/bid §7to bid. Bids go up §e" + bidIncrement + " §7at a time"
                + (resetTimerOnBid ? " and every bid resets the clock to §e" + auctionSeconds + "s§7." : ". §7Highest bid when the clock hits 0 wins!"));

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (timeLeft <= 0) {
                finishNomination();
                return;
            }

            String actionText = "§6Bidding for " + nomineeName + " §8| §e" + timeLeft + "s §8| §aTop Bid: " +
                    (highestBidder != null ? highestBidder.getName() + " (" + highestBid + ")" : "None") +
                    " §8| §7Next: §e/bid §7(" + getNextBid() + ")";

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionText));
            }

            if (timeLeft == 10 || timeLeft <= 5) {
                Bukkit.broadcastMessage("§e[Draft] §f" + timeLeft + " seconds remaining for " + nomineeName + "!");
            }

            timeLeft--;
        }, 0L, 20L);

        notifyChange();
        return true;
    }

    /** Bids must be exactly the next step (top bid + increment), so nobody can jump straight to a huge number. */
    public boolean placeBid(Player captain, int amount) {
        if (!isDraftActive() || captain == null) return false;
        if (amount != getNextBid()) return false;

        highestBidder = captain;
        highestBid = amount;

        if (resetTimerOnBid) {
            timeLeft = auctionSeconds;
            endsAtMillis = System.currentTimeMillis() + auctionSeconds * 1000L;
        }

        Bukkit.broadcastMessage("§6[Draft] Captain " + captain.getName() + " bid §a" + amount + " Credits §ffor " + nomineeName + "!"
                + (resetTimerOnBid ? " §7(clock reset to " + auctionSeconds + "s)" : ""));
        notifyChange();
        return true;
    }

    public void cancelDraft() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        boolean wasActive = nomineeUuid != null;
        if (wasActive) {
            Bukkit.broadcastMessage("§c[DRAFT CANCELLED] The draft auction for " + nomineeName + " was cancelled.");
        }
        nomineeUuid = null;
        nomineeName = null;
        highestBidder = null;
        highestBid = 0;
        timeLeft = 0;
        endsAtMillis = 0L;
        if (wasActive) notifyChange();
    }

    private void finishNomination() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        if (highestBidder != null) {
            Team team = teamManager.getTeamByPlayer(highestBidder.getUniqueId());
            if (team != null) {
                creditManager.removeDraftCredits(highestBidder.getUniqueId(), highestBid);
                teamManager.addPlayerToTeam(team, nomineeUuid);
                int startingGameCredits = plugin.getConfig().getInt("starting-game-credits", 100);
                creditManager.setGameCredits(nomineeUuid, startingGameCredits);

                Bukkit.broadcastMessage("§6§l[DRAFT SOLD] §eCaptain " + highestBidder.getName() + " §fwon §e" +
                        nomineeName + " §ffor §a" + highestBid + " Draft Credits!");
                recentPicks.addFirst(new String[]{nomineeName, team.getName(), String.valueOf(highestBid)});
                while (recentPicks.size() > MAX_RECENT_PICKS) recentPicks.removeLast();
            } else {
                Bukkit.broadcastMessage("§c[DRAFT ERROR] Winning captain's team was not found! " + nomineeName + " remains unassigned.");
            }
        } else {
            Bukkit.broadcastMessage("§c[DRAFT EXPIRED] No bids received for " + nomineeName + ". Player remains unassigned!");
        }

        nomineeUuid = null;
        nomineeName = null;
        highestBidder = null;
        highestBid = 0;
        timeLeft = 0;
        endsAtMillis = 0L;
        notifyChange();
    }
}
