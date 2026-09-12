package com.sinter.daily;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Versioned, platform-independent JSON import contract. */
final class QuoteLibrary {
    static final int MAX_BYTES = 5 * 1024 * 1024;
    static final int MAX_ITEMS = 10000;
    final JSONObject json;
    final String id;
    final String title;
    final JSONArray items;

    private QuoteLibrary(JSONObject json) {
        this.json = json;
        this.id = json.optString("id");
        this.title = json.optString("title");
        this.items = json.optJSONArray("items");
    }

    static QuoteLibrary parse(String input) throws JSONException {
        if (input.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw invalid("文件不能超过 5 MB");
        if (input.startsWith("\uFEFF")) input = input.substring(1);
        JSONTokener tokener = new JSONTokener(input);
        Object value = tokener.nextValue();
        if (tokener.nextClean() != 0) throw invalid("JSON 末尾有多余内容");
        JSONObject root;
        if (value instanceof JSONArray) {
            // Compatibility with the previously extracted quotes.json.
            root = new JSONObject().put("schemaVersion", 1).put("id", "import-" + digest(input))
                    .put("title", "导入句库").put("items", value);
        } else if (value instanceof JSONObject) {
            root = (JSONObject)value;
        } else throw invalid("顶层应为句库对象或语录数组");
        if (!(root.opt("schemaVersion") instanceof Number) || root.optDouble("schemaVersion") != 1)
            throw invalid("schemaVersion 必须为 1");
        String id = required(root, "id", 80);
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) throw invalid("句库 id 只能包含英文、数字、点、横线和下划线");
        String title = required(root, "title", 100);
        JSONArray inputItems = root.optJSONArray("items");
        if (inputItems == null || inputItems.length() > MAX_ITEMS)
            throw invalid("items 必须是数组，最多包含 10000 条内容");
        JSONObject normalized = new JSONObject().put("schemaVersion", 1).put("id", id).put("title", title);
        copyText(root, normalized, "author", 200);
        copyText(root, normalized, "source", 500);
        copyText(root, normalized, "translator", 200);
        JSONArray items = new JSONArray();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < inputItems.length(); i++) {
            Object item = inputItems.get(i);
            JSONObject q;
            if (item instanceof String) q = new JSONObject().put("text", item);
            else if (item instanceof JSONObject) q = (JSONObject)item;
            else throw invalid("第 " + (i + 1) + " 条应为文本或对象");
            String text = required(q, "text", 4000);
            if (q.has("id") && !(q.opt("id") instanceof String)
                    && (!(q.opt("id") instanceof Number) || q.optDouble("id") != Math.floor(q.optDouble("id"))))
                throw invalid("第 " + (i + 1) + " 条的 id 必须是文本或整数");
            String quoteId = q.has("id") ? q.optString("id", "").trim() : digest(text);
            if (quoteId.isEmpty() || quoteId.length() > 128 || !ids.add(quoteId))
                throw invalid("第 " + (i + 1) + " 条的 id 为空、过长或重复");
            JSONObject entry = new JSONObject().put("id", quoteId).put("text", text);
            copyText(q, entry, "author", 200);
            copyText(q, entry, "source", 500);
            for (String field : new String[]{"page", "endPage"}) {
                if (q.has(field)) {
                    Object number = q.get(field);
                    if (!(number instanceof Number) || q.optDouble(field) < 1
                            || q.optDouble(field) != Math.floor(q.optDouble(field)) || q.optDouble(field) > 1000000)
                        throw invalid("第 " + (i + 1) + " 条的页码必须为正整数");
                    entry.put(field, q.getInt(field));
                }
            }
            if (entry.has("endPage") && (!entry.has("page") || entry.getInt("endPage") < entry.getInt("page")))
                throw invalid("第 " + (i + 1) + " 条的结束页码无效");
            items.put(entry);
        }
        normalized.put("items", items);
        return new QuoteLibrary(normalized);
    }

    private static String required(JSONObject object, String key, int max) throws JSONException {
        if (!(object.opt(key) instanceof String)) throw invalid(key + " 必须是文本");
        String text = object.getString(key).trim();
        if (text.isEmpty() || text.length() > max) throw invalid(key + " 不能为空且不能超过 " + max + " 个字符");
        return text;
    }
    private static void copyText(JSONObject from, JSONObject to, String key, int max) throws JSONException {
        if (from.has(key)) {
            if (!(from.opt(key) instanceof String) || from.getString(key).length() > max) throw invalid(key + " 格式或长度无效");
            to.put(key, from.getString(key).trim());
        }
    }
    static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 12; i++) result.append(String.format(java.util.Locale.ROOT, "%02x", hash[i] & 255));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static JSONException invalid(String message) { return new JSONException(message); }
}
