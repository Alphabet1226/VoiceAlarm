package com.francis.voicealarm;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 现代极简声波脉冲同心圆动效控件
 * 用于闹钟响铃界面，提供温和有机的视觉呼吸感
 */
public class PulseWaveView extends View {

    private Paint wavePaint;
    private Paint centerPaint;
    private ValueAnimator animator;
    private float progress = 0f;
    private static final int WAVE_COUNT = 3;

    public PulseWaveView(Context context) {
        super(context);
        init();
    }

    public PulseWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(dp(2.5f));

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.parseColor("#8AB4F8"));

        startAnimation();
    }

    private void startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        float maxRadius = Math.min(cx, cy) * 0.92f;
        float baseRadius = maxRadius * 0.28f;

        // 绘制三层连续扩散波纹
        for (int i = 0; i < WAVE_COUNT; i++) {
            float phase = (progress + (float) i / WAVE_COUNT) % 1.0f;
            float currentRadius = baseRadius + (maxRadius - baseRadius) * phase;
            // 透明度随扩散淡出
            int alpha = (int) ((1.0f - phase) * 160);
            wavePaint.setColor(Color.argb(alpha, 138, 180, 248)); // #8AB4F8
            wavePaint.setStrokeWidth(dp(3f * (1.0f - phase * 0.5f)));
            canvas.drawCircle(cx, cy, currentRadius, wavePaint);
        }

        // 绘制中心微呼吸亮核
        float centerBreath = 1.0f + 0.08f * (float) Math.sin(progress * Math.PI * 2);
        canvas.drawCircle(cx, cy, baseRadius * 0.75f * centerBreath, centerPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
