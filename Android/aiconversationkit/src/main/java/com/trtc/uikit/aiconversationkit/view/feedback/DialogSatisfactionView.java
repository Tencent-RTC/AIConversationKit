package com.trtc.uikit.aiconversationkit.view.feedback;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;

public class DialogSatisfactionView extends FrameLayout implements Survey {
    private ImageFilterView mIfvVeryDissatisfied;
    private ImageFilterView mIfvDissatisfied;
    private ImageFilterView mIfvNeutral;
    private ImageFilterView mIfvSatisfied;
    private ImageFilterView mIfvVerySatisfied;

    private SurveyCallback mSurveyCallback;

    public DialogSatisfactionView(@NonNull Context context) {
        this(context, null);
    }

    public DialogSatisfactionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    public void setSurveyCallback(SurveyCallback callback) {
        mSurveyCallback = callback;
    }

    private void initView(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_conversation_satisfaction_survey,
                this);
        mIfvVeryDissatisfied = root.findViewById(R.id.conversation_iv_very_dissatisfied);
        mIfvDissatisfied = root.findViewById(R.id.conversation_iv_dissatisfied);
        mIfvNeutral = root.findViewById(R.id.conversation_iv_neutral);
        mIfvSatisfied = root.findViewById(R.id.conversation_iv_satisfied);
        mIfvVerySatisfied = root.findViewById(R.id.conversation_iv_very_satisfied);

        initListener(mIfvVeryDissatisfied, R.drawable.conversation_ic_very_dissatisfied_light,
                ConversationConstant.FEEDBACK_VERY_DISSATISFIED);
        initListener(mIfvDissatisfied, R.drawable.conversation_ic_dissatisfied_light,
                ConversationConstant.FEEDBACK_DISSATISFIED);
        initListener(mIfvNeutral, R.drawable.conversation_ic_neutral_light,
                ConversationConstant.FEEDBACK_NEUTRAL);
        initListener(mIfvSatisfied, R.drawable.conversation_ic_satisfied_light,
                ConversationConstant.FEEDBACK_SATISFIED);
        initListener(mIfvVerySatisfied, R.drawable.conversation_ic_very_satisfied_light,
                ConversationConstant.FEEDBACK_VERY_SATISFIED);
    }

    private void initListener(ImageFilterView view, int selectResId, int feedback) {
        view.setOnClickListener(v -> {
            setAllViewDark();
            view.setImageResource(selectResId);
            if (mSurveyCallback != null) {
                mSurveyCallback.onSurvey(feedback);
            }
        });
    }

    private void setAllViewDark() {
        mIfvVeryDissatisfied.setImageResource(R.drawable.conversation_ic_very_dissatisfied_dark);
        mIfvDissatisfied.setImageResource(R.drawable.conversation_ic_dissatisfied_dark);
        mIfvNeutral.setImageResource(R.drawable.conversation_ic_neutral_dark);
        mIfvSatisfied.setImageResource(R.drawable.conversation_ic_satisfied_dark);
        mIfvVerySatisfied.setImageResource(R.drawable.conversation_ic_very_satisfied_dark);
    }
}
