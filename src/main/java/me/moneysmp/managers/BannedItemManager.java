package me.moneysmp.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BannedItemManager {
    public static final String GUI_TITLE = "§c§lBanned Tournament Items";

    public static class BanEntry {
        private final Material material;
        private final String reason;
        private final String alternative;

        public BanEntry(Material material, String reason, String alternative) {
            this.material = material;
            this.reason = reason;
            this.alternative = alternative;
        }

        public Material getMaterial() { return material; }
        public String getReason() { return reason; }
        public String getAlternative() { return alternative; }
    }

    private final JavaPlugin plugin;
    private final Map<Material, BanEntry> bannedItems = new LinkedHashMap<>();
    private File bannedConfigFile;
    private FileConfiguration bannedConfig;

    public BannedItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        bannedItems.clear();
        bannedConfigFile = new File(plugin.getDataFolder(), "banned-items.yml");
        if (!bannedConfigFile.exists()) {
            try {
                plugin.saveResource("banned-items.yml", false);
            } catch (Exception ignored) {
            }
            if (!bannedConfigFile.exists()) {
                createDefaultConfig();
            }
        }
        bannedConfig = YamlConfiguration.loadConfiguration(bannedConfigFile);
        ConfigurationSection sec = bannedConfig.getConfigurationSection("banned-items");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    String reason = sec.getString(key + ".reason", "Disabled for competitive tournament balance");
                    String alt = sec.getString(key + ".alternative", "Wind Charges");
                    bannedItems.put(mat, new BanEntry(mat, reason, alt));
                }
            }
        }

        // Ensure ENDER_PEARL is always in the ban list by default
        if (!bannedItems.containsKey(Material.ENDER_PEARL)) {
            bannedItems.put(Material.ENDER_PEARL, new BanEntry(Material.ENDER_PEARL,
                    "Ender Pearls are disabled for competitive tournament balance",
                    "Wind Charges"));
            saveConfig();
        }
    }

    private void createDefaultConfig() {
        bannedConfigFile.getParentFile().mkdirs();
        bannedConfig = new YamlConfiguration();
        bannedConfig.set("banned-items.ENDER_PEARL.reason", "Ender Pearls are disabled for competitive tournament balance");
        bannedConfig.set("banned-items.ENDER_PEARL.alternative", "Wind Charges");
        try {
            bannedConfig.save(bannedConfigFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save default banned-items.yml: " + e.getMessage());
        }
    }

    public void saveConfig() {
        if (bannedConfigFile == null) {
            bannedConfigFile = new File(plugin.getDataFolder(), "banned-items.yml");
        }
        bannedConfig = new YamlConfiguration();
        for (Map.Entry<Material, BanEntry> entry : bannedItems.entrySet()) {
            String key = entry.getKey().name();
            bannedConfig.set("banned-items." + key + ".reason", entry.getValue().getReason());
            bannedConfig.set("banned-items." + key + ".alternative", entry.getValue().getAlternative());
        }
        try {
            bannedConfig.save(bannedConfigFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save banned-items.yml: " + e.getMessage());
        }
    }

    public boolean isBanned(Material material) {
        if (material == null) return false;
        return bannedItems.containsKey(material);
    }

    public BanEntry getBanEntry(Material material) {
        if (material == null) return null;
        return bannedItems.get(material);
    }

    public void banItem(Material material, String reason, String alternative) {
        if (material == null) return;
        bannedItems.put(material, new BanEntry(material, reason, alternative));
        saveConfig();
    }

    public boolean unbanItem(Material material) {
        if (material == null) return false;
        BanEntry removed = bannedItems.remove(material);
        if (removed != null) {
            saveConfig();
            return true;
        }
        return false;
    }

    public Map<Material, BanEntry> getBannedItems() {
        return Collections.unmodifiableMap(bannedItems);
    }

    public void openBannedItemsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill background with Red and Black glass panes
        ItemStack redBorder = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta rbMeta = redBorder.getItemMeta();
        if (rbMeta != null) {
            rbMeta.setDisplayName(" ");
            redBorder.setItemMeta(rbMeta);
        }

        ItemStack blackBorder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bbMeta = blackBorder.getItemMeta();
        if (bbMeta != null) {
            bbMeta.setDisplayName(" ");
            blackBorder.setItemMeta(bbMeta);
        }

        // Fill entire inventory initially with black panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, blackBorder);
        }

        // Outer red perimeter
        int[] redSlots = {
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 18, 27, 36, 45,
                17, 26, 35, 44, 53,
                46, 47, 51, 52
        };
        for (int s : redSlots) {
            inv.setItem(s, redBorder);
        }

        // Slot 4: Header
        ItemStack header = new ItemStack(Material.BARRIER);
        ItemMeta hMeta = header.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName("§c§lTournament Ban List");
            hMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7The items listed here cannot be used",
                    "§7during this tournament.",
                    "§7Attempting to use, throw, consume,",
                    "§7or place these items is blocked!",
                    "§8--------------------------",
                    "§eTotal Banned Items: §c" + bannedItems.size(),
                    "§8--------------------------"
            ));
            hMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            hMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            header.setItemMeta(hMeta);
        }
        inv.setItem(4, header);

        // Slot 10: Rule explanation book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bMeta = book.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName("§e§lWhy Are Items Banned?");
            bMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7Items such as Ender Pearls allow",
                    "§7instant teleportation escapes that disrupt",
                    "§7PvP pacing, chase mechanics, and arena boundaries.",
                    "§8--------------------------",
                    "§7They have been banned in favor of",
                    "§bWind Charges §7which require skilled aiming!",
                    "§8--------------------------"
            ));
            book.setItemMeta(bMeta);
        }
        inv.setItem(10, book);

        // Slot 16: Wind Charge Replacement Spotlight
        ItemStack wcSpotlight = new ItemStack(Material.WIND_CHARGE);
        ItemMeta wcMeta = wcSpotlight.getItemMeta();
        if (wcMeta != null) {
            wcMeta.setDisplayName("§b§lApproved Mobility: Wind Charge");
            wcMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§a✔ 100% TOURNAMENT LEGAL",
                    "§7Replaces Ender Pearls for movement.",
                    "§7Use for vertical rocket-jumping, knockback,",
                    "§7and tactical repositioning!",
                    "§8--------------------------",
                    "§eAvailable in: §fAirdrops & /shop",
                    "§8--------------------------"
            ));
            wcMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            wcMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            wcSpotlight.setItemMeta(wcMeta);
        }
        inv.setItem(16, wcSpotlight);

        // Centered Display for banned items
        int[] displaySlots = {
                20, 21, 22, 23, 24,
                29, 30, 31, 32, 33,
                38, 39, 40, 41, 42
        };

        boolean isAdmin = player.hasPermission("moneysmp.admin");
        int slotIdx = 0;
        for (BanEntry entry : bannedItems.values()) {
            if (slotIdx >= displaySlots.length) break;
            int slot = displaySlots[slotIdx++];

            ItemStack icon = new ItemStack(entry.getMaterial());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                String cleanName = formatMaterialName(entry.getMaterial());
                meta.setDisplayName("§c§l✖ " + cleanName);
                List<String> lore = new ArrayList<>();
                lore.add("§8--------------------------");
                lore.add("§c§lSTATUS: FORBIDDEN / BANNED");
                lore.add("§f• Reason: §7" + entry.getReason());
                lore.add("§f• Approved Alternative: §b" + entry.getAlternative());
                lore.add("§8--------------------------");
                lore.add("§7Blocked Actions:");
                lore.add("§8• §cRight-Click / Throwing");
                lore.add("§8• §cConsuming / Placing / Crafting");
                lore.add("§8--------------------------");
                if (isAdmin) {
                    lore.add("§e▶ Admin Click: §cRemove / Unban item");
                } else {
                    lore.add("§cCannot be used during tournament!");
                }
                meta.setLore(lore);
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
        }

        // Slot 48 (Admin only - Ban item in main hand)
        if (isAdmin) {
            ItemStack banHand = new ItemStack(Material.HOPPER);
            ItemMeta bhMeta = banHand.getItemMeta();
            if (bhMeta != null) {
                bhMeta.setDisplayName("§a§lBan Item In Hand");
                bhMeta.setLore(Arrays.asList(
                        "§8--------------------------",
                        "§7Hold an item in your main hand",
                        "§7and click here to ban it!",
                        "§8--------------------------",
                        "§e▶ Click: §aBan held item"
                ));
                banHand.setItemMeta(bhMeta);
            }
            inv.setItem(48, banHand);
        }

        // Slot 49: Close Menu Barrier
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = closeBtn.getItemMeta();
        if (cMeta != null) {
            cMeta.setDisplayName("§c§lClose Menu");
            cMeta.setLore(Collections.singletonList("§7Click to exit banned items view."));
            closeBtn.setItemMeta(cMeta);
        }
        inv.setItem(49, closeBtn);

        // Slot 50: Command Help
        ItemStack helpItem = new ItemStack(Material.PAPER);
        ItemMeta pMeta = helpItem.getItemMeta();
        if (pMeta != null) {
            pMeta.setDisplayName("§e§lBan Commands");
            pMeta.setLore(Arrays.asList(
                    "§8--------------------------",
                    "§7View list anytime: §f/banneditems",
                    isAdmin ? "§7Ban item: §f/banneditems add <material>" : "§7Only admins can modify bans.",
                    isAdmin ? "§7Unban item: §f/banneditems remove <material>" : "§7Check with admins for rules.",
                    isAdmin ? "§7Ban hand: §f/banneditems hand [reason]" : "§8--------------------------",
                    "§8--------------------------"
            ));
            helpItem.setItemMeta(pMeta);
        }
        inv.setItem(50, helpItem);

        player.openInventory(inv);
    }

    public static String formatMaterialName(Material mat) {
        if (mat == null) return "Unknown";
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
