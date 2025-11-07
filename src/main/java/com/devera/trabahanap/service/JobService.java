package com.devera.trabahanap.service;

import com.devera.trabahanap.core.Job;
import com.devera.trabahanap.system.Config;
import com.devera.trabahanap.system.FirebaseInitializer;
import com.devera.trabahanap.system.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for reading/writing Job documents to Firestore via REST API.
 *
 * Notes:
 * - Uses FirebaseInitializer to ensure credentials are available. It is expected that
 *   FirebaseInitializer exposes a method to obtain an OAuth2 access token for Firestore.
 *   If your FirebaseInitializer implementation differs, adjust getAccessToken() usage below.
 * - All network calls are executed off the JavaFX thread. Callers who need UI updates
 *   should use the returned CompletableFuture and perform Platform.runLater(...) there.
 */
public class JobService {

    private static final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    // Firestore collection name for jobs
    private static final String JOBS_COLLECTION = "jobs";

    // Project id read from config.properties (key: firebase.projectId)
    private final String projectId;

    public JobService() {
        this.projectId = Config.get("firebase.projectId");
    }

    /**
     * Add a Job to Firestore. Returns a CompletableFuture that completes when write finished.
     * On success the job's jobId will be set to the created document id.
     *
     * Usage example:
     *   jobService.addJob(job).whenComplete((docId, thr) -> {
     *       Platform.runLater(() -> { ... update UI ... });
     *   });
     *
     * @param job job to add
     * @return CompletableFuture<String> containing created document id
     */
    public CompletableFuture<String> addJob(Job job) {
        Objects.requireNonNull(job, "job must not be null");

        // ensure initializer (may throw runtime exception if misconfigured)
        FirebaseInitializer.ensureInitialized();

        // Build Firestore REST URL:
        // POST https://firestore.googleapis.com/v1/projects/{projectId}/databases/(default)/documents/{collection}
        String url = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s", projectId, JOBS_COLLECTION);

        // Convert Job -> Firestore document fields JSON (Firestore REST expects 'fields' wrapper)
        JsonObject doc = new JsonObject();
        JsonObject fields = new JsonObject();

        // Helper to add string field
        Map<String, Object> map = job.toMap();
        map.forEach((k, v) -> {
            if (v == null) return;
        });

        addStringField(fields, "title", job.getTitle());
        addStringField(fields, "companyName", job.getCompanyName());
        addStringField(fields, "location", job.getLocation());
        addStringField(fields, "description", job.getDescription());
        addStringField(fields, "salaryRange", job.getSalaryRange());
        addStringField(fields, "postedByUserId", job.getPostedByUserId());
        // timestamp as integer value (epoch millis)
        JsonObject ts = new JsonObject();
        ts.addProperty("integerValue", Long.toString(job.getTimestamp() > 0 ? job.getTimestamp() : Instant.now().toEpochMilli()));
        fields.add("timestamp", ts);

        doc.add("fields", fields);

        String body = gson.toJson(doc);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String accessToken = obtainAccessTokenForFirestore();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = resp.statusCode();
                String respBody = resp.body();

                if (status >= 200 && status < 300) {
                    JsonObject json = gson.fromJson(respBody, JsonObject.class);
                    // Firestore returns name: projects/{projectId}/databases/(default)/documents/{collection}/{documentId}
                    if (json != null && json.has("name")) {
                        String name = json.get("name").getAsString();
                        String[] parts = name.split("/");
                        String docId = parts.length > 0 ? parts[parts.length - 1] : null;
                        // set jobId for caller convenience
                        if (docId != null) {
                            job.setJobId(docId);
                        }
                        return docId;
                    } else {
                        throw new RuntimeException("Unexpected Firestore response: missing name field");
                    }
                } else {
                    throw new RuntimeException("Failed to add job. HTTP " + status + ". Body: " + respBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Fetch all jobs from Firestore. Returns CompletableFuture with List<Job>.
     *
     * Note: results are not ordered by default. If you want ordering, change the REST call to use structuredQuery.
     *
     * Usage:
     *   jobService.getAllJobs().whenComplete((jobs, thr) -> {
     *       Platform.runLater(() -> { ... update UI with jobs ... });
     *   });
     *
     * @return CompletableFuture<List<Job>>
     */
    public CompletableFuture<List<Job>> getAllJobs() {
        FirebaseInitializer.ensureInitialized();

        // GET documents list:
        // GET https://firestore.googleapis.com/v1/projects/{projectId}/databases/(default)/documents/{collection}
        String url = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s", projectId, JOBS_COLLECTION);

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
                        JsonArray docs = json.getAsJsonArray("documents");
                        for (JsonElement el : docs) {
                            JsonObject doc = el.getAsJsonObject();
                            String name = doc.has("name") ? doc.get("name").getAsString() : null;
                            String docId = null;
                            if (name != null) {
                                String[] parts = name.split("/");
                                docId = parts[parts.length - 1];
                            }
                            JsonObject fields = doc.has("fields") ? doc.getAsJsonObject("fields") : null;
                            Map<String, Object> m = fieldsToMap(fields);
                            Job j = Job.fromMap(docId, m);
                            out.add(j);
                        }
                    }
                    // Sort by timestamp descending (most recent first)
                    out.sort(Comparator.comparingLong(Job::getTimestamp).reversed());
                    return out;
                } else {
                    throw new RuntimeException("Failed to fetch jobs. HTTP " + status + ". Body: " + body);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // -----------------------
    // Helper methods
    // -----------------------

    private static void addStringField(JsonObject parent, String fieldName, String value) {
        if (value == null) return;
        JsonObject v = new JsonObject();
        v.addProperty("stringValue", value);
        parent.add(fieldName, v);
    }

    /**
     * Convert Firestore document fields JSON into a simple Map<String,Object>.
     * Handles stringValue and integerValue. Extend as needed.
     */
    private static Map<String, Object> fieldsToMap(JsonObject fields) {
        Map<String, Object> m = new HashMap<>();
        if (fields == null) return m;
        for (String key : fields.keySet()) {
            JsonObject v = fields.getAsJsonObject(key);
            if (v == null) continue;
            if (v.has("stringValue")) {
                m.put(key, v.get("stringValue").getAsString());
            } else if (v.has("integerValue")) {
                try {
                    m.put(key, Long.parseLong(v.get("integerValue").getAsString()));
                } catch (NumberFormatException ex) {
                    m.put(key, 0L);
                }
            } else if (v.has("doubleValue")) {
                m.put(key, v.get("doubleValue").getAsDouble());
            } else if (v.has("timestampValue")) {
                m.put(key, v.get("timestampValue").getAsString());
            } else {
                // fallback to raw JSON element
                m.put(key, v.toString());
            }
        }
        return m;
    }

    /**
     * Obtain an access token suitable for Firestore REST calls.
     *
     * This method first tries to obtain an admin/service account access token via FirebaseInitializer.
     * If that fails it falls back to the user's idToken from SessionManager (less preferred, but works for security rules allowing user-write).
     *
     * Adjust this method to your project's authentication flow if FirebaseInitializer exposes a different API.
     */
    private String obtainAccessTokenForFirestore() throws IOException {
        // Try to get admin/service account token from FirebaseInitializer (recommended)
        try {
            // FirebaseInitializer is expected to expose a helper like getAccessToken().
            // If your initializer provides different API, update this call accordingly.
            String adminToken = FirebaseInitializer.getAccessToken(); // <-- expected to exist
            if (adminToken != null && !adminToken.isBlank()) {
                return adminToken;
            }
        } catch (Throwable ignored) {
            // ignore and try fallback
        }

        // Fallback: use idToken from SessionManager (the signed-in user's token)
        Optional<String> maybeIdToken = SessionManager.get().getIdToken();
        if (maybeIdToken.isPresent()) {
            return maybeIdToken.get();
        }

        throw new IOException("Could not obtain access token for Firestore. Ensure FirebaseInitializer provides an access token or a user is signed-in.");
    }
}
