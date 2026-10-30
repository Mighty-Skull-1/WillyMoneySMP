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
            int maxDraft = plugin != null ? plugin.getConfig().getInt("starting-draft-credits", 300) : 300;

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
                sender.sendMessage(" §8- " + tier.getPrefix(targetTeam.getColorCode()) + " " + mName + elimStatus + " §7(" + creditManager.getGameCredits(mUuid) + " Credits)");
            }
            sender.sendMessage("§6§m--------------------------------------------------");
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
            triggerCloudSync();
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

            int draftCredits = plugin != null ? plugin.getConfig().getInt("starting-draft-credits", 300) : 300;
            int gameCredits = plugin != null ? plugin.getConfig().getInt("starting-game-credits", 100) : 100;

            Team team = teamManager.createTeam(teamName, captain.getUniqueId());
            tierManager.setTier(captain.getUniqueId(), PlayerTier.CAPTAIN);
            creditManager.setDraftCredits(captain.getUniqueId(), draftCredits);
            creditManager.setGameCredits(captain.getUniqueId(), gameCredits);

            sender.sendMessage("§aTeam " + teamName + " created with Captain " + captain.getName() +
                    " (" + draftCredits + " Draft / " + gameCredits + " Game Credits)!");
            triggerCloudSync();
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
            triggerCloudSync();
        } else if (sub.equals("sub") || sub.equals("swap") || sub.equals("substitute")) {
            substitute(sender, args);
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
            triggerCloudSync();
        } else {
            sendHelp(sender);
        }
        return true;
    }

    /**
     * /team sub <oldPlayer> <newPlayer>
     * The new player takes the old player's exact spot: same team, tier, Game Credits, kills and deaths
     * (and captain role + draft purse if the old player was captain). The old player is removed completely.
     * Works while the old player is offline. Team totals don't change. Saves straight away.
     */
    private void substitute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /team sub <player leaving> <player coming in>");
            sender.sendMessage("§7The new player keeps the old player's team, tier, credits, kills and deaths. Works if the old player is offline.");
            return;
        }
        if (plugin instanceof me.moneysmp.MoneySMP smpCheck && smpCheck.getFfaManager() != null && smpCheck.getFfaManager().isActive()) {
            sender.sendMessage("§cYou can't sub players during the Team Deathmatch - do it before or after.");
            return;
        }

        // Old player: look them up among team members by name, so it works when they're offline.
        UUID oldId = null;
        String oldName = args[1];
        for (Team t : teamManager.getTeams()) {
            for (UUID m : t.getMembers()) {
                String n = Bukkit.getOfflinePlayer(m).getName();
                if (n != null && n.equalsIgnoreCase(args[1])) { oldId = m; oldName = n; }
            }
        }
        if (oldId == null) {
            sender.sendMessage("§c" + args[1] + " isn't on any team. Check the spelling with /team info <team>.");
            return;
        }

        // New player: must be online, or have joined this server before (so we know their real UUID).
        UUID newId;
        String newName;
        Player onlineNew = Bukkit.getPlayerExact(args[2]);
        if (onlineNew != null) {
            newId = onlineNew.getUniqueId();
            newName = onlineNew.getName();
        } else {
            org.bukkit.OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(args[2]);
            if (cached == null || !cached.hasPlayedBefore()) {
                sender.sendMessage("§c" + args[2] + " has never joined this server. Get them to log in once (or be online), then run this again.");
                return;
            }
            newId = cached.getUniqueId();
            newName = cached.getName() != null ? cached.getName() : args[2];
        }

        if (newId.equals(oldId)) { sender.sendMessage("§cThat's the same player."); return; }
        Team newCurrent = teamManager.getTeamByPlayer(newId);
        if (newCurrent != null) {
            sender.sendMessage("§c" + newName + " is already on " + newCurrent.getName() + ". Remove them first.");
            return;
        }

        Team team = teamManager.getTeamByPlayer(oldId);
        boolean wasCaptain = team.isCaptain(oldId);
        PlayerTier tier = tierManager.getTier(oldId);
        int game = creditManager.getGameCredits(oldId);
        int draft = creditManager.getDraftCredits(oldId);
        int replacedCredits = creditManager.hasGameCreditsEntry(newId) ? creditManager.getGameCredits(newId) : -1;
        boolean eliminated = eliminationManager != null && eliminationManager.isEliminated(oldId);
        int kills = 0, deaths = 0;
        me.moneysmp.managers.StatsManager stats = (plugin instanceof me.moneysmp.MoneySMP smpS) ? smpS.getStatsManager() : null;
        if (stats != null) { kills = stats.getKills(oldId); deaths = stats.getDeaths(oldId); }

        // Put the new player in.
        teamManager.addPlayerToTeam(team, newId);
        if (wasCaptain) team.setCaptain(newId);
        tierManager.setTier(newId, tier);
        creditManager.setGameCredits(newId, game);
        if (wasCaptain) creditManager.setDraftCredits(newId, draft);
        if (stats != null) {
            stats.clearPlayer(newId);
            if (kills > 0) stats.setKills(newId, kills);
            if (deaths > 0) stats.setDeaths(newId, deaths);
        }
        if (eliminationManager != null) eliminationManager.setEliminated(newId, eliminated);

        // Take the old player out completely.
        teamManager.removePlayerFromTeam(oldId);
        tierManager.removeTier(oldId);
        creditManager.clearPlayer(oldId);
        if (stats != null) stats.clearPlayer(oldId);
        if (eliminationManager != null) eliminationManager.setEliminated(oldId, false);

        // Save right away so nothing is lost if the server stops.
        if (plugin instanceof me.moneysmp.MoneySMP smp && smp.getDataManager() != null) smp.getDataManager().saveData();
        triggerCloudSync();

        Bukkit.broadcastMessage("§6§l[SUB] " + team.getColorCode() + newName + " §ereplaces " + team.getColorCode() + oldName
                + " §eon " + team.getColorCode() + team.getName() + (wasCaptain ? " §6as Captain" : "") + "§e!");
        sender.sendMessage("§aDone. " + newName + " now has " + oldName + "'s spot: tier " + tier.name() + ", " + game
                + " Game Credits, " + kills + " kills, " + deaths + " deaths" + (wasCaptain ? ", captain + " + draft + " draft purse" : "") + ".");
        if (replacedCredits >= 0) {
            sender.sendMessage("§7(" + newName + " had " + replacedCredits + " credits of their own from before - those were replaced, so the team total stays the same.)");
        }
        Player oldOnline = Bukkit.getPlayer(oldId);
        if (oldOnline != null) oldOnline.sendMessage("§eYou've been subbed out of " + team.getName() + " by an admin.");
        Player newOnline = Bukkit.getPlayer(newId);
        if (newOnline != null) newOnline.sendMessage("§aYou've been subbed into " + team.getColorCode() + team.getName() + "§a! You take over " + oldName + "'s spot.");
    }

    private void triggerCloudSync() {
        if (plugin instanceof me.moneysmp.MoneySMP smp && smp.getWebServerManager() != null) {
            smp.getWebServerManager().requestPush();
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l[Willy Money SMP Team Commands]");
        sender.sendMessage("§e/team list §7- View all registered tournament teams");
        sender.sendMessage("§e/team info [team] §7- View team roster, tiers, and credits");
        sender.sendMessage("§e/team rename <newName> §7- Captain only: Rename your team");
        if (sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§c/team create <name> <captain> §7- Create new team");
            sender.sendMessage("§c/team remove <player> §7- Remove member or disband if captain");
            sender.sendMessage("§c/team sub <old> <new> §7- Swap a player out (works offline); new player keeps their spot, credits and stats");
            sender.sendMessage("§c/team disband <name> §7- Disband an existing team");
        }
    }
}
