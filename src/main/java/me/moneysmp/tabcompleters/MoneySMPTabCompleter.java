package me.moneysmp.tabcompleters;

import me.moneysmp.managers.EventManager;
import me.moneysmp.managers.TeamManager;
import me.moneysmp.managers.TierManager;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MoneySMPTabCompleter implements TabCompleter {
    private final TierManager tierManager;
    private final TeamManager teamManager;
    private final EventManager eventManager;
    private final me.moneysmp.managers.BannedItemManager bannedItemManager;

    public MoneySMPTabCompleter(TierManager tierManager, TeamManager teamManager, EventManager eventManager,
                                me.moneysmp.managers.BannedItemManager bannedItemManager) {
        this.tierManager = tierManager;
        this.teamManager = teamManager;
        this.eventManager = eventManager;
        this.bannedItemManager = bannedItemManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("smp")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("gui", "phase", "reload", "reset", "save"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("phase")) {
                candidates.addAll(Arrays.asList("draft", "grace", "pvp", "finale"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                candidates.add("confirm");
            }
        } else if (command.getName().equalsIgnoreCase("bingo")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("start", "stop"));
            }
        } else if (command.getName().equalsIgnoreCase("airdrop")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("common", "rare", "legendary", "random"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("random")) {
                candidates.addAll(Arrays.asList("common", "rare", "legendary"));
            }
        } else if (command.getName().equalsIgnoreCase("koth")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("start", "stop"));
            }
        } else if (command.getName().equalsIgnoreCase("gladiator")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("start", "reward"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
                candidates.addAll(Arrays.asList("C", "B", "A", "CAPTAIN"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    candidates.add(player.getName());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("reward")) {
                candidates.addAll(Arrays.asList("C", "B", "A", "CAPTAIN"));
            }
        } else if (command.getName().equalsIgnoreCase("bid")) {
            if (args.length == 1) {
                if (sender.hasPermission("moneysmp.admin")) {
                    candidates.add("nominate");
                    candidates.add("cancel");
                }
                candidates.addAll(Arrays.asList("10", "25", "50", "100"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("nominate") && sender.hasPermission("moneysmp.admin")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerTier tier = tierManager.getTier(player.getUniqueId());
                    if (tier == PlayerTier.A || tier == PlayerTier.B || tier == PlayerTier.C) {
                        if (teamManager.getTeamByPlayer(player.getUniqueId()) == null) {
                            candidates.add(player.getName());
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("tier")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("set", "remove"));
            } else if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    candidates.add(player.getName());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                candidates.addAll(Arrays.asList("A", "B", "C"));
            }
        } else if (command.getName().equalsIgnoreCase("team")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("list", "info", "rename", "revive"));
                if (sender.hasPermission("moneysmp.admin")) {
                    candidates.addAll(Arrays.asList("create", "remove", "disband"));
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                for (Team team : teamManager.getTeams()) {
                    candidates.add(team.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("revive")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    candidates.add(player.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                candidates.addAll(Arrays.asList("TeamRed", "TeamYellow", "TeamBlue", "TeamPurple", "TeamGreen"));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (teamManager.getTeamByPlayer(player.getUniqueId()) == null) {
                        candidates.add(player.getName());
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (teamManager.getTeamByPlayer(player.getUniqueId()) != null) {
                        candidates.add(player.getName());
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("disband")) {
                for (Team team : teamManager.getTeams()) {
                    candidates.add(team.getName());
                }
            }
        } else if (command.getName().equalsIgnoreCase("credits")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("give", "take", "set", "balance"));
            } else if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    candidates.add(player.getName());
                }
            } else if (args.length == 3 && !args[0].equalsIgnoreCase("balance")) {
                candidates.addAll(Arrays.asList("25", "50", "100", "250", "500"));
            } else if (args.length == 4 && !args[0].equalsIgnoreCase("balance")) {
                candidates.addAll(Arrays.asList("game", "draft"));
            }
        } else if (command.getName().equalsIgnoreCase("banneditems")) {
            if (args.length == 1) {
                candidates.addAll(Arrays.asList("gui", "list"));
                if (sender.hasPermission("moneysmp.admin")) {
                    candidates.addAll(Arrays.asList("add", "remove", "hand"));
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("add") && sender.hasPermission("moneysmp.admin")) {
                for (org.bukkit.Material mat : org.bukkit.Material.values()) {
                    if (mat.isItem()) {
                        candidates.add(mat.name());
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("remove") && sender.hasPermission("moneysmp.admin")) {
                if (bannedItemManager != null) {
                    for (org.bukkit.Material mat : bannedItemManager.getBannedItems().keySet()) {
                        candidates.add(mat.name());
                    }
                }
            }
        }

        List<String> completions = new ArrayList<>();
        String currentArg = args[args.length - 1];
        StringUtil.copyPartialMatches(currentArg, candidates, completions);
        Collections.sort(completions);
        return completions;
    }
}
