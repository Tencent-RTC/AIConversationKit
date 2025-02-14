package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class LocalSpeechTextView extends androidx.appcompat.widget.AppCompatTextView {
    private final Observer<ConversationState.SpeechText> mSpeechTextObserver = this::updateSpeechText;

    public LocalSpeechTextView(@NonNull Context context) {
        this(context, null);
    }

    public LocalSpeechTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationState state = ConversationManager.sharedInstance().getConversationState();
        state.localSpeechText.observe(mSpeechTextObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationState state = ConversationManager.sharedInstance().getConversationState();
        state.localSpeechText.removeObserver(mSpeechTextObserver);
    }

    private void updateSpeechText(ConversationState.SpeechText speechText) {
        setText(speechText.text);
    }
}
