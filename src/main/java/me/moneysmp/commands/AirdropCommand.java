package me.moneysmp.commands;

import me.moneysmp.managers.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AirdropCommand implements CommandExecutor {
    private final EventManager eventManager;

    public AirdropCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cAdmin only!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("random")) {
            String tier = (args.length > 1) ? args[1] : "rare";
            World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
            eventManager.spawnRandomAirdrop(world, tier);
            sender.sendMessage("§aDispatched a random " + tier + " airdrop near an active player!");
            return true;
        }

        String tier = (args.length > 0) ? args[0] : "common";

        if (sender instanceof Player player) {
            eventManager.spawnTieredAirdrop(player.getLocation(), tier);
        } else {
            World world = Bukkit.getWorlds().get(0);
            eventManager.spawnRandomAirdrop(world, tier);
            sender.sendMessage("§aDispatched random airdrop into " + world.getName() + "!");
        }
        return true;
    }
}
