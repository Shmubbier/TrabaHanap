package com.devera.trabahanap.service;

import com.devera.trabahanap.system.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal user service to fetch basic profile info from Firebase using the idToken.
 * Uses Identity Toolkit: accounts:lookup
 */
public class FirebaseUserService {

    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Calls https://identitytoolkit.googleapis.com/v1/accounts:lookup?key={API_KEY}
     * The apiKey must be provided via Config or Session; here we expect caller to pass idToken.
     *
     * Returns CompletableFuture of JsonObject with user info or completes exceptionally on error.
     */
    public CompletableFuture<JsonObject> getUserProfile(String apiKey, String idToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + apiKey;
                JsonObject payload = new JsonObject();
                payload.addProperty("idToken", idToken);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = resp.statusCode();
                String body = resp.body();
                if (status != 200) {
                    throw new IOException("Failed to lookup user profile: HTTP " + status + " - " + body);
                }
                JsonObject json = gson.fromJson(body, JsonObject.class);
                // The response contains "users": [ { ... } ]
                if (json.has("users") && json.getAsJsonArray("users").size() > 0) {
                    return json.getAsJsonArray("users").get(0).getAsJsonObject();
                } else {
                    throw new IOException("No user returned from accounts:lookup");
                }
            } catch (IOException | InterruptedException | JsonParseException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
