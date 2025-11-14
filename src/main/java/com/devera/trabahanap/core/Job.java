package com.devera.trabahanap.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * Job model used across the application and stored in Firestore.
 *
 * Fields:
 *  - jobId: Firestore document id (may be null before creation)
 *  - title, companyName, location, description, salaryRange
 *  - postedByUserId: id of the session user who posted the job
 *  - timestamp: epoch millis when the job was posted
 *
 * Extended fields:
 *  - budgetMin, budgetMax (Double)
 *  - categoryDisplay (user-facing), category (key)
 *  - imageKey (used for local image mapping)
 *  - skills (List<String>)
 *  - experienceLevel (Entry, Intermediate, Expert)
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

    // Extended fields
    private Double budgetMin;
    private Double budgetMax;
    private String categoryDisplay;
    private String category;
    private String imageKey;
    private List<String> skills;
    private String experienceLevel;

    public Job() {}

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

    // New factory with extended fields
    public static Job createForPosting(String title,
                                       String companyName,
                                       String location,
                                       String description,
                                       String salaryRange,
                                       String postedByUserId,
                                       Double budgetMin,
                                       Double budgetMax,
                                       String categoryDisplay,
                                       String categoryKey,
                                       List<String> skills,
                                       String experienceLevel) {
        Job j = new Job(
                null,
                title,
                companyName,
                location,
                description,
                salaryRange,
                postedByUserId,
                Instant.now().toEpochMilli()
        );
        j.setBudgetMin(budgetMin);
        j.setBudgetMax(budgetMax);
        j.setCategoryDisplay(categoryDisplay);
        j.setCategory(categoryKey);
        j.setImageKey(categoryKey);
        j.setSkills(skills != null ? new ArrayList<>(skills) : Collections.emptyList());
        j.setExperienceLevel(experienceLevel);
        return j;
    }

    // Getters / Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
    public String getPostedByUserId() { return postedByUserId; }
    public void setPostedByUserId(String postedByUserId) { this.postedByUserId = postedByUserId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Double getBudgetMin() { return budgetMin; }
    public void setBudgetMin(Double budgetMin) { this.budgetMin = budgetMin; }
    public Double getBudgetMax() { return budgetMax; }
    public void setBudgetMax(Double budgetMax) { this.budgetMax = budgetMax; }
    public String getCategoryDisplay() { return categoryDisplay; }
    public void setCategoryDisplay(String categoryDisplay) { this.categoryDisplay = categoryDisplay; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

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
        if (budgetMin != null) m.put("budgetMin", budgetMin);
        if (budgetMax != null) m.put("budgetMax", budgetMax);
        if (categoryDisplay != null) m.put("categoryDisplay", categoryDisplay);
        if (category != null) m.put("category", category);
        if (imageKey != null) m.put("imageKey", imageKey);
        if (skills != null) m.put("skills", new ArrayList<>(skills));
        if (experienceLevel != null) m.put("experienceLevel", experienceLevel);
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

        o = map.get("budgetMin");
        if (o instanceof Number) j.setBudgetMin(((Number) o).doubleValue());
        o = map.get("budgetMax");
        if (o instanceof Number) j.setBudgetMax(((Number) o).doubleValue());
        o = map.get("categoryDisplay");
        if (o != null) j.setCategoryDisplay(o.toString());
        o = map.get("category");
        if (o != null) j.setCategory(o.toString());
        o = map.get("imageKey");
        if (o != null) j.setImageKey(o.toString());
        o = map.get("experienceLevel");
        if (o != null) j.setExperienceLevel(o.toString());
        o = map.get("skills");
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> raw = (List<Object>) o;
            List<String> out = new ArrayList<>();
            for (Object item : raw) {
                if (item != null) out.add(item.toString());
            }
            j.setSkills(out);
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
                ", budgetMin=" + budgetMin +
                ", budgetMax=" + budgetMax +
                ", categoryDisplay='" + categoryDisplay + '\'' +
                ", category='" + category + '\'' +
                ", imageKey='" + imageKey + '\'' +
                ", skills=" + skills +
                ", experienceLevel='" + experienceLevel + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job)) return false;
        Job job = (Job) o;
        return Objects.equals(jobId, job.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }
}
