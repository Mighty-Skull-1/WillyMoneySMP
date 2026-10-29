package me.moneysmp.managers;

import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Free Fall placings. A team stays in while at least one member is alive. When its last member is
 * eliminated it locks in its place (first team out = last place). The last team standing wins.
 * The knock-out order is saved to finale.yml so a restart mid-finale keeps the placings.
 */
public class FinaleManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final EliminationManager eliminationManager;
    private final CreditManager creditManager;
    private final File file;
    private final List<String> outOrder = new ArrayList<>();   // lower-case team names, first out first
    private boolean finished = false;

    public FinaleManager(JavaPlugin plugin, TeamManager teamManager, EliminationManager eliminationManager, CreditManager creditManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.eliminationManager = eliminationManager;
        this.creditManager = creditManager;
        this.file = new File(plugin.getDataFolder(), "finale.yml");
        load();
    }

    public boolean isFinished() { return finished; }

    public boolean isAlive(Team team) {
        if (team == null || team.getMembers().isEmpty()) return false;
        for (UUID m : team.getMembers()) if (!eliminationManager.isEliminated(m)) return true;
        return false;
    }

    public List<Team> aliveTeams() {
        List<Team> list = new ArrayList<>();
        for (Team t : teamManager.getTeams()) if (isAlive(t)) list.add(t);
        list.sort((a, b) -> {
            int d = Integer.compare(teamCredits(b), teamCredits(a));
            return d != 0 ? d : a.getName().compareToIgnoreCase(b.getName());
        });
        return list;
    }

    /** Called after a player is eliminated during the finale. Locks in the team's place if it is wiped out. */
    public void checkTeam(Team team) {
        if (team == null || finished) return;
        String key = team.getName().toLowerCase();
        if (isAlive(team) || outOrder.contains(key)) return;
        outOrder.add(key);
        save();

        List<Team> alive = aliveTeams();
        int place = alive.size() + 1;
        String name = team.getColorCode() + team.getName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(name + " §c§lELIMINATED", "§eThey finish in §6§l" + ordinal(place) + " place", 10, 70, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.7f, 1.2f);
        }
        Bukkit.broadcastMessage("§4§l[FREE FALL] " + name + " §chas been wiped out and finishes §6§l" + ordinal(place)
                + " place§c! §7(" + alive.size() + " team" + (alive.size() == 1 ? "" : "s") + " left)");

        if (alive.size() == 1 && teamManager.getTeams().size() > 1) crownWinner(alive.get(0));
    }

    private void crownWinner(Team winner) {
        finished = true;
        save();
        freezeBorders();
        String name = winner.getColorCode() + winner.getName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(name + " §6§lWINS!", "§eLast team standing - Willy Money SMP champions!", 10, 140, 30);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
        }
        broadcastStandings("§6§l  WILLY MONEY SMP - FINAL STANDINGS");
        Bukkit.broadcastMessage("§e" + name + " §eis the last team standing and wins Willy Money SMP!");
    }

    /** Admin ends the finale early: teams still alive are placed above every knocked-out team (by credits). */
    public void endNow() {
        finished = true;
        save();
        freezeBorders();
        List<Team> s = standings();
        if (s.isEmpty()) { Bukkit.broadcastMessage("§6§l[FREE FALL] §eThe tournament is over - no teams to rank."); return; }
        Team winner = s.get(0);
        String name = winner.getColorCode() + winner.getName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(name + " §6§lWINS!", "§eWilly Money SMP champions!", 10, 140, 30);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        broadcastStandings("§6§l  WILLY MONEY SMP - FINAL STANDINGS");
        if (aliveTeams().size() > 1) Bukkit.broadcastMessage("§7(Ended by an admin: teams still standing are ranked by credits.)");
    }

    /** Alive teams first (by credits), then knocked-out teams - last out = best place. */
    public List<Team> standings() {
        List<Team> list = new ArrayList<>(aliveTeams());
        for (int i = outOrder.size() - 1; i >= 0; i--) {
            Team t = teamManager.getTeamByName(outOrder.get(i));
            if (t != null && !list.contains(t)) list.add(t);
        }
        for (Team t : teamManager.getTeams()) if (!list.contains(t)) list.add(t);   // anything untracked goes last
        return list;
    }

    public void sendStandings(org.bukkit.command.CommandSender to) {
        to.sendMessage("§6§m------------------------------------------");
        to.sendMessage("§6§l  FREE FALL STANDINGS" + (finished ? " §7(final)" : " §7(live)"));
        List<Team> s = standings(); List<Team> alive = aliveTeams();
        for (int i = 0; i < s.size(); i++) {
            Team t = s.get(i);
            String status = alive.contains(t) ? "§a" + aliveCount(t) + " alive" : "§cout";
            to.sendMessage("  §e" + ordinal(i + 1) + " §r" + t.getColorCode() + t.getName() + " §8- " + status);
        }
        to.sendMessage("§6§m------------------------------------------");
    }

    private void broadcastStandings(String header) {
        Bukkit.broadcastMessage("§6§m------------------------------------------");
        Bukkit.broadcastMessage(header);
        String[] medals = {"§6§l1st", "§f§l2nd", "§c§l3rd"};
        List<Team> s = standings();
        for (int i = 0; i < s.size(); i++) {
            Team t = s.get(i);
            String place = i < 3 ? medals[i] : "§7" + ordinal(i + 1);
            Bukkit.broadcastMessage("  " + place + " §r" + t.getColorCode() + t.getName());
        }
        Bukkit.broadcastMessage("§6§m------------------------------------------");
    }

    public void reset() { outOrder.clear(); finished = false; save(); }

    private int aliveCount(Team t) {
        int n = 0;
        for (UUID m : t.getMembers()) if (!eliminationManager.isEliminated(m)) n++;
        return n;
    }

    private int teamCredits(Team team) {
        int total = 0;
        for (UUID m : team.getMembers()) total += creditManager.getGameCredits(m);
        return total;
    }

    private void freezeBorders() {
        for (World w : Bukkit.getWorlds()) { WorldBorder b = w.getWorldBorder(); b.setSize(b.getSize()); }
    }

    private static String ordinal(int n) {
        int m100 = n % 100;
        if (m100 >= 11 && m100 <= 13) return n + "th";
        return n + switch (n % 10) { case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th"; };
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        outOrder.addAll(y.getStringList("out-order"));
        finished = y.getBoolean("finished", false);
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("out-order", outOrder);
        y.set("finished", finished);
        try { y.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save finale.yml: " + e.getMessage()); }
    }
}
