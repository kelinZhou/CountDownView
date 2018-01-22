package com.kelin.countdownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.annotation.Size;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * <strong>描述: </strong> 倒计时控件。
 * <p><strong>创建人: </strong> kelin
 * <p><strong>创建时间: </strong> 2018/1/16  下午4:25
 * <p><strong>版本: </strong> v 1.0.0
 */

public class CountDownView extends View {
    private static final String DEFAULT_TEXT = "跳过";
    private static final float DEFAULT_TEXT_SIZE = 0x0000_000E;
    private static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    private static final float DEFAULT_STROKE_WIDTH = 0x0000_000F;
    private static final int DEFAULT_STROKE_COLOR = 0xFF66BEE0;
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFF666666;
    private static final int DEFAULT_DURATION = 0x0000_1388;
    /**
     * 表示进度模式为从无到有。
     */
    private static final int MODE_FROM_NOTHING = 0x0000_00F1;
    /**
     * 表示进度模式为从有到无。
     */
    private static final int MODE_FROM_EXIST = 0x0000_00F2;

    /**
     * 背景颜色。
     */
    private int mBackgroundColor = DEFAULT_BACKGROUND_COLOR;
    /**
     * 倒计时的监听。
     */
    private OnFinishListener mOnFinishListener;
    /**
     * 一个范围为：0~360的小数，用来记录当前的进度。
     */
    private float mProgress;
    /**
     * 边框的宽度。
     */
    private float mStrokeWidth = DEFAULT_STROKE_WIDTH;
    /**
     * 边框的颜色。
     */
    private int mStrokeColor = DEFAULT_STROKE_COLOR;
    /**
     * 要显示的文字。
     */
    private String mContentText = DEFAULT_TEXT;
    /**
     * 字体大小。
     */
    private float mTextSize;
    /**
     * 字体颜色。
     */
    private int mTextColor = DEFAULT_TEXT_COLOR;
    /**
     * 显示时长。
     */
    private long duration = DEFAULT_DURATION;
    private Paint mCirclePaint;
    private Paint mStrokePaint;
    private StaticLayout staticLayout;
    private CountDownTimer mCountDownTimer;
    /**
     * 用来记录当前的宽度。
     */
    private int mContentWidth;
    /**
     * 用来记录当前的高度。
     */
    private int mContentHeight;
    /**
     * 用来记录当前的进度条模式。
     */
    private int progressMode = MODE_FROM_EXIST;
    RectF mRect = new RectF();

    public CountDownView(Context context) {
        this(context, null);
    }

    public CountDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        mTextSize = (int) (DEFAULT_TEXT_SIZE * fontScale + 0.5f);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CountDownView);
            mBackgroundColor = ta.getColor(R.styleable.CountDownView_backgroundColor, DEFAULT_BACKGROUND_COLOR);
            mStrokeWidth = ta.getDimension(R.styleable.CountDownView_strokeWidth, DEFAULT_STROKE_WIDTH);
            mStrokeColor = ta.getColor(R.styleable.CountDownView_strokeColor, DEFAULT_STROKE_COLOR);
            mTextSize = ta.getDimension(R.styleable.CountDownView_android_textSize, mTextSize);
            mTextColor = ta.getColor(R.styleable.CountDownView_android_textColor, DEFAULT_TEXT_COLOR);
            if ((mContentText = ta.getString(R.styleable.CountDownView_android_text)) == null) {
                mContentText = DEFAULT_TEXT;
            }
            duration = ta.getInteger(R.styleable.CountDownView_duration, DEFAULT_DURATION);
            ta.recycle();
        }

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setDither(true);
        mCirclePaint.setColor(mBackgroundColor);
        mCirclePaint.setStyle(Paint.Style.FILL);

        TextPaint mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mStrokePaint = new Paint();
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setDither(true);
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setStyle(Paint.Style.STROKE);

        staticLayout = new StaticLayout(mContentText, mTextPaint, (int) mTextPaint.measureText(mContentText), Layout.Alignment.ALIGN_NORMAL, 1F, 0, false);
    }

    /**
     * 设置时长，单位为毫秒。
     *
     * @param duration 要设置的时长。
     */
    public void setDuration(@Size(min = 1000, max = 20000) long duration) {
        this.duration = duration;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int w;
        if (widthMode != MeasureSpec.EXACTLY) {
            w = staticLayout.getWidth();
        } else {
            w = MeasureSpec.getSize(widthMeasureSpec);
        }
        w += (getPaddingLeft() + getPaddingRight());
        int h;
        if (heightMode != MeasureSpec.EXACTLY) {
            h = staticLayout.getHeight();
        } else {
            h = MeasureSpec.getSize(heightMeasureSpec);
        }
        h += (getPaddingTop() + getPaddingBottom());
        mContentHeight = mContentWidth = Math.max(w, h);
        setMeasuredDimension(mContentWidth, mContentHeight);
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        int min = Math.min(mContentWidth, mContentHeight);

        int w = mContentWidth >>> 1;
        int m = min >>> 1;
        int h = mContentHeight >>> 1;
        canvas.drawCircle(w, h, m, mCirclePaint);

        if (mContentWidth > mContentHeight) {
            mRect.left = w - m + mStrokeWidth / 2;
            mRect.top = mStrokeWidth / 2;
            mRect.right = w + m - mStrokeWidth / 2;
            mRect.bottom = mContentHeight - mStrokeWidth / 2;
        } else {
            mRect.left = mStrokeWidth / 2;
            mRect.top = h - m + mStrokeWidth / 2;
            mRect.right = mContentWidth - mStrokeWidth / 2;
            mRect.bottom = h - mStrokeWidth / 2 + m;
        }
        canvas.drawArc(mRect, -90, mProgress, false, mStrokePaint);
        canvas.translate(w, h - staticLayout.getHeight() / 2);
        staticLayout.draw(canvas);
    }

    public void start() {
        if (mCountDownTimer != null) {
            throw new IllegalStateException("The countdown has begun!");
        }
        mCountDownTimer = new CD(duration, duration / 360);
        mCountDownTimer.start();
    }

    public void setOnFinishListener(OnFinishListener listener) {
        mOnFinishListener = listener;
    }

    public interface OnFinishListener {
        void onFinish();
    }

    private class CD extends CountDownTimer {

        private final long mCountDownInterval;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        CD(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            mCountDownInterval = countDownInterval;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mProgress = progressMode == MODE_FROM_NOTHING ?
                    360 - millisUntilFinished / mCountDownInterval
                    : ~(millisUntilFinished / mCountDownInterval);
            invalidate();
        }

        @Override
        public void onFinish() {
            mProgress = progressMode == MODE_FROM_NOTHING ? 360 : 0;
            invalidate();
            if (mOnFinishListener != null) {
                mOnFinishListener.onFinish();
            }
        }
    }
}
