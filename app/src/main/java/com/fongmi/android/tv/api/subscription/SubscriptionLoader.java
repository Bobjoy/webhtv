package com.fongmi.android.tv.api.subscription;

import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.update.GithubProxy;
import com.github.catvod.net.OkHttp;

import java.util.List;

import okhttp3.HttpUrl;

/** 订阅地址 → 候选条目：每次实时拉取，不缓存、不重试；GitHub 地址复用更新设置里的代理（ADR-0006）。 */
public class SubscriptionLoader {

    private static final String[] GITHUB_HOSTS = {"github.com", "raw.githubusercontent.com", "gist.githubusercontent.com"};

    public static List<SubscriptionParser.Item> load(String url) throws Exception {
        return SubscriptionParser.parse(text(url));
    }

    /** 取原文：空响应判为不可访问，让调用方能把「拉不到」与「内容不对」分开处理。 */
    public static String text(String url) throws Exception {
        String content = OkHttp.string(githubProxied(url, githubProxy()));
        if (content.isEmpty()) throw new Exception("订阅地址无法访问：" + url);
        return content;
    }

    private static GithubProxy.Config githubProxy() {
        return GithubProxy.resolve(Setting.getUpdateGithubProxy(), Setting.getUpdateGithubProxyUrl(), Setting.getUpdateGithubProxyMode());
    }

    /** 只有 https 的 GitHub 地址走代理，其余原样返回。 */
    static String githubProxied(String url, GithubProxy.Config config) {
        if (!isGithub(url)) return url;
        return config.rewrite(url);
    }

    private static boolean isGithub(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url == null ? "" : url);
        if (httpUrl == null || !"https".equals(httpUrl.scheme())) return false;
        for (String host : GITHUB_HOSTS) if (host.equals(httpUrl.host())) return true;
        return false;
    }
}
