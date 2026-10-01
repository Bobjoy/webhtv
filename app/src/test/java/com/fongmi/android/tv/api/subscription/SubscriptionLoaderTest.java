package com.fongmi.android.tv.api.subscription;

import static org.junit.Assert.assertEquals;

import com.fongmi.android.tv.update.GithubProxy;

import org.junit.Test;

/** 订阅地址的 GitHub 代理改写（ADR-0006）：纯函数，不触碰网络与偏好。 */
public class SubscriptionLoaderTest {

    private final GithubProxy.Config ghfast = GithubProxy.resolve("ghfast", "", "");
    private final GithubProxy.Config chenc = GithubProxy.resolve("github_chenc", "", "");
    private final GithubProxy.Config direct = GithubProxy.resolve(GithubProxy.DIRECT, "", "");

    @Test
    public void githubHostsAreRewritten() {
        assertEquals("https://ghfast.top/https://raw.githubusercontent.com/o/r/main/a.json",
                SubscriptionLoader.githubProxied("https://raw.githubusercontent.com/o/r/main/a.json", ghfast));
        assertEquals("https://ghfast.top/https://gist.githubusercontent.com/o/abc/raw/a.json",
                SubscriptionLoader.githubProxied("https://gist.githubusercontent.com/o/abc/raw/a.json", ghfast));
        assertEquals("https://ghfast.top/https://github.com/o/r/raw/main/a.json",
                SubscriptionLoader.githubProxied("https://github.com/o/r/raw/main/a.json", ghfast));
    }

    @Test
    public void stripSchemeModeDropsTheScheme() {
        assertEquals("https://github.chenc.dev/raw.githubusercontent.com/o/r/main/a.json",
                SubscriptionLoader.githubProxied("https://raw.githubusercontent.com/o/r/main/a.json", chenc));
    }

    @Test
    public void nonGithubHostsStayUntouched() {
        assertEquals("http://www.饭太硬.net/tv", SubscriptionLoader.githubProxied("http://www.饭太硬.net/tv", ghfast));
        assertEquals("https://xhztv.top/dc", SubscriptionLoader.githubProxied("https://xhztv.top/dc", ghfast));
        assertEquals("https://ghfast.top/already/proxied.json", SubscriptionLoader.githubProxied("https://ghfast.top/already/proxied.json", ghfast));
    }

    @Test
    public void plainHttpGithubUrlIsNotRewritten() {
        assertEquals("http://raw.githubusercontent.com/o/r/main/a.json", SubscriptionLoader.githubProxied("http://raw.githubusercontent.com/o/r/main/a.json", ghfast));
    }

    @Test
    public void directPresetKeepsGithubUrlAsIs() {
        assertEquals("https://github.com/o/r/raw/main/a.json", SubscriptionLoader.githubProxied("https://github.com/o/r/raw/main/a.json", direct));
    }

    @Test
    public void blankOrGarbageUrlIsNotRewritten() {
        assertEquals("", SubscriptionLoader.githubProxied("", ghfast));
        assertEquals("raw.githubusercontent.com/o/a.json", SubscriptionLoader.githubProxied("raw.githubusercontent.com/o/a.json", ghfast));
    }
}
