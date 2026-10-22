package me.moneysmp.commands;

import me.moneysmp.managers.PhaseManager;
import me.moneysmp.models.SMPPhase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /freefall - the Finale Showdown. Switches to the FINALE phase and shrinks the world border
 * so every team is forced into the middle.
 *
 *   /freefall start [minutes] [finalSize]  start the finale and close the border
 *   /freefall center                       set the border centre to where you are standing
 *   /freefall status                       current border size / time left
 *   /freefall stop                         freeze the border where it is
 *   /freefall reset                        put the border back to normal (huge)
 *
 * Defaults come from config.yml (freefall: section).
 */
public class FreeFallCommand implements CommandExecutor {
    private static final double NORMAL_BORDER = 59_999_968;

    private final JavaPlugin plugin;
    private final PhaseManager phaseManager;
    private int announceTaskId = -1;
    private long endsAtMillis = 0L;

    public FreeFallCommand(JavaPlugin plugin, PhaseManager phaseManager) {
        this.plugin = plugin;
        this.phaseManager = phaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("moneysmp.admin")) {
            sender.sendMessage("§cAdmin only!");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase() : "help";
        World world = targetWorld(sender);

        switch (sub) {
            case "start" -> start(sender, world, args);
            case "center", "centre" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cRun this in game while standing where the centre should be.");
                    return true;
                }
                Location l = player.getLocation();
                plugin.getConfig().set("freefall.center-x", l.getBlockX());
                plugin.getConfig().set("freefall.center-z", l.getBlockZ());
                plugin.saveConfig();
                world.getWorldBorder().setCenter(l.getBlockX() + 0.5, l.getBlockZ() + 0.5);
                sender.sendMessage("§a[Free Fall] Border centre set to §f" + l.getBlockX() + ", " + l.getBlockZ() + "§a.");
            }
            case "status" -> {
                WorldBorder b = world.getWorldBorder();
                long left = Math.max(0, (endsAtMillis - System.currentTimeMillis()) / 1000);
                sender.sendMessage("§6[Free Fall] §eBorder: §f" + (int) b.getSize() + " blocks §7(centre "
                        + (int) b.getCenter().getX() + ", " + (int) b.getCenter().getZ() + ")"
                        + (left > 0 ? " §e- shrinking for another §f" + formatTime(left) : ""));
            }
            case "stop" -> {
                WorldBorder b = world.getWorldBorder();
                b.setSize(b.getSize());
                cancelAnnouncements();
                endsAtMillis = 0L;
                Bukkit.broadcastMessage("§6§l[FREE FALL] §eThe border has stopped at §f" + (int) b.getSize() + " blocks§e.");
            }
            case "reset" -> {
                cancelAnnouncements();
                endsAtMillis = 0L;
                world.getWorldBorder().setSize(NORMAL_BORDER);
                sender.sendMessage("§a[Free Fall] World border reset to normal.");
            }
            default -> sender.sendMessage("§cUsage:\n/freefall start [minutes] [finalSize]\n/freefall center\n/freefall status\n/freefall stop\n/freefall reset");
        }
        return true;
    }

    private void start(CommandSender sender, World world, String[] args) {
        int minutes = plugin.getConfig().getInt("freefall.shrink-minutes", 20);
        int finalSize = plugin.getConfig().getInt("freefall.end-size", 50);
        int startSize = plugin.getConfig().getInt("freefall.start-size", 1000);
        try {
            if (args.length > 1) minutes = Integer.parseInt(args[1]);
            if (args.length > 2) finalSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cUsage: /freefall start [minutes] [finalSize]   e.g. /freefall start 20 50");
            return;
        }
        minutes = Math.max(1, minutes);
        finalSize = Math.max(5, finalSize);
        startSize = Math.max(finalSize + 1, startSize);

        double cx = plugin.getConfig().contains("freefall.center-x")
                ? plugin.getConfig().getInt("freefall.center-x") + 0.5 : world.getSpawnLocation().getBlockX() + 0.5;
        double cz = plugin.getConfig().contains("freefall.center-z")
                ? plugin.getConfig().getInt("freefall.center-z") + 0.5 : world.getSpawnLocation().getBlockZ() + 0.5;

        WorldBorder border = world.getWorldBorder();
        border.setCenter(cx, cz);
        border.setDamageBuffer(0);
        border.setDamageAmount(plugin.getConfig().getDouble("freefall.damage-per-block", 1.0));
        border.setWarningDistance(10);
        border.setSize(startSize);
        border.setSize(finalSize, minutes * 60L);
        endsAtMillis = System.currentTimeMillis() + minutes * 60_000L;

        if (phaseManager.getCurrentPhase() != SMPPhase.FINALE) phaseManager.setPhase(SMPPhase.FINALE);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§4§lFREE FALL", "§eThe border is closing in!", 10, 80, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }
        Bukkit.broadcastMessage("§4§l[FREE FALL] §eThe Finale Showdown has begun! The border shrinks from §f" + startSize
                + " §eto §f" + finalSize + " blocks §eover §f" + minutes + " minutes§e, centred on §f" + (int) cx + ", " + (int) cz + "§e.");
        Bukkit.broadcastMessage("§7Stay inside the border - it hurts outside. Last team standing takes the crown!");

        cancelAnnouncements();
        announceTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long left = (endsAtMillis - System.currentTimeMillis()) / 1000;
            if (left <= 0) {
                Bukkit.broadcastMessage("§4§l[FREE FALL] §eThe border has fully closed at §f" + (int) border.getSize() + " blocks§e. Fight!");
                cancelAnnouncements();
                return;
            }
            Bukkit.broadcastMessage("§4[Free Fall] §eBorder: §f" + (int) border.getSize() + " blocks §7- " + formatTime(left) + " until it stops.");
        }, 60 * 20L, 60 * 20L);
    }

    private World targetWorld(CommandSender sender) {
        if (sender instanceof Player p && p.getWorld().getEnvironment() == World.Environment.NORMAL) return p.getWorld();
        return Bukkit.getWorlds().get(0);
    }

    private void cancelAnnouncements() {
        if (announceTaskId != -1) {
            Bukkit.getScheduler().cancelTask(announceTaskId);
            announceTaskId = -1;
        }
    }

    private static String formatTime(long seconds) {
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}
