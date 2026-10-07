package me.moneysmp.managers;

import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DisplayManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final PhaseManager phaseManager;

    public DisplayManager(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager, TierManager tierManager, PhaseManager phaseManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.phaseManager = phaseManager;
    }

    public void updateAllDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
            updateScoreboard(player);
            cleanGlowEffects(player);
        }
    }

    public void cleanGlowEffects(Player player) {
        // Strip lingering global glowing flags to ensure no white outlines render
        if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
            player.removePotionEffect(PotionEffectType.GLOWING);
        }
        if (player.isGlowing()) {
            player.setGlowing(false);
        }
    }

    public void updateTabList(Player player) {
        PlayerTier tier = tierManager.getTier(player.getUniqueId());
        Team team = teamManager.getTeamByPlayer(player.getUniqueId());

        String teamColor = (team != null) ? team.getColorCode() : "§7";
        String teamDisplay = (team != null) ? teamColor + "[" + team.getName() + "]" : "§7[No Team]";
        String isCaptainHighlight = (team != null && team.isCaptain(player.getUniqueId())) ? "§e★ " : "";

        String tabName = tier.getPrefix() + " " + teamDisplay + " " + isCaptainHighlight + teamColor + player.getName();
        player.setPlayerListName(tabName);

        player.setPlayerListHeader("§6§l★ WILLY MONEY SMP ★\n§7Phase: " + phaseManager.getCurrentPhase().getDisplayName() + "\n");
        player.setPlayerListFooter("\n§eOnline Players: §f" + Bukkit.getOnlinePlayers().size() + " §8| §ewillymoneysmp.net");
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        if (board == manager.getMainScoreboard() || board.getObjective("WillySMP") == null) {
            board = manager.getNewScoreboard();
            Objective obj = board.registerNewObjective("WillySMP", Criteria.DUMMY, "§6§lWILLY MONEY SMP");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            obj.getScore("§7--------------------").setScore(10);
            registerLine(board, obj, "line_phase", "§fPhase: ", 9, "§a");
            registerLine(board, obj, "line_tier", "§fTier: ", 8, "§b");
            registerLine(board, obj, "line_team", "§fTeam: ", 7, "§c");
            registerLine(board, obj, "line_capt", "§fCaptain: ", 6, "§d");
            registerLine(board, obj, "line_draft", "§fDraft Purse: ", 5, "§e");
            registerLine(board, obj, "line_game", "§fGame Balance: ", 4, "§f");
            obj.getScore("§7-------------------").setScore(3);
            obj.getScore("§ewillymoneysmp.net").setScore(2);

            player.setScoreboard(board);
        }

        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        PlayerTier tier = tierManager.getTier(player.getUniqueId());

        String teamColor = (team != null) ? team.getColorCode() : "§c";
        String captainName = "None";
        int draftCredits = 0;
        int gameCredits = creditManager.getGameCredits(player.getUniqueId());

        org.bukkit.scoreboard.Team sbTeam = board.getTeam("tm_tag");
        if (team != null) {
            Player capPlayer = Bukkit.getPlayer(team.getCaptain());
            captainName = (capPlayer != null) ? capPlayer.getName() : "Offline";
            draftCredits = creditManager.getDraftCredits(team.getCaptain());

            // Set up clean teammate name tag coloring
            if (sbTeam == null) {
                sbTeam = board.registerNewTeam("tm_tag");
            }
            sbTeam.color(getNamedColor(team.getName()));

            Set<String> currentTeammates = new HashSet<>();
            for (UUID memberUuid : team.getMembers()) {
                Player teammate = Bukkit.getPlayer(memberUuid);
                if (teammate != null) {
                    currentTeammates.add(teammate.getName());
                }
            }

            for (String entry : new HashSet<>(sbTeam.getEntries())) {
                if (!currentTeammates.contains(entry)) {
                    sbTeam.removeEntry(entry);
                }
            }

            for (String name : currentTeammates) {
                if (!sbTeam.hasEntry(name)) {
                    sbTeam.addEntry(name);
                }
            }
        } else {
            if (sbTeam != null) {
                for (String entry : new HashSet<>(sbTeam.getEntries())) {
                    sbTeam.removeEntry(entry);
                }
            }
        }

        int maxDraft = plugin != null ? plugin.getConfig().getInt("starting-draft-credits", 400) : 400;

        setLineText(board, "line_phase", "§fPhase: " + phaseManager.getCurrentPhase().getDisplayName());
        setLineText(board, "line_tier", "§fTier: " + tier.getPrefix());
        setLineText(board, "line_team", "§fTeam: " + (team != null ? teamColor + team.getName() : "§cNone"));
        setLineText(board, "line_capt", "§fCaptain: §e★ " + captainName);
        setLineText(board, "line_draft", "§fDraft Purse: §a" + draftCredits + " / " + maxDraft);
        setLineText(board, "line_game", "§fGame Balance: §b" + gameCredits + " Credits");
    }

    private NamedTextColor getNamedColor(String teamName) {
        String lower = teamName.toLowerCase();
        if (lower.contains("red")) return NamedTextColor.RED;
        if (lower.contains("blue")) return NamedTextColor.BLUE;
        if (lower.contains("yellow")) return NamedTextColor.YELLOW;
        if (lower.contains("green")) return NamedTextColor.GREEN;
        if (lower.contains("purple")) return NamedTextColor.LIGHT_PURPLE;
        if (lower.contains("gold") || lower.contains("orange")) return NamedTextColor.GOLD;
        if (lower.contains("aqua") || lower.contains("cyan")) return NamedTextColor.AQUA;
        return NamedTextColor.WHITE;
    }

    private void registerLine(Scoreboard board, Objective obj, String teamKey, String defaultPrefix, int score, String entryKey) {
        org.bukkit.scoreboard.Team lineTeam = board.registerNewTeam(teamKey);
        lineTeam.addEntry(entryKey);
        lineTeam.setPrefix(defaultPrefix);
        obj.getScore(entryKey).setScore(score);
    }

    private void setLineText(Scoreboard board, String teamKey, String text) {
        org.bukkit.scoreboard.Team lineTeam = board.getTeam(teamKey);
        if (lineTeam != null) {
            lineTeam.setPrefix(text);
        }
    }
}
