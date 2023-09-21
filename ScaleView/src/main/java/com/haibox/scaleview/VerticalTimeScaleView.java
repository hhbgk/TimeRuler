package com.haibox.scaleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.haibox.scaleview.bean.ScaleInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class VerticalTimeScaleView extends ViewGroup {
    private final String tag = getClass().getSimpleName();

    private static final long SEC_MILLIS = 1000;//millisecond
    private static final long MIN_MILLIS = 60 * SEC_MILLIS;
    private static final long HALF_HOUR_MILLIS = 30 * MIN_MILLIS;// half hour
    private static final long HOUR_MILLIS = 60 * MIN_MILLIS;
    private static final long DAY_MILLIS = HOUR_MILLIS * 24;

    private TextPaint textPaint;

    private Paint scalePaint, middlePaint, rulerPaint;
    private Paint videoPaint;

    /**
     * width of view in pixels
     */
    private int width;
    /**
     * Height of view in pixels
     */
    private int mHeight;
    /**
     * left and right limit of the ruler in view, in milliseconds
     */
    private long left, right;
    /**
     * how many fingers are being used? 0, 1, 2
     */
    int fingers;
    /**
     * holds pointer id of #1/#2 fingers
     */
    int finger1id, finger2id = -1;
    /**
     * holds x/y in pixels of #1/#2 fingers from last frame
     */
    volatile float finger1x, finger1y, finger2x, finger2y;

    /**
     * width of the view in milliseconds, cached value of (right-left)
     */
    float span;//跨度

    /**
     * reusable calendar class object for rounding time to nearest applicable unit in onDraw
     */
    private Calendar acalendar;

    private OnRulerListener onRulerListener;

    private boolean zoomable = false;// 是否缩放

    /**
     * The logical density of the display.
     */
    private float mDensity;
    /**
     * 数值文字宽度的一半：时间格式为“00:00”，所以长度固定
     */
    private float mTextHalfWidth;
    private int mDeviceWidth;// 手机屏幕宽度
    private final int textColor, rulerColor, scaleColor;
    private final int mTotalCellNum = 48;
    private float marginTop, rulerSize, scaleSize;
    private final List<ScaleInfo> scaleList = new ArrayList<>();
    public VerticalTimeScaleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        @SuppressLint("CustomViewStyleable")
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalTimeScaleView);
        textColor = typedArray.getColor(R.styleable.VerticalTimeScaleView_textColor, Color.RED);
        rulerColor = typedArray.getColor(R.styleable.VerticalTimeScaleView_rulerColor, Color.GRAY);
        scaleColor = typedArray.getColor(R.styleable.VerticalTimeScaleView_scaleColor, Color.GRAY);
        marginTop = typedArray.getDimensionPixelSize(R.styleable.VerticalTimeScaleView_scaleMarginTop, 0);
        rulerSize = typedArray.getDimensionPixelSize(R.styleable.VerticalTimeScaleView_rulerSize, 2);
        scaleSize = typedArray.getDimensionPixelSize(R.styleable.VerticalTimeScaleView_scaleSize, 2);
        typedArray.recycle();
        init(context);
        initData();
    }

    public VerticalTimeScaleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalTimeScaleView(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        mDensity = getResources().getDisplayMetrics().density;

        final Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point deviceDisplay = new Point();
        display.getSize(deviceDisplay);
        mDeviceWidth = deviceDisplay.x;

        Log.i(tag, "init: mDensity=" + mDensity + ", width=" + getWidth() +
                ", mDeviceWidth=" + mDeviceWidth + ",deviceDisplay.y="+ deviceDisplay.y);

        middlePaint = new Paint();// 中间线画笔
        middlePaint.setStrokeWidth(2f);
        middlePaint.setAntiAlias(true);
        middlePaint.setColor(getResources().getColor(android.R.color.black));
        middlePaint.setStyle(Style.FILL);

        rulerPaint = new Paint();
        rulerPaint.setColor(rulerColor);
        rulerPaint.setStrokeWidth(rulerSize);
        rulerPaint.setAntiAlias(true);
        rulerPaint.setStyle(Style.FILL);

        scalePaint = new Paint();
        scalePaint.setColor(scaleColor);
        scalePaint.setStrokeWidth(scaleSize);
        scalePaint.setAntiAlias(true);
        scalePaint.setStyle(Style.FILL);

        textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setStrokeWidth(2f);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Style.FILL);
        textPaint.setTextSize(context.getResources().getDimension(R.dimen.text_12sp));
        mTextHalfWidth = textPaint.measureText("0") * .5f;

        videoPaint = new Paint();// 视频文件画笔
        videoPaint.setStrokeWidth(2f);
        videoPaint.setAntiAlias(true);

        acalendar = new GregorianCalendar();

        // start the view off somewhere, +/- some time around now
        left = System.currentTimeMillis() -  3 * HOUR_MILLIS;
        right = System.currentTimeMillis() + 3 * HOUR_MILLIS;
        span = right - left;
        Log.w(tag, "right - left=" +(right - left));
    }

    private void initData() {
        int year = acalendar.get(Calendar.YEAR);
        int month = acalendar.get(Calendar.MONTH);
        int day = acalendar.get(Calendar.DAY_OF_MONTH);
        int hour = acalendar.get(Calendar.HOUR_OF_DAY);
        acalendar.set(year, month, day, 12, 0, 0);

        Calendar setTime = Calendar.getInstance();
        for (int i = 0; i <= mTotalCellNum; i ++ ) {
            setTime.set(
                    year, month, day,
                    mTotalCellNum == 48 ? i / 2 : i,
                    mTotalCellNum == 48 ? ((i % 2 == 0) ? 0 : 30) : 0, 0);
            ScaleInfo scaleInfo = new ScaleInfo();
            scaleInfo.setTime(setTime.getTimeInMillis());
            scaleInfo.setText(i);
            scaleList.add(scaleInfo);
//            Log.i(tag, "i=" + i + ", getTimeInMillis=" + setTime.getTimeInMillis());
        }
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        width = getWidth();
        mHeight = getHeight();
        Log.i(tag, "onlayout===Height=" + mHeight + ", width=" + width);
        final int count = getChildCount();
        int curWidth, curHeight, curLeft, curTop, maxHeight;

        //get the available size of child view
        final int childLeft = this.getPaddingLeft();
        final int childTop = this.getPaddingTop();
        final int childRight = this.getMeasuredWidth() - this.getPaddingRight();
        final int childBottom = this.getMeasuredHeight() - this.getPaddingBottom();
        final int childWidth = childRight - childLeft;
        final int childHeight = childBottom - childTop;

        maxHeight = 0;
        curLeft = childLeft;
        curTop = childTop;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE)
                return;

            //Get the maximum size of the child
            child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST));
            curWidth = child.getMeasuredWidth();
            curHeight = child.getMeasuredHeight();
            //wrap is reach to the end
            if (curLeft + curWidth >= childRight) {
                curLeft = childLeft;
                curTop += maxHeight;
                maxHeight = 0;
            }
            //do the layout
            child.layout(curLeft, curTop, curLeft + curWidth, curTop + curHeight);
            //store the max height
            if (maxHeight < curHeight)
                maxHeight = curHeight;
            curLeft += curWidth;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.w(tag, "========onMeasure==========width="+getWidth() + ", mDeviceWidth="+ mDeviceWidth);
        int count = getChildCount();
        // Measurement will ultimately be computing these values.
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        int mLeftWidth = 0;
        int rowCount = 0;

        // Iterate through all children, measuring them and computing our dimensions
        // from their size.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE)
                continue;

            // Measure the child.
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            maxWidth += Math.max(maxWidth, child.getMeasuredWidth());
            mLeftWidth += child.getMeasuredWidth();

            if ((mLeftWidth / mDeviceWidth) > rowCount) {
                maxHeight += child.getMeasuredHeight();
                rowCount++;
            } else {
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
            }
            childState = combineMeasuredStates(childState, child.getMeasuredState());
        }

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Report our final dimensions.
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 画刻度尺中间的日期和时间文本
        drawMiddleTimeText(canvas);

        // round calendar down to leftmost hour
        acalendar.setTimeInMillis(left);
        // floor the calendar to various time units to find where (in ms) they start
        acalendar.set(Calendar.MILLISECOND, 0); // second start
        acalendar.set(Calendar.SECOND, 0); // minute start
        acalendar.set(Calendar.MINUTE, 0); // hour start
        // draw ruler
        canvas.drawLine(0, marginTop, width, marginTop, rulerPaint);
        drawScaleLine(canvas);

//        drawMiddleLine(canvas);
    }

    private void drawScaleLine(Canvas canvas) {
        long next;
        for (int i = 0; i < scaleList.size(); i++) {
            ScaleInfo info = scaleList.get(i);
            next = info.getTime();
            float x = ((float) (next - left) / span * (float) width);
            if (info.getText() %2 == 0) {
                canvas.drawLine(x, marginTop + rulerSize/2, x, marginTop + rulerSize/2 + 8 * mDensity, scalePaint);
            } else {
                canvas.drawLine(x, marginTop + rulerSize/2, x, marginTop + rulerSize/2 + 4 * mDensity, scalePaint);
            }
            float y = mHeight - 5 * mDensity;
            drawHourText(canvas, x, y, info.getText());
        }
    }

    private volatile boolean isMoving = false;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionevent) {
        switch (motionevent.getActionMasked()) {
            // First finger down, start panning
            case MotionEvent.ACTION_DOWN:
                isMoving = true;
                Log.i(tag, "ACTION_DOWN=");
                fingers = 1; // panning mode

                // save id and coords
                finger1id = motionevent.getPointerId(motionevent.getActionIndex());
                finger1x = motionevent.getX();
                finger1y = motionevent.getY();

                invalidate(); // redraw
                return true;
            case MotionEvent.ACTION_MOVE:
                if (fingers == 0) // if not tracking fingers as down
                    return false; // ignore move events

                float new1x,
                        new1y,
                        new2x,
                        new2y; // Hold new positions of two fingers

                // get finger 1 position
                int pointerindex = motionevent.findPointerIndex(finger1id);
                if (pointerindex == -1) // no change
                {
                    new1x = finger1x; // use values from previous frame
                    new1y = finger1y;
                } else
                // changed
                {
                    // get new values
                    new1x = motionevent.getX(pointerindex);
                    new1y = motionevent.getY(pointerindex);
                }

                // get finger 2 position
                pointerindex = motionevent.findPointerIndex(finger2id);
                if (pointerindex == -1) {
                    new2x = finger2x;
                    new2y = finger2y;
                } else {
                    new2x = motionevent.getX(pointerindex);
                    new2y = motionevent.getY(pointerindex);
                }

                // panning
                if (fingers == 1) {
                    // how far to scroll in milliseconds to match the scroll input in pixels
                    long delta1xinmillis = (long) ((finger1x - new1x) * span / width); // (deltax)*span/width
//                    Log.i(tag, "move new1y="+ new1y + ", (mHeight - 100 * mDensity)=" + (mHeight - 100 * mDensity));
//                    if (new1y > (mHeight - 100 * mDensity))
                    {
                        left += delta1xinmillis;
                        right += delta1xinmillis;

                        long movingOut = left + (right - left) / 2;
                        long max = scaleList.get(mTotalCellNum).getTime();
                        long min = scaleList.get(0).getTime();
                        long leftOffset = movingOut - min;
                        long rightOffset = movingOut - max;
                        if (rightOffset > 0) {
                            doNotMoving = true;
                            left -= rightOffset;
                            right -= rightOffset;
                            return true;
                        } else if (leftOffset < 0) {
                            doNotMoving = true;
                            left -= leftOffset;
                            right -= leftOffset;
                            return true;
                        }
                    }
                }

                // save
                finger1x = new1x;
                finger1y = new1y;
                finger2x = new2x;
                finger2y = new2y;

                invalidate(); // redraw with new left,right
                return true;

            case MotionEvent.ACTION_UP:// last pointer up, no more motionevents
                Log.i(tag, "ACTION_UP=");
                fingers = 0;
                isMoving = false;
                if (doNotMoving) {
                    doNotMoving = false;
                    return true;
                }
                long time = left + (right - left) / 2;
                acalendar.setTimeInMillis(time);
                int hourOfDay = acalendar.get(Calendar.HOUR_OF_DAY);
                Log.w(tag, "ACTION_UP: time=" + time + ", hourOfDay=" + hourOfDay);

                if (onRulerListener != null) {
                    onRulerListener.onStopMoving(acalendar.getTimeInMillis());
                }

                invalidate(); // redraw
                requestLayout();
                return true;
        }

        return super.onTouchEvent(motionevent);
    }

    private boolean doNotMoving = false;
    /**
     * 画中间的红色指示线、阴影等。指示线两端简单的用了两个矩形代替
     *
     * @param canvas canvas
     */
    private void drawMiddleLine(Canvas canvas) {
        canvas.save();
        //canvas.drawLine(width / 2, 0, width / 2, mHeight, middlePaint);
        canvas.drawLine(width / 2.0f, 0, width / 2.0f, mHeight, middlePaint);
        canvas.restore();
    }

    private void drawHourText(Canvas canvas, float x, float y, int h24) {
        x = x - mTextHalfWidth;
        y = marginTop - 40 * mDensity;
//        canvas.drawText(String.format(Locale.getDefault(), "%02d:00", h24), x, y, textPaint);

        if (h24 % 2 == 0) {
            canvas.save();
            canvas.rotate(90, x, y);
            canvas.drawText(String.format(Locale.getDefault(), "%02d:00", h24 / 2), x, y, textPaint);
            canvas.restore();
        }
    }
    private SimpleDateFormat timeFormat1 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private void drawMiddleTimeText(Canvas canvas) {
        if (isMoving) return;

//        Log.d(tag, "drawMiddleTimeText width=" + width + ", mHeight=" + mHeight);
        // round calendar down to leftmost hour
        acalendar.setTimeInMillis(left + (right-left)/2);
        if (onRulerListener != null) {
            onRulerListener.onChanging(acalendar.getTimeInMillis());
//            Log.i(tag, "getTimeInMillis=" + acalendar.getTimeInMillis());
        }
    }

    /**
     * 设置数据
     * @param data 视频文件数据
     */
//    public void setData(List<VideoInfo> data) {
//        videoList.clear();
//        videoList.addAll(data);
//        invalidate();
//    }

    /**
     * 清除数据
     */
    public void clearData() {
//        videoList.clear();
        invalidate();
    }

    public interface OnRulerListener {
        void onChanging(long timeInMillis);

        void onStopMoving(long timeInMillis);
    }

    /**
     * 设置用于接收结果的监听器
     *
     * @param listener 回调的实现
     */
    public void setOnRulerListener(OnRulerListener listener) {
        onRulerListener = listener;
    }

    /**
     * 设置是否允许缩放
     * @param zoomable 缩放
     */
    public void enableZoom(boolean zoomable) {
        this.zoomable = zoomable;
    }

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public void setTimeOffset (long offsetMilliseconds) {
        if (isMoving) return;
        if (offsetMilliseconds == 0) {
            Log.e(tag, "offsetMilliseconds = 0");
            return;
        }
        long last = left + (right - left) / 2;
        long move = offsetMilliseconds - last;

        if (move >= 900 || move <= -1200) {
            left += move;
            right += move;
            postInvalidate();
            last = left + (right - left) / 2;
//            Log.w("PlaybackFragment", "move =" + move + ", offsetMilliseconds=" + offsetMilliseconds
//                    + ", format offsetMilliseconds=" + timeFormat.format(last));
        }
    }
}