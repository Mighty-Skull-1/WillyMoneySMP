package me.moneysmp.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;

public class ShopManager {
    public static final String SHOP_TITLE = "§6§lTournament Credit Shop";
    private final CreditManager creditManager;

    public static class ShopItem {
        private final int slot;
        private final ItemStack itemStack;
        private final int cost;
        private final String displayName;

        public ShopItem(int slot, ItemStack itemStack, int cost, String displayName) {
            this.slot = slot;
            this.itemStack = itemStack;
            this.cost = cost;
            this.displayName = displayName;
        }

        public int getSlot() { return slot; }
        public ItemStack getItemStack() { return itemStack.clone(); }
        public int getCost() { return cost; }
        public String getDisplayName() { return displayName; }
    }

    private final Map<Integer, ShopItem> shopItems = new HashMap<>();

    public ShopManager(CreditManager creditManager) {
        this.creditManager = creditManager;
        setupShopItems();
    }

    private void setupShopItems() {
        // Slot 10: Golden Apple x2 (15 credits)
        addShopItem(10, new ItemStack(Material.GOLDEN_APPLE, 2), 15, "§eGolden Apple x2");

        // Slot 11: Enchanted Golden Apple x1 (75 credits)
        addShopItem(11, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 75, "§6Enchanted Golden Apple");

        // Slot 12: Wind Charge x4 (20 credits)
        addShopItem(12, new ItemStack(Material.WIND_CHARGE, 4), 20, "§bWind Charge x4");

        // Slot 13: Totem of Undying (85 credits)
        addShopItem(13, new ItemStack(Material.TOTEM_OF_UNDYING, 1), 85, "§eTotem of Undying");

        // Slot 14: Team Tracker Compass (25 credits)
        ItemStack tracker = new ItemStack(Material.COMPASS);
        ItemMeta trackerMeta = tracker.getItemMeta();
        if (trackerMeta != null) {
            trackerMeta.setDisplayName("§6§lTeam Tracker");
            trackerMeta.setLore(Collections.singletonList("§7Right-click to switch between tracking Teammates & Enemy Captains"));
            tracker.setItemMeta(trackerMeta);
        }
        addShopItem(14, tracker, 25, "§6Team Tracker Compass");

        // Slot 15: Arrow Bundle x32 (10 credits)
        addShopItem(15, new ItemStack(Material.ARROW, 32), 10, "§fArrows x32");

        // Slot 16: Diamond x4 (30 credits)
        addShopItem(16, new ItemStack(Material.DIAMOND, 4), 30, "§bDiamond x4");

        // Slot 19: Splash Potion of Healing II (25 credits)
        ItemStack healPot = new ItemStack(Material.SPLASH_POTION);
        PotionMeta healMeta = (PotionMeta) healPot.getItemMeta();
        if (healMeta != null) {
            healMeta.setBasePotionType(PotionType.STRONG_HEALING);
            healPot.setItemMeta(healMeta);
        }
        addShopItem(19, healPot, 25, "§cSplash Potion of Healing II");

        // Slot 20: Potion of Swiftness II (20 credits)
        ItemStack speedPot = new ItemStack(Material.POTION);
        PotionMeta speedMeta = (PotionMeta) speedPot.getItemMeta();
        if (speedMeta != null) {
            speedMeta.setBasePotionType(PotionType.STRONG_SWIFTNESS);
            speedPot.setItemMeta(speedMeta);
        }
        addShopItem(20, speedPot, 20, "§bPotion of Swiftness II");

        // Slot 24: Potion of Strength (35 credits)
        ItemStack strengthPot = new ItemStack(Material.POTION);
        PotionMeta strMeta = (PotionMeta) strengthPot.getItemMeta();
        if (strMeta != null) {
            strMeta.setBasePotionType(PotionType.STRENGTH);
            strengthPot.setItemMeta(strMeta);
        }
        addShopItem(24, strengthPot, 35, "§4Potion of Strength");

        // Slot 25: Firework Rockets x16 (15 credits)
        addShopItem(25, new ItemStack(Material.FIREWORK_ROCKET, 16), 15, "§fFirework Rockets x16");
    }

    private void addShopItem(int slot, ItemStack item, int cost, String name) {
        shopItems.put(slot, new ShopItem(slot, item, cost, name));
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, SHOP_TITLE);

        // Fill background with black stained glass pane
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Add shop items
        for (ShopItem shopItem : shopItems.values()) {
            ItemStack display = shopItem.getItemStack();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(shopItem.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§8----------------------");
                lore.add("§7Price: §e" + shopItem.getCost() + " Game Credits");
                lore.add("§aClick to purchase!");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(shopItem.getSlot(), display);
        }

        // Balance indicator at slot 22
        int balance = creditManager.getGameCredits(player.getUniqueId());
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bMeta = balanceItem.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName("§6§lYOUR BALANCE");
            bMeta.setLore(Arrays.asList(
                    "§7Current Credits: §e" + balance + " Game Credits",
                    "§7Earn more credits via Kills, KOTH, and Airdrops!"
            ));
            balanceItem.setItemMeta(bMeta);
        }
        inv.setItem(22, balanceItem);

        player.openInventory(inv);
    }

    public boolean handlePurchase(Player player, int slot) {
        ShopItem shopItem = shopItems.get(slot);
        if (shopItem == null) return false;

        int cost = shopItem.getCost();
        int balance = creditManager.getGameCredits(player.getUniqueId());

        if (balance < cost) {
            player.sendMessage("§cYou cannot afford this! Cost: §e" + cost + " Credits§c, Balance: §e" + balance + " Credits§c.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            return false;
        }

        if (creditManager.removeGameCredits(player.getUniqueId(), cost)) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(shopItem.getItemStack());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            player.sendMessage("§a[SHOP] Successfully bought §f" + shopItem.getDisplayName() + " §afor §e" + cost + " Credits§a!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

            // Re-open/update shop to refresh balance display
            openShop(player);
            return true;
        }

        return false;
    }
}
