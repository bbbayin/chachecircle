package com.ccq.share.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.ccq.share.utils.DensityUtils;

/**
 * Created by Administrator on 2017/11/14.
 */

public class ProgressView extends View {
    private Paint mPaint;
    private Paint textPaint;
    private int corner = 20;
    private int circleRadius = 26;
    private int marginTop = 26;
    private int startAngle = 0;
    private int sweepAngle = 0;
    private RectF progressRect;
    private boolean isMeasured = false;
    private ObjectAnimator rotateAnim;
    private ObjectAnimator sweepAnim;
    private AnimatorSet set;
    private final String word = "%d秒后开始分享";
    private int time = 10;
    private MyCount myCount;
    private RectF mBackgroundRectF;

    public int getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
        invalidate();
    }

    public int getSweepAngle() {
        return sweepAngle;
    }

    public void setSweepAngle(int sweepAngle) {
        this.sweepAngle = sweepAngle;
    }

    public ProgressView(Context context) {
        this(context, null);
    }

    public void setTime(int time) {
        this.time = time;
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(DensityUtils.sp2px(context, 13));
        textPaint.setColor(Color.WHITE);

        corner = DensityUtils.dp2px(context, corner);
        circleRadius = DensityUtils.dp2px(context, circleRadius);
        marginTop = DensityUtils.dp2px(context, marginTop);

        rotateAnim = ObjectAnimator.ofInt(this, "startAngle", 0, 360);
        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatMode(ValueAnimator.RESTART);
        rotateAnim.setInterpolator(new LinearInterpolator());
        rotateAnim.setRepeatCount(100);

        sweepAnim = ObjectAnimator.ofInt(this, "sweepAngle", 30, 270);
        sweepAnim.setDuration(1000);
        sweepAnim.setRepeatMode(ValueAnimator.REVERSE);
        sweepAnim.setInterpolator(new LinearInterpolator());
        sweepAnim.setRepeatCount(100);

        set = new AnimatorSet();
        set.play(rotateAnim).with(sweepAnim);
        set.setDuration(1000);
    }

    public void startCount(int time) {
        //开启动画
        if (set != null) {
            set.start();
        }
        //开启倒计时
        if (myCount != null) {
            myCount.cancel();
            myCount = null;
        }
        myCount = new MyCount(time * 1000, 1000);
        myCount.start();
        isShowing = true;
    }

    public void endCount() {
        //停止动画
        if (set != null) {
            set.end();
        }
        if (myCount != null) {
            myCount.cancel();
            myCount = null;
        }
        isShowing = false;
    }

    private boolean isShowing = false;

    public boolean isShowing() {
        return isShowing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //progress的矩形
        if (!isMeasured) {
            mBackgroundRectF = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
            //半径
            circleRadius = (int) (getMeasuredWidth() * 0.2);
            progressRect = new RectF(getMeasuredWidth() / 2 - circleRadius, marginTop,
                    getMeasuredWidth() / 2 + circleRadius, circleRadius * 2 + marginTop);
            isMeasured = true;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //背景
        mPaint.setColor(Color.parseColor("#FF302F2F"));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(mBackgroundRectF, corner, corner, mPaint);
        //progress
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(getContext(), 5));
        mPaint.setColor(Color.WHITE);
        canvas.drawArc(progressRect, startAngle, sweepAngle, false, mPaint);
        //text
        String text = String.format(word, time);
        float textWidth = textPaint.measureText(text);
        canvas.drawText(text, (getMeasuredWidth() - textWidth) / 2, progressRect.bottom + DensityUtils.dp2px(getContext(), 20), textPaint);
    }

    private class MyCount extends CountDownTimer {

        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            time = (int) (millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {
            cancel();
            if (timeEndListener != null) timeEndListener.onTimeEnd();
        }
    }

    private OnTimeEndListener timeEndListener;

    public void setOnTimeEndListener(OnTimeEndListener listener) {
        this.timeEndListener = listener;
    }

    public interface OnTimeEndListener {
        void onTimeEnd();
    }
}
