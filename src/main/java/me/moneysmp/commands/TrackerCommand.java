package me.moneysmp.commands;

import me.moneysmp.managers.TeamManager;
import me.moneysmp.models.Team;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class TrackerCommand implements CommandExecutor {
    private final TeamManager teamManager;

    public TrackerCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        Team team = teamManager != null ? teamManager.getTeamByPlayer(player.getUniqueId()) : null;
        boolean captain = team != null && team.isCaptain(player.getUniqueId());

        ItemStack tracker = new ItemStack(Material.COMPASS);
        ItemMeta meta = tracker.getItemMeta();
        if (meta != null) {
            // The name must keep "Team Tracker" - the click handler looks for it.
            meta.setDisplayName(captain ? "§6§lTeam Tracker §c(Captain)" : "§6§lTeam Tracker");
            meta.setLore(captain
                    ? Arrays.asList("§7Right-click to switch between", "§7your nearest §aTeammate §7and §cEnemy Captain")
                    : Collections.singletonList("§7Right-click to point at your nearest §aTeammate"));
            tracker.setItemMeta(meta);
        }

        player.getInventory().addItem(tracker);
        player.sendMessage(captain
                ? "§aReceived Captain Tracker! Right-click to switch between teammates and enemy captains."
                : "§aReceived Team Tracker! Right-click to point at your nearest teammate.");
        return true;
    }
}
