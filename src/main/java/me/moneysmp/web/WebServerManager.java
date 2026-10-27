package me.moneysmp.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import me.moneysmp.managers.*;
import me.moneysmp.models.PlayerTier;
import me.moneysmp.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Sends live tournament data to the website.
 *
 * How it works:
 *  1. Every 2 seconds (on the main server thread) we build a compact snapshot of the tournament.
 *  2. If anything changed, or a minute passed with no update, the snapshot is signed with the
 *     server's private key and posted to the ntfy.sh topic on a background thread.
 *  3. The website subscribes to that topic and only accepts messages with a valid signature,
 *     so nobody else can post fake standings.
 *
 * Draft auctions call {@link #requestPush()} so bids show up on the site instantly.
 *
 * Message format sent to ntfy:  v2.<base64url signature>.<json payload>
 * Payload is kept compact because ntfy turns messages over 4 KB into file attachments.
 */
public class WebServerManager {
    private static final long TICK_INTERVAL = 40L;          // 2 seconds
    private static final int NTFY_MESSAGE_LIMIT = 4096;

    // ntfy.sh allows a burst of ~60 requests, then about one every 5 seconds per IP. Going over that
    // gets HTTP 429, and hammering it after that gets the server's IP blocked (connect timeouts).
    // We mirror that limit with our own token bucket (a little stricter) so we never trip it:
    // short bursts (draft bids) still go out instantly, but sustained changes (bingo, KOTH) are paced.
    private static final int PUSH_BURST = 30;
    private static final long PUSH_REFILL_MS = 6000L;
    // After a failure, wait before trying again: 15s, 30s, 60s ... up to 5 minutes.
    private static final long BACKOFF_START_MS = 15_000L;
    private static final long BACKOFF_MAX_MS = 300_000L;

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final CreditManager creditManager;
    private final TierManager tierManager;
    private final PhaseManager phaseManager;
    private final EliminationManager eliminationManager;
    private final EventManager eventManager;
    private final DraftManager draftManager;
    private final StatsManager statsManager;

    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WillyMoneySMP-WebSync");
        t.setDaemon(true);
        return t;
    });

    private HttpServer server;
    private WebSigner signer;
    private int tickTaskId = -1;
    private volatile boolean running = false;

    // Main-thread state
    private String lastPayloadBody = null;
    private long lastPushAt = 0L;
    private boolean pushQueued = false;
    private boolean warnedAboutSize = false;

    // Rate limiting (main thread)
    private double pushTokens = PUSH_BURST;
    private long tokensUpdatedAt = System.currentTimeMillis();

    // Shared with the sender / HTTP threads
    private volatile boolean retrySoon = false;
    private volatile boolean sendInFlight = false;
    private volatile long backoffUntil = 0L;
    private volatile long backoffMs = 0L;
    private volatile int failuresSinceLastLog = 0;
    private volatile String lastSignedMessage = null;
    private volatile String lastStatusJson = "{}";
    private volatile String lastResult = "No updates sent yet.";

    public WebServerManager(JavaPlugin plugin,
                            TeamManager teamManager,
                            CreditManager creditManager,
                            TierManager tierManager,
                            PhaseManager phaseManager,
                            EliminationManager eliminationManager,
                            EventManager eventManager,
                            DraftManager draftManager,
                            StatsManager statsManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.creditManager = creditManager;
        this.tierManager = tierManager;
        this.phaseManager = phaseManager;
        this.eliminationManager = eliminationManager;
        this.eventManager = eventManager;
        this.draftManager = draftManager;
        this.statsManager = statsManager;
    }

    // ------------------------------------------------------------------ lifecycle

    public void start() {
        try {
            signer = WebSigner.loadOrCreate(plugin.getDataFolder(), plugin.getLogger());
            if (signer.isNewlyGenerated()) {
                plugin.getLogger().warning("==========================================================");
                plugin.getLogger().warning("[WebSync] Created a NEW website signing key.");
                plugin.getLogger().warning("[WebSync] The website ignores updates until its public key matches this one:");
                plugin.getLogger().warning(signer.getPublicKeyBase64());
                plugin.getLogger().warning("[WebSync] Put it in the website's src/config.ts (SIGNING_PUBLIC_KEY), or upload");
                plugin.getLogger().warning("[WebSync] the web-signing-key.properties file that matches the website instead.");
                plugin.getLogger().warning("==========================================================");
            } else {
                plugin.getLogger().info("[WebSync] Signing key loaded. Public key: " + signer.getPublicKeyBase64());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[WebSync] Could not load or create the signing key, website sync disabled: " + e.getMessage());
            signer = null;
        }

        startHttpApi();

        running = true;
        tickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tick(false), 40L, TICK_INTERVAL);
    }

    public void stop() {
        running = false;
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        if (server != null) {
            server.stop(1);
            server = null;
            plugin.getLogger().info("WebServer stopped.");
        }
        sender.shutdown();
    }

    private boolean syncEnabled() {
        return plugin.getConfig().getBoolean("web-sync.enabled", true);
    }

    private String ntfyUrl() {
        String base = plugin.getConfig().getString("web-sync.ntfy-server", "https://ntfy.sh").trim();
        String topic = plugin.getConfig().getString("web-sync.ntfy-topic", "willymoneysmp_leaderboard_live").trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/" + topic;
    }

    private long heartbeatMs() {
        return Math.max(15, plugin.getConfig().getInt("web-sync.heartbeat-seconds", 60)) * 1000L;
    }

    public String getPublicKey() { return signer != null ? signer.getPublicKeyBase64() : null; }
    public String getLastResult() { return lastResult; }

    // ------------------------------------------------------------------ pushing

    /** Ask for an update to go out on the next server tick (safe to call from anywhere). */
    public void requestPush() {
        if (!running || pushQueued) return;
        pushQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            pushQueued = false;
            tick(true);
        });
    }

    /** Used by /smp sync: always sends, then reports the result back through the callback (async thread). */
    public void forcePush(Consumer<String> callback) {
        if (signer == null) {
            callback.accept("Website sync is disabled: the signing key could not be loaded. Check the console.");
            return;
        }
        String body = buildPayloadBody();
        long now = System.currentTimeMillis();
        lastPayloadBody = body;
        lastPushAt = now;
        send(withTimestamp(body, now), callback);
    }

    /** Runs on the main thread. */
    private void tick(boolean urgent) {
        if (!running || signer == null) return;

        String body = buildPayloadBody();
        lastStatusJson = buildStatusJson();
        if (!syncEnabled()) return;

        long now = System.currentTimeMillis();
        boolean changed = retrySoon || !body.equals(lastPayloadBody);
        boolean heartbeatDue = now - lastPushAt >= heartbeatMs();

        if (!changed && !heartbeatDue) return;
        // One request at a time, and respect the back-off after a failure. Nothing is lost by
        // waiting: the next tick sends the latest snapshot, which replaces anything skipped.
        if (sendInFlight || now < backoffUntil) return;
        refillTokens(now);
        if (pushTokens < 1) return;

        pushTokens -= 1;
        retrySoon = false;
        lastPayloadBody = body;
        lastPushAt = now;
        send(withTimestamp(body, now), null);
    }

    private void refillTokens(long now) {
        long elapsed = now - tokensUpdatedAt;
        if (elapsed > 0) {
            pushTokens = Math.min(PUSH_BURST, pushTokens + (double) elapsed / PUSH_REFILL_MS);
            tokensUpdatedAt = now;
        }
    }

    /** Called on the sender thread after each attempt. */
    private void recordResult(boolean ok, boolean rateLimited, String result) {
        if (ok) {
            if (failuresSinceLastLog > 0) {
                plugin.getLogger().info("[WebSync] Website sync recovered after " + failuresSinceLastLog + " failed attempt(s).");
            }
            failuresSinceLastLog = 0;
            backoffMs = 0L;
            backoffUntil = 0L;
            return;
        }
        retrySoon = true;
        backoffMs = backoffMs == 0L ? BACKOFF_START_MS : Math.min(BACKOFF_MAX_MS, backoffMs * 2);
        if (rateLimited) {
            // ntfy is telling us we used up our allowance: start from an empty bucket.
            try {
                Bukkit.getScheduler().runTask(plugin, () -> pushTokens = 0);
            } catch (Exception ignored) {
                // Plugin is disabling.
            }
        }
        backoffUntil = System.currentTimeMillis() + backoffMs;
        failuresSinceLastLog++;
        // Log the first failure, then only every 10th, so the console isn't flooded.
        if (failuresSinceLastLog == 1 || failuresSinceLastLog % 10 == 0) {
            plugin.getLogger().warning("[WebSync] " + result + " (attempt " + failuresSinceLastLog
                    + ", next try in " + (backoffMs / 1000) + "s)");
        }
    }

    private static String withTimestamp(String body, long now) {
        // body is a JSON object "{...}"; prepend version + timestamp fields.
        return "{\"v\":2,\"t\":" + now + "," + body.substring(1);
    }

    private void send(String payload, Consumer<String> callback) {
        final String url = ntfyUrl();
        sendInFlight = true;
        try {
            sender.submit(() -> {
                String result;
                try {
                    String message = "v2." + signer.sign(payload) + "." + payload;
                    lastSignedMessage = message;
                    byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                    if (bytes.length > NTFY_MESSAGE_LIMIT && !warnedAboutSize) {
                        warnedAboutSize = true;
                        plugin.getLogger().warning("[WebSync] Update is " + bytes.length + " bytes (ntfy limit " + NTFY_MESSAGE_LIMIT
                                + "). It will be sent as an attachment; the website handles this but it is slower.");
                    }
                    int code = post(url, bytes);
                    boolean ok = code >= 200 && code < 300;
                    result = ok ? "Website updated (HTTP " + code + ")."
                            : "Cloud relay returned HTTP " + code + (code == 429 ? " (rate limited)" : "");
                    recordResult(ok, code == 429, result);
                } catch (Exception e) {
                    result = "Push failed: " + e.getMessage();
                    recordResult(false, false, result);
                } finally {
                    sendInFlight = false;
                }
                lastResult = result;
                if (callback != null) callback.accept(result);
            });
        } catch (Exception e) {
            // Executor shut down (plugin disabling) - nothing to do.
            sendInFlight = false;
        }
    }

    /** Returns the HTTP status code. */
    private int post(String url, byte[] body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "WillyMoneySMP-Plugin");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }
        int code = conn.getResponseCode();
        try (InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream()) {
            if (is != null) is.readAllBytes();
        }
        return code;
    }

    // ------------------------------------------------------------------ payload (main thread only)

    /**
     * Compact JSON snapshot (without version/timestamp). Keys:
     *  p=phase, on/max=players, k=KOTH active, b=bingo active,
     *  T=teams [name, hex, bingoClaims, members[[name, tier, gameCredits, draftCredits, eliminated, kills, deaths]]] (captain first),
     *  pool=undrafted tiered players [[name, tier]], d=live auction or null, h=recent picks [[player, team, price]].
     */
    String buildPayloadBody() {
        List<Team> teams = new ArrayList<>(teamManager.getTeams());
        Map<Team, Integer> totals = new HashMap<>();
        for (Team team : teams) totals.put(team, teamTotal(team));
        teams.sort((a, b) -> {
            int c = Integer.compare(totals.get(b), totals.get(a));
            return c != 0 ? c : a.getName().compareToIgnoreCase(b.getName());
        });

        Map<Team, Integer> bingoClaims = new HashMap<>();
        if (eventManager != null) {
            for (Team claimer : eventManager.getBingoBoard().values()) {
                if (claimer != null) bingoClaims.merge(claimer, 1, Integer::sum);
            }
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"p\":\"").append(phaseManager.getCurrentPhase().name()).append("\",");
        String address = plugin.getConfig().getString("server-address", "").trim();
        if (!address.isEmpty()) sb.append("\"ip\":").append(str(address)).append(",");
        sb.append("\"on\":").append(Bukkit.getOnlinePlayers().size()).append(",");
        sb.append("\"max\":").append(Bukkit.getMaxPlayers()).append(",");
        sb.append("\"k\":").append(eventManager != null && eventManager.isKothActive() ? 1 : 0).append(",");
        sb.append("\"b\":").append(eventManager != null && eventManager.isBingoActive() ? 1 : 0).append(",");

        // Teams
        sb.append("\"T\":[");
        boolean firstTeam = true;
        for (Team team : teams) {
            if (!firstTeam) sb.append(",");
            firstTeam = false;
            sb.append("[").append(str(team.getName())).append(",")
              .append(str(hexFor(team.getColorCode()))).append(",")
              .append(bingoClaims.getOrDefault(team, 0)).append(",[");

            List<UUID> members = new ArrayList<>(team.getMembers());
            members.sort((a, b) -> {
                if (team.isCaptain(a)) return -1;
                if (team.isCaptain(b)) return 1;
                int c = Integer.compare(tierRank(tierManager.getTier(a)), tierRank(tierManager.getTier(b)));
                return c != 0 ? c : nameOf(a).compareToIgnoreCase(nameOf(b));
            });

            boolean firstMember = true;
            for (UUID m : members) {
                if (!firstMember) sb.append(",");
                firstMember = false;
                PlayerTier tier = team.isCaptain(m) ? PlayerTier.CAPTAIN : tierManager.getTier(m);
                boolean elim = eliminationManager != null && eliminationManager.isEliminated(m);
                sb.append("[").append(str(nameOf(m))).append(",")
                  .append(str(tier.name())).append(",")
                  .append(creditManager.getGameCredits(m)).append(",")
                  .append(creditManager.getDraftCredits(m)).append(",")
                  .append(elim ? 1 : 0).append(",")
                  .append(statsManager != null ? statsManager.getKills(m) : 0).append(",")
                  .append(statsManager != null ? statsManager.getDeaths(m) : 0).append("]");
            }
            sb.append("]]");
        }
        sb.append("],");

        // Draft pool: tiered players (A/B/C) not yet on a team
        List<Map.Entry<UUID, PlayerTier>> pool = new ArrayList<>();
        for (Map.Entry<UUID, PlayerTier> e : tierManager.getAllTiers().entrySet()) {
            PlayerTier t = e.getValue();
            if ((t == PlayerTier.A || t == PlayerTier.B || t == PlayerTier.C)
                    && teamManager.getTeamByPlayer(e.getKey()) == null) {
                pool.add(e);
            }
        }
        pool.sort((a, b) -> {
            int c = Integer.compare(tierRank(a.getValue()), tierRank(b.getValue()));
            return c != 0 ? c : nameOf(a.getKey()).compareToIgnoreCase(nameOf(b.getKey()));
        });
        sb.append("\"pool\":[");
        for (int i = 0; i < pool.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("[").append(str(nameOf(pool.get(i).getKey()))).append(",")
              .append(str(pool.get(i).getValue().name())).append("]");
        }
        sb.append("],");

        // Live auction
        sb.append("\"d\":");
        if (draftManager != null && draftManager.isDraftActive()) {
            sb.append("{\"n\":").append(str(draftManager.getCurrentNomineeName()))
              .append(",\"tr\":").append(str(draftManager.getNomineeTierName()))
              .append(",\"bid\":").append(draftManager.getHighestBid())
              .append(",\"by\":").append(strOrNull(draftManager.getHighestBidderName()))
              .append(",\"tm\":").append(strOrNull(draftManager.getHighestBidderTeamName()))
              .append(",\"end\":").append(draftManager.getEndsAtMillis())
              .append(",\"len\":").append(draftManager.getAuctionSeconds())
              .append(",\"inc\":").append(draftManager.getBidIncrement())
              .append("}");
        } else {
            sb.append("null");
        }
        sb.append(",");

        // Banned items (from /banneditems), so the website's Banned Items tab always matches the server
        if (plugin instanceof me.moneysmp.MoneySMP smp && smp.getBannedItemManager() != null) {
            sb.append("\"ban\":[");
            boolean firstBan = true;
            for (BannedItemManager.BanEntry ban : smp.getBannedItemManager().getBannedItems().values()) {
                if (!firstBan) sb.append(",");
                firstBan = false;
                sb.append("[").append(str(BannedItemManager.formatMaterialName(ban.getMaterial()))).append(",")
                  .append(str(shorten(ban.getReason(), 160))).append(",")
                  .append(str(shorten(ban.getAlternative(), 80))).append("]");
            }
            sb.append("],");
        }

        // Recent picks
        sb.append("\"h\":[");
        if (draftManager != null) {
            List<String[]> picks = draftManager.getRecentPicks();
            for (int i = 0; i < picks.size(); i++) {
                if (i > 0) sb.append(",");
                String[] p = picks.get(i);
                sb.append("[").append(str(p[0])).append(",").append(str(p[1])).append(",").append(parseIntSafe(p[2])).append("]");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildStatusJson() {
        return "{\"phase\":\"" + phaseManager.getCurrentPhase().name() + "\"," +
                "\"onlinePlayers\":" + Bukkit.getOnlinePlayers().size() + "," +
                "\"kothActive\":" + (eventManager != null && eventManager.isKothActive()) + "," +
                "\"bingoActive\":" + (eventManager != null && eventManager.isBingoActive()) + "," +
                "\"draftActive\":" + (draftManager != null && draftManager.isDraftActive()) + "}";
    }

    private int teamTotal(Team team) {
        int total = 0;
        for (UUID m : team.getMembers()) total += creditManager.getGameCredits(m);
        return total;
    }

    private static int tierRank(PlayerTier tier) {
        switch (tier) {
            case CAPTAIN: return 0;
            case A: return 1;
            case B: return 2;
            case C: return 3;
            default: return 4;
        }
    }

    private static String nameOf(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private static String hexFor(String colorCode) {
        if (colorCode == null || colorCode.length() < 2) return "#eab308";
        switch (Character.toLowerCase(colorCode.charAt(colorCode.length() - 1))) {
            case '9': case '1': return "#3b82f6";
            case 'c': case '4': return "#ef4444";
            case 'e': return "#eab308";
            case 'a': case '2': return "#22c55e";
            case 'd': case '5': return "#a855f7";
            case '6': return "#f97316";
            case 'b': case '3': return "#06b6d4";
            case 'f': case '7': return "#e2e8f0";
            default: return "#eab308";
        }
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("§.", "").trim();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static String strOrNull(String s) {
        return s == null ? "null" : str(s);
    }

    /** JSON string literal with full escaping. */
    static String str(String s) {
        if (s == null) s = "";
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }

    // ------------------------------------------------------------------ optional local HTTP API

    private void startHttpApi() {
        if (!plugin.getConfig().getBoolean("web-server.enabled", false)) return;
        int port = plugin.getConfig().getInt("web-server.port", 8085);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/leaderboard", ex -> respond(ex, "text/plain; charset=UTF-8", lastSignedMessage));
            server.createContext("/api/status", ex -> respond(ex, "application/json; charset=UTF-8", lastStatusJson));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("Local REST API started on port " + port + " (/api/leaderboard, /api/status)");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not start local REST API on port " + port + ": " + e.getMessage());
        }
    }

    private static void respond(HttpExchange exchange, String contentType, String body) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (body == null) {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
            return;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
