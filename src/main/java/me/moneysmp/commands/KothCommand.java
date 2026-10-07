package me.moneysmp.commands;

import me.moneysmp.managers.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KothCommand implements CommandExecutor {
    private final EventManager eventManager;

    public KothCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cAdmin only!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("start")) {
            if (sender instanceof Player player) {
                eventManager.startKoth(player.getLocation());
            } else {
                sender.sendMessage("§cRun in-game to start KOTH at your position!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            eventManager.stopKoth();
            sender.sendMessage("§cKOTH stopped!");
            return true;
        }

        sender.sendMessage("§cUsage: /koth <start|stop>");
        return true;
    }
}
