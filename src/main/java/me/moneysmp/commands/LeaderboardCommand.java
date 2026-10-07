package me.moneysmp.commands;

import me.moneysmp.managers.CreditManager;
import me.moneysmp.managers.TeamManager;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class LeaderboardCommand implements CommandExecutor {
    private final TeamManager teamManager;
    private final CreditManager creditManager;

    public LeaderboardCommand(TeamManager teamManager, CreditManager creditManager) {
        this.teamManager = teamManager;
        this.creditManager = creditManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Collection<Team> allTeams = teamManager.getTeams();
        if (allTeams.isEmpty()) {
            sender.sendMessage("§cNo teams have been created yet!");
            return true;
        }

        // Rank teams by total credits (captain's game credits + draft credits, or total team game balance)
        List<Team> sorted = new ArrayList<>(allTeams);
        sorted.sort((t1, t2) -> {
            int t1Total = calculateTeamCredits(t1);
            int t2Total = calculateTeamCredits(t2);
            return Integer.compare(t2Total, t1Total);
        });

        sender.sendMessage("§6§m--------------------------------------------------");
        sender.sendMessage("§e§l         ★ WILLY MONEY SMP LEADERBOARD ★");
        sender.sendMessage("§6§m--------------------------------------------------");

        int rank = 1;
        for (Team team : sorted) {
            String rankColor = switch (rank) {
                case 1 -> "§6§l#1";
                case 2 -> "§f§l#2";
                case 3 -> "§c§l#3";
                default -> "§7#" + rank;
            };

            Player capPlayer = Bukkit.getPlayer(team.getCaptain());
            String captainName = (capPlayer != null) ? capPlayer.getName() : "Offline";
            int teamCredits = calculateTeamCredits(team);

            sender.sendMessage(rankColor + " " + team.getColorCode() + team.getName() +
                    " §8| §fCapt: §e" + captainName +
                    " §8| §fMembers: §a" + team.getMembers().size() +
                    " §8| §fBalance: §e" + teamCredits + " Credits");
            rank++;
            if (rank > 10) break;
        }

        sender.sendMessage("§6§m--------------------------------------------------");
        return true;
    }

    private int calculateTeamCredits(Team team) {
        int total = 0;
        for (UUID member : team.getMembers()) {
            total += creditManager.getGameCredits(member);
        }
        return total;
    }
}
