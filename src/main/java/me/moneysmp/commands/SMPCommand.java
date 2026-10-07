package me.moneysmp.commands;

import me.moneysmp.managers.DataManager;
import me.moneysmp.managers.EventManager;
import me.moneysmp.managers.PhaseManager;
import me.moneysmp.managers.SMPGuiManager;
import me.moneysmp.models.SMPPhase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SMPCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final PhaseManager phaseManager;
    private final EventManager eventManager;
    private final DataManager dataManager;
    private final SMPGuiManager smpGuiManager;

    public SMPCommand(JavaPlugin plugin, PhaseManager phaseManager, EventManager eventManager,
                      DataManager dataManager, SMPGuiManager smpGuiManager) {
        this.plugin = plugin;
        this.phaseManager = phaseManager;
        this.eventManager = eventManager;
        this.dataManager = dataManager;
        this.smpGuiManager = smpGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cAdmin only!");
            return true;
        }

        // Open Control Panel GUI if no arguments or /smp gui
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("gui"))) {
            if (sender instanceof Player player) {
                if (smpGuiManager != null) {
                    smpGuiManager.openControlPanel(player);
                }
            } else {
                sender.sendMessage("§cOnly players can open the GUI. Usage: /smp phase <phase> | /smp reload | /smp save");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (plugin != null) {
                plugin.reloadConfig();
            }
            if (eventManager != null) {
                eventManager.loadAirdropConfig();
            }
            sender.sendMessage("§a[Willy Money SMP] Configuration and airdrops reloaded successfully!");
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            if (dataManager != null) {
                dataManager.saveData();
                sender.sendMessage("§a[Willy Money SMP] All tournament data successfully saved to data.yml!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                if (plugin instanceof me.moneysmp.MoneySMP smp) {
                    smp.resetTournament(sender);
                }
            } else {
                sender.sendMessage("§c§l[RESET WARNING] §eAre you sure you want to completely reset the tournament?");
                sender.sendMessage("§7• All players will be set to Survival at spawn.");
                sender.sendMessage("§7• All eliminations cleared & all players revived.");
                sender.sendMessage("§7• All teams disbanded, player tiers reset, credits restored to starting defaults.");
                sender.sendMessage("§7• All events stopped and saved data wiped clean.");
                sender.sendMessage("§cType §f/smp reset confirm §cor Shift-Click the TNT in §f/smp §cto confirm!");
            }
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("phase")) {
            try {
                SMPPhase phase = SMPPhase.valueOf(args[1].toUpperCase());
                phaseManager.setPhase(phase);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid phase! Choose: draft, grace, pvp, finale");
            }
            return true;
        }

        sender.sendMessage("§cUsage:\n/smp (opens GUI)\n/smp phase <draft|grace|pvp|finale>\n/smp reset [confirm]\n/smp reload\n/smp save");
        return true;
    }
}
