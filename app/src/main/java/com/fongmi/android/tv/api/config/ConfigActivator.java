package com.fongmi.android.tv.api.config;

import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.Setting;

/** 资源地址生效入口：按 {@code type} 选择点播/直播/壁纸加载，设置页与订阅条目页共用。 */
public class ConfigActivator {

    public static void activate(Config config, Callback callback) {
        switch (config.getType()) {
            case 0:
                VodConfig.load(config, callback);
                break;
            case 1:
                LiveConfig.load(config, callback);
                break;
            case 2:
                Setting.putWall(0);
                WallConfig.load(config, callback);
                break;
        }
    }
}
