package me.moneysmp.commands;

import me.moneysmp.managers.CreditManager;
import me.moneysmp.managers.DraftManager;
import me.moneysmp.managers.TeamManager;
import me.moneysmp.managers.TierManager;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BidCommand implements CommandExecutor {
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final DraftManager draftManager;

    public BidCommand(TeamManager teamManager, CreditManager creditManager, TierManager tierManager, DraftManager draftManager) {
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.draftManager = draftManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("moneysmp.admin")) {
                sender.sendMessage("§cOnly server admins can cancel nominations!");
                return true;
            }
            if (!draftManager.isDraftActive()) {
                sender.sendMessage("§cNo active draft auction is currently running!");
                return true;
            }
            draftManager.cancelDraft();
            sender.sendMessage("§aDraft auction has been cancelled.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("nominate")) {
            if (!sender.hasPermission("moneysmp.admin")) {
                sender.sendMessage("§cOnly server admins can start nominations!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cUsage: /bid nominate <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cTarget player not found!");
                return true;
            }

            if (teamManager.getTeamByPlayer(target.getUniqueId()) != null) {
                sender.sendMessage("§c" + target.getName() + " is already a member of a team!");
                return true;
            }

            PlayerTier tier = tierManager.getTier(target.getUniqueId());
            if (tier == PlayerTier.NONE || tier == PlayerTier.CAPTAIN) {
                sender.sendMessage("§cPlayer is not in a draftable tier (A, B, C)!");
                return true;
            }

            if (!draftManager.startNomination(target)) {
                sender.sendMessage("§cA draft auction is already running!");
            }
            return true;
        }

        if (!(sender instanceof Player captain)) {
            sender.sendMessage("Only players can bid on nominated players.");
            return true;
        }

        Team team = teamManager.getTeamByPlayer(captain.getUniqueId());
        if (team == null || !team.isCaptain(captain.getUniqueId())) {
            captain.sendMessage("§c[Willy Money SMP] Access Denied! Only S-Tier Team Captains can bid!");
            return true;
        }

        if (!draftManager.isDraftActive()) {
            captain.sendMessage("§cThere is no active draft auction running! An admin must use /bid nominate <player>");
            return true;
        }

        if (args.length < 1) {
            captain.sendMessage("§cUsage: /bid <amount>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            captain.sendMessage("§cInvalid bid amount! Please specify a positive number.");
            return true;
        }

        if (amount <= 0) {
            captain.sendMessage("§cBid amount must be greater than 0!");
            return true;
        }

        java.util.UUID nomineeUuid = draftManager.getCurrentNomineeUuid();
        if (nomineeUuid == null) {
            captain.sendMessage("§cNo player is currently nominated!");
            return true;
        }

        PlayerTier nomineeTier = tierManager.getTier(nomineeUuid);
        for (java.util.UUID memberId : team.getMembers()) {
            if (tierManager.getTier(memberId) == nomineeTier) {
                captain.sendMessage("§cYour team already has a player from Tier " + nomineeTier.name() + "!");
                return true;
            }
        }

        int availableDraftCredits = creditManager.getDraftCredits(captain.getUniqueId());
        if (availableDraftCredits < amount) {
            captain.sendMessage("§cNot enough draft credits! Remaining Balance: " + availableDraftCredits);
            return true;
        }

        if (!draftManager.placeBid(captain, amount)) {
            captain.sendMessage("§cYour bid must be higher than the current top bid (" + draftManager.getHighestBid() + " Credits)!");
        }

        return true;
    }
}
