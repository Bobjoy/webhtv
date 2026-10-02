package com.fongmi.android.tv.setting;

import com.github.catvod.utils.Prefers;

public class LiveSetting {

    public static boolean isInvert() {
        return Prefers.getBoolean("invert");
    }
}
