package com.kelin.countdownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.annotation.IntRange;
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
    private final int DEFAULT_PADDING;
    /**
     * 表示进度模式为顺时针从无到有。
     */
    private static final int CLOCKWISE_FROM_NOTHING = 0b0000_0010;
    /**
     * 表示进度模式为顺时针从有到无。
     */
    private static final int CLOCKWISE_FROM_EXIST = 0b0000_0011;
    /**
     * 表示进度模式为逆时针从无到有。
     */
    private static final int ANTICLOCKWISE_FROM_NOTHING = 0b0000_0000;
    /**
     * 表示进度模式为逆时针从有到无。
     */
    private static final int ANTICLOCKWISE_FROM_EXIST = 0b0000_0001;
    /**
     * 背景颜色。
     */
    private int mBackgroundColor = 0xFF666666;
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
    private int mProgressWidth = 0x0000_000F;
    /**
     * 边框的颜色。
     */
    private int mProgressColor = 0xFF66BEE0;
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
    private int mTextColor = 0xFFFFFFFF;
    /**
     * 每行文字的长度，用作换号的依据。
     */
    private int mLineTextLength = 2;
    /**
     * 显示时长。
     */
    private int duration = 3000;
    /**
     * 用来画圆的画笔。
     */
    private Paint mCirclePaint;
    /**
     * 用来画进度条的画笔。
     */
    private Paint mProgressPaint;
    /**
     * 绘制进度条时需要用到的矩形，为了避免在onDraw的时候重复new，所以在这里直接创建了。
     */
    private final RectF mRect = new RectF();
    /**
     * 用来绘制文字的工具。
     */
    private StaticLayout mStaticLayout;
    /**
     * 倒计时工具。
     */
    private CountDownTimer mCountDownTimer;
    /**
     * 用来记录当前的进度条模式。
     */
    private int mProgressMode = CLOCKWISE_FROM_EXIST;
    /**
     * 当前圆的半径。
     */
    private int mRadios;

    public CountDownView(Context context) {
        this(context, null);
    }

    public CountDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        mTextSize = (int) (0x0000_000E * fontScale + 0.5);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CountDownView);
            mBackgroundColor = ta.getColor(R.styleable.CountDownView_backgroundColor, mBackgroundColor);
            mProgressWidth = (int) (ta.getDimension(R.styleable.CountDownView_progressWidth, mProgressWidth) + 0.9);
            mProgressColor = ta.getColor(R.styleable.CountDownView_progressColor, mProgressColor);
            mTextSize = ta.getDimension(R.styleable.CountDownView_android_textSize, mTextSize);
            mTextColor = ta.getColor(R.styleable.CountDownView_android_textColor, mTextColor);
            mLineTextLength = ta.getInteger(R.styleable.CountDownView_lineTextLength, mLineTextLength);
            mProgress = 360 * ta.getFloat(R.styleable.CountDownView_progress, 0);
            mProgressMode = ta.getInt(R.styleable.CountDownView_progressMode, mProgressMode);
            if ((mContentText = ta.getString(R.styleable.CountDownView_android_text)) == null) {
                mContentText = DEFAULT_TEXT;
            }
            setDuration(ta.getInteger(R.styleable.CountDownView_duration, duration));
            ta.recycle();
        }
        DEFAULT_PADDING = (int) (3 * context.getResources().getDisplayMetrics().density + 0.5f);
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

        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setDither(true);
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStrokeWidth(mProgressWidth);
        mProgressPaint.setStyle(Paint.Style.STROKE);

        mStaticLayout = new StaticLayout(mContentText, mTextPaint, (int) mTextPaint.measureText(mContentText.substring(0, mLineTextLength)), Layout.Alignment.ALIGN_NORMAL, 1F, 0, false);
    }

    /**
     * 设置时长，单位为毫秒。
     *
     * @param duration 要设置的时长，取值范围：1000 ~ 20000(1秒至20秒)。
     */
    public void setDuration(@IntRange(from = 1000, to = 20000) int duration) {
        if (duration < 1000) {
            // 这里做小于1秒的判断是因为如果小于1秒假如是200毫秒的话就会导致➗360的时候得不到整数，
            //而且如果小于一秒也没有倒计时的必要了，我是这么认为的。所以加了这个判断。
            //至于没有判断大于20秒，是考虑到有可能你真的需要显示20秒，虽然我建议不要超过20秒，但还是不要抛出异常的好，
            //我加了 @IntRange 注解只是想在超这个范围的时候在代码中有个警告。
            throw new IllegalArgumentException("the duration must be ≥ 1000 and must be ≤ 20000!");
        }
        this.duration = duration;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int w = mStaticLayout.getWidth() + getPaddingLeft() + getPaddingRight() + DEFAULT_PADDING;
        int h = mStaticLayout.getHeight() + getPaddingTop() + getPaddingBottom() + DEFAULT_PADDING;
        mRadios = (int) ((Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2)) + 0.5f) / 2);
        int width;
        if (widthMode != MeasureSpec.EXACTLY) {
            width = (mRadios << 1) + mProgressWidth;
        } else {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height;
        if (heightMode != MeasureSpec.EXACTLY) {
            height = (mRadios << 1) + mProgressWidth;
        } else {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        int cx = getMeasuredWidth() >>> 1;
        int cy = getMeasuredHeight() >>> 1;
        canvas.drawCircle(cx, cy, mRadios, mCirclePaint);

        mRect.left = cx - mRadios;
        mRect.top = cy - mRadios;
        mRect.right = cx + mRadios;
        mRect.bottom = cy + mRadios;
        canvas.drawArc(mRect, -90, mProgress, false, mProgressPaint);
        canvas.translate(cx, cy - (mStaticLayout.getHeight() >>> 1));
        mStaticLayout.draw(canvas);
    }

    public void start() {
        if (mCountDownTimer != null) {
            throw new IllegalStateException("The countdown has begun!");
        }
        mCountDownTimer = new CD(duration, duration / 360);
        mCountDownTimer.start();
    }

    /**
     * 判断是否是顺时针进度条。
     *
     * @return 如果是返回true，否则返回false。
     */
    private boolean hasClockwiseProgress() {
        return (mProgressMode & 2) != 0;
    }

    /**
     * 判断是否是从有到无绘制。
     *
     * @return 如果是返回true，否则返回false。
     */
    private boolean hasFromExistProgress() {
        return (mProgressMode & 1) != 0;
    }

    @Override
    public boolean performClick() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer.onFinish();
        }
        return super.performClick();
    }

    public void setOnFinishListener(OnFinishListener listener) {
        mOnFinishListener = listener;
    }

    public interface OnFinishListener {
        /**
         * 倒计时完成或空间被点击后执行。
         */
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
            mProgress = hasFromExistProgress() ?
                    hasClockwiseProgress() ?
                            ~(millisUntilFinished / mCountDownInterval) :
                            millisUntilFinished / mCountDownInterval :
                    hasClockwiseProgress() ?
                            360 - millisUntilFinished / mCountDownInterval :
                            ~(360 - millisUntilFinished / mCountDownInterval);
            invalidate();
        }

        @Override
        public void onFinish() {
            mProgress = hasFromExistProgress() ? 0 : 360;
            invalidate();
            if (mOnFinishListener != null) {
                mOnFinishListener.onFinish();
            }
            mCountDownTimer = null;
        }
    }
}
