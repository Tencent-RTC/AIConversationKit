package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.R;

public class LocalAvatarView extends ImageFilterView {
    public LocalAvatarView(Context context) {
        this(context, null);
    }

    public LocalAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        TUICore.callService("ConfigureService",
                "methodGetIsRTCCUbe", null, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        boolean isRTCube = bundle == null || bundle.getBoolean("paramIsRTCCUbe", true);
                        setBackgroundResource(isRTCube ? R.drawable.conversation_ic_local_avatar_internal
                                : R.drawable.conversation_ic_local_avatar_international);
                    }

                });
    }
}
