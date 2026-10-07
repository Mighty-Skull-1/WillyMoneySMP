package me.moneysmp.commands;

import me.moneysmp.managers.CreditManager;
import me.moneysmp.managers.TeamManager;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GladiatorCommand implements CommandExecutor {
    private final TeamManager teamManager;
    private final CreditManager creditManager;

    public GladiatorCommand(TeamManager teamManager, CreditManager creditManager) {
        this.teamManager = teamManager;
        this.creditManager = creditManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cAdmin only!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage:\n/gladiator start <Round>\n/gladiator reward <winner> <C|B|A|CAPTAIN>");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            Bukkit.broadcastMessage("§6§l[GLADIATOR TOURNAMENT] §e" + args[1].toUpperCase() + " Tournament starting! Enter the Arena!");
        } else if (args[0].equalsIgnoreCase("reward")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /gladiator reward <winner> <C|B|A|CAPTAIN>");
                return true;
            }

            Player winner = Bukkit.getPlayer(args[1]);
            if (winner == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            Team team = teamManager.getTeamByPlayer(winner.getUniqueId());
            if (team == null) {
                sender.sendMessage("§cPlayer is not on a team!");
                return true;
            }

            String roundTier = args[2].toUpperCase();
            int points = switch (roundTier) {
                case "C" -> 25;
                case "B" -> 35;
                case "A" -> 50;
                case "CAPTAIN" -> 100;
                default -> 10;
            };

            creditManager.addGameCredits(team.getCaptain(), points);
            Bukkit.broadcastMessage("§6§l[GLADIATOR WINNER] §e" + winner.getName() + " won the " + roundTier +
                    " Tier Tournament! §a+" + points + " Game Credits to " + team.getName() + "!");
        } else {
            sender.sendMessage("§cUsage:\n/gladiator start <Round>\n/gladiator reward <winner> <C|B|A|CAPTAIN>");
        }

        return true;
    }
}
