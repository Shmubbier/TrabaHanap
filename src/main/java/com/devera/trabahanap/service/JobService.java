package com.devera.trabahanap.service;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.FirebaseInitializer;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class JobService {

    private static final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String projectId;
    private static final String JOBS_COLLECTION = "jobs";

    public JobService() {
        this.projectId = Config.get("firebase.projectId");
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("Missing firebase.projectId in config.properties");
        }
    }

    /**
     * Add a Job to Firestore asynchronously. Returns the document ID.
     */
    public CompletableFuture<String> addJob(Job job) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            Map<String, Object> data = job.toMap();

            // Build Firestore "fields" JSON
            JsonObject fields = new JsonObject();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                switch (key) {
                    case "skills":
                        if (value instanceof List<?> list) {
                            JsonArray arr = new JsonArray();
                            for (Object o : list) {
                                if (o != null) {
                                    JsonObject skillObj = new JsonObject();
                                    skillObj.addProperty("stringValue", o.toString());
                                    arr.add(skillObj);
                                }
                            }
                            JsonObject arrWrapper = new JsonObject();
                            arrWrapper.add("values", arr);

                            JsonObject arrayValue = new JsonObject();
                            arrayValue.add("arrayValue", arrWrapper);

                            fields.add(key, arrayValue);
                        }
                        break;
                    case "budgetMin", "budgetMax":
                        if (value instanceof Number n) {
                            JsonObject numObj = new JsonObject();
                            numObj.addProperty("doubleValue", n.doubleValue());
                            fields.add(key, numObj);
                        }
                        break;
                    case "timestamp":
                        if (value instanceof Number n) {
                            JsonObject ts = new JsonObject();
                            ts.addProperty("timestampValue", new java.util.Date(n.longValue()).toInstant().toString());
                            fields.add(key, ts);
                        }
                        break;
                    default:
                        JsonObject strObj = new JsonObject();
                        strObj.addProperty("stringValue", value.toString());
                        fields.add(key, strObj);
                        break;
                }
            }

            JsonObject doc = new JsonObject();
            doc.add("fields", fields);

            String url = String.format(
                    "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/jobs",
                    projectId
            );

            String accessToken = obtainAccessTokenForFirestore();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(doc), StandardCharsets.UTF_8))
                    .build();

            http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(resp -> {
                        int status = resp.statusCode();
                        if (status >= 200 && status < 300) return resp.body();
                        else throw new RuntimeException("Failed to add job: HTTP " + status + " - " + resp.body());
                    })
                    .thenAccept(body -> {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.has("name")) {
                            String name = json.get("name").getAsString();
                            String[] parts = name.split("/");
                            future.complete(parts[parts.length - 1]);
                        } else {
                            future.completeExceptionally(new RuntimeException("Firestore response missing 'name' field"));
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
     * Obtain access token for Firestore (service account or user idToken).
     */
    private String obtainAccessTokenForFirestore() throws IOException {
        try {
            String adminToken = FirebaseInitializer.getAccessToken();
            if (adminToken != null && !adminToken.isBlank()) return adminToken;
        } catch (Throwable ignored) {}

        Optional<String> maybeIdToken = SessionManager.get().getIdToken();
        if (maybeIdToken.isPresent()) return maybeIdToken.get();

        throw new IOException("Could not obtain Firestore access token");
    }

    /**
     * Fetch all jobs from Firestore. Returns CompletableFuture with List<Job>.
     * Keeps existing functionality (timestamp descending sort).
     */
    public CompletableFuture<List<Job>> getAllJobs() {
        String url = String.format(
                "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s",
                projectId, JOBS_COLLECTION
        );

        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = obtainAccessTokenForFirestore();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = resp.statusCode();
                String body = resp.body();

                if (status >= 200 && status < 300) {
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    List<Job> out = new ArrayList<>();
                    if (json != null && json.has("documents")) {
                        for (var el : json.getAsJsonArray("documents")) {
                            JsonObject doc = el.getAsJsonObject();
                            String name = doc.has("name") ? doc.get("name").getAsString() : null;
                            String docId = name != null ? name.substring(name.lastIndexOf("/") + 1) : null;
                            JsonObject fields = doc.has("fields") ? doc.getAsJsonObject("fields") : null;
                            Map<String, Object> m = fieldsToMap(fields);
                            out.add(Job.fromMap(docId, m));
                        }
                    }
                    out.sort(Comparator.comparingLong(Job::getTimestamp).reversed());
                    return out;
                } else {
                    throw new RuntimeException("Failed to fetch jobs: HTTP " + status + " - " + body);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Convert Firestore document fields JSON into a simple Map<String,Object>
     */
    private static Map<String, Object> fieldsToMap(JsonObject fields) {
        Map<String, Object> m = new HashMap<>();
        if (fields == null) return m;
        for (String key : fields.keySet()) {
            JsonObject v = fields.getAsJsonObject(key);
            if (v.has("stringValue")) m.put(key, v.get("stringValue").getAsString());
            else if (v.has("integerValue")) m.put(key, Long.parseLong(v.get("integerValue").getAsString()));
            else if (v.has("doubleValue")) m.put(key, v.get("doubleValue").getAsDouble());
            else if (v.has("timestampValue")) m.put(key, v.get("timestampValue").getAsString());
            else if (v.has("arrayValue")) {
                List<String> list = new ArrayList<>();
                JsonArray arr = v.getAsJsonObject("arrayValue").getAsJsonArray("values");
                if (arr != null) {
                    for (var el : arr) list.add(el.getAsJsonObject().get("stringValue").getAsString());
                }
                m.put(key, list);
            } else m.put(key, v.toString());
        }
        return m;
    }
}
