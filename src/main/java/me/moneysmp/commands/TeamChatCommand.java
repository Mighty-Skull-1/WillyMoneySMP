package me.moneysmp.commands;

import me.moneysmp.managers.TeamManager;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamChatCommand implements CommandExecutor {
    private final TeamManager teamManager;

    public TeamChatCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou must be on a team to use team chat!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /tc <message>");
            return true;
        }

        String message = String.join(" ", args);
        String formatted = "§b[TEAM CHAT] " + team.getColorCode() + player.getName() + "§7: §f" + message;

        for (UUID memberUuid : team.getMembers()) {
            Player teammate = Bukkit.getPlayer(memberUuid);
            if (teammate != null) teammate.sendMessage(formatted);
        }
        return true;
    }
}
