package me.moneysmp.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.moneysmp.managers.*;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class WebServerManager {
    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final PhaseManager phaseManager;
    private final EliminationManager eliminationManager;
    private final EventManager eventManager;

    private HttpServer server;

    public WebServerManager(JavaPlugin plugin,
                            TeamManager teamManager,
                            CreditManager creditManager,
                            TierManager tierManager,
                            PhaseManager phaseManager,
                            EliminationManager eliminationManager,
                            EventManager eventManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.phaseManager = phaseManager;
        this.eliminationManager = eliminationManager;
        this.eventManager = eventManager;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web-server.enabled", true)) {
            return;
        }

        int port = plugin.getConfig().getInt("web-server.port", 8085);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/leaderboard", new LeaderboardHandler());
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/bingo", new BingoHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("Live REST API WebServer started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not start WebServer on port " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
            plugin.getLogger().info("WebServer stopped.");
        }
    }

    private class LeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            List<Team> teams = new ArrayList<>(teamManager.getTeams());
            teams.sort((a, b) -> Integer.compare(
                    creditManager.getGameCredits(b.getCaptain()),
                    creditManager.getGameCredits(a.getCaptain())
            ));

            int totalParticipants = 0;
            int totalEliminated = 0;

            StringBuilder teamsJson = new StringBuilder("[");
            boolean firstTeam = true;

            for (Team team : teams) {
                if (!firstTeam) teamsJson.append(",");
                firstTeam = false;

                int captainCredits = creditManager.getGameCredits(team.getCaptain());
                String captainName = Bukkit.getOfflinePlayer(team.getCaptain()).getName();
                if (captainName == null) captainName = "Unknown";

                int aliveCount = 0;
                int teamElimCount = 0;
                int teamTotalCredits = 0;

                StringBuilder membersJson = new StringBuilder("[");
                boolean firstMember = true;

                for (UUID mUuid : team.getMembers()) {
                    if (!firstMember) membersJson.append(",");
                    firstMember = false;

                    String mName = Bukkit.getOfflinePlayer(mUuid).getName();
                    if (mName == null) mName = "Unknown";
                    PlayerTier tier = tierManager.getTier(mUuid);
                    boolean isElim = eliminationManager != null && eliminationManager.isEliminated(mUuid);
                    int gameCreds = creditManager.getGameCredits(mUuid);
                    int draftCreds = creditManager.getDraftCredits(mUuid);
                    boolean isCap = team.isCaptain(mUuid);

                    if (isElim) teamElimCount++;
                    else aliveCount++;

                    teamTotalCredits += gameCreds;

                    membersJson.append("{")
                            .append("\"uuid\":\"").append(mUuid.toString()).append("\",")
                            .append("\"name\":\"").append(escapeJson(mName)).append("\",")
                            .append("\"tier\":\"").append(tier.name()).append("\",")
                            .append("\"gameCredits\":").append(gameCreds).append(",")
                            .append("\"draftCredits\":").append(draftCreds).append(",")
                            .append("\"isCaptain\":").append(isCap).append(",")
                            .append("\"isEliminated\":").append(isElim)
                            .append("}");
                }
                membersJson.append("]");

                totalParticipants += team.getMembers().size();
                totalEliminated += teamElimCount;

                String hexColor = "#eab308";
                String colorCode = team.getColorCode();
                if (colorCode != null) {
                    if (colorCode.contains("c") || colorCode.contains("4")) hexColor = "#ef4444";
                    else if (colorCode.contains("9") || colorCode.contains("1")) hexColor = "#3b82f6";
                    else if (colorCode.contains("a") || colorCode.contains("2")) hexColor = "#22c55e";
                    else if (colorCode.contains("e") || colorCode.contains("6")) hexColor = "#eab308";
                    else if (colorCode.contains("5") || colorCode.contains("d")) hexColor = "#a855f7";
                    else if (colorCode.contains("b") || colorCode.contains("3")) hexColor = "#06b6d4";
                    else if (colorCode.contains("f") || colorCode.contains("7")) hexColor = "#e2e8f0";
                }

                teamsJson.append("{")
                        .append("\"id\":\"").append(escapeJson(team.getName().toLowerCase())).append("\",")
                        .append("\"name\":\"").append(escapeJson(team.getName())).append("\",")
                        .append("\"colorCode\":\"").append(escapeJson(team.getColorCode())).append("\",")
                        .append("\"hexColor\":\"").append(hexColor).append("\",")
                        .append("\"captain\":\"").append(team.getCaptain().toString()).append("\",")
                        .append("\"captainName\":\"").append(escapeJson(captainName)).append("\",")
                        .append("\"totalCredits\":").append(teamTotalCredits).append(",")
                        .append("\"aliveCount\":").append(aliveCount).append(",")
                        .append("\"eliminatedCount\":").append(teamElimCount).append(",")
                        .append("\"members\":").append(membersJson)
                        .append("}");
            }
            teamsJson.append("]");

            String phase = phaseManager != null ? phaseManager.getCurrentPhase().name() : "PVP";
            int online = Bukkit.getOnlinePlayers().size();
            boolean koth = eventManager != null && eventManager.isKothActive();
            boolean bingo = eventManager != null && eventManager.isBingoActive();

            String phaseName = "Combat & Eliminations";
            if ("DRAFT".equalsIgnoreCase(phase)) phaseName = "Draft Auction";
            else if ("GRACE".equalsIgnoreCase(phase)) phaseName = "Grace Period";
            else if ("FINALE".equalsIgnoreCase(phase)) phaseName = "Finale Showdown";

            StringBuilder fullResponse = new StringBuilder("{");
            fullResponse.append("\"status\":{")
                    .append("\"serverName\":\"Willy Money SMP\",")
                    .append("\"onlinePlayers\":").append(online).append(",")
                    .append("\"maxPlayers\":").append(Bukkit.getMaxPlayers()).append(",")
                    .append("\"phase\":\"").append(phase).append("\",")
                    .append("\"phaseName\":\"").append(escapeJson(phaseName)).append("\",")
                    .append("\"kothActive\":").append(koth).append(",")
                    .append("\"bingoActive\":").append(bingo).append(",")
                    .append("\"totalTeams\":").append(teams.size()).append(",")
                    .append("\"totalParticipants\":").append(totalParticipants).append(",")
                    .append("\"eliminatedCount\":").append(totalEliminated).append(",")
                    .append("\"lastUpdated\":\"").append(java.time.Instant.now().toString()).append("\"")
                    .append("},");
            fullResponse.append("\"teams\":").append(teamsJson);
            fullResponse.append("}");

            byte[] bytes = fullResponse.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            String phase = phaseManager != null ? phaseManager.getCurrentPhase().name() : "DRAFT";
            int online = Bukkit.getOnlinePlayers().size();
            boolean koth = eventManager != null && eventManager.isKothActive();
            boolean bingo = eventManager != null && eventManager.isBingoActive();

            String json = "{" +
                    "\"phase\":\"" + phase + "\"," +
                    "\"onlinePlayers\":" + online + "," +
                    "\"kothActive\":" + koth + "," +
                    "\"bingoActive\":" + bingo +
                    "}";

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class BingoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            StringBuilder json = new StringBuilder("{");
            boolean active = eventManager != null && eventManager.isBingoActive();
            json.append("\"active\":").append(active).append(",");
            json.append("\"tasks\":[");

            if (eventManager != null) {
                Map<String, Team> board = eventManager.getBingoBoard();
                boolean first = true;
                for (Map.Entry<String, Team> entry : board.entrySet()) {
                    if (!first) json.append(",");
                    first = false;
                    String task = entry.getKey();
                    Team claimer = entry.getValue();

                    json.append("{")
                            .append("\"task\":\"").append(escapeJson(task)).append("\",")
                            .append("\"claimed\":").append(claimer != null).append(",")
                            .append("\"claimerTeam\":").append(claimer != null ? "\"" + escapeJson(claimer.getName()) + "\"" : "null")
                            .append("}");
                }
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
