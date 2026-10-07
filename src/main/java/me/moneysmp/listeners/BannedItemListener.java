package me.moneysmp.listeners;

import me.moneysmp.managers.BannedItemManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BannedItemListener implements Listener {
    private final BannedItemManager bannedItemManager;
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    public BannedItemListener(BannedItemManager bannedItemManager) {
        this.bannedItemManager = bannedItemManager;
    }

    private void notifyPlayer(Player player, Material mat) {
        if (player == null || !player.isOnline()) return;

        long now = System.currentTimeMillis();
        long last = messageCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last > 1500) {
            messageCooldowns.put(player.getUniqueId(), now);
            BannedItemManager.BanEntry entry = bannedItemManager.getBanEntry(mat);
            String reason = entry != null ? entry.getReason() : "Disabled in tournament";
            String alt = entry != null ? entry.getAlternative() : "Wind Charges";

            player.sendMessage("§c§l[BANNED ITEM] §f" + BannedItemManager.formatMaterialName(mat) +
                    " §cis banned in this tournament!");
            player.sendMessage("§cReason: §7" + reason + " §8| §eAlternative: §b" + alt);

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText("§c§l✖ " + BannedItemManager.formatMaterialName(mat) + " is BANNED! Use " + alt));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && bannedItemManager.isBanned(item.getType())) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            notifyPlayer(event.getPlayer(), item.getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl pearl) {
            event.setCancelled(true);
            pearl.remove();
            if (pearl.getShooter() instanceof Player player) {
                notifyPlayer(player, Material.ENDER_PEARL);
            }
            return;
        }

        if (event.getEntity().getShooter() instanceof Player player) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (bannedItemManager.isBanned(inHand.getType())) {
                event.setCancelled(true);
                notifyPlayer(player, inHand.getType());
            } else if (bannedItemManager.isBanned(offHand.getType())) {
                event.setCancelled(true);
                notifyPlayer(player, offHand.getType());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (bannedItemManager.isBanned(event.getItem().getType())) {
            event.setCancelled(true);
            notifyPlayer(event.getPlayer(), event.getItem().getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (bannedItemManager.isBanned(event.getBlock().getType())) {
            event.setCancelled(true);
            notifyPlayer(event.getPlayer(), event.getBlock().getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult() != null) {
            Material result = event.getRecipe().getResult().getType();
            if (bannedItemManager.isBanned(result)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    notifyPlayer(player, result);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (bannedItemManager.isBanned(event.getItem().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(BannedItemManager.GUI_TITLE)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot == 49 || rawSlot == 53) {
            player.closeInventory();
            return;
        }

        // Slot 48: Ban Item in Main Hand (Admin only)
        if (rawSlot == 48 && player.hasPermission("moneysmp.admin")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage("§c[Banned Items] You must hold an item in your main hand to ban it!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }
            if (bannedItemManager.isBanned(inHand.getType())) {
                player.sendMessage("§c[Banned Items] " + BannedItemManager.formatMaterialName(inHand.getType()) + " is already banned!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }
            bannedItemManager.banItem(inHand.getType(), "Banned by administrator", "None");
            player.sendMessage("§a[Banned Items] Successfully banned §e" + BannedItemManager.formatMaterialName(inHand.getType()) + "§a!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            bannedItemManager.openBannedItemsGUI(player);
            return;
        }

        // Clicking on a banned item to unban (Admin only)
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && player.hasPermission("moneysmp.admin")) {
            Material clickedMat = clickedItem.getType();
            if (bannedItemManager.isBanned(clickedMat)) {
                bannedItemManager.unbanItem(clickedMat);
                player.sendMessage("§a[Banned Items] Successfully unbanned §e" + BannedItemManager.formatMaterialName(clickedMat) + "§a!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                bannedItemManager.openBannedItemsGUI(player);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(BannedItemManager.GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
}
