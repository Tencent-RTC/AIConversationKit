package com.trtc.uikit.aiconversationkit.view.feedback;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;

public class FeedbackStarView extends FrameLayout implements Survey {
    private ImageFilterView[] mViewStars;
    private TextView          mTvName;

    private SurveyCallback mSurveyCallback;

    public FeedbackStarView(@NonNull Context context) {
        this(context, null);
    }

    public FeedbackStarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public void setText(int stringId) {
        mTvName.setText(stringId);
    }

    @Override
    public void setSurveyCallback(SurveyCallback callback) {
        mSurveyCallback = callback;
    }

    private void initView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FeedbackStarView);
        String name = typedArray.getString(R.styleable.FeedbackStarView_text);
        typedArray.recycle();
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_feedback_star, this);
        mTvName = findViewById(R.id.conversation_tv_feedback_name);
        mTvName.setText(name);

        ImageFilterView mIfvStarFirst = root.findViewById(R.id.conversation_ifv_first_star);
        ImageFilterView mIfvStarSecond = root.findViewById(R.id.conversation_ifv_second_star);
        ImageFilterView mIfvStarThird = root.findViewById(R.id.conversation_ifv_third_star);
        ImageFilterView mIfvStarFourth = root.findViewById(R.id.conversation_ifv_fourth_star);
        ImageFilterView mIfvStarFifth = root.findViewById(R.id.conversation_ifv_fifth_star);
        mViewStars = new ImageFilterView[] {mIfvStarFirst, mIfvStarSecond, mIfvStarThird, mIfvStarFourth,
                mIfvStarFifth};

        initListener(mIfvStarFirst, ConversationConstant.FEEDBACK_VERY_DISSATISFIED);
        initListener(mIfvStarSecond, ConversationConstant.FEEDBACK_DISSATISFIED);
        initListener(mIfvStarThird, ConversationConstant.FEEDBACK_NEUTRAL);
        initListener(mIfvStarFourth, ConversationConstant.FEEDBACK_SATISFIED);
        initListener(mIfvStarFifth, ConversationConstant.FEEDBACK_VERY_SATISFIED);
    }

    private void initListener(View view, int feedback) {
        view.setOnClickListener(v -> {
            updateStarStatus(feedback);
            if (mSurveyCallback != null) {
                mSurveyCallback.onSurvey(feedback);
            }
        });
    }

    private void updateStarStatus(int starNum) {
        for (int i = 0; i < starNum; i++) {
            mViewStars[i].setImageResource(R.drawable.conversation_ic_star_selected);
        }
        for (int i = starNum; i < mViewStars.length; i++) {
            mViewStars[i].setImageResource(R.drawable.conversation_ic_star_unselected);
        }
    }
}
