package com.fongmi.android.tv.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class GithubProxy {

    public static final String DIRECT = "direct";
    public static final String CUSTOM = "custom";
    public static final String MODE_FULL_URL = "full_url";
    public static final String MODE_STRIP_SCHEME = "strip_scheme";

    /** 降级顺序按本机实测排：ghfast 对部分仓库返 403 黑名单，故排在可用代理之后。 */
    private static final String[] FALLBACK = {"gh_monlor", "github_chenc", "gh_acmsz", "ghfast", DIRECT};

    /** 一次订阅拉取最多试几跳，末跳固定是直连。 */
    public static final int MAX_HOPS = 4;

    private static final Preset[] PRESETS = {
            new Preset(DIRECT, "GitHub", "", MODE_FULL_URL),
            new Preset("github_chenc", "github.chenc.dev", "https://github.chenc.dev", MODE_STRIP_SCHEME),
            new Preset("gh_acmsz", "gh.acmsz.top", "https://gh.acmsz.top", MODE_FULL_URL),
            new Preset("ghfast", "ghfast.top", "https://ghfast.top", MODE_FULL_URL),
            new Preset("gh_monlor", "gh.monlor.com", "https://gh.monlor.com", MODE_FULL_URL),
            new Preset(CUSTOM, "Custom", "", MODE_FULL_URL),
    };

    private GithubProxy() {
    }

    public static Preset[] presets() {
        return Arrays.copyOf(PRESETS, PRESETS.length);
    }

    public static Preset find(String id) {
        for (Preset preset : PRESETS) if (preset.id.equals(id)) return preset;
        return PRESETS[0];
    }

    public static Config resolve(String id, String customUrl, String customMode) {
        Preset preset = find(id);
        if (DIRECT.equals(preset.id)) return new Config(DIRECT, "", MODE_FULL_URL);
        if (CUSTOM.equals(preset.id)) return new Config(CUSTOM, UpdateUrl.requireHttpsOrigin(customUrl), normalizeMode(customMode));
        return new Config(preset.id, preset.baseUrl, preset.mode);
    }

    public static String normalizeMode(String mode) {
        return MODE_STRIP_SCHEME.equals(mode) ? MODE_STRIP_SCHEME : MODE_FULL_URL;
    }

    /**
     * 用户选的排第一，其余预设按 {@link #FALLBACK} 补齐，末跳固定直连；上次成功的排到最前，省去每次重头试。
     * 自定义地址会连带另一种拼接方式，用户不必理解这个开关。
     */
    public static List<Config> chain(String id, String customUrl, String customMode, String lastGood) {
        List<Config> items = new ArrayList<>();
        add(items, id, customUrl, customMode);
        if (CUSTOM.equals(find(id).id)) add(items, CUSTOM, customUrl, MODE_STRIP_SCHEME.equals(normalizeMode(customMode)) ? MODE_FULL_URL : MODE_STRIP_SCHEME);
        for (String preset : FALLBACK) {
            if (items.size() >= MAX_HOPS - 1) break;
            add(items, preset, "", "");
        }
        add(items, DIRECT, "", "");
        for (int i = 1; i < items.size(); i++) {
            if (!items.get(i).key().equals(lastGood)) continue;
            items.add(0, items.remove(i));
            break;
        }
        return List.copyOf(items);
    }

    private static void add(List<Config> items, String id, String customUrl, String customMode) {
        try {
            Config config = resolve(id, customUrl, customMode);
            if (!items.contains(config)) items.add(config);
        } catch (Exception ignored) {
        }
    }

    public static final class Preset {

        public final String id;
        public final String label;
        public final String baseUrl;
        public final String mode;

        private Preset(String id, String label, String baseUrl, String mode) {
            this.id = id;
            this.label = label;
            this.baseUrl = baseUrl;
            this.mode = mode;
        }
    }

    public static final class Config {

        public final String id;
        public final String baseUrl;
        public final String mode;

        private Config(String id, String baseUrl, String mode) {
            this.id = id;
            this.baseUrl = baseUrl;
            this.mode = mode;
        }

        public String rewrite(String url) {
            String target = UpdateUrl.requireHttpsUrl(url);
            if (DIRECT.equals(id)) return target;
            if (MODE_STRIP_SCHEME.equals(mode)) return baseUrl + "/" + target.substring("https://".length());
            return baseUrl + "/" + target;
        }

        /** 落盘用的稳定标识，见 {@link #chain}。 */
        public String key() {
            return id + "|" + baseUrl + "|" + mode;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Config config && config.id.equals(id) && config.baseUrl.equals(baseUrl) && config.mode.equals(mode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, baseUrl, mode);
        }
    }
}
