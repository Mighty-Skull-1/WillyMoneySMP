package me.moneysmp.commands;

import me.moneysmp.managers.TierManager;
import me.moneysmp.models.PlayerTier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TierCommand implements CommandExecutor {
    private final TierManager tierManager;

    public TierCommand(TierManager tierManager) {
        this.tierManager = tierManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cOnly server admins can set player tiers!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage:\n/tier set <player> <A|B|C>\n/tier remove <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tier set <player> <A|B|C>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            try {
                PlayerTier tier = PlayerTier.valueOf(args[2].toUpperCase());
                tierManager.setTier(target.getUniqueId(), tier);
                sender.sendMessage("§aAssigned " + target.getName() + " to Tier " + tier.name());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid Tier! Use A, B, or C.");
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            tierManager.removeTier(target.getUniqueId());
            sender.sendMessage("§aRemoved tier from " + target.getName());
        } else {
            sender.sendMessage("§cUsage:\n/tier set <player> <A|B|C>\n/tier remove <player>");
        }
        return true;
    }
}
