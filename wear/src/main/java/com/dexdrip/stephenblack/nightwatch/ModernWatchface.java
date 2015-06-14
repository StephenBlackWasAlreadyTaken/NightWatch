package com.dexdrip.stephenblack.nightwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public abstract class ModernWatchface extends WatchFace {
    public final float PADDING = 20f;
    public final float CIRCLE_WIDTH = 10f;
    public final int BIG_HAND_WIDTH = 16;
    public final int SMALL_HAND_WIDTH = 8;
    public final int NEAR = 2; //how near do the hands have to be to activate overlapping mode
    public final boolean ALWAYS_HIGHLIGT_SMALL = false;

    //variables for time
    private float angleBig = 0f;
    private float angleSMALL = 0f;
    private int hour, minute;
    private int color;
    private Paint circlePaint = new Paint();
    private RectF rect, rectDelete;
    private boolean overlapping;


    public Point displaySize = new Point();
    private MessageReceiver messageReceiver = new MessageReceiver();

    private int sgvLevel = 0;
    private String sgvString = "999";
    private int batteryLevel = 0;
    private double datetime = 0;
    private String direction = "";
    private String delta = "";
    public ArrayList<BgWatchData> bgDataList = new ArrayList<>();

    private View layoutView;
    private int specW;
    private int specH;
    private View myLayout;


    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CreateWakelock");
        wakeLock.acquire(30000);
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        display.getSize(displaySize);

        specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                View.MeasureSpec.EXACTLY);
        specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                View.MeasureSpec.EXACTLY);

        //register Message Receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Intent.ACTION_SEND));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myLayout = inflater.inflate(R.layout.modern_layout, null);
        prepareLayout();
        prepareDrawTime();
        ListenerService.requestData(this);
        wakeLock.release();
    }

    /*@Override
    protected void onLayout(WatchShape shape, Rect screenBounds, WindowInsets screenInsets) {
        super.onLayout(shape, screenBounds, screenInsets);
        layoutView.onApplyWindowInsets(screenInsets);
    }*/

    @Override
    public void onDestroy() {
        if (messageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawTime(canvas);
        myLayout.draw(canvas);
    }

    private void prepareLayout() {
        // prepare fields
        ((TextView) myLayout.findViewById(R.id.sgvString)).setText(getSgvString());
        ((TextView) myLayout.findViewById(R.id.sgvString)).setTextColor(getTextColor());


        ((TextView) myLayout.findViewById(R.id.agoString)).setText(getMinutes());
        ((TextView) myLayout.findViewById(R.id.agoString)).setTextColor(getTextColor());
        ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta());
        ((TextView) myLayout.findViewById(R.id.deltaString)).setTextColor(getTextColor());
        //TODO: add more view elements?


        myLayout.measure(specW, specH);
        myLayout.layout(0, 0, myLayout.getMeasuredWidth(),
                myLayout.getMeasuredHeight());
    }

    private void drawTime(Canvas canvas) {

        //delete Canvas
        canvas.drawColor(getBackgroundColor());

        //draw circle
        circlePaint.setColor(color);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        canvas.drawArc(rect, 0, 360, false, circlePaint);
        //"remove" hands from circle
        circlePaint.setColor(getBackgroundColor());
        //circlePaint.setColor(Color.RED);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH * 3);

        canvas.drawArc(rectDelete, angleBig, (float) BIG_HAND_WIDTH, false, circlePaint);
        canvas.drawArc(rectDelete, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);


        if (overlapping) {
            //add small hand with extra
            circlePaint.setStrokeWidth(CIRCLE_WIDTH * 2);
            circlePaint.setColor(color);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);

            //remove inner part of hands
            circlePaint.setColor(getBackgroundColor());
            circlePaint.setStrokeWidth(CIRCLE_WIDTH);
            canvas.drawArc(rect, angleBig, (float) BIG_HAND_WIDTH, false, circlePaint);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);
        }
        drawOtherStuff(canvas);
    }

    private void prepareDrawTime() {
        hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 12;
        minute = Calendar.getInstance().get(Calendar.MINUTE);
        angleBig = (((hour + minute / 60f) / 12f * 360) - 90 - BIG_HAND_WIDTH / 2f + 360) % 360;
        angleSMALL = ((minute / 60f * 360) - 90 - SMALL_HAND_WIDTH / 2f + 360) % 360;


        color = 0;
        switch (getSgvLevel()) {
            case -1:
                color = getLowColor();
                break;
            case 0:
                color = getInRangeColor();
                break;
            case 1:
                color = getHighColor();
                break;
        }

        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        circlePaint.setAntiAlias(true);
        ;

        rect = new RectF(PADDING, PADDING, (float) (displaySize.x - PADDING), (float) (displaySize.y - PADDING));
        rectDelete = new RectF(PADDING - CIRCLE_WIDTH / 2, PADDING - CIRCLE_WIDTH / 2, (float) (displaySize.x - PADDING + CIRCLE_WIDTH / 2), (float) (displaySize.y - PADDING + CIRCLE_WIDTH / 2));
        overlapping = ALWAYS_HIGHLIGT_SMALL || areOverlapping(angleSMALL, angleSMALL + SMALL_HAND_WIDTH + NEAR, angleBig, angleBig + BIG_HAND_WIDTH + NEAR);
    }

    private boolean areOverlapping(float aBegin, float aEnd, float bBegin, float bEnd) {
        return
                aBegin <= bBegin && aEnd >= bBegin ||
                        aBegin <= bBegin && (bEnd > 360) && bEnd % 360 > aBegin ||
                        bBegin <= aBegin && bEnd >= aBegin ||
                        bBegin <= aBegin && aEnd > 360 && aEnd % 360 > bBegin;
    }

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        if (oldTime.hasMinuteChanged(newTime)) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimeChangedWakelock");
            wakeLock.acquire(30000);
            /*Preparing the layout just on every minute tick:
            *  - hopefully better battery life
            *  - drawback: might update the minutes since last reading up to one minute late*/
            prepareLayout();
            prepareDrawTime();
            invalidate();  //redraw the time
            wakeLock.release();
        }
    }

    /*Some methods to implement by child classes*/
    public abstract int getLowColor();

    public abstract int getInRangeColor();

    public abstract int getHighColor();

    public abstract int getBackgroundColor();

    public abstract int getTextColor();

    public void drawOtherStuff(Canvas canvas) {}
    public int holdInMemory() { return 1;}


    //getters & setters

    private synchronized int getSgvLevel() {
        return sgvLevel;
    }

    private synchronized void setSgvLevel(int sgvLevel) {
        this.sgvLevel = sgvLevel;
    }

    private synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    private synchronized void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }


    private synchronized double getDatetime() {
        return datetime;
    }

    private synchronized void setDatetime(double datetime) {
        this.datetime = datetime;
    }

    private synchronized String getDirection() {
        return direction;
    }

    private void setDirection(String direction) {
        this.direction = direction;
    }

    String getSgvString() {
        return sgvString;
    }

    void setSgvString(String sgvString) {
        this.sgvString = sgvString;
    }

    public String getDelta() {
        return delta;
    }

    private void setDelta(String delta) {
        this.delta = delta;
    }

    public String getMinutes() {
        String minutes = "--\'";
        if (getDatetime() != 0) {
            minutes = ((int) Math.floor((System.currentTimeMillis() - getDatetime()) / 60000)) + "\'";
        }
        return minutes;
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire(30000);

            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            setSgvLevel((int) dataMap.getLong("sgvLevel"));
            Log.d("ModernWatchface", "sgv level : " + getSgvLevel());

            setSgvString(dataMap.getString("sgvString"));
            setDelta(dataMap.getString("delta"));
            setDatetime(dataMap.getDouble("timestamp"));
            addToWatchSet(dataMap);

            prepareLayout();
            prepareDrawTime();

            invalidate();
            wakeLock.release();
        }
    }

    public void addToWatchSet(DataMap dataMap) {
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            for (DataMap entry : entries) {
                double sgv = entry.getDouble("sgvDouble");
                double high = entry.getDouble("high");
                double low = entry.getDouble("low");
                double timestamp = entry.getDouble("timestamp");

                final int size = bgDataList.size();
                if (size > 0) {
                    if (bgDataList.get(bgDataList.size() - 1).timestamp == timestamp)
                        continue; // Ignore duplicates.
                }
                bgDataList.add(new BgWatchData(sgv, high, low, timestamp));
            }
        } else {
            double sgv = dataMap.getDouble("sgvDouble");
            double high = dataMap.getDouble("high");
            double low = dataMap.getDouble("low");
            double timestamp = dataMap.getDouble("timestamp");

            final int size = bgDataList.size();
            if (size > 0) {
                if (bgDataList.get(bgDataList.size() - 1).timestamp == timestamp)
                    return; // Ignore duplicates.
            }
            bgDataList.add(new BgWatchData(sgv, high, low, timestamp));
        }

        for (int i = 0; i < bgDataList.size(); i++) {
            if (bgDataList.get(i).timestamp < (new Date().getTime() - (1000 * 60 * 5 * holdInMemory()))) {
                bgDataList.remove(i);
            }
        }
    }

    public static int darken(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = darkenColor(red, fraction);
        green = darkenColor(green, fraction);
        blue = darkenColor(blue, fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private static int darkenColor(int color, double fraction) {
        return (int)Math.max(color - (color * fraction), 0);
    }


    public void addArch(Canvas canvas, float offset, int color, float size) {
        Paint paint = new Paint();
        paint.setColor(color);
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2),(displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, 270, size, true, paint);
    }

    public void addArch(Canvas canvas, float start, float offset, int color, float size) {
        Paint paint = new Paint();
        paint.setColor(color);
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2),(displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, start+270, size, true, paint);
    }

    public void addIndicator(Canvas canvas, float bg, int color) {
        float convertedBg;
        if(bg > 100){
            convertedBg = (((bg - 100f) / 300f) * 225f) + 135;
        } else {
            convertedBg = ((bg / 100) * 135);
        }
        convertedBg += 270;
        Paint paint = new Paint();
        paint.setColor(color);
        float offset = 9;
        RectF rectTemp = new RectF(PADDING + offset - CIRCLE_WIDTH / 2, PADDING + offset - CIRCLE_WIDTH / 2, (displaySize.x - PADDING - offset + CIRCLE_WIDTH / 2),(displaySize.y - PADDING - offset + CIRCLE_WIDTH / 2));
        canvas.drawArc(rectTemp, convertedBg, 2, true, paint);
    }

}
