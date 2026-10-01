package com.fongmi.android.tv.api.subscription;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 订阅清单解析：JSON 认 {@code list} 与 {@code urls} 两种键名，非 JSON 按纯文本行解析。
 * 解析规则以 {@code app/src/test/resources/subscription} 下的社区真实形态为准；
 * JSON 解析失败即报错，绝不退回按行解析。
 */
public class SubscriptionParser {

    private static final String[] ARRAY_KEYS = {"list", "urls"};
    private static final Pattern URL_TOKEN = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");

    public static List<Item> parse(String content) throws Exception {
        String body = skipLeadingComments(content == null ? "" : content);
        if (body.isEmpty()) throw new Exception("订阅内容为空");
        if (body.startsWith("{") || body.startsWith("[")) return json(body);
        if (body.startsWith("<")) throw new Exception("响应是 HTML 页面，不是清单");
        List<Item> items = lines(body);
        if (items.isEmpty()) throw new Exception("没有解析出任何条目");
        return items;
    }

    private static String skipLeadingComments(String text) {
        int i = 0;
        while (i < text.length()) {
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i += 1;
            if (text.startsWith("//", i)) {
                int nl = text.indexOf('\n', i);
                i = nl == -1 ? text.length() : nl + 1;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                i = end == -1 ? text.length() : end + 2;
            } else {
                break;
            }
        }
        return text.substring(i);
    }

    private static List<Item> json(String body) throws Exception {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (Exception e) {
            throw new Exception("JSON 解析失败：" + e.getMessage());
        }
        JsonArray array = root != null && root.isJsonArray() ? root.getAsJsonArray() : itemsOf(root);
        if (array == null) throw new Exception("JSON 里既没有 list 也没有 urls 数组");
        List<Item> items = new ArrayList<>();
        for (JsonElement element : array) add(items, toItem(element));
        return items;
    }

    private static JsonArray itemsOf(JsonElement root) {
        if (root == null || !root.isJsonObject()) return null;
        JsonObject object = root.getAsJsonObject();
        for (String key : ARRAY_KEYS) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonArray()) return value.getAsJsonArray();
        }
        return null;
    }

    private static Item toItem(JsonElement raw) {
        if (raw == null || !raw.isJsonObject()) return null;
        JsonObject object = raw.getAsJsonObject();
        String url = text(object, "url");
        if (url.isEmpty()) return null;
        String name = text(object, "name");
        return new Item(name.isEmpty() ? host(url) : name, url, text(object, "logo"), text(object, "remark"));
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return "";
        return value.getAsString().trim();
    }

    private static List<Item> lines(String body) {
        List<Item> items = new ArrayList<>();
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue;
            String[] parts = trimmed.split("\\s", 2);
            String url = parts[0];
            if (!URL_TOKEN.matcher(url).find()) continue;
            String label = label(parts.length > 1 ? parts[1] : "");
            add(items, new Item(label.isEmpty() ? host(url) : label, url, "", ""));
        }
        return items;
    }

    private static void add(List<Item> items, Item item) {
        if (item != null) items.add(item);
    }

    private static String label(String rest) {
        int hash = rest.indexOf('#');
        int slash = rest.indexOf("//");
        int index = hash != -1 && slash != -1 ? Math.min(hash, slash) : Math.max(hash, slash);
        if (index == -1) return "";
        return rest.substring(rest.charAt(index) == '#' ? index + 1 : index + 2).trim();
    }

    /** 展示用 host：直接截原样字符串，保留中文域名，而不是 IDN 编码后的 punycode。 */
    private static String host(String url) {
        String authority = SCHEME.matcher(url).replaceFirst("");
        int slash = authority.indexOf('/');
        if (slash != -1) authority = authority.substring(0, slash);
        int at = authority.lastIndexOf('@');
        if (at != -1) authority = authority.substring(at + 1);
        return authority.isEmpty() ? url : authority;
    }

    public static class Item {

        private final String name;
        private final String url;
        private final String logo;
        private final String remark;

        public Item(String name, String url, String logo, String remark) {
            this.name = name == null ? "" : name;
            this.url = url == null ? "" : url;
            this.logo = logo == null ? "" : logo;
            this.remark = remark == null ? "" : remark;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getLogo() {
            return logo;
        }

        public String getRemark() {
            return remark;
        }
    }
}
