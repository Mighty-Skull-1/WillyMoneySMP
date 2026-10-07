package me.moneysmp.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class TrackerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        ItemStack tracker = new ItemStack(Material.COMPASS);
        ItemMeta meta = tracker.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lTeam Tracker");
            meta.setLore(Collections.singletonList("§7Right-click to switch between tracking Teammates & Enemy Captains"));
            tracker.setItemMeta(meta);
        }

        player.getInventory().addItem(tracker);
        player.sendMessage("§aReceived Team Tracker compass! Right-click to toggle targets.");
        return true;
    }
}
