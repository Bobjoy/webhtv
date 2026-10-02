package com.fongmi.android.tv.api.subscription;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.StateEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 简易版内置点播源：冷启动后台拉一次 {@code sub.txt}，按行序挑一条生效，走 {@link VodConfig#load} 既有接缝。
 * 拉不到就沿用旧源、不打扰用户；当前源仍在清单里则完全不动（ADR-0008）。
 */
public class BuiltinSource {

    public static final String LIST_URL = "https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/sub.txt";

    private static final long RETRY_STEP = TimeUnit.SECONDS.toMillis(20);

    private static final AtomicInteger mMisses = new AtomicInteger();
    private static final AtomicBoolean mRunning = new AtomicBoolean();
    private static volatile boolean mPending;

    /** 当前生效源是否出现在清单里 —— 在则本轮什么都不用做。 */
    public static boolean listed(List<SubscriptionParser.Item> items, String currentUrl) {
        String current = trim(currentUrl);
        for (SubscriptionParser.Item item : items) if (item.getUrl().trim().equals(current)) return true;
        return false;
    }

    /** 按行序产出候选：跳过非 http 行、重复项与当前生效源。 */
    public static List<String> candidates(List<SubscriptionParser.Item> items, String currentUrl) {
        String current = trim(currentUrl);
        List<String> urls = new ArrayList<>();
        for (SubscriptionParser.Item item : items) {
            String url = item.getUrl().trim();
            if (!url.startsWith("http") || url.equals(current) || urls.contains(url)) continue;
            urls.add(url);
        }
        return urls;
    }

    public static boolean isPending() {
        return mPending;
    }

    public static void refresh(Config current) {
        refresh(current, null);
    }

    /**
     * {@code done} 收 {@code true} 表示已生效或无需变更，{@code false} 表示本轮没拿到可用源；在后台线程回调。
     * 调用方传入已读到的 {@code current}，好让 pending 在首帧之前就位，不跟 {@code VodFragment} 的订阅抢跑。
     */
    public static void refresh(Config current, Consumer<Boolean> done) {
        if (!mRunning.compareAndSet(false, true)) return;
        String url = trim(current.getUrl());
        mPending = url.isEmpty();
        if (mPending) StateEvent.progress();
        Task.submit(() -> {
            List<String> queue;
            try {
                List<SubscriptionParser.Item> items = SubscriptionLoader.load(LIST_URL);
                queue = listed(items, url) ? null : candidates(items, url);
            } catch (Exception e) {
                queue = new ArrayList<>();
            }
            if (queue == null) finish(done, true);
            else activate(queue, done);
        });
    }

    private static void activate(List<String> queue, Consumer<Boolean> done) {
        if (queue.isEmpty()) {
            finish(done, false);
            schedule();
            return;
        }
        String url = queue.remove(0);
        VodConfig.load(Config.find(url, 0), new Callback() {
            @Override
            public void success() {
                mMisses.set(0);
                mPending = false;
                finish(done, true);
            }

            @Override
            public void error(String msg) {
                Task.submit(() -> activate(queue, done));
            }
        });
    }

    private static void finish(Consumer<Boolean> done, boolean ok) {
        mRunning.set(false);
        if (done != null) done.accept(ok);
    }

    private static void schedule() {
        Task.schedule(BuiltinSource::retry, Math.min(mMisses.incrementAndGet(), 5) * RETRY_STEP, TimeUnit.MILLISECONDS);
    }

    /** 重试在后台线程，自己读当前源即可。 */
    private static void retry() {
        refresh(Config.vod());
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
