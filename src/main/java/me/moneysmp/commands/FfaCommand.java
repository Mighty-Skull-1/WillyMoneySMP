package me.moneysmp.commands;

import me.moneysmp.managers.FfaManager;
import me.moneysmp.managers.PhaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FfaCommand implements CommandExecutor {
    private final FfaManager ffa;
    private final PhaseManager phaseManager;

    public FfaCommand(FfaManager ffa, PhaseManager phaseManager) {
        this.ffa = ffa;
        this.phaseManager = phaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (sub) {
            case "start" -> {
                if (!phaseManager.isPvPAllowed()) {
                    sender.sendMessage("§cPvP is off in " + phaseManager.getCurrentPhase().getDisplayName() + " - switch to the PvP phase first.");
                    return true;
                }
                String err = ffa.start();
                if (err != null) sender.sendMessage("§c" + err);
            }
            case "status", "standings" -> ffa.sendStatus(sender);
            case "out" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /ffa out <player>"); return true; }
                if (!ffa.isActive()) { sender.sendMessage("§cNo FFA is running."); return true; }
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!ffa.isAlive(target.getUniqueId())) { sender.sendMessage("§c" + args[1] + " isn't standing in this FFA."); return true; }
                ffa.knockOut(target.getUniqueId(), "removed by admin");
            }
            case "end" -> {
                if (!ffa.isActive()) { sender.sendMessage("§cNo FFA is running."); return true; }
                ffa.finish();
            }
            case "stop", "cancel" -> {
                if (!ffa.isActive()) { sender.sendMessage("§cNo FFA is running."); return true; }
                ffa.stop();
            }
            default -> {
                sender.sendMessage("§c§lTeam Deathmatch commands (/ffa or /tdm):");
                sender.sendMessage("§e/ffa start §7- everyone on a team who's online joins; last team standing wins");
                sender.sendMessage("§e/ffa status §7- who's still standing and placings so far");
                sender.sendMessage("§e/ffa out <player> §7- knock out an AFK/inactive player");
                sender.sendMessage("§e/ffa end §7- end now and pay places (teams still in ranked by credits)");
                sender.sendMessage("§e/ffa stop §7- cancel with no rewards");
            }
        }
        return true;
    }
}
