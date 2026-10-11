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

            StringBuilder json = new StringBuilder("[");
            List<Team> teams = new ArrayList<>(teamManager.getTeams());
            teams.sort((a, b) -> Integer.compare(
                    creditManager.getGameCredits(b.getCaptain()),
                    creditManager.getGameCredits(a.getCaptain())
            ));

            boolean first = true;
            for (Team team : teams) {
                if (!first) json.append(",");
                first = false;

                int captainCredits = creditManager.getGameCredits(team.getCaptain());
                String captainName = Bukkit.getOfflinePlayer(team.getCaptain()).getName();
                if (captainName == null) captainName = "Unknown";

                json.append("{")
                        .append("\"name\":\"").append(escapeJson(team.getName())).append("\",")
                        .append("\"captain\":\"").append(escapeJson(captainName)).append("\",")
                        .append("\"credits\":").append(captainCredits).append(",")
                        .append("\"color\":\"").append(escapeJson(team.getColorCode())).append("\",")
                        .append("\"members\":[");

                boolean firstM = true;
                for (UUID mUuid : team.getMembers()) {
                    if (!firstM) json.append(",");
                    firstM = false;
                    String mName = Bukkit.getOfflinePlayer(mUuid).getName();
                    if (mName == null) mName = "Unknown";
                    PlayerTier tier = tierManager.getTier(mUuid);
                    boolean elim = eliminationManager != null && eliminationManager.isEliminated(mUuid);

                    json.append("{")
                            .append("\"name\":\"").append(escapeJson(mName)).append("\",")
                            .append("\"tier\":\"").append(tier.name()).append("\",")
                            .append("\"eliminated\":").append(elim)
                            .append("}");
                }
                json.append("]}");
            }
            json.append("]");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
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
