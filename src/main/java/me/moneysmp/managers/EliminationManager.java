package me.moneysmp.managers;

import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EliminationManager {
    private final JavaPlugin plugin;
    private final Set<UUID> eliminatedPlayers = new HashSet<>();

    public EliminationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEliminated(UUID uuid) {
        return eliminatedPlayers.contains(uuid);
    }

    public void setEliminated(UUID uuid, boolean eliminated) {
        if (eliminated) {
            eliminatedPlayers.add(uuid);
        } else {
            eliminatedPlayers.remove(uuid);
        }
    }

    public Set<UUID> getEliminatedPlayers() {
        return Collections.unmodifiableSet(eliminatedPlayers);
    }

    public void eliminate(Player player, Team team) {
        if (player == null) return;
        eliminatedPlayers.add(player.getUniqueId());

        if (plugin.getConfig().getBoolean("elimination.lightning-on-death", true)) {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.0f);
        }

        String teamName = (team != null) ? team.getColorCode() + team.getName() : "§7No Team";
        Bukkit.broadcastMessage("§4§l[ELIMINATED] §e" + player.getName() + " §c(" + teamName + "§c) has been eliminated from the tournament!");
    }

    public boolean revive(Player target, Player captain, Team team, int cost) {
        if (target == null || team == null || captain == null) return false;
        if (!eliminatedPlayers.contains(target.getUniqueId())) return false;

        eliminatedPlayers.remove(target.getUniqueId());
        target.setGameMode(GameMode.SURVIVAL);
        target.teleport(captain.getLocation());

        target.sendTitle("§a§lREVIVED!", "§eCaptain " + captain.getName() + " bought you back!", 10, 70, 20);
        target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }

        Bukkit.broadcastMessage("§a§l[REVIVED] §eCaptain " + captain.getName() + " §arevived §e" +
                target.getName() + " §aback into " + team.getColorCode() + team.getName() + " §afor §e" + cost + " Game Credits§a!");
        return true;
    }

    public void clearAll() {
        eliminatedPlayers.clear();
    }
}
