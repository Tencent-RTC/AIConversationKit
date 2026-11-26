package com.trtc.uikit.aiconversationkit.common;

import com.tencent.liteav.base.util.LiteavLog;

public class Logger {
    private static final String AIC_API = "[aic-api]";

    public static void v(String tag, String msg) {
        LiteavLog.v(AIC_API, String.format("%s %s", tag, msg));
    }

    public static void d(String tag, String msg) {
        LiteavLog.d(AIC_API, String.format("%s %s", tag, msg));
    }

    public static void i(String tag, String msg) {
        LiteavLog.i(AIC_API, String.format("%s %s", tag, msg));
    }

    public static void w(String tag, String msg) {
        LiteavLog.w(AIC_API, String.format("%s %s", tag, msg));
    }

    public static void e(String tag, String msg) {
        LiteavLog.e(AIC_API, String.format("%s %s", tag, msg));
    }
}
