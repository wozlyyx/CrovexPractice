package com.crovex.practice.webhook;

import com.crovex.practice.CrovexPractice;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class WebhookManager {

    private final CrovexPractice plugin;
    private final HttpClient httpClient;

    public WebhookManager(CrovexPractice plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
    }

    private void sendWebhook(JsonObject payload) {
        if (!plugin.getConfig().getBoolean("webhook.enabled", false)) {
            return;
        }
        String url = plugin.getConfig().getString("webhook.url", "");
        if (url == null || url.isEmpty() || url.equals("DISCORD_WEBHOOK_URL_HERE")) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "CrovexPractice-Webhook")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() >= 400) {
                                plugin.getLogger().warning("Error sending Discord Webhook! HTTP Code: " + response.statusCode() + " - Response: " + response.body());
                            }
                        })
                        .exceptionally(ex -> {
                            plugin.getLogger().warning("Discord Webhook connection error: " + ex.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                plugin.getLogger().warning("Error creating Discord Webhook: " + e.getMessage());
            }
        });
    }

    public void sendMatchEndWebhook(String winner, String loser, String kitName, String matchType, 
                                    int eloChange, int winnerNewElo, int loserNewElo, int durationSeconds, boolean ranked) {
        if (!plugin.getConfig().getBoolean("webhook.events.match-end", true)) {
            return;
        }

        JsonObject payload = new JsonObject();
        String username = plugin.getMessageManager().getRawMessage("webhook.username");
        payload.addProperty("username", username.isEmpty() ? "CrovexPractice" : username);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        
        String title = plugin.getMessageManager().getRawMessage("webhook.match-end.title").replace("%type%", matchType);
        embed.addProperty("title", title.isEmpty() ? "⚔ Match Ended! (" + matchType + ")" : title);
        embed.addProperty("color", ranked ? 3447003 : 3066993); // Blue for ranked, green for unranked
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footer = new JsonObject();
        String footerText = plugin.getMessageManager().getRawMessage("webhook.footer");
        footer.addProperty("text", footerText.isEmpty() ? "CrovexPractice v1.0.0" : footerText);
        embed.add("footer", footer);

        JsonArray fields = new JsonArray();

        // Kit
        JsonObject kitField = new JsonObject();
        String kitLabel = plugin.getMessageManager().getRawMessage("webhook.match-end.kit");
        kitField.addProperty("name", kitLabel.isEmpty() ? "📦 Kit" : kitLabel);
        kitField.addProperty("value", kitName);
        kitField.addProperty("inline", true);
        fields.add(kitField);

        // Duration
        JsonObject durationField = new JsonObject();
        String durationLabel = plugin.getMessageManager().getRawMessage("webhook.match-end.duration");
        durationField.addProperty("name", durationLabel.isEmpty() ? "⏱ Duration" : durationLabel);
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        durationField.addProperty("value", String.format("%02d:%02d", minutes, seconds));
        durationField.addProperty("inline", true);
        fields.add(durationField);

        // Blank Field to balance columns (inline=true)
        JsonObject blankField = new JsonObject();
        blankField.addProperty("name", "\u200B");
        blankField.addProperty("value", "\u200B");
        blankField.addProperty("inline", true);
        fields.add(blankField);

        // Winner
        JsonObject winnerField = new JsonObject();
        String winnerLabel = plugin.getMessageManager().getRawMessage("webhook.match-end.winner");
        winnerField.addProperty("name", winnerLabel.isEmpty() ? "🏆 Winner" : winnerLabel);
        if (ranked) {
            String winnerFormat = plugin.getMessageManager().getRawMessage("webhook.match-end.winner-ranked-format")
                    .replace("%winner%", winner)
                    .replace("%elo_change%", String.valueOf(eloChange))
                    .replace("%new_elo%", String.valueOf(winnerNewElo));
            winnerField.addProperty("value", winnerFormat.isEmpty() ? winner + " (+" + eloChange + " ELO | Yeni: " + winnerNewElo + ")" : winnerFormat);
        } else {
            winnerField.addProperty("value", winner);
        }
        winnerField.addProperty("inline", true);
        fields.add(winnerField);

        // Loser
        JsonObject loserField = new JsonObject();
        String loserLabel = plugin.getMessageManager().getRawMessage("webhook.match-end.loser");
        loserField.addProperty("name", loserLabel.isEmpty() ? "💀 Loser" : loserLabel);
        if (ranked) {
            String loserFormat = plugin.getMessageManager().getRawMessage("webhook.match-end.loser-ranked-format")
                    .replace("%loser%", loser)
                    .replace("%elo_change%", String.valueOf(eloChange))
                    .replace("%new_elo%", String.valueOf(loserNewElo));
            loserField.addProperty("value", loserFormat.isEmpty() ? loser + " (-" + eloChange + " ELO | Yeni: " + loserNewElo + ")" : loserFormat);
        } else {
            loserField.addProperty("value", loser);
        }
        loserField.addProperty("inline", true);
        fields.add(loserField);

        embed.add("fields", fields);
        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhook(payload);
    }

    public void sendFfaStreakWebhook(String playerName, int streakValue) {
        if (!plugin.getConfig().getBoolean("webhook.events.ffa-streak", true)) {
            return;
        }

        JsonObject payload = new JsonObject();
        String username = plugin.getMessageManager().getRawMessage("webhook.username");
        payload.addProperty("username", username.isEmpty() ? "CrovexPractice" : username);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        
        String title = plugin.getMessageManager().getRawMessage("webhook.ffa-streak.title");
        embed.addProperty("title", title.isEmpty() ? "🔥 FFA Win Streak!" : title);
        
        String desc = plugin.getMessageManager().getRawMessage("webhook.ffa-streak.description")
                .replace("%player%", playerName)
                .replace("%streak%", String.valueOf(streakValue));
        embed.addProperty("description", desc.isEmpty() ? "**" + playerName + "** reached a win streak of **" + streakValue + "** in FFA!" : desc);
        embed.addProperty("color", 16753920); // Gold
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footer = new JsonObject();
        String footerText = plugin.getMessageManager().getRawMessage("webhook.footer");
        footer.addProperty("text", footerText.isEmpty() ? "CrovexPractice v1.0.0" : footerText);
        embed.add("footer", footer);

        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhook(payload);
    }

    public void sendAdminActionWebhook(String adminName, String action) {
        if (!plugin.getConfig().getBoolean("webhook.events.admin-actions", true)) {
            return;
        }

        JsonObject payload = new JsonObject();
        String username = plugin.getMessageManager().getRawMessage("webhook.username");
        payload.addProperty("username", username.isEmpty() ? "CrovexPractice" : username);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        
        String title = plugin.getMessageManager().getRawMessage("webhook.admin-action.title");
        embed.addProperty("title", title.isEmpty() ? "⚙ Admin Action" : title);
        
        String desc = plugin.getMessageManager().getRawMessage("webhook.admin-action.description")
                .replace("%admin%", adminName)
                .replace("%action%", action);
        embed.addProperty("description", desc.isEmpty() ? "**" + adminName + "** performed an action:\n" + action : desc);
        embed.addProperty("color", 15158332); // Red
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footer = new JsonObject();
        String footerText = plugin.getMessageManager().getRawMessage("webhook.footer");
        footer.addProperty("text", footerText.isEmpty() ? "CrovexPractice v1.0.0" : footerText);
        embed.add("footer", footer);

        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhook(payload);
    }
}
