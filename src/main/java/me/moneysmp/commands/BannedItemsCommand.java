package me.moneysmp.commands;

import me.moneysmp.managers.BannedItemManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BannedItemsCommand implements CommandExecutor {
    private final BannedItemManager bannedItemManager;

    public BannedItemsCommand(BannedItemManager bannedItemManager) {
        this.bannedItemManager = bannedItemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can open the Banned Items GUI. Use '/banneditems list' from console.");
                return true;
            }
            bannedItemManager.openBannedItemsGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§c§l=== Banned Tournament Items ===");
            Map<Material, BannedItemManager.BanEntry> items = bannedItemManager.getBannedItems();
            if (items.isEmpty()) {
                sender.sendMessage("§7No items are currently banned.");
            } else {
                for (BannedItemManager.BanEntry entry : items.values()) {
                    sender.sendMessage("§c• " + BannedItemManager.formatMaterialName(entry.getMaterial()) +
                            " §8- §7Reason: " + entry.getReason() + " §8(Alt: §b" + entry.getAlternative() + "§8)");
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("ban")) {
            if (!sender.hasPermission("moneysmp.admin")) {
                sender.sendMessage("§cYou do not have permission to ban items.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /banneditems add <material> [reason]");
                return true;
            }
            Material mat = Material.matchMaterial(args[1].toUpperCase());
            if (mat == null) {
                sender.sendMessage("§cUnknown material: " + args[1]);
                return true;
            }
            String reason = "Disabled in tournament";
            if (args.length >= 3) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                reason = sb.toString().trim();
            }
            bannedItemManager.banItem(mat, reason, "Wind Charges");
            sender.sendMessage("§a[Banned Items] Successfully banned §e" + BannedItemManager.formatMaterialName(mat) + "§a!");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("unban")) {
            if (!sender.hasPermission("moneysmp.admin")) {
                sender.sendMessage("§cYou do not have permission to unban items.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /banneditems remove <material>");
                return true;
            }
            Material mat = Material.matchMaterial(args[1].toUpperCase());
            if (mat == null) {
                sender.sendMessage("§cUnknown material: " + args[1]);
                return true;
            }
            if (bannedItemManager.unbanItem(mat)) {
                sender.sendMessage("§a[Banned Items] Successfully unbanned §e" + BannedItemManager.formatMaterialName(mat) + "§a!");
            } else {
                sender.sendMessage("§cThat item is not on the ban list.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("hand")) {
            if (!sender.hasPermission("moneysmp.admin")) {
                sender.sendMessage("§cYou do not have permission to ban items.");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can ban their held item.");
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                sender.sendMessage("§cYou must hold an item in your main hand.");
                return true;
            }
            String reason = "Disabled by administrator";
            if (args.length >= 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                reason = sb.toString().trim();
            }
            bannedItemManager.banItem(hand.getType(), reason, "None");
            sender.sendMessage("§a[Banned Items] Successfully banned §e" + BannedItemManager.formatMaterialName(hand.getType()) + "§a!");
            return true;
        }

        sender.sendMessage("§cUsage: /banneditems [gui|list|add|remove|hand]");
        return true;
    }
}
