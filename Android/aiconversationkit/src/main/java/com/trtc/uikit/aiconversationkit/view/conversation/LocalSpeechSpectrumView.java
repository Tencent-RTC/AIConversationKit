package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.util.ScreenUtil;
import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;

public class LocalSpeechSpectrumView extends View {
    private static final int SPECTRUM_COUNT        = 48;
    private static final int MAX_SPECTRUM_ENERGY   = 300;

    private final int[] mSpectrumEnergy  = new int[SPECTRUM_COUNT];
    private final int   mSpectrumSpacePx = ScreenUtil.dip2px(4f);
    private       int   mSpectrumWidthPx;
    private       int   mSpectrumRadiusPx;
    private       int   mSpectrumMaxHeightPx;

    private final Paint             mPaint            = new Paint();
    private final Observer<float[]> mSpectrumObserver = this::updateSpectrumEffect;

    public LocalSpeechSpectrumView(Context context) {
        this(context, null);
    }

    public LocalSpeechSpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().localSpectrumData.observe(mSpectrumObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().localSpectrumData.removeObserver(mSpectrumObserver);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mSpectrumWidthPx = (w - mSpectrumSpacePx * (SPECTRUM_COUNT - 1)) / SPECTRUM_COUNT;
        mSpectrumMaxHeightPx = h;
        mSpectrumRadiusPx = mSpectrumWidthPx >> 1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(getPaintColor());
        for (int i = 0; i < mSpectrumEnergy.length; i++) {
            int energyLen = (mSpectrumMaxHeightPx - mSpectrumWidthPx) * mSpectrumEnergy[i] / MAX_SPECTRUM_ENERGY;
            int rectL = i * (mSpectrumWidthPx + mSpectrumSpacePx);
            int rectT = (mSpectrumMaxHeightPx - energyLen) >> 1;
            int rectR = rectL + mSpectrumWidthPx;
            int rectB = mSpectrumMaxHeightPx - rectT;
            canvas.drawRect(rectL, rectT, rectR, rectB, mPaint);

            int circleX = (rectL + rectR) >> 1;
            canvas.drawCircle(circleX, rectT, mSpectrumRadiusPx, mPaint);
            canvas.drawCircle(circleX, rectB, mSpectrumRadiusPx, mPaint);
        }
    }

    private void updateSpectrumEffect(float[] spectrumData) {
        int step = spectrumData.length / SPECTRUM_COUNT;
        for (int i = 0; i < SPECTRUM_COUNT; i++) {
            int srcIndex = i * step;
            if (srcIndex >= spectrumData.length) {
                return;
            }
            mSpectrumEnergy[i] = (int) (spectrumData[i * step] + MAX_SPECTRUM_ENERGY);
        }
        if (isMuteData()) {
            return;
        }
        invalidate();
    }

    private int getPaintColor() {
        int colorResId = isMuteData() ? R.color.conversation_white_7 : R.color.conversation_white_3;
        return getContext().getResources().getColor(colorResId);
    }

    private boolean isMuteData() {
        int checkLen = Math.min(6, mSpectrumEnergy.length);
        for (int i = 0; i < checkLen; i++) {
            if (mSpectrumEnergy[i] > 0) {
                return false;
            }
        }
        return true;
    }
}
