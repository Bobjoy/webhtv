package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public record ConfigEvent(Type type) {

    public static void common() {
        EventBus.getDefault().post(new ConfigEvent(Type.COMMON));
    }

    public static void vod() {
        EventBus.getDefault().post(new ConfigEvent(Type.VOD));
    }

    public static void playerPerformance() {
        EventBus.getDefault().post(new ConfigEvent(Type.PLAYER_PERFORMANCE));
    }

    public boolean isVod() {
        return type == Type.VOD;
    }

    public boolean isPlayerPerformance() {
        return type == Type.PLAYER_PERFORMANCE;
    }

    public enum Type {
        COMMON, VOD, PLAYER_PERFORMANCE
    }
}
