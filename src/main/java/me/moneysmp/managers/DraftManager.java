package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

    public boolean startNomination(Player target) {
        if (isDraftActive() || target == null) return false;

        this.nomineeUuid = target.getUniqueId();
        this.nomineeName = target.getName();
        this.highestBidder = null;
        this.highestBid = 0;
        this.timeLeft = 30;

        PlayerTier tier = tierManager.getTier(target.getUniqueId());
        Bukkit.broadcastMessage("§6§l[DRAFT] §e" + nomineeName + " §7(" + tier.getPrefix() + "§7) is up for auction! §a30 seconds on the clock!");

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (timeLeft <= 0) {
                finishNomination();
                return;
            }

            String actionText = "§6Bidding for " + nomineeName + " §8| §e" + timeLeft + "s §8| §aTop Bid: " +
                    (highestBidder != null ? highestBidder.getName() + " (" + highestBid + " pts)" : "None");

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionText));
            }

            if (timeLeft == 15 || timeLeft == 10 || timeLeft <= 5) {
                Bukkit.broadcastMessage("§e[Draft] §f" + timeLeft + " seconds remaining for " + nomineeName + "!");
            }

            timeLeft--;
        }, 0L, 20L);

        return true;
    }

    public boolean placeBid(Player captain, int amount) {
        if (!isDraftActive() || captain == null) return false;
        if (amount <= highestBid) return false;

        highestBidder = captain;
        highestBid = amount;

        Bukkit.broadcastMessage("§6[Draft] Captain " + captain.getName() + " raised bid to §a" + amount + " Credits §ffor " + nomineeName + "!");
        return true;
    }

    public void cancelDraft() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (nomineeUuid != null) {
            Bukkit.broadcastMessage("§c[DRAFT CANCELLED] The draft auction for " + nomineeName + " was cancelled.");
        }
        nomineeUuid = null;
        nomineeName = null;
        highestBidder = null;
        highestBid = 0;
        timeLeft = 0;
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
    }
}
