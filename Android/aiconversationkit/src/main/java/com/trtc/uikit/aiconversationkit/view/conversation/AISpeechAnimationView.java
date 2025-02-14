package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

import java.util.LinkedList;

public class AISpeechAnimationView extends FrameLayout {
    private static final int AI_LISTENING_PLAY_DURATION_MS = 1330;
    private static final int AI_LISTENED_PLAY_DURATION_MS  = 200;
    private static final int AI_THINKING_PLAY_DURATION_MS  = 830;
    private static final int AI_THOUGHT_PLAY_DURATION_MS   = 670;

    private final Context              mContext;
    private final VideoView            mViewPlayer;
    private final AISpeechSpectrumView mViewSpectrum;
    private final Handler              mMainHandler = new Handler(Looper.getMainLooper());

    private final Observer<ConversationState.AIStatus> mAIStateObserver  = this::handleAIStateChanged;
    private final Runnable                             mPlayAnimationRun = this::startPlayAIAnimation;


    private final LinkedList<ConversationState.AIStatus> mAIStatusCache = new LinkedList<>();
    private       ConversationState.AIStatus             mCurAIStatus   = ConversationState.AIStatus.INITIALIZING;

    public AISpeechAnimationView(@NonNull Context context) {
        this(context, null);
    }

    public AISpeechAnimationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_ai_speech_animation, this);
        mViewPlayer = root.findViewById(R.id.conversation_vv_ai_speech_animation);
        mViewSpectrum = root.findViewById(R.id.conversation_view_ai_speech_spectrum);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().aiStatus.observe(mAIStateObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().aiStatus.removeObserver(mAIStateObserver);
        stopPlayAIAnimation();
    }

    private void startPlayAIAnimation() {
        if (!mAIStatusCache.isEmpty()) {
            mCurAIStatus = mAIStatusCache.removeFirst();
        }
        mViewPlayer.setVisibility(mCurAIStatus == ConversationState.AIStatus.SPEAKING ? INVISIBLE : VISIBLE);
        mViewSpectrum.setVisibility(mCurAIStatus == ConversationState.AIStatus.SPEAKING ? VISIBLE : INVISIBLE);
        if (mCurAIStatus == ConversationState.AIStatus.SPEAKING) {
            return;
        }
        mViewPlayer.setVideoURI(getVideoUri(mCurAIStatus));
        mViewPlayer.start();
        mViewPlayer.setOnCompletionListener(mp -> {
            int duration = mp.getDuration();
            mMainHandler.postDelayed(mPlayAnimationRun, getAnimationTime(mCurAIStatus) - duration);
        });
    }

    private void stopPlayAIAnimation() {
        mViewPlayer.stopPlayback();
        mMainHandler.removeCallbacks(mPlayAnimationRun);
    }

    private void handleAIStateChanged(ConversationState.AIStatus status) {
        if (status == ConversationState.AIStatus.LISTENING) {
            mAIStatusCache.clear();
            mCurAIStatus = ConversationState.AIStatus.LISTENING;
            startPlayAIAnimation();
        } else {
            mAIStatusCache.add(status);
        }
        mViewPlayer.setVisibility(status == ConversationState.AIStatus.SPEAKING ? INVISIBLE : VISIBLE);
        mViewSpectrum.setVisibility(status == ConversationState.AIStatus.SPEAKING ? VISIBLE : INVISIBLE);
    }

    private Uri getVideoUri(ConversationState.AIStatus status) {
        int resId;
        if (status == ConversationState.AIStatus.LISTENING) {
            resId = R.raw.ai_listening;
        } else if (status == ConversationState.AIStatus.LISTENED) {
            resId = R.raw.ai_listened;
        } else if (status == ConversationState.AIStatus.THINKING) {
            resId = R.raw.ai_thinking;
        } else if (status == ConversationState.AIStatus.THOUGHT) {
            resId = R.raw.ai_thought;
        } else {
            resId = R.raw.ai_listening;
        }
        return Uri.parse("android.resource://" + mContext.getPackageName() + "/" + resId);
    }

    private int getAnimationTime(ConversationState.AIStatus status) {
        if (status == ConversationState.AIStatus.LISTENING) {
            return AI_LISTENING_PLAY_DURATION_MS;
        } else if (status == ConversationState.AIStatus.LISTENED) {
            return AI_LISTENED_PLAY_DURATION_MS;
        } else if (status == ConversationState.AIStatus.THINKING) {
            return AI_THINKING_PLAY_DURATION_MS;
        } else if (status == ConversationState.AIStatus.THOUGHT) {
            return AI_THOUGHT_PLAY_DURATION_MS;
        } else {
            return AI_LISTENING_PLAY_DURATION_MS;
        }
    }
}
