package me.moneysmp.models;

import org.bukkit.Material;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    private String name;
    private final UUID captain;
    private final Set<UUID> members = new HashSet<>();

    public Team(String name, UUID captain) {
        this.name = name;
        this.captain = captain;
        this.members.add(captain);
    }

    public String getName() { return name; }
    public void setName(String newName) { this.name = newName; }
    public UUID getCaptain() { return captain; }
    public Set<UUID> getMembers() { return members; }

    public boolean addMember(UUID uuid) { return members.add(uuid); }
    public boolean removeMember(UUID uuid) { return members.remove(uuid); }
    public boolean isCaptain(UUID uuid) { return captain.equals(uuid); }
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }

    public String getColorCode() {
        String lower = name.toLowerCase();
        if (lower.contains("blue")) return "§9";
        if (lower.contains("red")) return "§c";
        if (lower.contains("yellow")) return "§e";
        if (lower.contains("green")) return "§a";
        if (lower.contains("purple")) return "§d";
        if (lower.contains("gold") || lower.contains("orange")) return "§6";
        if (lower.contains("aqua") || lower.contains("cyan")) return "§b";
        return "§f";
    }

    public Material getWoolColor() {
        String lower = name.toLowerCase();
        if (lower.contains("blue")) return Material.BLUE_WOOL;
        if (lower.contains("red")) return Material.RED_WOOL;
        if (lower.contains("yellow")) return Material.YELLOW_WOOL;
        if (lower.contains("green")) return Material.LIME_WOOL;
        if (lower.contains("purple")) return Material.PURPLE_WOOL;
        if (lower.contains("gold") || lower.contains("orange")) return Material.ORANGE_WOOL;
        if (lower.contains("aqua") || lower.contains("cyan")) return Material.CYAN_WOOL;
        return Material.WHITE_WOOL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return name.equalsIgnoreCase(team.name) && captain.equals(team.captain);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode() * 31 + captain.hashCode();
    }
}
