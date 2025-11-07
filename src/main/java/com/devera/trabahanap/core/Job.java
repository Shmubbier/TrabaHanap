package com.devera.trabahanap.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Job model used across the application and stored in Firestore.
 *
 * Fields:
 *  - jobId: Firestore document id (may be null before creation)
 *  - title, companyName, location, description, salaryRange
 *  - postedByUserId: id of the session user who posted the job
 *  - timestamp: epoch millis when the job was posted
 */
public class Job implements Serializable {

    private String jobId;
    private String title;
    private String companyName;
    private String location;
    private String description;
    private String salaryRange;
    private String postedByUserId;
    private long timestamp;

    public Job() {
        // Default constructor required for some deserializers / frameworks
    }

    public Job(String jobId,
               String title,
               String companyName,
               String location,
               String description,
               String salaryRange,
               String postedByUserId,
               long timestamp) {
        this.jobId = jobId;
        this.title = title;
        this.companyName = companyName;
        this.location = location;
        this.description = description;
        this.salaryRange = salaryRange;
        this.postedByUserId = postedByUserId;
        this.timestamp = timestamp;
    }

    public static Job createForPosting(String title,
                                       String companyName,
                                       String location,
                                       String description,
                                       String salaryRange,
                                       String postedByUserId) {
        return new Job(
                null,
                title,
                companyName,
                location,
                description,
                salaryRange,
                postedByUserId,
                Instant.now().toEpochMilli()
        );
    }

    // Getters / Setters

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getPostedByUserId() {
        return postedByUserId;
    }

    public void setPostedByUserId(String postedByUserId) {
        this.postedByUserId = postedByUserId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convert to a Map suitable for Firestore (REST/JSON).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("companyName", companyName);
        m.put("location", location);
        m.put("description", description);
        m.put("salaryRange", salaryRange);
        m.put("postedByUserId", postedByUserId);
        m.put("timestamp", timestamp);
        return m;
    }

    /**
     * Construct a Job from a Map (e.g. parsed Firestore document fields).
     * Leaves jobId to be set by caller if needed.
     */
    public static Job fromMap(String jobId, Map<String, Object> map) {
        if (map == null) return null;
        Job j = new Job();
        j.setJobId(jobId);
        Object o;
        o = map.get("title");
        j.setTitle(o != null ? o.toString() : null);
        o = map.get("companyName");
        j.setCompanyName(o != null ? o.toString() : null);
        o = map.get("location");
        j.setLocation(o != null ? o.toString() : null);
        o = map.get("description");
        j.setDescription(o != null ? o.toString() : null);
        o = map.get("salaryRange");
        j.setSalaryRange(o != null ? o.toString() : null);
        o = map.get("postedByUserId");
        j.setPostedByUserId(o != null ? o.toString() : null);
        o = map.get("timestamp");
        if (o instanceof Number) {
            j.setTimestamp(((Number) o).longValue());
        } else if (o instanceof String) {
            try {
                j.setTimestamp(Long.parseLong((String) o));
            } catch (NumberFormatException ignored) {
                j.setTimestamp(0L);
            }
        } else {
            j.setTimestamp(0L);
        }
        return j;
    }

    @Override
    public String toString() {
        return "Job{" +
                "jobId='" + jobId + '\'' +
                ", title='" + title + '\'' +
                ", companyName='" + companyName + '\'' +
                ", location='" + location + '\'' +
                ", description='" + (description != null ? (description.length() > 80 ? description.substring(0, 80) + "..." : description) : null) + '\'' +
                ", salaryRange='" + salaryRange + '\'' +
                ", postedByUserId='" + postedByUserId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;
        return timestamp == job.timestamp && Objects.equals(jobId, job.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, timestamp);
    }
}
