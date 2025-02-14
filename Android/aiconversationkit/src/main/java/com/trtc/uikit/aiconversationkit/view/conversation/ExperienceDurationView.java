package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;

import java.text.DecimalFormat;

public class ExperienceDurationView extends FrameLayout {
    private static final long COUNT_DOWN_INTERVAL_MS    = 1000L;
    private static final int  TIME_DEDUCTION_INTERVAL_S = 10;
    private static final long COUNT_DOWN_LATER_MS       = TIME_DEDUCTION_INTERVAL_S * 1000 >> 1;

    private TextView mTvMinute;
    private TextView mTvSecond;

    private final Handler           mMainHandler           = new Handler(Looper.getMainLooper());
    private final Runnable          mCountDownRun          = this::executeCountDown;
    private final DecimalFormat     mDecimalFormat         = new DecimalFormat("00");
    private final Observer<Integer> mTimeRemainingObserver = this::initTimeRemaining;

    private long mCountDownRemaining;
    private long mStartTime;

    public ExperienceDurationView(@NonNull Context context) {
        this(context, null);
    }

    public ExperienceDurationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!PackageService.isInternalDemo()) {
            return;
        }
        mStartTime = SystemClock.elapsedRealtime();
        ConversationManager.sharedInstance().getConversationState().remainingExperienceTimeS
                .observe(mTimeRemainingObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!PackageService.isInternalDemo()) {
            return;
        }
        mMainHandler.removeCallbacks(mCountDownRun);
        ConversationManager.sharedInstance().getConversationState().remainingExperienceTimeS
                .removeObserver(mTimeRemainingObserver);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.conversation_view_experience_duration, this);
        mTvMinute = findViewById(R.id.conversation_tv_tip_minute);
        mTvSecond = findViewById(R.id.conversation_tv_tip_second);
    }

    private void executeCountDown() {
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
        ExperienceTimeoutDialogFragment fragment = new ExperienceTimeoutDialogFragment();
        boolean isNeedFeedback = ConversationManager.sharedInstance().getConversationState().isNeedFeedback;
        fragment.showDialog(getContext(), "ExperienceTimeoutDialogFragment", isNeedFeedback);
        ConversationManager.sharedInstance().stopConversation();
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
