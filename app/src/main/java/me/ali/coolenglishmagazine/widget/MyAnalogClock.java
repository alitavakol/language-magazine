package me.ali.coolenglishmagazine.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

import me.ali.coolenglishmagazine.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 * <a href="http://stackoverflow.com/a/16804506">This</a> is the reference.
 */
@RemoteView
public class MyAnalogClock extends View {

    private Drawable mHourHand;
    private Drawable mMinuteHand;
//    private Drawable mSecondHand;
    private Drawable mDial;

    private int mDialWidth;
    private int mDialHeight;

    private float mSeconds;
    private float mMinutes;
    private float mHour;

    Context mContext;

    public MyAnalogClock(Context context) {
        super(context);
    }

    public MyAnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyAnalogClock(Context context, AttributeSet attrs,
                         int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
//        TypedArray a =
//                context.obtainStyledAttributes(
//                        attrs, R.styleable.AnalogClock, defStyle, 0);
        mContext = context;
        // mDial = a.getDrawable(com.android.internal.R.styleable.AnalogClock_dial);
        // if (mDial == null) {
        mDial = r.getDrawable(R.drawable.clock_dial);
        // }

        //  mHourHand = a.getDrawable(com.android.internal.R.styleable.AnalogClock_hand_hour);
        //  if (mHourHand == null) {
        mHourHand = r.getDrawable(R.drawable.clock_hour);
        //  }

        //   mMinuteHand = a.getDrawable(com.android.internal.R.styleable.AnalogClock_hand_minute);
        //   if (mMinuteHand == null) {
        mMinuteHand = r.getDrawable(R.drawable.clock_minute);
//        mSecondHand = r.getDrawable(R.drawable.clockgoog_minute);
        //   }

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
    }

    int mWidthSize;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        mWidthSize = widthSize;

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float) heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSize((int) (mDialWidth * scale), widthMeasureSpec),
                resolveSize((int) (mDialHeight * scale), widthMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Here you can set the size of your clock
//        int availableWidth = mWidthSize;
//        int availableHeight = mWidthSize;

        //Actual size
        int x = mWidthSize / 2;
        int y = mWidthSize / 2;

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (mWidthSize < w || mWidthSize < h) {
            scaled = true;
            float scale = Math.min((float) mWidthSize / (float) w,
                    (float) mWidthSize / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        dial.draw(canvas);

        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        w = mHourHand.getIntrinsicWidth();
        h = mHourHand.getIntrinsicHeight();
        mHourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        mHourHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
        w = mMinuteHand.getIntrinsicWidth();
        h = mMinuteHand.getIntrinsicHeight();
        mMinuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        mMinuteHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mSeconds, x, y);
//        w = mSecondHand.getIntrinsicWidth();
//        h = mSecondHand.getIntrinsicHeight();
//        mSecondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
//        mSecondHand.draw(canvas);
        canvas.restore();
        if (scaled) {
            canvas.restore();
        }
    }

    public void setTime(int hours, int minutes, int seconds) {
        mSeconds = 6.0f * seconds;
        mMinutes = minutes + seconds / 60.0f;
        mHour = hours + mMinutes / 60.0f;
    }
}
