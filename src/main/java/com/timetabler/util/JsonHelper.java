package com.timetabler.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-dependency JSON helper for parsing simple key-value payloads.
 */
public class JsonHelper {

    /**
     * Parses flat JSON object properties into a string-to-string map.
     */
    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        // Match "key"\s*:\s*("value"|true|false|[0-9.]+)
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(true|false|[0-9.-]+))");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String strVal = matcher.group(2);
            String literalVal = matcher.group(3);

            if (strVal != null) {
                map.put(key, strVal);
            } else if (literalVal != null) {
                map.put(key, literalVal);
            }
        }

        return map;
    }

    public static String getString(Map<String, String> map, String key, String defaultVal) {
        String v = map.get(key);
        return (v != null && !v.trim().isEmpty()) ? v.trim() : defaultVal;
    }

    public static int getInt(Map<String, String> map, String key, int defaultVal) {
        String v = map.get(key);
        if (v == null) return defaultVal;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public static boolean getBoolean(Map<String, String> map, String key, boolean defaultVal) {
        String v = map.get(key);
        if (v == null) return defaultVal;
        return Boolean.parseBoolean(v.trim());
    }
}
