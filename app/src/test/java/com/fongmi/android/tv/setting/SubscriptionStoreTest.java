package com.fongmi.android.tv.setting;

import com.fongmi.android.tv.api.subscription.SubscriptionParser;
import com.fongmi.android.tv.bean.Subscription;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscriptionStoreTest {

    private static final int VOD = 0;
    private static final int LIVE = 1;

    private final MemoryBackend backend = new MemoryBackend();
    private final MemoryPrefs prefs = new MemoryPrefs();
    private final SubscriptionStore store = new SubscriptionStore(backend, prefs, new Gson());

    @Test
    public void seedDefaultsAddsBuiltInVodAndLiveOnlyOnce() {
        store.seedDefaults();

        List<Subscription> items = store.getAll();
        assertEquals(2, items.size());
        assertEquals(0, items.get(0).getType());
        assertEquals("https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/vod.json", items.get(0).getUrl());
        assertEquals(1, items.get(1).getType());
        assertEquals("https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/live.json", items.get(1).getUrl());

        store.delete(items.get(0));
        store.delete(items.get(1));
        store.seedDefaults();
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    public void seedDefaultsAppendsBuiltInToExistingUserSubscriptionsOnce() {
        store.add(sub("我的订阅", "http://a/top", VOD));

        store.seedDefaults();

        List<Subscription> items = store.getAll();
        assertEquals(3, items.size());
        assertEquals("我的订阅", items.get(0).getName());
        store.delete(items.get(1));
        store.delete(items.get(2));
        store.seedDefaults();
        assertEquals(1, store.getAll().size());
    }

    @Test
    public void cacheRoundTripsPerTypeAndUrl() {
        List<SubscriptionParser.Item> items = List.of(new SubscriptionParser.Item("线路", "http://a/x.json", "http://a/logo.png", "备注"));
        store.cacheItems(VOD, "http://sub.top/vod.json", items);

        List<SubscriptionParser.Item> cached = store.getCachedItems(VOD, "http://sub.top/vod.json");
        assertEquals(1, cached.size());
        assertEquals("线路", cached.get(0).getName());
        assertEquals("http://a/x.json", cached.get(0).getUrl());
        assertEquals("http://a/logo.png", cached.get(0).getLogo());
        assertEquals("备注", cached.get(0).getRemark());

        assertTrue(store.getCachedItems(LIVE, "http://sub.top/vod.json").isEmpty());
        assertTrue(store.getCachedItems(VOD, "http://sub.top/other.json").isEmpty());
    }

    @Test
    public void cacheSurvivesReopenedStoreAndDegradesOnCorruption() {
        store.cacheItems(VOD, "http://sub.top/vod.json", List.of(new SubscriptionParser.Item("线路", "http://a/x.json", "", "")));

        SubscriptionStore reopened = new SubscriptionStore(backend, prefs, new Gson());
        assertEquals(1, reopened.getCachedItems(VOD, "http://sub.top/vod.json").size());

        prefs.values.put(reopened.cacheKey(VOD, "http://sub.top/vod.json"), "{broken");
        assertTrue(reopened.getCachedItems(VOD, "http://sub.top/vod.json").isEmpty());
    }

    @Test
    public void deleteAndEditedUrlDropTheirCache() {
        store.seedDefaults();
        Subscription vod = store.getAll().get(0);
        store.cacheItems(VOD, vod.getUrl(), List.of(new SubscriptionParser.Item("线路", "http://a/x.json", "", "")));

        store.delete(vod);
        assertTrue(store.getCachedItems(VOD, vod.getUrl()).isEmpty());

        Subscription live = store.getAll().get(0);
        store.cacheItems(LIVE, live.getUrl(), List.of(new SubscriptionParser.Item("线路", "http://a/y.json", "", "")));
        store.update(live, sub(live.getName(), "http://sub.top/live2.json", LIVE));
        assertTrue(store.getCachedItems(LIVE, live.getUrl()).isEmpty());
    }

    @Test
    public void onlyUntouchedBuiltInIsLocked() {
        store.seedDefaults();

        assertTrue(store.isDefault(store.getAll().get(0)));
        assertFalse(store.isDefault(sub("TV 点播资源", "http://a/top", VOD)));
        assertFalse(store.isDefault(sub("同址不同类型", "https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/vod.json", LIVE)));
        assertFalse(store.isDefault(null));
    }

    @Test
    public void missingKeyUsesDefaultWithoutWriting() {
        assertTrue(store.getAll().isEmpty());
        assertEquals("", backend.value);
    }

    @Test
    public void addPersistsAndKeepsInsertionOrder() {
        assertTrue(store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD)));
        assertTrue(store.add(sub("肥猫", "http://肥猫.net", VOD)));

        List<Subscription> items = store.getAll();
        assertEquals(2, items.size());
        assertEquals("饭太硬", items.get(0).getName());
        assertEquals("肥猫", items.get(1).getName());
        assertTrue(backend.value.contains("饭太硬"));
    }

    @Test
    public void getByTypeOnlyReturnsThatType() {
        store.add(sub("点播", "http://a/top", VOD));
        store.add(sub("直播", "http://b/live", LIVE));

        assertEquals(1, store.getByType(VOD).size());
        assertEquals("点播", store.getByType(VOD).get(0).getName());
        assertEquals("直播", store.getByType(LIVE).get(0).getName());
    }

    @Test
    public void duplicateUrlWithinSameTypeIsRejected() {
        store.add(sub("小盒子", "https://xhztv.top/dc", VOD));
        String persisted = backend.value;

        assertFalse(store.add(sub("另一个名字", "https://xhztv.top/dc", VOD)));
        assertEquals(1, store.getAll().size());
        assertEquals(persisted, backend.value);
    }

    @Test
    public void sameUrlUnderDifferentTypeIsAllowed() {
        assertTrue(store.add(sub("点播", "https://a/top", VOD)));
        assertTrue(store.add(sub("直播", "https://a/top", LIVE)));
        assertEquals(2, store.getAll().size());
    }

    @Test
    public void blankUrlIsRejected() {
        assertFalse(store.add(sub("无地址", "", VOD)));
        assertFalse(store.add(sub("空格", "   ", VOD)));
        assertTrue(store.getAll().isEmpty());
        assertEquals("", backend.value);
    }

    @Test
    public void corruptedContentDegradesToEmptyWithoutDestroyingStorage() {
        backend.value = "{not json";

        assertTrue(store.getAll().isEmpty());
        assertTrue(store.add(sub("恢复", "http://a/top", VOD)));
        assertEquals(1, store.getAll().size());
    }

    @Test
    public void blankNameFallsBackToUrlHost() {
        assertEquals("xhztv.top", sub("", "http://xhztv.top/dc", VOD).getDisplayName());
        assertEquals("肥猫.net", sub(null, "http://肥猫.net", VOD).getDisplayName());
        assertEquals("给定名字", sub("给定名字", "http://a/top", VOD).getDisplayName());
    }

    @Test
    public void updateChangesNameAndUrlInPlace() {
        store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD));
        store.add(sub("肥猫", "http://肥猫.net", VOD));

        assertTrue(store.update(sub("", "http://www.饭太硬.net/tv", VOD), sub("新饭太硬", "http://www.饭太硬.net/dc", VOD)));

        List<Subscription> items = store.getAll();
        assertEquals(2, items.size());
        assertEquals("新饭太硬", items.get(0).getName());
        assertEquals("http://www.饭太硬.net/dc", items.get(0).getUrl());
        assertEquals("肥猫", items.get(1).getName());
    }

    @Test
    public void updateIntoExistingUrlIsRejectedAndStorageKept() {
        store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD));
        store.add(sub("肥猫", "http://肥猫.net", VOD));
        String persisted = backend.value;

        assertFalse(store.update(sub("饭太硬", "http://www.饭太硬.net/tv", VOD), sub("饭太硬", "http://肥猫.net", VOD)));

        assertEquals(persisted, backend.value);
        assertEquals("http://www.饭太硬.net/tv", store.getAll().get(0).getUrl());
    }

    @Test
    public void updateSameUrlUnderAnotherTypeStaysIndependent() {
        store.add(sub("点播", "https://a/top", VOD));
        store.add(sub("直播", "https://a/top", LIVE));

        assertTrue(store.update(sub("直播", "https://a/top", LIVE), sub("直播改名", "https://a/top", LIVE)));

        assertEquals("点播", store.getAll().get(0).getName());
        assertEquals("直播改名", store.getAll().get(1).getName());
    }

    @Test
    public void updateOfMissingOrInvalidSubscriptionChangesNothing() {
        store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD));

        assertFalse(store.update(sub("不存在", "http://none.net", VOD), sub("改名", "http://www.饭太硬.net/tv", VOD)));
        assertFalse(store.update(null, sub("改名", "http://a/top", VOD)));
        assertFalse(store.update(sub("饭太硬", "http://www.饭太硬.net/tv", VOD), sub("改名", "   ", VOD)));
        assertEquals("饭太硬", store.getAll().get(0).getName());
    }

    @Test
    public void deleteRemovesOnlyThatSubscription() {
        store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD));
        store.add(sub("直播", "https://a/live", LIVE));

        assertTrue(store.delete(sub("", "http://www.饭太硬.net/tv", VOD)));

        List<Subscription> items = store.getAll();
        assertEquals(1, items.size());
        assertEquals(LIVE, items.get(0).getType());
        assertFalse(store.delete(sub("饭太硬", "http://www.饭太硬.net/tv", VOD)));
        assertFalse(store.delete(null));
    }

    @Test
    public void reopenedStoreReadsPersistedItems() {
        assertTrue(store.add(sub("饭太硬", "http://www.饭太硬.net/tv", VOD)));

        SubscriptionStore reopened = new SubscriptionStore(backend, prefs, new Gson());
        assertEquals(1, reopened.getAll().size());
        assertEquals("饭太硬", reopened.getAll().get(0).getName());
    }

    @Test
    public void activeMarkerFollowsTheSubscriptionTheLineWasPickedFrom() {
        store.putActive(VOD, "http://a/subscribe.json", "http://a/live.json");

        assertTrue(store.isActive(VOD, "http://a/subscribe.json", "http://a/live.json"));
        assertFalse(store.isActive(VOD, "http://b/subscribe.json", "http://a/live.json"));
        assertFalse(store.isActive(LIVE, "http://a/subscribe.json", "http://a/live.json"));
        assertFalse(store.isActive(VOD, "http://a/subscribe.json", "http://c/other.json"));
        assertFalse(store.isActive(VOD, "http://a/subscribe.json", ""));
    }

    @Test
    public void lastActivationWinsPerType() {
        store.putActive(VOD, "http://a/subscribe.json", "http://a/live.json");
        store.putActive(VOD, "http://b/subscribe.json", "http://b/live.json");

        assertFalse(store.isActive(VOD, "http://a/subscribe.json", "http://a/live.json"));
        assertTrue(store.isActive(VOD, "http://b/subscribe.json", "http://b/live.json"));
    }

    @Test
    public void activeMarkerIgnoresMalformedValue() {
        prefs.write("subscription_active_0", "not json");

        assertFalse(store.isActive(VOD, "http://a/subscribe.json", "http://a/live.json"));
    }

    private Subscription sub(String name, String url, int type) {
        return new Subscription(name, url, type);
    }

    private static final class MemoryBackend implements SubscriptionStore.Backend {

        private String value = "";

        @Override
        public String read() {
            return value;
        }

        @Override
        public void write(String value) {
            this.value = value == null ? "" : value;
        }
    }

    private static final class MemoryPrefs implements SubscriptionStore.Prefs {

        private final Map<String, String> values = new HashMap<>();

        @Override
        public String read(String key) {
            return values.getOrDefault(key, "");
        }

        @Override
        public void write(String key, String value) {
            if (value == null || value.isEmpty()) values.remove(key);
            else values.put(key, value);
        }
    }
}
