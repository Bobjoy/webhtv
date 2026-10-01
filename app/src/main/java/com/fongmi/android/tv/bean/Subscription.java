package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;

import java.net.IDN;

import okhttp3.HttpUrl;

/** 订阅源：一条 URL 拉回候选资源地址列表；生效仍走 Config 表。 */
public class Subscription {

    private String name;
    private String url;
    private int type;

    public Subscription() {
        this.name = "";
        this.url = "";
    }

    public Subscription(String name, String url, int type) {
        this.name = trim(name);
        this.url = trim(url);
        this.type = type;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = trim(name);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = trim(url);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isEmpty() {
        return getUrl().isEmpty();
    }

    public String getDisplayName() {
        if (!getName().isEmpty()) return getName();
        String host = getHost();
        return host.isEmpty() ? getUrl() : host;
    }

    private String getHost() {
        try {
            HttpUrl httpUrl = HttpUrl.parse(getUrl());
            return httpUrl == null ? "" : IDN.toUnicode(httpUrl.host());
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getDisplayName() + " [" + getType() + "] " + getUrl();
    }
}
