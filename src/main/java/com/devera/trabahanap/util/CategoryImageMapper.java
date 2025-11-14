package com.devera.trabahanap.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.*;

/**
 * Local category image mapper (no Firebase Storage).
 *
 * Keys follow your PostJob category keys:
 *  GRAPHIC_DESIGN, WRITING, PROGRAMMING, VIDEO_EDITING, MARKETING,
 *  DATA_ENTRY, CONSULTING, OTHER
 *
 * Images must exist under:
 *  src/main/resources/images/categories/
 * With file names below (jpg):
 *  graphic_design.jpg, writing.jpg, programming.jpg, video_editing.jpg, marketing.jpg,
 *  data_entry.jpg, consulting.jpg, default.jpg
 */
public final class CategoryImageMapper {

    private static final Map<String, String> KEY_TO_RESOURCE = new HashMap<>();
    private static final Map<String, String> KEY_TO_DISPLAY = new LinkedHashMap<>();

    static {
        // Key -> Resource path
        KEY_TO_RESOURCE.put("GRAPHIC_DESIGN", "/images/categories/graphic.jpg");
        KEY_TO_RESOURCE.put("WRITING", "/images/categories/writing.jpg");
        KEY_TO_RESOURCE.put("PROGRAMMING", "/images/categories/programming.jpg");
        KEY_TO_RESOURCE.put("VIDEO_EDITING", "/images/categories/video.jpg");
        KEY_TO_RESOURCE.put("MARKETING", "/images/categories/marketing.jpg");
        KEY_TO_RESOURCE.put("DATA_ENTRY", "/images/categories/data_entry.jpg");
        KEY_TO_RESOURCE.put("CONSULTING", "/images/categories/consulting.jpg");
        KEY_TO_RESOURCE.put("OTHER", "/images/categories/default.jpg");

        // Key -> Display name (used by getAllDisplayNames)
        KEY_TO_DISPLAY.put("GRAPHIC_DESIGN", "Graphic Design");
        KEY_TO_DISPLAY.put("WRITING", "Writing & Content");
        KEY_TO_DISPLAY.put("PROGRAMMING", "Programming");
        KEY_TO_DISPLAY.put("VIDEO_EDITING", "Video Editing");
        KEY_TO_DISPLAY.put("MARKETING", "Marketing");
        KEY_TO_DISPLAY.put("DATA_ENTRY", "Data Entry");
        KEY_TO_DISPLAY.put("CONSULTING", "Consulting");
        KEY_TO_DISPLAY.put("OTHER", "Other");
    }

    private CategoryImageMapper() {}

    public static Image getImage(String key) {
        String path = getImagePath(key);
        URL url = CategoryImageMapper.class.getResource(path);
        if (url == null) {
            // fallback to default
            url = CategoryImageMapper.class.getResource(KEY_TO_RESOURCE.get("OTHER"));
        }
        return url != null ? new Image(url.toExternalForm(), true) : null;
    }

    public static String getImagePath(String key) {
        if (key == null) return KEY_TO_RESOURCE.get("OTHER");
        return KEY_TO_RESOURCE.getOrDefault(key, KEY_TO_RESOURCE.get("OTHER"));
    }

    /**
     * Returns a list of all user-facing category display names
     * in a stable order suitable for ComboBoxes.
     */
    public static List<String> getAllDisplayNames() {
        return new ArrayList<>(KEY_TO_DISPLAY.values());
    }

    public static String toKeyFromDisplay(String display) {
        if (display == null) return "OTHER";
        for (Map.Entry<String, String> e : KEY_TO_DISPLAY.entrySet()) {
            if (display.equalsIgnoreCase(e.getValue())) return e.getKey();
        }
        return "OTHER";
    }

    public static String toDisplay(String key) {
        if (key == null) return KEY_TO_DISPLAY.get("OTHER");
        return KEY_TO_DISPLAY.getOrDefault(key, KEY_TO_DISPLAY.get("OTHER"));
    }
}
