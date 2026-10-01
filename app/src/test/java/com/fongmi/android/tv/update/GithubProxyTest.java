package com.fongmi.android.tv.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

public class GithubProxyTest {

    private static final String ASSET = "https://github.com/fish2018/webhtv/releases/download/v1/mobile-arm64_v8a.apk";

    @Test
    public void rewritesKnownProxyModes() {
        assertEquals(
                "https://github.chenc.dev/github.com/fish2018/webhtv/releases/download/v1/mobile-arm64_v8a.apk",
                GithubProxy.resolve("github_chenc", "", "").rewrite(ASSET));
        assertEquals(
                "https://gh.acmsz.top/https://github.com/fish2018/webhtv/releases/download/v1/mobile-arm64_v8a.apk",
                GithubProxy.resolve("gh_acmsz", "", "").rewrite(ASSET));
    }

    @Test
    public void rejectsUnsafeCustomOrigins() {
        assertThrows(IllegalArgumentException.class, () -> GithubProxy.resolve(GithubProxy.CUSTOM, "http://proxy.example", GithubProxy.MODE_FULL_URL));
        assertThrows(IllegalArgumentException.class, () -> GithubProxy.resolve(GithubProxy.CUSTOM, "https://user:pass@proxy.example", GithubProxy.MODE_FULL_URL));
        assertThrows(IllegalArgumentException.class, () -> GithubProxy.resolve(GithubProxy.CUSTOM, "https://proxy.example/path", GithubProxy.MODE_FULL_URL));
    }

    @Test
    public void chainStartsAtSelectionAndEndsAtDirect() {
        List<GithubProxy.Config> chain = GithubProxy.chain("ghfast", "", "", "");
        assertEquals(4, chain.size());
        assertEquals("ghfast", chain.get(0).id);
        assertEquals("gh_monlor", chain.get(1).id);
        assertEquals(GithubProxy.DIRECT, chain.get(3).id);
    }

    @Test
    public void chainTriesBothCustomModes() {
        List<GithubProxy.Config> chain = GithubProxy.chain(GithubProxy.CUSTOM, "https://p.example", GithubProxy.MODE_STRIP_SCHEME, "");
        assertEquals(GithubProxy.MODE_STRIP_SCHEME, chain.get(0).mode);
        assertEquals(GithubProxy.MODE_FULL_URL, chain.get(1).mode);
    }

    @Test
    public void chainDropsUnusableCustom() {
        List<GithubProxy.Config> chain = GithubProxy.chain(GithubProxy.CUSTOM, "https://p.example/path", "", "");
        assertEquals(4, chain.size());
        assertFalse(chain.stream().anyMatch(item -> GithubProxy.CUSTOM.equals(item.id)));
        assertEquals(GithubProxy.DIRECT, chain.get(3).id);
    }

    @Test
    public void chainPromotesLastWorkingHop() {
        String chenc = GithubProxy.resolve("github_chenc", "", "").key();
        List<GithubProxy.Config> chain = GithubProxy.chain(GithubProxy.DIRECT, "", "", chenc);
        assertEquals("github_chenc", chain.get(0).id);
        assertEquals(GithubProxy.DIRECT, chain.get(1).id);
    }
}
