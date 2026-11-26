package com.trtc.uikit.aiconversationkit.manager.internal;

import android.text.TextUtils;

import com.trtc.tuikit.common.system.ContextProvider;

public class PackageService {
    private static final String PACKAGE_RT_CUBE     = "com.tencent.trtc";
    private static final String PACKAGE_TENCENT_RTC = "com.tencent.rtc.app";

    public static boolean isInternalDemo() {
        return isRTCube() || isTencentRTC();
    }

    public static boolean isRTCube() {
        return TextUtils.equals(PACKAGE_RT_CUBE, ContextProvider.getApplicationContext().getPackageName());
    }

    public static boolean isTencentRTC() {
        return TextUtils.equals(PACKAGE_TENCENT_RTC, ContextProvider.getApplicationContext().getPackageName());
    }
}
