package me.moneysmp.listeners;

import me.moneysmp.managers.*;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.SMPPhase;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillEventListener implements Listener {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final PhaseManager phaseManager;
    private final EventManager eventManager;
    private final EliminationManager eliminationManager;
    private final ShopManager shopManager;
    private final SMPGuiManager smpGuiManager;

    private final Map<UUID, Boolean> trackerModes = new HashMap<>();

    public KillEventListener(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager,
                             TierManager tierManager, PhaseManager phaseManager, EventManager eventManager,
                             EliminationManager eliminationManager, ShopManager shopManager, SMPGuiManager smpGuiManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.phaseManager = phaseManager;
        this.eventManager = eventManager;
        this.eliminationManager = eliminationManager;
        this.shopManager = shopManager;
        this.smpGuiManager = smpGuiManager;
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null || attacker.equals(victim)) return;

        // GRACE PERIOD BLOCK
        if (!phaseManager.isPvPAllowed()) {
            event.setCancelled(true);
            attacker.sendMessage("§cPvP is disabled during " + phaseManager.getCurrentPhase().getDisplayName() + "!");
            return;
        }

        Team attackerTeam = teamManager.getTeamByPlayer(attacker.getUniqueId());
        Team victimTeam = teamManager.getTeamByPlayer(victim.getUniqueId());

        // FRIENDLY FIRE BLOCK
        if (attackerTeam != null && victimTeam != null && attackerTeam.equals(victimTeam)) {
            event.setCancelled(true);
            attacker.sendMessage("§cYou cannot hit your teammate!");
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Team victimTeam = teamManager.getTeamByPlayer(victim.getUniqueId());

        // Elimination handling in PVP and FINALE phases
        if (eliminationManager != null && plugin.getConfig().getBoolean("elimination.enabled", true)) {
            SMPPhase phase = phaseManager.getCurrentPhase();
            if (phase == SMPPhase.PVP || phase == SMPPhase.FINALE) {
                eliminationManager.eliminate(victim, victimTeam);
            }
        }

        if (killer == null || killer.equals(victim)) return;

        Team killerTeam = teamManager.getTeamByPlayer(killer.getUniqueId());

        if (killerTeam != null && victimTeam != null && killerTeam.equals(victimTeam)) {
            killer.sendMessage("§c[Willy Money SMP] Team Kill! No credits rewarded.");
            return;
        }

        if (killerTeam == null || victimTeam == null) return;

        PlayerTier victimTier = tierManager.getTier(victim.getUniqueId());
        int rewardPoints = 5;

        if (victimTeam.isCaptain(victim.getUniqueId()) || victimTier == PlayerTier.CAPTAIN) {
            rewardPoints = plugin != null ? plugin.getConfig().getInt("rewards.captain-kill", 25) : 25;
        } else if (victimTier == PlayerTier.A) {
            rewardPoints = plugin != null ? plugin.getConfig().getInt("rewards.tier-a-kill", 15) : 15;
        } else if (victimTier == PlayerTier.B) {
            rewardPoints = plugin != null ? plugin.getConfig().getInt("rewards.tier-b-kill", 10) : 10;
        } else if (victimTier == PlayerTier.C) {
            rewardPoints = plugin != null ? plugin.getConfig().getInt("rewards.tier-c-kill", 10) : 10;
        }

        creditManager.addGameCredits(killer.getUniqueId(), rewardPoints);
        killer.sendMessage("§a+" + rewardPoints + " Game Credits for eliminating " + victim.getName() + " (" + victimTier.name() + " Tier)!");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (eliminationManager != null && eliminationManager.isEliminated(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§c§l[ELIMINATED] §eYou are in Spectator mode! Your team captain can revive you with §f/team revive " + player.getName());
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.contains("Bingo")) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(ShopManager.SHOP_TITLE)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && shopManager != null) {
                shopManager.handlePurchase(player, event.getRawSlot());
            }
            return;
        }

        if (title.equals(SMPGuiManager.GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && smpGuiManager != null) {
                smpGuiManager.handleClick(player, event.getRawSlot(), event.getClick());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Bingo") || title.equals(ShopManager.SHOP_TITLE) || title.equals(SMPGuiManager.GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAirdropInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CHEST) return;

        if (eventManager != null) {
            EventManager.AirdropData data = eventManager.claimAirdrop(event.getClickedBlock().getLocation());
            if (data != null) {
                Player player = event.getPlayer();
                creditManager.addGameCredits(player.getUniqueId(), data.getCreditsReward());
                Bukkit.broadcastMessage("§6[AIRDROP] §e" + player.getName() + " opened the " + data.getTitle() + " §eand earned §a+" + data.getCreditsReward() + " Game Credits§e!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        }
    }

    @EventHandler
    public void onRadarInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.COMPASS) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().contains("Team Tracker")) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            boolean mode = trackerModes.getOrDefault(player.getUniqueId(), true);
            mode = !mode;
            trackerModes.put(player.getUniqueId(), mode);

            Team playerTeam = teamManager.getTeamByPlayer(player.getUniqueId());
            if (playerTeam == null) {
                player.sendMessage("§cYou must be on a team to use the Team Tracker!");
                return;
            }

            Player target = null;
            double closestDistance = Double.MAX_VALUE;

            if (mode) {
                for (UUID memberUuid : playerTeam.getMembers()) {
                    if (memberUuid.equals(player.getUniqueId())) continue;
                    Player member = player.getServer().getPlayer(memberUuid);
                    if (member != null && member.getWorld().equals(player.getWorld())) {
                        double dist = player.getLocation().distance(member.getLocation());
                        if (dist < closestDistance) {
                            closestDistance = dist;
                            target = member;
                        }
                    }
                }
                if (target != null) {
                    player.setCompassTarget(target.getLocation());
                    player.sendMessage("§a[Tracker] Compass pointing to nearest Teammate: §e" + target.getName() + " §7(" + (int) closestDistance + "m)");
                } else {
                    player.sendMessage("§c[Tracker] No online teammates found nearby in this dimension.");
                }
            } else {
                for (Player online : player.getServer().getOnlinePlayers()) {
                    Team targetTeam = teamManager.getTeamByPlayer(online.getUniqueId());
                    if (targetTeam != null && !targetTeam.equals(playerTeam) && targetTeam.isCaptain(online.getUniqueId())) {
                        if (online.getWorld().equals(player.getWorld())) {
                            double dist = player.getLocation().distance(online.getLocation());
                            if (dist < closestDistance) {
                                closestDistance = dist;
                                target = online;
                            }
                        }
                    }
                }
                if (target != null) {
                    player.setCompassTarget(target.getLocation());
                    player.sendMessage("§c[Tracker] Compass pointing to Enemy Captain: §e" + target.getName() + " §7(" + (int) closestDistance + "m)");
                } else {
                    player.sendMessage("§c[Tracker] No online enemy captains found nearby.");
                }
            }
        }
    }
}
