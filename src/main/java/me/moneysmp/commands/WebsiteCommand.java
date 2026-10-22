package me.moneysmp.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** /website - sends a clickable link to the live standings website. */
public class WebsiteCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public WebsiteCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String url = plugin.getConfig().getString("website-url", "https://willy-money-smp-web.vercel.app/");
        if (!url.startsWith("http")) url = "https://" + url;

        sender.sendMessage("§6§l[Willy Money SMP] §eLive standings, draft board & team rosters:");
        if (sender instanceof Player player) {
            TextComponent link = new TextComponent("§b§n" + url);
            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to open the website")));
            player.spigot().sendMessage(link);
        } else {
            sender.sendMessage(url);
        }
        return true;
    }
}
