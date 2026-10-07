package me.moneysmp.managers;

import me.moneysmp.models.Team;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, Team> playerTeamMap = new HashMap<>();

    public Team createTeam(String name, UUID captainUuid) {
        removePlayerFromTeam(captainUuid);
        Team team = new Team(name, captainUuid);
        teams.put(name.toLowerCase(), team);
        playerTeamMap.put(captainUuid, team);
        return team;
    }

    public Team getTeamByPlayer(UUID uuid) { return playerTeamMap.get(uuid); }
    public Team getTeamByName(String name) { return name != null ? teams.get(name.toLowerCase()) : null; }
    public boolean teamExists(String name) { return name != null && teams.containsKey(name.toLowerCase()); }
    public Collection<Team> getTeams() { return Collections.unmodifiableCollection(teams.values()); }

    public void addPlayerToTeam(Team team, UUID playerUuid) {
        if (team == null || playerUuid == null) return;
        removePlayerFromTeam(playerUuid);
        team.addMember(playerUuid);
        playerTeamMap.put(playerUuid, team);
    }

    public boolean removePlayerFromTeam(UUID playerUuid) {
        if (playerUuid == null) return false;
        Team team = playerTeamMap.remove(playerUuid);
        if (team != null) {
            team.removeMember(playerUuid);
            return true;
        }
        return false;
    }

    public boolean disbandTeam(Team team) {
        if (team == null) return false;
        teams.remove(team.getName().toLowerCase());
        for (UUID member : team.getMembers()) {
            playerTeamMap.remove(member);
        }
        team.getMembers().clear();
        return true;
    }

    public boolean renameTeam(Team team, String newName) {
        if (team == null || newName == null || teamExists(newName)) return false;
        teams.remove(team.getName().toLowerCase());
        team.setName(newName);
        teams.put(newName.toLowerCase(), team);
        return true;
    }

    public void clearAllTeams() {
        for (Team team : teams.values()) {
            team.getMembers().clear();
        }
        teams.clear();
        playerTeamMap.clear();
    }
}
