package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Team Deathmatch - the FINAL FIGHT of the tournament (/tdm start switches to the Finale phase).
 * When it ends, placings are paid and the team on top of the leaderboard wins the tournament.
 *
 * - Everyone on a team who is online (and not a spectator) when /ffa start runs takes part.
 * - Dying (or logging out) knocks you out of the deathmatch only: you spectate until the FFA ends,
 *   then you're put back in Survival at your respawn point. Nothing in the tournament is reset.
 * - When a team has nobody left standing it's out: first team out = last place, next = one place up...
 * - The last team with someone standing wins. Places pay rewards.ffa.* to the team captain.
 */
public class FfaManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;

    private boolean active = false;
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> knockedOut = new HashSet<>();
    private final Map<UUID, Location> returnSpot = new HashMap<>();
    /** Knocked-out players who were offline when the FFA ended; restored when they next join. */
    private final Set<UUID> restoreOnJoin = new HashSet<>();
    /** Team names (lower case) that started the FFA. */
    private final List<String> startingTeams = new ArrayList<>();
    /** Team names (lower case) in the order they were wiped out (first out first). */
    private final List<String> outOrder = new ArrayList<>();

    // --- Bounty tier: every few minutes one tier glows and is worth extra per kill ---
    private int bountyTaskId = -1;
    private int bountyIndex = -1;
    private PlayerTier bountyTier = null;

    private static final String[] PLACE = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th"};

    public FfaManager(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
    }

    public boolean isActive() { return active; }
    public boolean isAlive(UUID uuid) { return active && alive.contains(uuid); }
    public boolean isKnockedOut(UUID uuid) { return active && knockedOut.contains(uuid); }

    /** Returns null on success, otherwise the reason it couldn't start. */
    public String start() {
        if (active) return "A deathmatch is already running. Use /ffa stop or /ffa end first.";
        alive.clear(); knockedOut.clear(); returnSpot.clear(); startingTeams.clear(); outOrder.clear();

        for (Team team : teamManager.getTeams()) {
            boolean any = false;
            for (UUID id : team.getMembers()) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;
                alive.add(id);
                any = true;
            }
            if (any) startingTeams.add(team.getName().toLowerCase());
        }
        if (startingTeams.size() < 2) {
            alive.clear(); startingTeams.clear();
            return "Need players online from at least 2 teams to start a deathmatch.";
        }
        active = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§4§lFINAL FIGHT", "§eTeam Deathmatch - last team standing wins!", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }
        Bukkit.broadcastMessage("§4§l[FINAL FIGHT] §eThe Team Deathmatch has started! §7(" + startingTeams.size() + " teams, " + alive.size() + " players)");
        Bukkit.broadcastMessage("§7 - Die and you're out of the deathmatch (spectate until it ends, then you're back).");
        Bukkit.broadcastMessage("§7 - When your whole team is out, your team is out. First team out = last place.");
        Bukkit.broadcastMessage("§7 - Fight whoever you want, but §cdon't focus one team the whole deathmatch§7 - that gets a fine.");
        Bukkit.broadcastMessage("§7 - Logging out during the deathmatch counts as being knocked out.");
        Bukkit.broadcastMessage("§6 Kills are worth §a+" + killBonus() + " §6extra during the deathmatch. Every "
                + (rotateSeconds() / 60) + " min a tier glows and is worth another §a+" + bountyBonus() + "§6.");
        Bukkit.broadcastMessage("§6 Placings: " + rewardsLine());
        Bukkit.broadcastMessage("§6 When it ends, the team with the most Game Credits wins Willy Money SMP!");
        bountyIndex = -1;
        nextBounty();
        long ticks = rotateSeconds() * 20L;
        bountyTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::nextBounty, ticks, ticks).getTaskId();
        return null;
    }

    /** Knock a player out of the deathmatch (death, logout, or /ffa out). */
    public void knockOut(UUID uuid, String reason) {
        if (!active || !alive.remove(uuid)) return;
        knockedOut.add(uuid);
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        Team team = teamManager.getTeamByPlayer(uuid);
        String tName = team != null ? team.getColorCode() + team.getName() : "§7No Team";
        Bukkit.broadcastMessage("§c[TDM] §e" + op.getName() + " §7(" + tName + "§7) is out of the deathmatch" + (reason != null ? " §8(" + reason + ")" : "") + "§7.");
        if (team != null) checkTeam(team);
    }

    private boolean teamHasAlive(String teamLower) {
        Team t = teamManager.getTeamByName(teamLower);
        if (t == null) return false;
        for (UUID id : t.getMembers()) if (alive.contains(id)) return true;
        return false;
    }

    private List<String> teamsStillIn() {
        List<String> in = new ArrayList<>();
        for (String t : startingTeams) if (!outOrder.contains(t) && teamHasAlive(t)) in.add(t);
        return in;
    }

    private void checkTeam(Team team) {
        String key = team.getName().toLowerCase();
        if (!startingTeams.contains(key) || outOrder.contains(key) || teamHasAlive(key)) return;
        outOrder.add(key);
        int place = startingTeams.size() - outOrder.size() + 1;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(team.getColorCode() + "§l" + team.getName().toUpperCase() + " IS OUT", "§eThey finish §6" + PLACE[Math.min(place - 1, PLACE.length - 1)] + " §ein the deathmatch", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.6f, 1.2f);
        }
        Bukkit.broadcastMessage("§c§l[TDM] " + team.getColorCode() + team.getName() + " §chas no players left - they finish §6" + PLACE[Math.min(place - 1, PLACE.length - 1)] + "§c!");

        // Also mark any teams that somehow have nobody left (e.g. disbanded) so the count stays right.
        List<String> in = teamsStillIn();
        if (in.size() <= 1) Bukkit.getScheduler().runTask(plugin, this::finish);
    }

    /** Places: teams still in (most credits first if ended early), then the wiped teams in reverse order. */
    public List<String> standings() {
        List<String> in = teamsStillIn();
        in.sort((a, b) -> Integer.compare(teamCredits(b), teamCredits(a)));
        List<String> order = new ArrayList<>(in);
        for (int i = outOrder.size() - 1; i >= 0; i--) order.add(outOrder.get(i));
        for (String t : startingTeams) if (!order.contains(t)) order.add(t);
        return order;
    }

    private int teamCredits(String teamLower) {
        Team t = teamManager.getTeamByName(teamLower);
        if (t == null) return 0;
        int sum = 0;
        for (UUID id : t.getMembers()) sum += creditManager.getGameCredits(id);
        return sum;
    }

    /** Ends the FFA, pays places and brings knocked-out players back. Ending early ranks the teams still in by credits. */
    public void finish() {
        if (!active) return;
        List<String> order = standings();
        int[] rewards = rewards();
        Bukkit.broadcastMessage("§6§l===== TEAM DEATHMATCH RESULTS =====");
        for (int i = 0; i < order.size(); i++) {
            Team t = teamManager.getTeamByName(order.get(i));
            if (t == null) continue;
            int reward = i < rewards.length ? rewards[i] : 0;
            if (reward > 0) creditManager.addGameCredits(t.getCaptain(), reward);
            Bukkit.broadcastMessage("  §6" + PLACE[Math.min(i, PLACE.length - 1)] + ": " + t.getColorCode() + t.getName()
                    + (reward > 0 ? " §a+" + reward + " Game Credits" : ""));
        }
        Bukkit.broadcastMessage("§6§l=================================");
        if (!order.isEmpty()) {
            Team winner = teamManager.getTeamByName(order.get(0));
            if (winner != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(winner.getColorCode() + "§l" + winner.getName().toUpperCase() + " WINS", "§eTeam Deathmatch champions!", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
        cleanup();
        // This is the final fight: once placings are paid, crown the tournament winner (most Game Credits).
        Bukkit.getScheduler().runTaskLater(plugin, this::crownTournament, 100L);
    }

    /** The tournament winner is the team with the most combined Game Credits (the website's #1). */
    public void crownTournament() {
        List<Team> ranked = new ArrayList<>(teamManager.getTeams());
        ranked.sort((a, c) -> {
            int diff = Integer.compare(teamCredits(c.getName()), teamCredits(a.getName()));
            return diff != 0 ? diff : a.getName().compareToIgnoreCase(c.getName());
        });
        if (ranked.isEmpty()) return;
        Team winner = ranked.get(0);
        String winName = winner.getColorCode() + winner.getName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(winName + " §6§lWINS!", "§e" + teamCredits(winner.getName()) + " Game Credits - Willy Money SMP champions", 10, 140, 30);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
        }
        Bukkit.broadcastMessage("§6§m------------------------------------------");
        Bukkit.broadcastMessage("§6§l  WILLY MONEY SMP - FINAL STANDINGS");
        String[] medals = {"§6§l1st", "§f§l2nd", "§c§l3rd"};
        for (int i = 0; i < ranked.size(); i++) {
            Team t = ranked.get(i);
            String place = i < 3 ? medals[i] : "§7" + (i + 1) + "th";
            Bukkit.broadcastMessage("  " + place + " §r" + t.getColorCode() + t.getName() + " §8- §e" + teamCredits(t.getName()) + " Credits");
        }
        Bukkit.broadcastMessage("§6§m------------------------------------------");
        Bukkit.broadcastMessage("§e" + winName + " §ewins Willy Money SMP! §7Final standings are on the website.");
    }

    /** Cancels the FFA with no rewards and brings everyone back. */
    public void stop() {
        if (!active) return;
        Bukkit.broadcastMessage("§c§l[TDM] §eThe Team Deathmatch was cancelled. No placings were paid.");
        cleanup();
    }

    private void cleanup() {
        active = false;
        if (bountyTaskId != -1) { Bukkit.getScheduler().cancelTask(bountyTaskId); bountyTaskId = -1; }
        bountyTier = null;
        for (Player p : Bukkit.getOnlinePlayers()) if (p.isGlowing()) p.setGlowing(false);
        for (UUID id : knockedOut) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !p.isDead()) restore(p);
            else restoreOnJoin.add(id); // offline, or still on the death screen - restored on join/respawn
        }
        alive.clear(); knockedOut.clear(); startingTeams.clear(); outOrder.clear();
    }

    /** Called when a player joins: brings back anyone who missed the end of an FFA. */
    public void handleJoin(Player p) {
        if (active && knockedOut.contains(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> p.setGameMode(GameMode.SPECTATOR));
            p.sendMessage("§c[TDM] §eYou're out of the deathmatch - spectating until it ends.");
        } else if (restoreOnJoin.remove(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> restore(p));
        }
    }

    /** Called on respawn: knocked-out players spectate; anyone owed a restore gets it now. */
    public void handleRespawn(Player p, Location respawnAt) {
        UUID id = p.getUniqueId();
        if (active && knockedOut.contains(id)) {
            if (respawnAt != null) returnSpot.put(id, respawnAt.clone());
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage("§c[TDM] §eYou're out of the deathmatch - spectating until it ends. You'll be put back afterwards.");
            });
        } else if (restoreOnJoin.remove(id)) {
            returnSpot.remove(id); // they're respawning normally anyway
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.getGameMode() == GameMode.SPECTATOR) restore(p);
            });
        }
    }

    private void restore(Player p) {
        if (plugin instanceof me.moneysmp.MoneySMP smp && smp.getEliminationManager() != null
                && smp.getEliminationManager().isEliminated(p.getUniqueId())) {
            returnSpot.remove(p.getUniqueId());
            return; // permanently eliminated for some other reason - leave them spectating
        }
        Location back = returnSpot.remove(p.getUniqueId());
        if (back == null) back = p.getWorld().getSpawnLocation();
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(back);
        p.sendMessage("§a[TDM] §eThe deathmatch is over - you're back in the game.");
    }

    public void sendStatus(CommandSender s) {
        if (!active) { s.sendMessage("§7[TDM] No Team Deathmatch is running."); return; }
        if (bountyTier != null) s.sendMessage("§6[TDM] Bounty tier: §e" + tierName(bountyTier));
        s.sendMessage("§c§l[TDM] §eStill standing:");
        for (String t : teamsStillIn()) {
            Team team = teamManager.getTeamByName(t);
            if (team == null) continue;
            List<String> names = new ArrayList<>();
            for (UUID id : team.getMembers()) if (alive.contains(id)) names.add(Bukkit.getOfflinePlayer(id).getName());
            s.sendMessage("  " + team.getColorCode() + team.getName() + " §7(" + names.size() + "): §f" + String.join(", ", names));
        }
        if (!outOrder.isEmpty()) {
            s.sendMessage("§7Out so far:");
            for (int i = 0; i < outOrder.size(); i++) {
                Team team = teamManager.getTeamByName(outOrder.get(i));
                int place = startingTeams.size() - i;
                if (team != null) s.sendMessage("  §6" + PLACE[Math.min(place - 1, PLACE.length - 1)] + ": " + team.getColorCode() + team.getName());
            }
        }
    }

    private int[] rewards() {
        return new int[]{
                plugin.getConfig().getInt("rewards.ffa.1st", 125),
                plugin.getConfig().getInt("rewards.ffa.2nd", 100),
                plugin.getConfig().getInt("rewards.ffa.3rd", 75),
                plugin.getConfig().getInt("rewards.ffa.4th", 50),
                plugin.getConfig().getInt("rewards.ffa.5th", 25)};
    }

    private String rewardsLine() {
        int[] r = rewards();
        return "§e1st §a+" + r[0] + " §7| §e2nd §a+" + r[1] + " §7| §e3rd §a+" + r[2] + " §7| §e4th §a+" + r[3] + " §7| §e5th §a+" + r[4];
    }

    // ---------------- Bounty tier ----------------

    private List<PlayerTier> bountyOrder() {
        List<PlayerTier> order = new ArrayList<>();
        for (String name : plugin.getConfig().getStringList("ffa.bounty-order")) {
            try { order.add(PlayerTier.valueOf(name.trim().toUpperCase())); } catch (IllegalArgumentException ignored) { }
        }
        if (order.isEmpty()) order = List.of(PlayerTier.CAPTAIN, PlayerTier.C, PlayerTier.A, PlayerTier.B);
        return order;
    }

    private void nextBounty() {
        if (!active) return;
        List<PlayerTier> order = bountyOrder();
        bountyIndex = (bountyIndex + 1) % order.size();
        bountyTier = order.get(bountyIndex);
        String name = tierName(bountyTier);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§lBOUNTY: " + name, "§eThey're glowing - kills worth §a+" + bountyBonus() + " §emore", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        }
        Bukkit.broadcastMessage("§6§l[TDM] §eBounty tier is now §6" + name + "§e! They glow for the next "
                + (rotateSeconds() / 60) + " minutes and each kill on them pays §a+" + bountyBonus() + " §eon top.");
    }

    private static String tierName(PlayerTier t) {
        return t == PlayerTier.CAPTAIN ? "CAPTAINS" : "TIER " + t.name();
    }

    /** True for players still standing in the deathmatch whose tier is the current bounty - they glow. */
    public boolean shouldGlow(Player p) {
        if (!active || bountyTier == null || !alive.contains(p.getUniqueId())) return false;
        return tierOf(p.getUniqueId()) == bountyTier;
    }

    private PlayerTier tierOf(UUID id) {
        if (plugin instanceof me.moneysmp.MoneySMP smp && smp.getTierManager() != null) {
            PlayerTier t = smp.getTierManager().getTier(id);
            Team team = teamManager.getTeamByPlayer(id);
            if (team != null && team.isCaptain(id)) return PlayerTier.CAPTAIN;
            return t;
        }
        return PlayerTier.NONE;
    }

    /**
     * Extra credits for killing this player during the deathmatch (on top of the normal kill reward).
     * Call BEFORE knocking the victim out. Returns 0 if the victim isn't standing in a deathmatch.
     */
    public int extraKillReward(UUID victim) {
        if (!isAlive(victim)) return 0;
        int extra = killBonus();
        if (bountyTier != null && tierOf(victim) == bountyTier) extra += bountyBonus();
        return extra;
    }

    public boolean isBounty(UUID victim) {
        return isAlive(victim) && bountyTier != null && tierOf(victim) == bountyTier;
    }

    private int killBonus() { return plugin.getConfig().getInt("ffa.kill-bonus", 20); }
    private int bountyBonus() { return plugin.getConfig().getInt("ffa.bounty-bonus", 20); }
    private int rotateSeconds() { return Math.max(30, plugin.getConfig().getInt("ffa.bounty-rotate-seconds", 240)); }
}
