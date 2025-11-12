package com.devera.trabahanap.service;

import com.devera.trabahanap.system.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class FirestoreService {

    private static final Gson gson = new Gson();
    private final String projectId;

    public FirestoreService() {
        this.projectId = Config.get("firebase.projectId");
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("Missing firebase.projectId in config.properties");
        }
    }

    private String getFirestoreUrl(String uid) {
        return String.format(
                "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s",
                projectId, uid
        );
    }

    /**
     * Writes user profile to Firestore users/{uid}.
     * This does NOT require a service account; uses ID Token (client-side auth)
     */
    public CompletableFuture<Void> writeUserProfile(String idToken, String uid, String displayName, String email) {
        try {
            String url = getFirestoreUrl(uid);

            JsonObject fields = new JsonObject();
            fields.add("uid", wrapString(uid));
            fields.add("displayName", wrapString(displayName));
            fields.add("email", wrapString(email));

            JsonObject root = new JsonObject();
            root.add("fields", fields);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + idToken)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(root), StandardCharsets.UTF_8))
                    .build();

            return HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            System.out.println("[Firestore] ✅ User saved " + uid);
                        } else {
                            System.err.println("[Firestore] ❌ Failed to save user: "
                                    + response.statusCode() + " — " + response.body());
                            throw new RuntimeException("Firestore error: " + response.body());
                        }
                    });

        } catch (Exception e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /** Firestore string wrapper format */
    private JsonObject wrapString(String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("stringValue", value);
        return obj;
    }

    /**
     * Fetch users/{uid} document and return optional JsonObject of fields.
     * If document doesn't exist, returns Optional.empty()
     */
    public CompletableFuture<java.util.Optional<JsonObject>> getUserProfileDocument(String idToken, String uid) {
        CompletableFuture<java.util.Optional<JsonObject>> future = new CompletableFuture<>();
        try {
            String url = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s",
                    projectId, java.net.URLEncoder.encode(uid, java.nio.charset.StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + idToken)
                    .GET()
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
                        JsonObject responseJson = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        if (responseJson.has("fields")) {
                            future.complete(java.util.Optional.of(responseJson.getAsJsonObject("fields")));
                        } else {
                            future.complete(java.util.Optional.empty());
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Add a Job document to collection 'jobs' (auto-id). Returns created document id.
     *
     * Signature matches callers in PostJobController: addJob(idToken, employerId, title, description, location)
     */
    public CompletableFuture<String> addJob(String idToken, String employerId, String title, String description, String location) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String url = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/jobs", projectId);

            JsonObject doc = new JsonObject();
            JsonObject fields = new JsonObject();
            if (employerId != null) fields.add("employerId", wrapString(employerId));
            if (title != null) fields.add("title", wrapString(title));
            if (description != null) fields.add("description", wrapString(description));
            if (location != null) fields.add("location", wrapString(location));
            JsonObject ts = new JsonObject();
            ts.addProperty("timestampValue", java.time.Instant.now().toString());
            fields.add("postedAt", ts);
            doc.add("fields", fields);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + idToken)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(doc), StandardCharsets.UTF_8))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int status = response.statusCode();
                        if (status >= 200 && status < 300) {
                            return response.body();
                        } else {
                            throw new RuntimeException("Failed to add job: HTTP " + status + " - " + response.body());
                        }
                    })
                    .thenAccept(body -> {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json != null && json.has("name")) {
                            String name = json.get("name").getAsString();
                            String[] parts = name.split("/");
                            String docId = parts[parts.length - 1];
                            future.complete(docId);
                        } else {
                            future.completeExceptionally(new RuntimeException("Unexpected Firestore response: missing name"));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
