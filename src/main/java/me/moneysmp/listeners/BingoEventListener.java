package me.moneysmp.listeners;

import me.moneysmp.managers.EventManager;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public class BingoEventListener implements Listener {
    private final JavaPlugin plugin;
    private final EventManager eventManager;

    public BingoEventListener(JavaPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();

        if (mat == Material.ANCIENT_DEBRIS) {
            eventManager.claimBingo(player, "Mine Ancient Debris");
        }

        if (mat == Material.COAL_ORE || mat == Material.DEEPSLATE_COAL_ORE || mat == Material.COAL_BLOCK) {
            if (plugin != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.getInventory().containsAtLeast(new ItemStack(Material.COAL), 64)) {
                        eventManager.claimBingo(player, "Mine 64 Coal");
                    }
                }, 5L);
            } else if (player.getInventory().containsAtLeast(new ItemStack(Material.COAL), 64)) {
                eventManager.claimBingo(player, "Mine 64 Coal");
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();

        if (result.getType() == Material.DIAMOND_BLOCK) eventManager.claimBingo(player, "Craft Diamond Block");
        if (result.getType() == Material.CAKE) eventManager.claimBingo(player, "Bake a Cake");
        if (result.getType() == Material.ENCHANTING_TABLE) eventManager.claimBingo(player, "Craft Enchanting Table");
        if (result.getType() == Material.BEACON) eventManager.claimBingo(player, "Craft Beacon");
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;

        if (event.getEntity() instanceof Cat) eventManager.claimBingo(player, "Tame a Cat");
        if (event.getEntity() instanceof Wolf) eventManager.claimBingo(player, "Tame a Wolf");
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (event.getCaught() instanceof Item caughtItem) {
            Material mat = caughtItem.getItemStack().getType();
            if (mat == Material.PUFFERFISH) {
                eventManager.claimBingo(player, "Catch a Pufferfish");
            } else if (mat == Material.ENCHANTED_BOOK) {
                eventManager.claimBingo(player, "Catch Enchanted Book");
            }
        }
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Enderman) eventManager.claimBingo(killer, "Kill an Enderman");
        if (event.getEntity() instanceof Warden) eventManager.claimBingo(killer, "Kill a Warden");
        if (event.getEntity() instanceof Blaze) eventManager.claimBingo(killer, "Kill Blazes");
        if (event.getEntity() instanceof WitherSkeleton) {
            for (ItemStack drop : event.getDrops()) {
                if (drop.getType() == Material.WITHER_SKELETON_SKULL) {
                    eventManager.claimBingo(killer, "Obtain Wither Skull");
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItem().getType();
        if (mat == Material.GOLDEN_APPLE || mat == Material.ENCHANTED_GOLDEN_APPLE) {
            eventManager.claimBingo(player, "Eat Golden Apple");
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof AbstractWindCharge && event.getEntity().getShooter() instanceof Player player) {
            eventManager.claimBingo(player, "Use Wind Charge");
        }
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        if (event.getEntity() instanceof Sheep) {
            eventManager.claimBingo(event.getPlayer(), "Shear a Sheep");
        }
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        if (event.getEntity() instanceof Player player && event.getMount() instanceof Strider) {
            eventManager.claimBingo(player, "Ride a Strider");
        }
    }

    @EventHandler
    public void onSmeltExtract(FurnaceExtractEvent event) {
        if (event.getItemType() == Material.NETHERITE_SCRAP) {
            eventManager.claimBingo(event.getPlayer(), "Smelt Netherite");
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player && event.getNewEffect() != null) {
            if (event.getNewEffect().getType() == PotionEffectType.INVISIBILITY) {
                eventManager.claimBingo(player, "Brew Invisibility");
            } else if (event.getNewEffect().getType() == PotionEffectType.STRENGTH && event.getNewEffect().getAmplifier() >= 1) {
                eventManager.claimBingo(player, "Brew Strength II");
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Material mat = event.getItem().getItemStack().getType();
            if (mat == Material.ELYTRA) {
                eventManager.claimBingo(player, "Obtain Elytra");
            } else if (mat == Material.WITHER_SKELETON_SKULL) {
                eventManager.claimBingo(player, "Obtain Wither Skull");
            } else if (mat == Material.COAL) {
                if (plugin != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline() && player.getInventory().containsAtLeast(new ItemStack(Material.COAL), 64)) {
                            eventManager.claimBingo(player, "Mine 64 Coal");
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager) {
            eventManager.claimBingo(event.getPlayer(), "Find a Village");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        checkFullIron(player);

        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.STONE_PRESSURE_PLATE) {
                Biome biome = event.getClickedBlock().getBiome();
                if (biome.name().contains("DESERT")) {
                    eventManager.claimBingo(player, "Find Desert Temple");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ELYTRA) {
                eventManager.claimBingo(player, "Obtain Elytra");
            }
            if (plugin != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> checkFullIron(player));
            }
        }
    }

    private void checkFullIron(Player player) {
        if (player == null || !player.isOnline()) return;
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        if (helmet != null && helmet.getType() == Material.IRON_HELMET &&
                chestplate != null && chestplate.getType() == Material.IRON_CHESTPLATE &&
                leggings != null && leggings.getType() == Material.IRON_LEGGINGS &&
                boots != null && boots.getType() == Material.IRON_BOOTS) {
            eventManager.claimBingo(player, "Wear Full Iron");
        }
    }
}
