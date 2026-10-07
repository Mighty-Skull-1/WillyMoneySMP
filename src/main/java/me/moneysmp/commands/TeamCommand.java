package me.moneysmp.commands;

import me.moneysmp.managers.CreditManager;
import me.moneysmp.managers.EliminationManager;
import me.moneysmp.managers.TeamManager;
import me.moneysmp.managers.TierManager;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final EliminationManager eliminationManager;

    public TeamCommand(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager,
                       TierManager tierManager, EliminationManager eliminationManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.eliminationManager = eliminationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- /team list ---
        if (sub.equals("list")) {
            Collection<Team> teams = teamManager.getTeams();
            if (teams.isEmpty()) {
                sender.sendMessage("§cNo teams have been created yet!");
                return true;
            }

            sender.sendMessage("§6§m--------------------------------------------------");
            sender.sendMessage("§e§l         ★ WILLY MONEY SMP TEAMS ★");
            sender.sendMessage("§6§m--------------------------------------------------");
            for (Team team : teams) {
                Player cap = Bukkit.getPlayer(team.getCaptain());
                String capName = (cap != null) ? cap.getName() : "Offline";
                sender.sendMessage(team.getColorCode() + "• " + team.getName() +
                        " §8| §fCaptain: §e" + capName +
                        " §8| §fMembers: §a" + team.getMembers().size());
            }
            sender.sendMessage("§6§m--------------------------------------------------");
            return true;
        }

        // --- /team info [teamName] ---
        if (sub.equals("info")) {
            Team targetTeam = null;
            if (args.length >= 2) {
                targetTeam = teamManager.getTeamByName(args[1]);
            } else if (sender instanceof Player player) {
                targetTeam = teamManager.getTeamByPlayer(player.getUniqueId());
            }

            if (targetTeam == null) {
                sender.sendMessage("§cTeam not found! Use /team info <teamName>");
                return true;
            }

            Player capPlayer = Bukkit.getPlayer(targetTeam.getCaptain());
            String capName = (capPlayer != null) ? "§a" + capPlayer.getName() : "§7Offline (" + targetTeam.getCaptain().toString().substring(0, 8) + ")";
            int draftPurse = creditManager.getDraftCredits(targetTeam.getCaptain());
            int maxDraft = plugin != null ? plugin.getConfig().getInt("starting-draft-credits", 400) : 400;

            sender.sendMessage("§6§m--------------------------------------------------");
            sender.sendMessage("§6Team: " + targetTeam.getColorCode() + "§l" + targetTeam.getName());
            sender.sendMessage("§fCaptain: §e★ " + capName);
            sender.sendMessage("§fDraft Purse: §a" + draftPurse + " / " + maxDraft + " Credits");
            sender.sendMessage("§fMembers (" + targetTeam.getMembers().size() + "):");

            for (UUID mUuid : targetTeam.getMembers()) {
                Player mPlayer = Bukkit.getPlayer(mUuid);
                String mName = (mPlayer != null) ? "§a" + mPlayer.getName() : "§7" + Bukkit.getOfflinePlayer(mUuid).getName();
                PlayerTier tier = tierManager.getTier(mUuid);
                String elimStatus = (eliminationManager != null && eliminationManager.isEliminated(mUuid)) ? " §c[ELIMINATED]" : "";
                sender.sendMessage(" §8- " + tier.getPrefix() + " " + mName + elimStatus + " §7(" + creditManager.getGameCredits(mUuid) + " Credits)");
            }
            sender.sendMessage("§6§m--------------------------------------------------");
            return true;
        }

        // --- /team revive <player> ---
        if (sub.equals("revive") || sub.equals("buyback")) {
            if (!(sender instanceof Player captain)) {
                sender.sendMessage("Only team captains can revive eliminated players!");
                return true;
            }

            Team team = teamManager.getTeamByPlayer(captain.getUniqueId());
            if (team == null || !team.isCaptain(captain.getUniqueId())) {
                captain.sendMessage("§cOnly team captains can revive eliminated teammates!");
                return true;
            }

            if (args.length < 2) {
                captain.sendMessage("§cUsage: /team revive <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                captain.sendMessage("§cPlayer not found or is currently offline!");
                return true;
            }

            if (!team.hasMember(target.getUniqueId())) {
                captain.sendMessage("§c" + target.getName() + " is not a member of your team!");
                return true;
            }

            if (eliminationManager == null || !eliminationManager.isEliminated(target.getUniqueId())) {
                captain.sendMessage("§c" + target.getName() + " is not currently eliminated!");
                return true;
            }

            int reviveCost = plugin != null ? plugin.getConfig().getInt("elimination.revive-cost", 150) : 150;
            int captainBalance = creditManager.getGameCredits(captain.getUniqueId());

            if (captainBalance < reviveCost) {
                captain.sendMessage("§cNot enough Game Credits to revive! Cost: §e" + reviveCost + " Credits§c, Balance: §e" + captainBalance + " Credits§c.");
                return true;
            }

            if (creditManager.removeGameCredits(captain.getUniqueId(), reviveCost)) {
                eliminationManager.revive(target, captain, team, reviveCost);
            }
            return true;
        }

        // --- /team rename <newName> ---
        if (sub.equals("rename")) {
            if (!(sender instanceof Player captain)) {
                sender.sendMessage("Only players can rename teams.");
                return true;
            }

            Team team = teamManager.getTeamByPlayer(captain.getUniqueId());
            if (team == null || !team.isCaptain(captain.getUniqueId())) {
                captain.sendMessage("§cOnly team captains can rename their team!");
                return true;
            }

            if (args.length < 2) {
                captain.sendMessage("§cUsage: /team rename <newName>");
                return true;
            }

            String oldName = team.getName();
            String newName = args[1];

            if (teamManager.teamExists(newName)) {
                captain.sendMessage("§cA team with the name '" + newName + "' already exists!");
                return true;
            }

            teamManager.renameTeam(team, newName);
            Bukkit.broadcastMessage("§6[Willy Money SMP] Captain " + captain.getName() + " renamed " + oldName + " to §a" + newName + "§6!");
            return true;
        }

        // Admin subcommands below
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cOnly server admins can run this team command!");
            return true;
        }

        // --- /team create <teamName> <captain> ---
        if (sub.equals("create")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /team create <teamName> <captain>");
                return true;
            }
            String teamName = args[1];
            if (teamManager.teamExists(teamName)) {
                sender.sendMessage("§cA team named '" + teamName + "' already exists!");
                return true;
            }

            Player captain = Bukkit.getPlayer(args[2]);
            if (captain == null) {
                sender.sendMessage("§cCaptain player not found!");
                return true;
            }

            if (teamManager.getTeamByPlayer(captain.getUniqueId()) != null) {
                sender.sendMessage("§c" + captain.getName() + " is already in a team! Remove them first.");
                return true;
            }

            int draftCredits = plugin != null ? plugin.getConfig().getInt("starting-draft-credits", 400) : 400;
            int gameCredits = plugin != null ? plugin.getConfig().getInt("starting-game-credits", 100) : 100;

            Team team = teamManager.createTeam(teamName, captain.getUniqueId());
            tierManager.setTier(captain.getUniqueId(), PlayerTier.CAPTAIN);
            creditManager.setDraftCredits(captain.getUniqueId(), draftCredits);
            creditManager.setGameCredits(captain.getUniqueId(), gameCredits);

            sender.sendMessage("§aTeam " + teamName + " created with Captain " + captain.getName() +
                    " (" + draftCredits + " Draft / " + gameCredits + " Game Credits)!");
        } else if (sub.equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /team remove <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }

            Team team = teamManager.getTeamByPlayer(target.getUniqueId());
            if (team == null) {
                sender.sendMessage("§c" + target.getName() + " is not currently in any team.");
                return true;
            }

            if (team.isCaptain(target.getUniqueId())) {
                teamManager.disbandTeam(team);
                sender.sendMessage("§aCaptain " + target.getName() + " removed. Team " + team.getName() + " was disbanded.");
            } else {
                teamManager.removePlayerFromTeam(target.getUniqueId());
                sender.sendMessage("§aSuccessfully removed " + target.getName() + " from " + team.getName() + "!");
            }
        } else if (sub.equals("disband")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /team disband <teamName>");
                return true;
            }

            String teamName = args[1];
            Team team = teamManager.getTeamByName(teamName);
            if (team == null) {
                sender.sendMessage("§cTeam '" + teamName + "' not found!");
                return true;
            }

            teamManager.disbandTeam(team);
            sender.sendMessage("§aSuccessfully disbanded team " + teamName + "!");
        } else {
            sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l[Willy Money SMP Team Commands]");
        sender.sendMessage("§e/team list §7- View all registered tournament teams");
        sender.sendMessage("§e/team info [team] §7- View team roster, tiers, and credits");
        sender.sendMessage("§e/team rename <newName> §7- Captain only: Rename your team");
        sender.sendMessage("§e/team revive <player> §7- Captain only: Buy back eliminated teammate");
        if (sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§c/team create <name> <captain> §7- Create new team");
            sender.sendMessage("§c/team remove <player> §7- Remove member or disband if captain");
            sender.sendMessage("§c/team disband <name> §7- Disband an existing team");
        }
    }
}
