package me.moneysmp.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnnounceCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cOnly server admins can issue announcements!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage:\n/announce <message>\n/titleannounce <Title> | <Subtitle>");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("titleannounce")) {
            String fullInput = String.join(" ", args);
            String[] parts = fullInput.split("\\|");

            String title = parts[0].trim().replace("&", "§");
            String subtitle = (parts.length > 1) ? parts[1].trim().replace("&", "§") : "";

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle("§6§l" + title, "§e" + subtitle, 10, 70, 20);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            return true;
        }

        String message = String.join(" ", args).replace("&", "§");
        String banner = "§6§m--------------------------------------------------";
        String announcementHeader = "§e§l[WILLY MONEY SMP ANNOUNCEMENT]";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(banner);
            player.sendMessage(announcementHeader);
            player.sendMessage("§f" + message);
            player.sendMessage(banner);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }

        return true;
    }
}
