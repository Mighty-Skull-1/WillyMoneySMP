package me.moneysmp.managers;

import me.moneysmp.models.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class EventManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;

    // KOTH State
    private Location kothLoc = null;
    private int kothTaskId = -1;
    private int kothTimeLeft = 180;
    private final Map<Team, Integer> kothScores = new HashMap<>();
    private ArmorStand kothHologram = null;

    // Bingo State
    private final List<String> taskOrder = new ArrayList<>();
    private final Map<String, Team> bingoBoard = new LinkedHashMap<>();
    private final Map<String, Material> bingoIcons = new LinkedHashMap<>();
    private final Set<String> completedLines = new HashSet<>();
    private boolean bingoActive = false;

    // Airdrop State
    public static class AirdropData {
        private final String title;
        private final int creditsReward;

        public AirdropData(String title, int creditsReward) {
            this.title = title;
            this.creditsReward = creditsReward;
        }

        public String getTitle() { return title; }
        public int getCreditsReward() { return creditsReward; }
    }

    private static class LootCandidate {
        final Material material;
        final int minAmt;
        final int maxAmt;
        final int weight;

        LootCandidate(Material material, int minAmt, int maxAmt, int weight) {
            this.material = material;
            this.minAmt = minAmt;
            this.maxAmt = maxAmt;
            this.weight = weight;
        }
    }

    private final Map<Location, AirdropData> activeAirdrops = new HashMap<>();
    private File airdropFile;
    private FileConfiguration airdropConfig;

    public EventManager(JavaPlugin plugin, TeamManager teamManager, CreditManager creditManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        loadAirdropConfig();
        setupBingoTasks();
    }

    public void loadAirdropConfig() {
        airdropFile = new File(plugin.getDataFolder(), "airdrops.yml");
        if (!airdropFile.exists()) {
            plugin.saveResource("airdrops.yml", false);
        }
        airdropConfig = YamlConfiguration.loadConfiguration(airdropFile);
        if (!airdropConfig.contains("common.loot-pool") || airdropConfig.getInt("common.credits-reward", 10) != 0 || airdropConfig.getInt("rare.credits-reward", 20) != 5) {
            plugin.saveResource("airdrops.yml", true);
            airdropConfig = YamlConfiguration.loadConfiguration(airdropFile);
        }
    }

    // --- KOTH WITH PARTICLES & HOLOGRAM ---
    public void startKoth(Location loc) {
        if (kothTaskId != -1) stopKoth();

        this.kothLoc = loc.getBlock().getLocation().add(0.5, 0, 0.5);
        this.kothTimeLeft = 180;
        this.kothScores.clear();

        World world = loc.getWorld();
        if (world == null) return;

        kothHologram = (ArmorStand) world.spawnEntity(kothLoc.clone().add(0, 2.5, 0), EntityType.ARMOR_STAND);
        kothHologram.setVisible(false);
        kothHologram.setGravity(false);
        kothHologram.setCustomNameVisible(true);
        kothHologram.setMarker(true);
        kothHologram.setInvulnerable(true);
        kothHologram.setPersistent(false);
        kothHologram.setCustomName("§6§lKING OF THE HILL §e(180s)");

        Bukkit.broadcastMessage("§6§l[KOTH EVENT] §eKing of the Hill is active at §f(" +
                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")§e! Stand inside the particle ring!");

        kothTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (kothLoc == null || kothLoc.getWorld() == null) {
                stopKoth();
                return;
            }

            if (kothTimeLeft <= 0) {
                finishKoth();
                return;
            }

            for (int degree = 0; degree < 360; degree += 20) {
                double radians = Math.toRadians(degree);
                double x = kothLoc.getX() + (5.0 * Math.cos(radians));
                double z = kothLoc.getZ() + (5.0 * Math.sin(radians));
                kothLoc.getWorld().spawnParticle(Particle.FLAME, new Location(kothLoc.getWorld(), x, kothLoc.getY() + 0.1, z), 1, 0, 0, 0, 0);
            }

            Set<Team> teamsInZone = new HashSet<>();
            for (Player player : kothLoc.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR) continue;
                if (player.getLocation().distance(kothLoc) <= 5.0) {
                    Team team = teamManager.getTeamByPlayer(player.getUniqueId());
                    if (team != null) {
                        teamsInZone.add(team);
                    }
                }
            }

            Team dominantTeam = null;
            if (teamsInZone.size() == 1) {
                dominantTeam = teamsInZone.iterator().next();
                kothScores.put(dominantTeam, kothScores.getOrDefault(dominantTeam, 0) + 1);
            }

            String capperName;
            if (teamsInZone.isEmpty()) {
                capperName = "§7Uncontested";
            } else if (teamsInZone.size() > 1) {
                capperName = "§c§lCONTESTED";
            } else {
                capperName = dominantTeam.getColorCode() + dominantTeam.getName();
            }

            if (kothHologram != null && kothHologram.isValid()) {
                kothHologram.setCustomName("§6§lKOTH §8| §e" + kothTimeLeft + "s §8| §f" + capperName);
            }

            String action = "§6KOTH Time: §e" + kothTimeLeft + "s §8| §fHolding: " + capperName;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(action));
            }

            kothTimeLeft--;
        }, 0L, 20L);
    }

    private void finishKoth() {
        stopKoth();

        Team winner = null;
        int maxPoints = -1;
        for (Map.Entry<Team, Integer> entry : kothScores.entrySet()) {
            if (entry.getValue() > maxPoints) {
                maxPoints = entry.getValue();
                winner = entry.getKey();
            }
        }

        if (winner != null && maxPoints > 0) {
            int reward = plugin.getConfig().getInt("rewards.koth-win-reward", 50);
            creditManager.addGameCredits(winner.getCaptain(), reward);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§6§lKOTH WINNER", winner.getColorCode() + winner.getName() + " §7(" + maxPoints + " pts) (+" + reward + " Credits)", 10, 80, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            Bukkit.broadcastMessage("§6§l[KOTH FINISHED] " + winner.getColorCode() + winner.getName() + " won the Hill with " + maxPoints + " points! Awarded +" + reward + " Game Credits!");
        } else {
            Bukkit.broadcastMessage("§c[KOTH FINISHED] No winner!");
        }
    }

    public void stopKoth() {
        if (kothTaskId != -1) {
            Bukkit.getScheduler().cancelTask(kothTaskId);
            kothTaskId = -1;
            kothLoc = null;
        }
        if (kothHologram != null) {
            kothHologram.remove();
            kothHologram = null;
        }
    }

    public boolean isKothActive() {
        return kothTaskId != -1;
    }


    // --- RANDOM AIRDROP DISPATCHER (Near a Player) ---
    public void spawnRandomAirdrop(World world, String tier) {
        Random rand = new Random();
        List<Player> eligiblePlayers = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) {
                eligiblePlayers.add(p);
            }
        }

        Location target;
        if (!eligiblePlayers.isEmpty()) {
            // Prefer players in Overworld if available
            List<Player> overworldPlayers = new ArrayList<>();
            for (Player p : eligiblePlayers) {
                if (p.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    overworldPlayers.add(p);
                }
            }

            Player chosenPlayer = !overworldPlayers.isEmpty()
                    ? overworldPlayers.get(rand.nextInt(overworldPlayers.size()))
                    : eligiblePlayers.get(rand.nextInt(eligiblePlayers.size()));

            Location playerLoc = chosenPlayer.getLocation();
            World targetWorld = chosenPlayer.getWorld();

            // Random angle and distance between 50 and 130 blocks away
            double angle = rand.nextDouble() * 2 * Math.PI;
            int distance = 50 + rand.nextInt(80); // 50 to 130 blocks
            int x = playerLoc.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = playerLoc.getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = targetWorld.getHighestBlockYAt(x, z);

            target = new Location(targetWorld, x, Math.max(y + 1, targetWorld.getMinHeight() + 10), z);
        } else {
            // Fallback if no players are online
            if (world == null) world = Bukkit.getWorlds().get(0);
            int x = rand.nextInt(400) - 200;
            int z = rand.nextInt(400) - 200;
            int y = world.getHighestBlockYAt(x, z);
            target = new Location(world, x, y + 1, z);
        }

        spawnTieredAirdrop(target, tier);
    }

    // --- RANDOMIZED AIRDROP LOOT GENERATION ---
    public List<ItemStack> generateRandomAirdropLoot(String selectedTier) {
        List<ItemStack> result = new ArrayList<>();
        List<String> pool = airdropConfig != null ? airdropConfig.getStringList(selectedTier + ".loot-pool") : new ArrayList<>();
        if (pool.isEmpty() && airdropConfig != null) {
            pool = airdropConfig.getStringList(selectedTier + ".items");
        }

        int minItems = airdropConfig != null ? airdropConfig.getInt(selectedTier + ".min-items", 4) : 4;
        int maxItems = airdropConfig != null ? airdropConfig.getInt(selectedTier + ".max-items", 7) : 7;
        if (maxItems < minItems) maxItems = minItems;

        Random random = new Random();
        int totalItemStacks = minItems + random.nextInt((maxItems - minItems) + 1);

        List<LootCandidate> candidates = new ArrayList<>();
        for (String entry : pool) {
            try {
                String[] parts = entry.split(":");
                Material mat = Material.matchMaterial(parts[0]);
                if (mat == null) continue;

                int minAmt = 1;
                int maxAmt = 1;
                if (parts.length > 1) {
                    if (parts[1].contains("-")) {
                        String[] range = parts[1].split("-");
                        minAmt = Integer.parseInt(range[0]);
                        maxAmt = Integer.parseInt(range[1]);
                    } else {
                        minAmt = maxAmt = Integer.parseInt(parts[1]);
                    }
                }

                int weight = 50;
                if (parts.length > 2) {
                    weight = Integer.parseInt(parts[2]);
                }

                candidates.add(new LootCandidate(mat, minAmt, maxAmt, weight));
            } catch (Exception ignored) {}
        }

        if (candidates.isEmpty()) {
            candidates.addAll(getDefaultCandidates(selectedTier));
        }

        Collections.shuffle(candidates);

        int totalWeight = 0;
        for (LootCandidate c : candidates) totalWeight += c.weight;

        for (int i = 0; i < totalItemStacks; i++) {
            if (candidates.isEmpty() || totalWeight <= 0) break;
            int r = random.nextInt(totalWeight);
            int current = 0;
            LootCandidate chosen = candidates.get(0);
            for (LootCandidate c : candidates) {
                current += c.weight;
                if (r < current) {
                    chosen = c;
                    break;
                }
            }

            candidates.remove(chosen);
            totalWeight -= chosen.weight;

            int count = chosen.minAmt;
            if (chosen.maxAmt > chosen.minAmt) {
                count += random.nextInt((chosen.maxAmt - chosen.minAmt) + 1);
            }
            result.add(new ItemStack(chosen.material, Math.max(1, count)));
        }

        return result;
    }

    private List<LootCandidate> getDefaultCandidates(String tier) {
        List<LootCandidate> list = new ArrayList<>();
        if ("legendary".equalsIgnoreCase(tier)) {
            list.add(new LootCandidate(Material.NETHERITE_SCRAP, 1, 2, 20));
            list.add(new LootCandidate(Material.TOTEM_OF_UNDYING, 1, 1, 15));
            list.add(new LootCandidate(Material.DIAMOND, 3, 5, 35));
            list.add(new LootCandidate(Material.GOLDEN_APPLE, 2, 4, 35));
            list.add(new LootCandidate(Material.WIND_CHARGE, 3, 6, 35));
            list.add(new LootCandidate(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 10));
            list.add(new LootCandidate(Material.EXPERIENCE_BOTTLE, 12, 24, 40));
            list.add(new LootCandidate(Material.CROSSBOW, 1, 1, 25));
            list.add(new LootCandidate(Material.OBSIDIAN, 4, 8, 35));
            list.add(new LootCandidate(Material.BLAZE_ROD, 1, 3, 25));
            list.add(new LootCandidate(Material.FIREWORK_ROCKET, 6, 12, 35));
            list.add(new LootCandidate(Material.COBWEB, 2, 6, 25));
            list.add(new LootCandidate(Material.GUNPOWDER, 3, 8, 30));
        } else if ("rare".equalsIgnoreCase(tier)) {
            list.add(new LootCandidate(Material.DIAMOND, 1, 3, 30));
            list.add(new LootCandidate(Material.GOLDEN_APPLE, 1, 2, 30));
            list.add(new LootCandidate(Material.WIND_CHARGE, 2, 4, 35));
            list.add(new LootCandidate(Material.EXPERIENCE_BOTTLE, 6, 12, 40));
            list.add(new LootCandidate(Material.BOW, 1, 1, 25));
            list.add(new LootCandidate(Material.IRON_INGOT, 6, 12, 45));
            list.add(new LootCandidate(Material.OBSIDIAN, 2, 5, 30));
            list.add(new LootCandidate(Material.TNT, 1, 3, 20));
            list.add(new LootCandidate(Material.GOLD_INGOT, 3, 8, 35));
            list.add(new LootCandidate(Material.BONE, 3, 8, 40));
            list.add(new LootCandidate(Material.STRING, 3, 8, 40));
            list.add(new LootCandidate(Material.ROTTEN_FLESH, 4, 10, 35));
            list.add(new LootCandidate(Material.ARROW, 16, 32, 45));
        } else {
            // Common default candidates
            list.add(new LootCandidate(Material.IRON_INGOT, 3, 8, 50));
            list.add(new LootCandidate(Material.GOLDEN_CARROT, 4, 10, 45));
            list.add(new LootCandidate(Material.ARROW, 12, 28, 50));
            list.add(new LootCandidate(Material.EXPERIENCE_BOTTLE, 3, 6, 40));
            list.add(new LootCandidate(Material.GOLDEN_APPLE, 1, 2, 25));
            list.add(new LootCandidate(Material.SHIELD, 1, 1, 30));
            list.add(new LootCandidate(Material.CROSSBOW, 1, 1, 20));
            list.add(new LootCandidate(Material.BOW, 1, 1, 25));
            list.add(new LootCandidate(Material.COAL, 8, 16, 45));
            list.add(new LootCandidate(Material.COOKED_BEEF, 4, 10, 50));
            list.add(new LootCandidate(Material.BREAD, 6, 14, 50));
            list.add(new LootCandidate(Material.ROTTEN_FLESH, 3, 10, 45));
            list.add(new LootCandidate(Material.STICK, 6, 16, 45));
            list.add(new LootCandidate(Material.COBWEB, 1, 4, 35));
            list.add(new LootCandidate(Material.POISONOUS_POTATO, 1, 2, 30));
            list.add(new LootCandidate(Material.FEATHER, 4, 10, 40));
            list.add(new LootCandidate(Material.DIRT, 8, 16, 30));
            list.add(new LootCandidate(Material.FLINT, 2, 6, 40));
            list.add(new LootCandidate(Material.GRAVEL, 6, 14, 35));
            list.add(new LootCandidate(Material.STRING, 2, 6, 40));
            list.add(new LootCandidate(Material.BONE, 2, 6, 40));
            list.add(new LootCandidate(Material.LEATHER, 2, 5, 40));
        }
        return list;
    }

    private int calculateCreditsReward(String tier) {
        if ("common".equalsIgnoreCase(tier)) {
            return airdropConfig != null ? airdropConfig.getInt("common.credits-reward", 0) : 0;
        } else if ("rare".equalsIgnoreCase(tier)) {
            return airdropConfig != null ? airdropConfig.getInt("rare.credits-reward", 5) : 5;
        } else if ("legendary".equalsIgnoreCase(tier)) {
            return airdropConfig != null ? airdropConfig.getInt("legendary.credits-reward", 15) : 15;
        }
        return airdropConfig != null ? airdropConfig.getInt(tier + ".credits-reward", 0) : 0;
    }

    // --- AIRDROP SUMMON WITH RANDOMIZED SCATTERED LOOT ---
    public void spawnTieredAirdrop(Location targetLoc, String tier) {
        String selectedTier = (tier != null) ? tier.toLowerCase() : "common";
        if (airdropConfig != null && !airdropConfig.contains(selectedTier)) selectedTier = "common";

        World world = targetLoc.getWorld();
        if (world == null) return;

        Location dropStart = targetLoc.clone().add(0, 30, 0);
        FallingBlock fallingChest = world.spawnFallingBlock(dropStart, Material.CHEST.createBlockData());
        fallingChest.setDropItem(false);

        String title = airdropConfig != null ? airdropConfig.getString(selectedTier + ".title", "§eSupply Drop") : "§eSupply Drop";
        int creditsReward = calculateCreditsReward(selectedTier);
        List<ItemStack> lootItems = generateRandomAirdropLoot(selectedTier);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!fallingChest.isValid() || fallingChest.isOnGround() || ticks >= 200) {
                    Location landLoc = fallingChest.isValid() ? fallingChest.getLocation() : targetLoc;
                    fallingChest.remove();

                    Block block = landLoc.getBlock();
                    block.setType(Material.CHEST);
                    if (block.getState() instanceof Chest chest) {
                        List<Integer> availableSlots = new ArrayList<>();
                        for (int i = 0; i < 27; i++) availableSlots.add(i);
                        Collections.shuffle(availableSlots);

                        int slotIdx = 0;
                        for (ItemStack is : lootItems) {
                            if (slotIdx >= availableSlots.size()) break;
                            chest.getInventory().setItem(availableSlots.get(slotIdx++), is);
                        }
                    }

                    // Register active airdrop for credit rewards
                    activeAirdrops.put(block.getLocation(), new AirdropData(title, creditsReward));

                    // Smoke flare beacon
                    Block below = block.getRelative(0, -1, 0);
                    if (below.getType() != Material.BEDROCK) {
                        below.setType(Material.HAY_BLOCK);
                    }
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, block.getLocation().add(0.5, 1, 0.5), 60, 0.2, 1.5, 0.2, 0.05);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(title, "§eLanded at §f(" + block.getX() + ", " + block.getZ() + ")", 10, 60, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                    }

                    this.cancel();
                    return;
                }

                // Cloud particle trail while descending
                world.spawnParticle(Particle.CLOUD, fallingChest.getLocation().add(0, 0.5, 0), 4, 0.2, 0.2, 0.2, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 5L);

        Bukkit.broadcastMessage("§6§l[AIRDROP SUMMONED] " + title + " §eis descending at §f(" + targetLoc.getBlockX() + ", " + targetLoc.getBlockZ() + ")§e!");
    }

    public AirdropData claimAirdrop(Location loc) {
        return activeAirdrops.remove(loc);
    }

    // --- INTERACTIVE BINGO 5X5 GUI & AUTOMATION ---
    private void setupBingoTasks() {
        taskOrder.clear();
        bingoBoard.clear();
        bingoIcons.clear();

        addTask("Mine Ancient Debris", Material.ANCIENT_DEBRIS);
        addTask("Tame a Cat", Material.COD);
        addTask("Brew Invisibility", Material.POTION);
        addTask("Craft Diamond Block", Material.DIAMOND_BLOCK);
        addTask("Kill an Enderman", Material.ENDER_EYE);
        addTask("Eat Golden Apple", Material.GOLDEN_APPLE);
        addTask("Obtain Elytra", Material.ELYTRA);
        addTask("Catch a Pufferfish", Material.PUFFERFISH);
        addTask("Ride a Strider", Material.WARPED_FUNGUS_ON_A_STICK);
        addTask("Wear Full Iron", Material.IRON_CHESTPLATE);
        addTask("Smelt Netherite", Material.NETHERITE_SCRAP);
        addTask("Tame a Wolf", Material.BONE);
        addTask("Find a Village", Material.EMERALD);
        addTask("Bake a Cake", Material.CAKE);
        addTask("Craft Enchanting Table", Material.ENCHANTING_TABLE);
        addTask("Obtain Wither Skull", Material.WITHER_SKELETON_SKULL);
        addTask("Brew Strength II", Material.BLAZE_POWDER);
        addTask("Kill a Warden", Material.SCULK_SHRIEKER);
        addTask("Craft Beacon", Material.BEACON);
        addTask("Shear a Sheep", Material.SHEARS);
        addTask("Find Desert Temple", Material.TNT);
        addTask("Kill Blazes", Material.BLAZE_ROD);
        addTask("Mine 64 Coal", Material.COAL);
        addTask("Catch Enchanted Book", Material.ENCHANTED_BOOK);
        addTask("Use Wind Charge", Material.WIND_CHARGE);
    }

    private void addTask(String task, Material icon) {
        taskOrder.add(task);
        bingoBoard.put(task, null);
        bingoIcons.put(task, icon);
    }

    public void startBingo() {
        bingoActive = true;
        completedLines.clear();
        setupBingoTasks();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§lLOCKOUT BINGO", "§eAuto-tracking active! Type /bingo to view board", 10, 80, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        Bukkit.broadcastMessage("§6§l[BINGO] §eInteractive 5x5 Lockout Bingo is now LIVE! Complete tasks automatically to claim them!");
    }

    public void stopBingo() {
        if (!bingoActive) return;
        bingoActive = false;
        Bukkit.broadcastMessage("§c[BINGO] Lockout Bingo event has ended.");
    }

    public boolean isBingoActive() {
        return bingoActive;
    }

    public boolean claimBingo(Player player, String taskName) {
        if (!bingoActive || player == null) return false;
        Team team = teamManager.getTeamByPlayer(player.getUniqueId());
        if (team == null) return false;

        for (Map.Entry<String, Team> entry : bingoBoard.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(taskName) && entry.getValue() == null) {
                entry.setValue(team);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(team.getColorCode() + team.getName() + " §fClaimed Task!", "§e" + entry.getKey() + " §7(Locked Out)", 5, 50, 15);
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
                }

                Bukkit.broadcastMessage("§6[BINGO] " + team.getColorCode() + team.getName() + " §ecompleted: §f" + entry.getKey() + "§e!");

                checkBingoLines(team, player);
                return true;
            }
        }
        return false;
    }

    private void checkBingoLines(Team team, Player player) {
        int[][] lines = {
                // Rows
                {0, 1, 2, 3, 4},
                {5, 6, 7, 8, 9},
                {10, 11, 12, 13, 14},
                {15, 16, 17, 18, 19},
                {20, 21, 22, 23, 24},
                // Columns
                {0, 5, 10, 15, 20},
                {1, 6, 11, 16, 21},
                {2, 7, 12, 17, 22},
                {3, 8, 13, 18, 23},
                {4, 9, 14, 19, 24},
                // Diagonals
                {0, 6, 12, 18, 24},
                {4, 8, 12, 16, 20}
        };

        String[] lineNames = {
                "Row 1", "Row 2", "Row 3", "Row 4", "Row 5",
                "Column 1", "Column 2", "Column 3", "Column 4", "Column 5",
                "Diagonal 1", "Diagonal 2"
        };

        for (int i = 0; i < lines.length; i++) {
            String lineKey = team.getName().toLowerCase() + "_" + lineNames[i];
            if (completedLines.contains(lineKey)) continue;

            boolean lineComplete = true;
            for (int idx : lines[i]) {
                if (idx >= taskOrder.size()) {
                    lineComplete = false;
                    break;
                }
                String task = taskOrder.get(idx);
                Team claimer = bingoBoard.get(task);
                if (claimer == null || !claimer.equals(team)) {
                    lineComplete = false;
                    break;
                }
            }

            if (lineComplete) {
                completedLines.add(lineKey);
                int reward = plugin.getConfig().getInt("rewards.bingo-line-win", 150);
                creditManager.addGameCredits(team.getCaptain(), reward);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle("§6§lBINGO LINE COMPLETED!", team.getColorCode() + team.getName() + " completed " + lineNames[i] + "! (+" + reward + " Credits)", 10, 80, 20);
                    online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
                Bukkit.broadcastMessage("§6§l[BINGO LINE WIN] " + team.getColorCode() + team.getName() +
                        " §ecompleted §f" + lineNames[i] + "§e! Awarded §a+" + reward + " Game Credits §eto Captain!");
            }
        }
    }

    public String getTaskDescription(String task) {
        if (task == null) return "Complete the challenge";
        switch (task) {
            case "Mine Ancient Debris": return "Mine an Ancient Debris block in the Nether";
            case "Tame a Cat": return "Feed raw fish to a stray cat to tame it";
            case "Brew Invisibility": return "Brew and drink an Invisibility potion";
            case "Craft Diamond Block": return "Combine 9 Diamonds in a crafting table";
            case "Kill an Enderman": return "Slay an Enderman in combat";
            case "Eat Golden Apple": return "Consume a Golden or Enchanted Apple";
            case "Obtain Elytra": return "Acquire an Elytra into your inventory";
            case "Catch a Pufferfish": return "Catch a Pufferfish using a Fishing Rod";
            case "Ride a Strider": return "Saddle and ride a Strider over lava";
            case "Wear Full Iron": return "Equip a full set of Iron Armor";
            case "Smelt Netherite": return "Smelt Ancient Debris into Netherite Scrap";
            case "Tame a Wolf": return "Feed bones to a wild wolf to tame it";
            case "Find a Village": return "Locate a village and interact with a villager";
            case "Bake a Cake": return "Craft a Cake (wheat, sugar, milk, egg)";
            case "Craft Enchanting Table": return "Craft an Enchanting Table with obsidian & diamonds";
            case "Obtain Wither Skull": return "Slay a Wither Skeleton and loot its skull";
            case "Brew Strength II": return "Brew and drink a Strength II potion";
            case "Kill a Warden": return "Summon and defeat a Warden in the Deep Dark";
            case "Craft Beacon": return "Craft a Beacon using a Nether Star";
            case "Shear a Sheep": return "Use Shears to collect wool from a sheep";
            case "Find Desert Temple": return "Trigger the pressure plate in a Desert Pyramid";
            case "Kill Blazes": return "Slay a Blaze in a Nether Fortress";
            case "Mine 64 Coal": return "Gather a full stack of 64 Coal";
            case "Catch Enchanted Book": return "Reel in an Enchanted Book while fishing";
            case "Use Wind Charge": return "Launch a Wind Charge in movement or combat";
            default: return "Complete the survival challenge";
        }
    }

    public void openBingoGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lLockout Bingo Board");

        // Fill background with border panes
        ItemStack outerFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta ofMeta = outerFiller.getItemMeta();
        if (ofMeta != null) {
            ofMeta.setDisplayName(" ");
            outerFiller.setItemMeta(ofMeta);
        }

        ItemStack innerBorder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta ibMeta = innerBorder.getItemMeta();
        if (ibMeta != null) {
            ibMeta.setDisplayName(" ");
            innerBorder.setItemMeta(ibMeta);
        }

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, outerFiller);
        }

        // Left 2 columns and right 2 columns use innerBorder for dark contrast
        int[] sideSlots = {
                0, 1, 9, 10, 18, 19, 27, 28, 36, 37, 45, 46,
                7, 8, 16, 17, 25, 26, 34, 35, 43, 44, 52, 53
        };
        for (int s : sideSlots) {
            inv.setItem(s, innerBorder);
        }

        // Top Header (Slot 4)
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        ItemMeta hMeta = header.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName("§6§lLockout Bingo Race");
            hMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7Status: " + (bingoActive ? "§aActive (Live Race)" : "§cInactive"),
                    "§7Race across 25 survival challenges.",
                    "§7First team to complete a task locks it out!",
                    "§7Complete any line (5 in a row) for §e+150 Credits§7!",
                    "§8--------------------------"
            ));
            hMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            hMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            header.setItemMeta(hMeta);
        }
        inv.setItem(4, header);

        // Sidebar Info: Your Team Progress (Slot 17)
        Team playerTeam = player != null ? teamManager.getTeamByPlayer(player.getUniqueId()) : null;
        int teamClaimedCount = 0;
        if (playerTeam != null) {
            for (Team t : bingoBoard.values()) {
                if (t != null && t.equals(playerTeam)) teamClaimedCount++;
            }
        }
        ItemStack teamInfo = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta tiMeta = teamInfo.getItemMeta();
        if (tiMeta != null) {
            tiMeta.setDisplayName("§e§lYour Team Progress");
            tiMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7Team: " + (playerTeam != null ? playerTeam.getColorCode() + playerTeam.getName() : "§cNo Team"),
                    "§7Tasks Claimed: §e" + teamClaimedCount + " §7/ 25",
                    "§8--------------------------",
                    "§7Work with teammates to claim a row!"
            ));
            teamInfo.setItemMeta(tiMeta);
        }
        inv.setItem(17, teamInfo);

        // Sidebar Info: Leaderboard (Slot 26)
        ItemStack boardStats = new ItemStack(Material.FILLED_MAP);
        ItemMeta bsMeta = boardStats.getItemMeta();
        if (bsMeta != null) {
            bsMeta.setDisplayName("§6§lLive Standings");
            List<String> bsLore = new ArrayList<>();
            bsLore.add("§8--------------------------");
            Map<Team, Integer> counts = new HashMap<>();
            for (Team t : bingoBoard.values()) {
                if (t != null) counts.put(t, counts.getOrDefault(t, 0) + 1);
            }
            if (counts.isEmpty()) {
                bsLore.add("§7No tasks claimed yet!");
            } else {
                for (Map.Entry<Team, Integer> entry : counts.entrySet()) {
                    bsLore.add(entry.getKey().getColorCode() + entry.getKey().getName() + "§7: §e" + entry.getValue() + " tasks");
                }
            }
            bsLore.add("§8--------------------------");
            bsMeta.setLore(bsLore);
            boardStats.setItemMeta(bsMeta);
        }
        inv.setItem(26, boardStats);

        // Sidebar Info: Line Rewards (Slot 35)
        ItemStack rewardInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta riMeta = rewardInfo.getItemMeta();
        if (riMeta != null) {
            riMeta.setDisplayName("§a§lLine Rewards");
            riMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7Reward: §e+150 Game Credits",
                    "§7Awarded to the Captain whenever",
                    "§7your team completes any row,",
                    "§7column, or diagonal!",
                    "§8--------------------------"
            ));
            rewardInfo.setItemMeta(riMeta);
        }
        inv.setItem(35, rewardInfo);

        // Close Menu Button (Slot 53)
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = closeBtn.getItemMeta();
        if (cMeta != null) {
            cMeta.setDisplayName("§c§lClose Board");
            cMeta.setLore(Collections.singletonList("§7Click to exit."));
            closeBtn.setItemMeta(cMeta);
        }
        inv.setItem(53, closeBtn);

        // 5x5 Centered Board (Columns 2, 3, 4, 5, 6)
        int[] centeredSlots = {
                11, 12, 13, 14, 15,
                20, 21, 22, 23, 24,
                29, 30, 31, 32, 33,
                38, 39, 40, 41, 42,
                47, 48, 49, 50, 51
        };

        int index = 0;
        for (Map.Entry<String, Team> entry : bingoBoard.entrySet()) {
            if (index >= centeredSlots.length) break;
            int slot = centeredSlots[index++];

            String task = entry.getKey();
            Team claimer = entry.getValue();

            ItemStack item;
            if (claimer != null) {
                item = new ItemStack(claimer.getWoolColor());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(claimer.getColorCode() + "§l" + task);
                    List<String> lore = new ArrayList<>();
                    lore.add("§8--------------------------");
                    lore.add("§c✖ LOCKED OUT");
                    lore.add("§7Claimed by: " + claimer.getColorCode() + claimer.getName());
                    lore.add("§7Objective: §8" + getTaskDescription(task));
                    lore.add("§8--------------------------");
                    meta.setLore(lore);
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
            } else {
                Material icon = bingoIcons.getOrDefault(task, Material.PAPER);
                item = new ItemStack(icon);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e§l" + task);
                    List<String> lore = new ArrayList<>();
                    lore.add("§8--------------------------");
                    lore.add("§7Objective: §f" + getTaskDescription(task));
                    lore.add("§7Status: §a✔ Available to claim!");
                    lore.add("§8--------------------------");
                    lore.add("§eFirst team to complete this locks it out!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(slot, item);
        }

        player.openInventory(inv);
    }

    public Map<String, Team> getBingoBoard() { return bingoBoard; }
}
