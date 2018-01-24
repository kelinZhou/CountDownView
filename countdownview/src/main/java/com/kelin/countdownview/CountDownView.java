package com.kelin.countdownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <strong>描述: </strong> 倒计时控件。
 * <p><strong>创建人: </strong> kelin
 * <p><strong>创建时间: </strong> 2018/1/16  下午4:25
 * <p><strong>版本: </strong> v 1.0.0
 */

public class CountDownView extends View {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CLOCKWISE_FROM_EXIST, CLOCKWISE_FROM_NOTHING, ANTICLOCKWISE_FROM_EXIST, ANTICLOCKWISE_FROM_NOTHING})
    private @interface ProgressMode {
    }

    private static final String DEFAULT_TEXT = "跳过";
    private final int DEFAULT_PADDING;
    /**
     * 表示进度条模式为顺时针从无到有。
     */
    public static final int CLOCKWISE_FROM_NOTHING = 0b0000_0010;
    /**
     * 表示进度条模式为顺时针从有到无。
     */
    public static final int CLOCKWISE_FROM_EXIST = 0b0000_0011;
    /**
     * 表示进度条模式为逆时针从无到有。
     */
    public static final int ANTICLOCKWISE_FROM_NOTHING = 0b0000_0000;
    /**
     * 表示进度条模式为逆时针从有到无。
     */
    public static final int ANTICLOCKWISE_FROM_EXIST = 0b0000_0001;
    /**
     * 背景颜色。
     */
    private int mBackgroundColor;
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
    private int mProgressBarWidth;
    /**
     * 边框的颜色。
     */
    private int mProgressBarColor;
    /**
     * 要显示的文字。
     */
    private CharSequence mContentText;
    /**
     * 字体颜色。
     */
    private int mTextColor;
    /**
     * 每行文字的长度，用作换号的依据。
     */
    private int mLineTextLength = 2;
    /**
     * 显示时长。
     */
    private int duration;
    /**
     * 用来画圆的画笔。
     */
    private final Paint mCirclePaint;
    /**
     * 用来画进度条的画笔。
     */
    private final Paint mProgressBarPaint;
    /**
     * 用来绘制字体的画笔。
     */
    private final TextPaint mTextPaint;
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
    private CD mCD;
    /**
     * 用来记录当前的进度条模式。
     */
    private int mProgressBarMode;
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
        float textSize = (int) (0x0000_000E * fontScale + 0.5);
        //获取自定义属性。
        TypedArray ta;
        if (attrs != null && (ta = context.obtainStyledAttributes(attrs, R.styleable.CountDownView)) != null) {
            mBackgroundColor = ta.getColor(R.styleable.CountDownView_backgroundColor, 0xFF666666);
            mProgressBarWidth = (int) (ta.getDimension(R.styleable.CountDownView_progressBarWidth, 0x0000_000F) + 0.9);
            mProgressBarColor = ta.getColor(R.styleable.CountDownView_progressBarColor, 0xFF66BEE0);
            textSize = ta.getDimension(R.styleable.CountDownView_android_textSize, textSize);
            mTextColor = ta.getColor(R.styleable.CountDownView_android_textColor, 0xFFFFFFFF);
            mLineTextLength = ta.getInteger(R.styleable.CountDownView_lineTextLength, 2);
            mProgress = 360 * ta.getFloat(R.styleable.CountDownView_progress, 0);
            mProgressBarMode = ta.getInt(R.styleable.CountDownView_progressBarMode, CLOCKWISE_FROM_EXIST);
            if ((mContentText = ta.getString(R.styleable.CountDownView_android_text)) == null) {
                mContentText = DEFAULT_TEXT;
            }
            setDuration(ta.getInteger(R.styleable.CountDownView_duration, 3000));
            ta.recycle();
        } else {
            mBackgroundColor = 0xFF666666;
            mProgressBarWidth = 0x0000_000F;
            mProgressBarColor = 0xFF66BEE0;
            mTextColor = 0xFFFFFFFF;
            mLineTextLength = 2;
            mProgressBarMode = CLOCKWISE_FROM_EXIST;
            mContentText = DEFAULT_TEXT;
            setDuration(3000);
        }
        DEFAULT_PADDING = (int) (3 * context.getResources().getDisplayMetrics().density + 0.5f);
        //创建用来画圆的画笔。
        mCirclePaint = createPaint(mBackgroundColor, 0, 0, Paint.Style.FILL, null);

        //创建用来画进度条的画笔。
        mProgressBarPaint = createPaint(mProgressBarColor, 0, mProgressBarWidth, Paint.Style.STROKE, null);

        //创建用来画文字的画笔以及给文字排版的工具。
        mTextPaint = new TextPaint(createPaint(mTextColor, textSize, 0, null, Paint.Align.CENTER));
        setText(mContentText);
    }

    private Paint createPaint(int color, float textSize, float strokeWidth, @Nullable Paint.Style style, @Nullable Paint.Align align) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        if (textSize > 0) {
            paint.setTextSize(textSize);
        }
        if (strokeWidth > 0) {
            paint.setStrokeWidth(strokeWidth);
        }
        if (style != null) {
            paint.setStyle(style);
        }
        if (align != null) {
            paint.setTextAlign(align);
        }
        return paint;
    }

    /**
     * 设置进度条模式,必须在{@link #start()}方法被调用前调用。
     *
     * @param mProgressMode 要设置的模式。分别为：{@link #CLOCKWISE_FROM_EXIST}、{@link #CLOCKWISE_FROM_NOTHING}、{@link #ANTICLOCKWISE_FROM_EXIST}、{@link #ANTICLOCKWISE_FROM_NOTHING}。
     */
    public CountDownView setProgressBarMode(@ProgressMode int mProgressMode) {
        checkIsStartedAndThrow();
        this.mProgressBarMode = mProgressMode;
        return this;
    }

    /**
     * 设置进度条宽度,必须在{@link #start()}方法被调用前调用。
     *
     * @param widthPx 要设置的宽度，单位为px。
     */
    public CountDownView setProgressBarWidth(int widthPx) {
        checkIsStartedAndThrow();
        this.mProgressBarWidth = widthPx;
        return this;
    }

    /**
     * 设置进度条颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public CountDownView setProgressColor(Color color) {
        checkIsStartedAndThrow();
        this.mProgressBarColor = color.toArgb();
        return this;
    }

    /**
     * 设置进度条颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    public CountDownView setProgressColor(@ColorInt int color) {
        checkIsStartedAndThrow();
        this.mProgressBarColor = color;
        return this;
    }


    /**
     * 设置进度条颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    public CountDownView setProgressColorResource(@ColorRes int color) {
        this.mProgressBarColor = ContextCompat.getColor(getContext(), color);
        return this;
    }

    /**
     * 设置圆形背景颜色。
     *
     * @param color 要设置的颜色。
     */
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        this.mBackgroundColor = color;
        mCirclePaint.setColor(color);
        invalidate();
    }

    /**
     * 设置圆形背景颜色。
     *
     * @param color 要设置的颜色。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public CountDownView setBackgroundColor(Color color) {
        this.mBackgroundColor = color.toArgb();
        mCirclePaint.setColor(this.mBackgroundColor);
        invalidate();
        return this;
    }

    /**
     * 设置圆形背景颜色。
     *
     * @param color 要设置的颜色。
     */
    public CountDownView setBackgroundColorResource(@ColorRes int color) {
        this.mBackgroundColor = ContextCompat.getColor(getContext(), color);
        mCirclePaint.setColor(this.mBackgroundColor);
        invalidate();
        return this;
    }

    /**
     * 设置文字,必须在{@link #start()}方法被调用前调用。
     *
     * @param text 要设置的文字的内容。
     */
    public CountDownView setText(CharSequence text) {
        checkIsStartedAndThrow();
        this.mContentText = text;
        createStaticLayout();
        return this;
    }

    private void createStaticLayout() {
        mStaticLayout = new StaticLayout(mContentText, mTextPaint, (int) mTextPaint.measureText(mContentText.subSequence(0, mLineTextLength).toString()), Layout.Alignment.ALIGN_NORMAL, 1F, 0, false);
    }

    /**
     * 设置字体颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    public CountDownView setTextColor(@ColorInt int color) {
        checkIsStartedAndThrow();
        this.mTextColor = color;
        mTextPaint.setColor(color);
        createStaticLayout();
        return this;
    }

    /**
     * 设置字体颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public CountDownView setTextColor(Color color) {
        checkIsStartedAndThrow();
        this.mTextColor = color.toArgb();
        mTextPaint.setColor(this.mTextColor);
        createStaticLayout();
        return this;
    }

    /**
     * 设置字体颜色,必须在{@link #start()}方法被调用前调用。
     *
     * @param color 要设置的颜色。
     */
    public CountDownView setTextColorResource(@ColorRes int color) {
        checkIsStartedAndThrow();
        this.mTextColor = ContextCompat.getColor(getContext(), color);
        mTextPaint.setColor(this.mTextColor);
        createStaticLayout();
        return this;
    }

    /**
     * 设置单行文字个数的最大值,必须在{@link #start()}方法被调用前调用。
     *
     * @param lineTextLength 要设置的单行文字个数。
     */
    public CountDownView setLineTextLength(@IntRange(from = 1) int lineTextLength) {
        checkIsStartedAndThrow();
        if (lineTextLength > 0 && lineTextLength < mContentText.length()) {
            this.mLineTextLength = lineTextLength;
        }
        createStaticLayout();
        return this;
    }

    /**
     * 设置时长，单位为毫秒,必须在{@link #start()}方法被调用前调用。
     *
     * @param duration 要设置的时长，取值范围：1000 ~ 20000(1秒至20秒)。
     */
    public CountDownView setDuration(@IntRange(from = 1000, to = 20000) int duration) {
        checkIsStartedAndThrow();
        if (duration < 1000) {
            // 这里做小于1秒的判断是因为如果小于1秒假如是200毫秒的话就会导致➗360的时候得不到整数，
            //而且如果小于一秒也没有倒计时的必要了，我是这么认为的。所以加了这个判断。
            //至于没有判断大于20秒，是考虑到有可能你真的需要显示20秒，虽然我建议不要超过20秒，但还是不要抛出异常的好，
            //我加了 @IntRange 注解只是想在超这个范围的时候在代码中有个警告。
            throw new IllegalArgumentException("the duration must be ≥ 1000 and must be ≤ 20000!");
        }
        this.duration = duration;
        return this;
    }

    protected void checkIsStartedAndThrow() {
        if (isStarted()) {
            throw new IllegalStateException("The countDownView is started，You must call before the start method call.");
        }
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
            width = (mRadios << 1) + mProgressBarWidth;
        } else {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height;
        if (heightMode != MeasureSpec.EXACTLY) {
            height = (mRadios << 1) + mProgressBarWidth;
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
        canvas.drawArc(mRect, -90, mProgress, false, mProgressBarPaint);
        canvas.translate(cx, cy - (mStaticLayout.getHeight() >>> 1));
        mStaticLayout.draw(canvas);
    }

    public void start() {
        if (mCD != null) {
            throw new IllegalStateException("The countdown has begun!");
        }
        mCD = new CD(duration, duration / 360);
        mCD.startCountDown();
    }

    public boolean isStarted() {
        return mCD != null && mCD.isStarted();
    }

    /**
     * 判断是否是顺时针进度条。
     *
     * @return 如果是返回true，否则返回false。
     */
    private boolean hasClockwiseProgress() {
        return (mProgressBarMode & 2) != 0;
    }

    /**
     * 判断是否是从有到无绘制。
     *
     * @return 如果是返回true，否则返回false。
     */
    private boolean hasFromExistProgress() {
        return (mProgressBarMode & 1) != 0;
    }

    @Override
    public boolean performClick() {
        if (mCD != null) {
            mCD.cancelCountDown();
            mCD.onFinish();
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
        private boolean isStarted;
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
            mCD = null;
            isStarted = false;
        }

        void startCountDown() {
            start();
            isStarted = true;
        }

        void cancelCountDown() {
            cancel();
            isStarted = false;
        }

        boolean isStarted() {
            return isStarted;
        }
    }
}
