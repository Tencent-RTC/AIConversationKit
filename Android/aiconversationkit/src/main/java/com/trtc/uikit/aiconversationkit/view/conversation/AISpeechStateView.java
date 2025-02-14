package com.trtc.uikit.aiconversationkit.view.conversation;

import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENED;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.LISTENING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.SPEAKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THINKING;
import static com.trtc.uikit.aiconversationkit.state.ConversationState.AIStatus.THOUGHT;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class AISpeechStateView extends androidx.appcompat.widget.AppCompatTextView {
    private final Context mContext;

    private final Observer<ConversationState.AIStatus> mAIStateObserver = this::updateAIState;

    public AISpeechStateView(Context context) {
        this(context, null);
    }

    public AISpeechStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
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
    }

    private void updateAIState(ConversationState.AIStatus status) {
        String aiStateText = null;
        if (status == LISTENING || status == LISTENED) {
            aiStateText = mContext.getString(R.string.conversation_ai_state_listening);
        } else if (status == THINKING || status == THOUGHT) {
            aiStateText = mContext.getString(R.string.conversation_ai_state_thinking);
        } else {
            aiStateText = "";
        }
        setText(aiStateText);
        setVisibility(status == SPEAKING ? INVISIBLE : VISIBLE);
    }
}
