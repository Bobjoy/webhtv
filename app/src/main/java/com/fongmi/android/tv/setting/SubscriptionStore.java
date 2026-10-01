package com.fongmi.android.tv.setting;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.subscription.SubscriptionParser;
import com.fongmi.android.tv.bean.Subscription;
import com.github.catvod.utils.Prefers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/** 订阅列表持久化在一条 SharedPreferences JSON 上；生效仍走 Config 表，订阅本身不参与。 */
public final class SubscriptionStore {

    private static final String DEFAULT_VOD_URL = "https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/vod.json";
    private static final String DEFAULT_LIVE_URL = "https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/live.json";
    private static final String SEEDED = "subscription_seeded";
    private static final String ACTIVE = "subscription_active_";

    private static final Type LIST_TYPE = new TypeToken<List<Subscription>>() {
    }.getType();

    private static final Type ITEM_LIST_TYPE = new TypeToken<List<SubscriptionParser.Item>>() {
    }.getType();

    private final Backend backend;
    private final Prefs prefs;
    private final Gson gson;

    public SubscriptionStore(Backend backend, Prefs prefs, Gson gson) {
        this.backend = backend;
        this.prefs = prefs;
        this.gson = gson;
    }

    public static SubscriptionStore get() {
        return Default.INSTANCE;
    }

    /** 每个装机只种一次 webhtv-sub 点播/直播两条内置订阅；用户删掉后不再复活。 */
    public void seedDefaults() {
        if (!prefs.read(SEEDED).isEmpty()) return;
        prefs.write(SEEDED, "1");
        List<Subscription> items = getAll();
        for (Subscription item : defaults()) addMissing(items, item);
        backend.write(gson.toJson(items));
    }

    /** 内置订阅不可删除；用户改过地址后即视为普通订阅。 */
    public boolean isDefault(Subscription item) {
        return item != null && indexOf(defaults(), item) != -1;
    }

    private List<Subscription> defaults() {
        List<Subscription> items = new ArrayList<>();
        items.add(new Subscription("TV 点播资源", DEFAULT_VOD_URL, 0));
        items.add(new Subscription("TV 直播资源", DEFAULT_LIVE_URL, 1));
        return items;
    }

    private void addMissing(List<Subscription> items, Subscription item) {
        if (indexOf(items, item) == -1) items.add(item);
    }

    public List<Subscription> getAll() {
        try {
            List<Subscription> items = gson.fromJson(backend.read(), LIST_TYPE);
            return items == null ? new ArrayList<>() : sanitize(items);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Subscription> getByType(int type) {
        List<Subscription> items = new ArrayList<>();
        for (Subscription item : getAll()) if (item.getType() == type) items.add(item);
        return items;
    }

    public boolean add(Subscription item) {
        if (item == null || item.isEmpty()) return false;
        List<Subscription> items = getAll();
        if (indexOf(items, item) != -1) return false;
        items.add(item);
        backend.write(gson.toJson(items));
        return true;
    }

    /** 按 type + 原 url 定位后整条替换；改成他条的 type+url 视为重复。 */
    public boolean update(Subscription target, Subscription updated) {
        if (target == null || updated == null || updated.isEmpty()) return false;
        List<Subscription> items = getAll();
        int index = indexOf(items, target);
        if (index == -1) return false;
        int duplicate = indexOf(items, updated);
        if (duplicate != -1 && duplicate != index) return false;
        items.set(index, updated);
        backend.write(gson.toJson(items));
        if (target.getType() != updated.getType() || !target.getUrl().equals(updated.getUrl())) clearCache(target.getType(), target.getUrl());
        return true;
    }

    public boolean delete(Subscription target) {
        if (target == null) return false;
        List<Subscription> items = getAll();
        int index = indexOf(items, target);
        if (index == -1) return false;
        items.remove(index);
        backend.write(gson.toJson(items));
        clearCache(target.getType(), target.getUrl());
        return true;
    }

    /**
     * 记住"当前生效的线路是从哪条订阅启用的"：值里同时存订阅地址与线路地址。
     * 判断时要把线路地址跟 {@code config_<type>} 实际值对上，用户在别处改了配置，标记就自动消失，不需要清理逻辑。
     */
    public void putActive(int type, String subscriptionUrl, String itemUrl) {
        prefs.write(ACTIVE + type, gson.toJson(new String[]{subscriptionUrl, itemUrl}));
    }

    public boolean isActive(int type, String subscriptionUrl, String itemUrl) {
        if (itemUrl.isEmpty()) return false;
        try {
            String[] parts = gson.fromJson(prefs.read(ACTIVE + type), String[].class);
            return parts != null && parts.length == 2 && parts[0].equals(subscriptionUrl) && parts[1].equals(itemUrl);
        } catch (Exception e) {
            return false;
        }
    }

    /** 订阅拉取结果按 type + 订阅地址缓存在独立键上，不进备份。 */
    public List<SubscriptionParser.Item> getCachedItems(int type, String url) {
        try {
            List<SubscriptionParser.Item> items = gson.fromJson(prefs.read(cacheKey(type, url)), ITEM_LIST_TYPE);
            return items == null ? new ArrayList<>() : items;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void cacheItems(int type, String url, List<SubscriptionParser.Item> items) {
        prefs.write(cacheKey(type, url), items == null ? "" : gson.toJson(items));
    }

    private void clearCache(int type, String url) {
        prefs.write(cacheKey(type, url), "");
    }

    String cacheKey(int type, String url) {
        return "subscription_" + type + "_" + url;
    }

    private int indexOf(List<Subscription> items, Subscription target) {
        for (int i = 0; i < items.size(); i++) {
            Subscription item = items.get(i);
            if (item.getType() == target.getType() && item.getUrl().equals(target.getUrl())) return i;
        }
        return -1;
    }

    private List<Subscription> sanitize(List<Subscription> items) {
        List<Subscription> result = new ArrayList<>();
        for (Subscription item : items) if (item != null && !item.isEmpty()) result.add(item);
        return result;
    }

    public interface Backend {

        String read();

        void write(String value);
    }

    public interface Prefs {

        String read(String key);

        void write(String key, String value);
    }

    private static final class Default {

        private static final SubscriptionStore INSTANCE = new SubscriptionStore(new SubscriptionPreferences(), new SubscriptionPrefs(), App.gson());
    }

    static final class SubscriptionPreferences implements Backend {

        private static final String KEY = "subscription";

        @Override
        public String read() {
            return Prefers.getString(KEY);
        }

        @Override
        public void write(String value) {
            Prefers.put(KEY, value);
        }
    }

    static final class SubscriptionPrefs implements Prefs {

        @Override
        public String read(String key) {
            return Prefers.getString(key);
        }

        @Override
        public void write(String key, String value) {
            Prefers.put(key, value);
        }
    }
}
