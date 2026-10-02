package com.fongmi.android.tv.api.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 挑源纯函数接缝：输入清单条目 + 当前生效源，输出候选队列，无 Android、无网络。
 * 行序即优先级（CONTEXT.md「点播清单 sub.txt」），当前源仍在清单里时返回 null 表示本轮不动。
 */
public class BuiltinSourceTest {

    private static List<SubscriptionParser.Item> items(String... urls) {
        List<SubscriptionParser.Item> list = new ArrayList<>();
        for (String url : urls) list.add(new SubscriptionParser.Item(url, url, "", ""));
        return list;
    }

    @Test
    public void freshInstallTakesFirstLine() {
        assertEquals(Arrays.asList("http://a.top/1.json", "http://b.top/2.json"), BuiltinSource.candidates(items("http://a.top/1.json", "http://b.top/2.json"), ""));
    }

    @Test
    public void emptyListYieldsNoCandidate() {
        assertTrue(BuiltinSource.candidates(Collections.emptyList(), "").isEmpty());
        assertFalse(BuiltinSource.listed(Collections.emptyList(), "http://a.top"));
    }

    @Test
    public void currentSourceListedFirstMeansLeaveItAlone() {
        List<SubscriptionParser.Item> list = items("http://a.top/1.json", "http://b.top/2.json");
        assertTrue(BuiltinSource.listed(list, "http://a.top/1.json"));
        assertTrue(BuiltinSource.candidates(list, "http://a.top/1.json").equals(Collections.singletonList("http://b.top/2.json")));
    }

    @Test
    public void currentSourceListedAnywhereMeansLeaveItAlone() {
        List<SubscriptionParser.Item> list = items("http://a.top/1.json", "http://b.top/2.json", "http://c.top/3.json");
        assertTrue(BuiltinSource.listed(list, "http://c.top/3.json"));
    }

    @Test
    public void staleCurrentSourceRotatesToFirstLine() {
        assertEquals(Arrays.asList("http://a.top/1.json", "http://b.top/2.json"), BuiltinSource.candidates(items("http://a.top/1.json", "http://b.top/2.json"), "http://old.top/gone.json"));
    }

    @Test
    public void duplicateUrlsAppearOnce() {
        assertEquals(Collections.singletonList("http://a.top/1.json"), BuiltinSource.candidates(items("http://a.top/1.json", "http://a.top/1.json", "http://a.top/1.json"), ""));
    }

    @Test
    public void nonHttpLinesAreSkipped() {
        assertEquals(Collections.singletonList("https://a.top/1.json"), BuiltinSource.candidates(items("ftp://nope", "javascript:alert(1)", "", "  ", "https://a.top/1.json"), ""));
    }

    @Test
    public void blankUrlOnlyListNeverActivates() {
        assertTrue(BuiltinSource.candidates(items("ftp://only"), "").isEmpty());
    }
}
