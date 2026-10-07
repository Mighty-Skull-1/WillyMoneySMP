package me.moneysmp.managers;

import me.moneysmp.MoneySMP;
import me.moneysmp.models.SMPPhase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SMPGuiManager {
    public static final String GUI_TITLE = "§6§lMoney SMP Control Panel";

    private final JavaPlugin plugin;
    private final PhaseManager phaseManager;
    private final EventManager eventManager;
    private final TeamManager teamManager;
    private final EliminationManager eliminationManager;
    private final DataManager dataManager;

    public SMPGuiManager(JavaPlugin plugin, PhaseManager phaseManager, EventManager eventManager,
                         TeamManager teamManager, EliminationManager eliminationManager, DataManager dataManager) {
        this.plugin = plugin;
        this.phaseManager = phaseManager;
        this.eventManager = eventManager;
        this.teamManager = teamManager;
        this.eliminationManager = eliminationManager;
        this.dataManager = dataManager;
    }

    public void openControlPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fill background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, false);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        SMPPhase currentPhase = phaseManager.getCurrentPhase();

        // Slot 10: Draft Phase
        inv.setItem(10, createPhaseItem(
                Material.GOLD_INGOT,
                "§e§lPhase: Draft",
                "§7Captains bid on players using draft credits.",
                currentPhase == SMPPhase.DRAFT
        ));

        // Slot 11: Grace Period
        inv.setItem(11, createPhaseItem(
                Material.SHIELD,
                "§a§lPhase: Grace Period",
                "§7PvP is disabled. Gather gear and build bases.",
                currentPhase == SMPPhase.GRACE
        ));

        // Slot 12: PvP Phase
        inv.setItem(12, createPhaseItem(
                Material.DIAMOND_SWORD,
                "§c§lPhase: PvP Enabled",
                "§7Combat enabled & eliminations active!",
                currentPhase == SMPPhase.PVP
        ));

        // Slot 13: Finale Showdown
        inv.setItem(13, createPhaseItem(
                Material.NETHERITE_SWORD,
                "§4§lPhase: Finale Showdown",
                "§7Final battle for victory with permadeath!",
                currentPhase == SMPPhase.FINALE
        ));

        // Slot 16: Tournament Stats / Overview
        boolean kothOn = eventManager != null && eventManager.isKothActive();
        boolean bingoOn = eventManager != null && eventManager.isBingoActive();
        int teamCount = teamManager != null ? teamManager.getTeams().size() : 0;
        int elimCount = eliminationManager != null ? eliminationManager.getEliminatedPlayers().size() : 0;

        List<String> statsLore = Arrays.asList(
                "§8-------------------------",
                "§7Current Phase: " + currentPhase.getDisplayName(),
                "§7Total Teams: §e" + teamCount,
                "§7Online Players: §a" + Bukkit.getOnlinePlayers().size(),
                "§7Eliminated Players: §c" + elimCount,
                "§7KOTH Event: " + (kothOn ? "§aActive" : "§7Idle"),
                "§7Bingo Event: " + (bingoOn ? "§aActive" : "§7Idle"),
                "§8-------------------------",
                "§eClick to refresh overview"
        );
        inv.setItem(16, createItem(Material.NETHER_STAR, "§6§lTournament Status", statsLore, true));

        // Slot 18: King of the Hill (KOTH)
        List<String> kothLore = Arrays.asList(
                "§8-------------------------",
                "§7Status: " + (kothOn ? "§aActive (Running)" : "§cInactive"),
                "§7Spawns particle zone & timer at target location.",
                "§8-------------------------",
                "§e▶ Left-Click: §aStart KOTH at your position",
                "§c▶ Right-Click: §cStop active KOTH"
        );
        inv.setItem(18, createItem(Material.BEACON, "§b§lKing of the Hill", kothLore, kothOn));

        // Slot 19: Summon Common Airdrop
        List<String> commonDropLore = Arrays.asList(
                "§8-------------------------",
                "§7Spawns a Common supply drop near an active",
                "§7player with randomized survival & filler loot.",
                "§7Reward: §e0 Game Credits §7(Loot only)",
                "§8-------------------------",
                "§f▶ Click: §aSpawn Common Airdrop"
        );
        inv.setItem(19, createItem(Material.CHEST, "§f§lCommon Airdrop", commonDropLore, false));

        // Slot 20: Summon Rare Airdrop
        List<String> rareDropLore = Arrays.asList(
                "§8-------------------------",
                "§7Spawns a Rare supply drop near an active",
                "§7player with diamonds, gapples & pearls.",
                "§7Reward: §a+5 Game Credits",
                "§8-------------------------",
                "§9▶ Click: §aSpawn Rare Airdrop"
        );
        inv.setItem(20, createItem(Material.ENDER_CHEST, "§9§lRare Airdrop", rareDropLore, false));

        // Slot 21: Summon Legendary Airdrop
        List<String> legDropLore = Arrays.asList(
                "§8-------------------------",
                "§7Spawns a Legendary drop near an active",
                "§7player with totems, scraps & god apples.",
                "§7Reward: §a+15 Game Credits",
                "§8-------------------------",
                "§6▶ Click: §aSpawn Legendary Airdrop"
        );
        inv.setItem(21, createItem(Material.TRAPPED_CHEST, "§6§lLegendary Airdrop", legDropLore, true));

        // Slot 22: Lockout Bingo
        List<String> bingoLore = Arrays.asList(
                "§8-------------------------",
                "§7Status: " + (bingoOn ? "§aActive (Running)" : "§cInactive"),
                "§7Interactive 5x5 Lockout Bingo challenge.",
                "§8-------------------------",
                "§e▶ Left-Click: §aStart Bingo Event",
                "§c▶ Right-Click: §cStop Bingo Event"
        );
        inv.setItem(22, createItem(Material.FILLED_MAP, "§d§lLockout Bingo", bingoLore, bingoOn));

        // Slot 23: Save Tournament Data
        List<String> saveLore = Arrays.asList(
                "§8-------------------------",
                "§7Forces immediate save of all",
                "§7teams, balances, and states to data.yml.",
                "§8-------------------------",
                "§a▶ Click to save data now"
        );
        inv.setItem(23, createItem(Material.ENDER_EYE, "§a§lSave Tournament Data", saveLore, false));

        // Slot 24: Reload Configuration
        List<String> reloadLore = Arrays.asList(
                "§8-------------------------",
                "§7Reloads config.yml and airdrops.yml",
                "§7without restarting the server.",
                "§8-------------------------",
                "§e▶ Click to reload configs"
        );
        inv.setItem(24, createItem(Material.REDSTONE_TORCH, "§c§lReload Configs", reloadLore, false));

        // Slot 25: RESET TOURNAMENT
        List<String> resetLore = Arrays.asList(
                "§8-------------------------",
                "§c§lDANGER: FULL SYSTEM RESET",
                "§7• Restores all players to Survival at spawn",
                "§7• Clears eliminations & revives spectators",
                "§7• Resets draft & game credits to starting values",
                "§7• Disbands all teams & clears player tiers",
                "§7• Stops KOTH, Bingo & Draft events",
                "§7• Wipes and creates fresh data.yml",
                "§8-------------------------",
                "§c▶ Shift + Click: §4§lCONFIRM FULL RESET",
                "§7(Or run command: /smp reset confirm)"
        );
        inv.setItem(25, createItem(Material.TNT, "§c§lRESET TOURNAMENT", resetLore, true));

        // Slot 26: Close Menu
        List<String> closeLore = Collections.singletonList("§7Click to exit control panel.");
        inv.setItem(26, createItem(Material.BARRIER, "§c§lClose Menu", closeLore, false));

        player.openInventory(inv);
    }

    private ItemStack createPhaseItem(Material material, String name, String description, boolean active) {
        List<String> lore = new ArrayList<>();
        lore.add("§8-------------------------");
        if (active) {
            lore.add("§a✔ CURRENTLY ACTIVE");
        } else {
            lore.add("§7Status: §7Inactive");
        }
        lore.add(description);
        lore.add("§8-------------------------");
        if (!active) {
            lore.add("§e▶ Click to switch to this phase");
        }
        return createItem(material, name, lore, active);
    }

    private ItemStack createItem(Material material, String name, List<String> lore, boolean glowing) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            if (glowing) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, int rawSlot, ClickType clickType) {
        if (player == null || rawSlot < 0 || rawSlot >= 27) return;

        switch (rawSlot) {
            case 10: // Draft Phase
                phaseManager.setPhase(SMPPhase.DRAFT);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                openControlPanel(player);
                break;

            case 11: // Grace Period
                phaseManager.setPhase(SMPPhase.GRACE);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                openControlPanel(player);
                break;

            case 12: // PvP Phase
                phaseManager.setPhase(SMPPhase.PVP);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                openControlPanel(player);
                break;

            case 13: // Finale Showdown
                phaseManager.setPhase(SMPPhase.FINALE);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                openControlPanel(player);
                break;

            case 16: // Status Overview Refresh
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                openControlPanel(player);
                break;

            case 18: // KOTH
                if (eventManager != null) {
                    if (clickType.isRightClick()) {
                        if (eventManager.isKothActive()) {
                            eventManager.stopKoth();
                            player.sendMessage("§c[KOTH] Stopped King of the Hill!");
                        } else {
                            player.sendMessage("§c[KOTH] King of the Hill is not running!");
                        }
                    } else {
                        eventManager.startKoth(player.getLocation());
                        player.sendMessage("§a[KOTH] Started King of the Hill at your position!");
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                    openControlPanel(player);
                }
                break;

            case 19: // Common Airdrop
                if (eventManager != null) {
                    eventManager.spawnRandomAirdrop(player.getWorld(), "common");
                    player.sendMessage("§f[AIRDROP] Summoned Common proximity airdrop!");
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.2f);
                }
                break;

            case 20: // Rare Airdrop
                if (eventManager != null) {
                    eventManager.spawnRandomAirdrop(player.getWorld(), "rare");
                    player.sendMessage("§9[AIRDROP] Summoned Rare proximity airdrop!");
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.2f);
                }
                break;

            case 21: // Legendary Airdrop
                if (eventManager != null) {
                    eventManager.spawnRandomAirdrop(player.getWorld(), "legendary");
                    player.sendMessage("§6[AIRDROP] Summoned Legendary proximity airdrop!");
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.2f);
                }
                break;

            case 22: // Lockout Bingo
                if (eventManager != null) {
                    if (clickType.isRightClick()) {
                        if (eventManager.isBingoActive()) {
                            eventManager.stopBingo();
                            player.sendMessage("§c[BINGO] Stopped Lockout Bingo!");
                        } else {
                            player.sendMessage("§c[BINGO] Lockout Bingo is not active!");
                        }
                    } else {
                        eventManager.startBingo();
                        player.sendMessage("§a[BINGO] Started Lockout Bingo!");
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                    openControlPanel(player);
                }
                break;

            case 23: // Force Save Data
                if (dataManager != null) {
                    dataManager.saveData();
                    player.sendMessage("§a[Willy Money SMP] All tournament data successfully saved to data.yml!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                }
                break;

            case 24: // Reload Configs
                if (plugin != null) {
                    plugin.reloadConfig();
                }
                if (eventManager != null) {
                    eventManager.loadAirdropConfig();
                }
                player.sendMessage("§a[Willy Money SMP] Configuration and airdrops reloaded successfully!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                break;

            case 25: // Reset Tournament
                if (clickType.isShiftClick()) {
                    if (plugin instanceof MoneySMP smp) {
                        player.closeInventory();
                        smp.resetTournament(player);
                    }
                } else {
                    player.sendMessage("§c§l[RESET WARNING] §eShift-Click this button or type §f/smp reset confirm §eto completely reset the tournament!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
                break;

            case 26: // Close Menu
                player.closeInventory();
                break;
        }
    }
}
