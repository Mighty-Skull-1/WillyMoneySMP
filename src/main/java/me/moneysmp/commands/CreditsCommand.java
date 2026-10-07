package me.moneysmp.commands;

import me.moneysmp.managers.CreditManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreditsCommand implements CommandExecutor {
    private final CreditManager creditManager;

    public CreditsCommand(CreditManager creditManager) {
        this.creditManager = creditManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cOnly server admins can manage player credits!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("balance")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("§cUsage: /credits balance <player>");
                return true;
            }

            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            int draft = creditManager.getDraftCredits(target.getUniqueId());
            int game = creditManager.getGameCredits(target.getUniqueId());
            sender.sendMessage("§6[Credits] §e" + target.getName() + " §8| §fDraft: §a" + draft + " §8| §fGame: §b" + game);
            return true;
        }

        if (args.length < 3) {
            sendHelp(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) {
                sender.sendMessage("§cAmount must be positive!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return true;
        }

        String type = (args.length >= 4) ? args[3].toLowerCase() : "game";

        switch (sub) {
            case "give", "add" -> {
                if (type.equals("draft")) {
                    creditManager.setDraftCredits(target.getUniqueId(), creditManager.getDraftCredits(target.getUniqueId()) + amount);
                    sender.sendMessage("§aGave " + amount + " Draft Credits to " + target.getName() + "!");
                } else {
                    creditManager.addGameCredits(target.getUniqueId(), amount);
                    sender.sendMessage("§aGave " + amount + " Game Credits to " + target.getName() + "!");
                }
            }
            case "take", "remove" -> {
                if (type.equals("draft")) {
                    creditManager.removeDraftCredits(target.getUniqueId(), amount);
                    sender.sendMessage("§cTook " + amount + " Draft Credits from " + target.getName() + "!");
                } else {
                    creditManager.removeGameCredits(target.getUniqueId(), amount);
                    sender.sendMessage("§cTook " + amount + " Game Credits from " + target.getName() + "!");
                }
            }
            case "set" -> {
                if (type.equals("draft")) {
                    creditManager.setDraftCredits(target.getUniqueId(), amount);
                    sender.sendMessage("§aSet " + target.getName() + "'s Draft Credits to " + amount + "!");
                } else {
                    creditManager.setGameCredits(target.getUniqueId(), amount);
                    sender.sendMessage("§aSet " + target.getName() + "'s Game Credits to " + amount + "!");
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l[Credits Management]");
        sender.sendMessage("§e/credits give <player> <amount> [draft|game]");
        sender.sendMessage("§e/credits take <player> <amount> [draft|game]");
        sender.sendMessage("§e/credits set <player> <amount> [draft|game]");
        sender.sendMessage("§e/credits balance [player]");
    }
}
