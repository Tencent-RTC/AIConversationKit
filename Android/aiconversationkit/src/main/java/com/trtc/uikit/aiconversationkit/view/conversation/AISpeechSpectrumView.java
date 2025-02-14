package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.util.ScreenUtil;
import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;

public class AISpeechSpectrumView extends View {
    private static final int SPECTRUM_COUNT          = 20;
    private static final int SPECTRUM_START_COLOR_R  = 0x46;
    private static final int SPECTRUM_START_COLOR_G  = 0x7E;
    private static final int SPECTRUM_START_COLOR_B  = 0xE7;
    private static final int SPECTRUM_MIDDLE_COLOR_R = 0xE2;
    private static final int SPECTRUM_MIDDLE_COLOR_G = 0xF1;
    private static final int SPECTRUM_MIDDLE_COLOR_B = 0xFE;
    private static final int MAX_SPECTRUM_ENERGY     = 300;

    private final int[] mSpectrumEnergy  = new int[SPECTRUM_COUNT];
    private final int[] mSpectrumColors  = new int[SPECTRUM_COUNT];
    private final int   mSpectrumSpacePx = ScreenUtil.dip2px(5.2f);
    private       int   mSpectrumWidthPx;
    private       int   mSpectrumRadiusPx;
    private       int   mSpectrumMaxHeightPx;

    private final Paint             mPaint            = new Paint();
    private final Observer<float[]> mSpectrumObserver = this::updateSpectrumEffect;

    public AISpeechSpectrumView(Context context) {
        this(context, null);
    }

    public AISpeechSpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        initSpectrumColors();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().aiSpectrumData.observe(mSpectrumObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().aiSpectrumData.removeObserver(mSpectrumObserver);
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
        for (int i = 0; i < mSpectrumEnergy.length; i++) {
            mPaint.setColor(mSpectrumColors[i]);

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
        invalidate();
    }

    private void initSpectrumColors() {
        int halfCount = SPECTRUM_COUNT >> 1;
        int redStep = (SPECTRUM_MIDDLE_COLOR_R - SPECTRUM_START_COLOR_R) / halfCount;
        int greenStep = (SPECTRUM_MIDDLE_COLOR_G - SPECTRUM_START_COLOR_G) / halfCount;
        int blueStep = (SPECTRUM_MIDDLE_COLOR_B - SPECTRUM_START_COLOR_B) / halfCount;
        for (int i = 0; i < halfCount; i++) {
            mSpectrumColors[i] = Color.argb(255, SPECTRUM_START_COLOR_R + redStep * i,
                    SPECTRUM_START_COLOR_G + greenStep * i, SPECTRUM_START_COLOR_B + blueStep * i);
            mSpectrumColors[SPECTRUM_COUNT - i - 1] = mSpectrumColors[i];
        }
    }
}
