package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;
import com.trtc.uikit.aiconversationkit.store.AIConversationStoreImpl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

public class ExperienceDurationView extends FrameLayout {
    private static final String BUSINESS_LOGIN_SERVICE    = "LoginService";
    private static final String METHOD_GET_USER_MODEL     = "methodGetUserModel";
    private static final String PARAM_KEY_USER_MODEL      = "paramUserModel";
    private static final String INTERNAL_USER_PREFIX      = "moa";
    private static final int    CALL_BACK_CODE_SUCCESS    = 0;
    private static final long   COUNT_DOWN_INTERVAL_MS    = 1000L;
    private static final int    TIME_DEDUCTION_INTERVAL_S = 10;
    private static final long   COUNT_DOWN_LATER_MS       = TIME_DEDUCTION_INTERVAL_S * 1000 >> 1;

    private TextView mTvMinute;
    private TextView mTvSecond;

    private final Handler           mMainHandler           = new Handler(Looper.getMainLooper());
    private final Runnable          mCountDownRun          = this::executeCountDown;
    private final DecimalFormat     mDecimalFormat         = new DecimalFormat("00");
    private final Observer<Integer> mTimeRemainingObserver = this::initTimeRemaining;

    private long    mCountDownRemaining;
    private long    mStartTime;
    private boolean mIsMOALogin = false;

    public ExperienceDurationView(@NonNull Context context) {
        this(context, null);
    }

    public ExperienceDurationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        checkLoginType();
    }

    private void checkLoginType() {
        TUICore.callService(BUSINESS_LOGIN_SERVICE, METHOD_GET_USER_MODEL, null,
                new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        if (CALL_BACK_CODE_SUCCESS == code && bundle != null) {
                            Serializable userModel = bundle.getSerializable(PARAM_KEY_USER_MODEL);
                            mIsMOALogin = INTERNAL_USER_PREFIX.equals(parseLoginType(userModel));
                            if (mIsMOALogin) {
                                setVisibility(GONE);
                            }
                        }
                    }
                });
    }

    private String parseLoginType(Serializable userModel) {
        if (userModel == null) {
            return null;
        }
        try {
            Field field = userModel.getClass().getField("loginType");
            return (String) field.get(userModel);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!PackageService.isInternalDemo() || mIsMOALogin) {
            return;
        }
        mStartTime = SystemClock.elapsedRealtime();
        ConversationManager.sharedInstance().remainingExperienceTimeS
                .observe(mTimeRemainingObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!PackageService.isInternalDemo() || mIsMOALogin) {
            return;
        }
        mMainHandler.removeCallbacks(mCountDownRun);
        ConversationManager.sharedInstance().remainingExperienceTimeS
                .removeObserver(mTimeRemainingObserver);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.conversation_view_experience_duration, this);
        mTvMinute = findViewById(R.id.conversation_tv_tip_minute);
        mTvSecond = findViewById(R.id.conversation_tv_tip_second);
    }

    private void executeCountDown() {
        if (mIsMOALogin) {
            return;
        }
        updateViewByTimeRemaining();
        if (mCountDownRemaining <= 0) {
            handleTimeout();
            return;
        }
        mMainHandler.removeCallbacks(mCountDownRun);
        mMainHandler.postDelayed(mCountDownRun, COUNT_DOWN_INTERVAL_MS);
        deductionExperienceTime();
        mCountDownRemaining--;
    }

    private void handleTimeout() {
        if (mCountDownRemaining == 0) {
            deductionExperienceTime();
        }
        AIConversationStoreImpl.shared.stopAIConversation(null);
    }

    private void deductionExperienceTime() {
        if (mCountDownRemaining % TIME_DEDUCTION_INTERVAL_S != 0
                || SystemClock.elapsedRealtime() - mStartTime < (COUNT_DOWN_LATER_MS)) {
            return;
        }
        ConversationManager.sharedInstance().deductionExperienceTime();
    }

    private void updateViewByTimeRemaining() {
        long minutes = mCountDownRemaining / 60;
        long seconds = mCountDownRemaining % 60;
        mTvMinute.setText(mDecimalFormat.format(minutes));
        mTvSecond.setText(mDecimalFormat.format(seconds));
    }

    private void initTimeRemaining(int timeRemaining) {
        mCountDownRemaining = timeRemaining;
        mMainHandler.post(mCountDownRun);
    }
}
