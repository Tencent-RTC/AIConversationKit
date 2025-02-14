package com.trtc.uikit.aiconversationkit.view.conversation;

import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.SPEAKING;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class AISpeechTextView extends androidx.appcompat.widget.AppCompatTextView {
    private final Observer<ConversationState.AIStatus>   mAIStateObserver    = this::updateAIState;
    private final Observer<ConversationState.SpeechText> mSpeechTextObserver = this::updateSpeechText;

    public AISpeechTextView(Context context) {
        super(context);
    }

    public AISpeechTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationState state = ConversationManager.sharedInstance().getConversationState();
        state.aiStatus.observe(mAIStateObserver);
        state.aiSpeechText.observe(mSpeechTextObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationState state = ConversationManager.sharedInstance().getConversationState();
        state.aiStatus.removeObserver(mAIStateObserver);
        state.aiSpeechText.removeObserver(mSpeechTextObserver);
    }

    private void updateAIState(ConversationState.AIStatus status) {
        setVisibility(status == SPEAKING ? VISIBLE : INVISIBLE);
    }

    private void updateSpeechText(ConversationState.SpeechText speechText) {
        setText(speechText.text);
    }
}
