package com.fongmi.android.tv.api.subscription;

import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.update.GithubProxy;
import com.github.catvod.net.OkHttp;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/** 订阅地址 → 候选条目：每次实时拉取，不缓存；GitHub 地址复用更新设置里的代理并按链降级（ADR-0006）。 */
public class SubscriptionLoader {

    private static final String[] GITHUB_HOSTS = {"github.com", "raw.githubusercontent.com", "gist.githubusercontent.com"};

    private static final long HOP_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    public static List<SubscriptionParser.Item> load(String url) throws Exception {
        return SubscriptionParser.parse(text(url));
    }

    /**
     * 取原文：加速链逐跳试，非 2xx 与空响应都算这一跳失效（黑名单代理会回 403 正文），让调用方能把「拉不到」与「内容不对」分开处理。
     */
    public static String text(String url) throws Exception {
        List<GithubProxy.Config> chain = chain();
        for (int i = 0; i < chain.size(); i++) {
            String content = fetch(githubProxied(url, chain.get(i)));
            if (content == null) continue;
            if (i > 0 && isGithub(url)) Setting.putGithubProxyGood(chain.get(i).key());
            return content;
        }
        throw new Exception("订阅地址无法访问：" + url);
    }

    private static List<GithubProxy.Config> chain() {
        return GithubProxy.chain(Setting.getUpdateGithubProxy(), Setting.getUpdateGithubProxyUrl(), Setting.getUpdateGithubProxyMode(), Setting.getGithubProxyGood());
    }

    private static String fetch(String url) {
        if (!url.startsWith("http")) return null;
        Request request = new Request.Builder().url(url).build();
        try (Response res = OkHttp.client(HOP_TIMEOUT).newCall(request).execute()) {
            if (!res.isSuccessful()) return null;
            String body = res.body() == null ? "" : res.body().string();
            return body.isEmpty() ? null : body;
        } catch (Exception e) {
            return null;
        }
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
