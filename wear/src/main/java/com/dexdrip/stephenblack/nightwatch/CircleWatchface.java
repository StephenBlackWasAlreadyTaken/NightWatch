package com.dexdrip.stephenblack.nightwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
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
import java.util.HashSet;
import java.util.Set;


public class CircleWatchface extends WatchFace implements SharedPreferences.OnSharedPreferenceChangeListener {
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
    private Paint removePaint = new Paint();
    private RectF rect, rectDelete;
    private boolean overlapping;

    private int animationAngle = 0;
    private boolean isAnimated = false;


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

    protected SharedPreferences sharedPrefs;


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

        sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        //register Message Receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Intent.ACTION_SEND));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myLayout = inflater.inflate(R.layout.modern_layout, null);
        prepareLayout();
        prepareDrawTime();
        //ListenerService.requestData(this);

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
        if (sharedPrefs != null) {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        Log.d("CircleWatchface", "start onDraw");
        canvas.drawColor(getBackgroundColor());
        drawTime(canvas);
        drawOtherStuff(canvas);
        myLayout.draw(canvas);

    }

    private synchronized void prepareLayout() {

        Log.d("CircleWatchface", "start startPrepareLayout");

        // prepare fields


        if (sharedPrefs.getBoolean("showBG", true)) {
            ((TextView) myLayout.findViewById(R.id.sgvString)).setVisibility(View.VISIBLE);
            ((TextView) myLayout.findViewById(R.id.sgvString)).setText(getSgvString());
            ((TextView) myLayout.findViewById(R.id.sgvString)).setTextColor(getTextColor());

        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            ((TextView) myLayout.findViewById(R.id.sgvString)).setVisibility(View.GONE);
        }
        ;


        if (sharedPrefs.getBoolean("showAgo", true)) {
            ((TextView) myLayout.findViewById(R.id.agoString)).setVisibility(View.VISIBLE);

            if(sharedPrefs.getBoolean("showBigNumbers", false)){
                ((TextView) myLayout.findViewById(R.id.agoString)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            } else {
                ((TextView) myLayout.findViewById(R.id.agoString)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            }
            ((TextView) myLayout.findViewById(R.id.agoString)).setText(getMinutes());
            ((TextView) myLayout.findViewById(R.id.agoString)).setTextColor(getTextColor());



        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            ((TextView) myLayout.findViewById(R.id.agoString)).setVisibility(View.GONE);
        }
        if (sharedPrefs.getBoolean("showDelta", true)) {
            ((TextView) myLayout.findViewById(R.id.deltaString)).setVisibility(View.VISIBLE);
            ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta());
            ((TextView) myLayout.findViewById(R.id.deltaString)).setTextColor(getTextColor());
            if(sharedPrefs.getBoolean("showBigNumbers", false)) {
                ((TextView) myLayout.findViewById(R.id.deltaString)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
                if(delta.endsWith(" mg/dl")) {
                    ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta().substring(0, delta.length()-6));
                } else if (delta.endsWith(" mmol")) {
                    ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta().substring(0, delta.length()-5));
                }
            } else {
                ((TextView) myLayout.findViewById(R.id.deltaString)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta());
            }
        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            ((TextView) myLayout.findViewById(R.id.deltaString)).setVisibility(View.GONE);
        }
        //TODO: add more view elements?


        myLayout.measure(specW, specH);
        myLayout.layout(0, 0, myLayout.getMeasuredWidth(),
                myLayout.getMeasuredHeight());
    }


    public String getMinutes() {
        String minutes = "--\'";
        if (getDatetime() != 0) {
            minutes = ((int) Math.floor((System.currentTimeMillis() - getDatetime()) / 60000)) + "\'";
        }
        return minutes;
    }

    private void drawTime(Canvas canvas) {

        //draw circle
        circlePaint.setColor(color);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        canvas.drawArc(rect, 0, 360, false, circlePaint);
        //"remove" hands from circle
        removePaint.setStrokeWidth(CIRCLE_WIDTH * 3);

        canvas.drawArc(rectDelete, angleBig, (float) BIG_HAND_WIDTH, false, removePaint);
        canvas.drawArc(rectDelete, angleSMALL, (float) SMALL_HAND_WIDTH, false, removePaint);


        if (overlapping) {
            //add small hand with extra
            circlePaint.setStrokeWidth(CIRCLE_WIDTH * 2);
            circlePaint.setColor(color);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);

            //remove inner part of hands
            removePaint.setStrokeWidth(CIRCLE_WIDTH);
            canvas.drawArc(rect, angleBig, (float) BIG_HAND_WIDTH, false, removePaint);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, removePaint);
        }

    }

    private synchronized void prepareDrawTime() {
        Log.d("CircleWatchface", "start startPrepareDrawTime");

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


        if (isAnimated()) {
            //Animation matrix:
            int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE
                    , Color.CYAN};
            Shader shader = new LinearGradient(0, 0, 0, 20, rainbow,
                    null, Shader.TileMode.MIRROR);
            Matrix matrix = new Matrix();
            matrix.setRotate(animationAngle);
            shader.setLocalMatrix(matrix);
            circlePaint.setShader(shader);
        } else {
            circlePaint.setShader(null);
        }


        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(color);

        removePaint.setStyle(Paint.Style.STROKE);
        removePaint.setStrokeWidth(CIRCLE_WIDTH);
        removePaint.setAntiAlias(true);
        removePaint.setColor(getBackgroundColor());

        ;

        rect = new RectF(PADDING, PADDING, (float) (displaySize.x - PADDING), (float) (displaySize.y - PADDING));
        rectDelete = new RectF(PADDING - CIRCLE_WIDTH / 2, PADDING - CIRCLE_WIDTH / 2, (float) (displaySize.x - PADDING + CIRCLE_WIDTH / 2), (float) (displaySize.y - PADDING + CIRCLE_WIDTH / 2));
        overlapping = ALWAYS_HIGHLIGT_SMALL || areOverlapping(angleSMALL, angleSMALL + SMALL_HAND_WIDTH + NEAR, angleBig, angleBig + BIG_HAND_WIDTH + NEAR);
    }


    synchronized void animationStep() {
        animationAngle = (animationAngle + 1) % 360;
        prepareDrawTime();
        invalidate();
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


    // defining color for dark and bright
    public int getLowColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 255, 120, 120);

        } else {
            return Color.argb(255, 255, 80, 80);

        }
    }

    public int getInRangeColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 120, 255, 120);
        } else {
            return Color.argb(255, 0, 240, 0);

        }
    }

    public int getHighColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.argb(255, 255, 255, 120);
        } else {
            return Color.argb(255, 255, 200, 0);
        }

    }

    public int getBackgroundColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.BLACK;
        } else {
            return Color.WHITE;

        }
    }

    public int getTextColor() {
        if (sharedPrefs.getBoolean("dark", false)) {
            return Color.WHITE;
        } else {
            return Color.BLACK;

        }
    }

    public void drawOtherStuff(Canvas canvas) {
        Log.d("CircleWatchface", "start onDrawOtherStuff. bgDataList.size(): " + bgDataList.size());

        if(isAnimated()) return; // too many repaints when animated
        if (sharedPrefs.getBoolean("showRingHistory", true)) {
            //Perfect low and High indicators
            if (bgDataList.size() > 1) { //start with 2 values to avoid startup overload
                addIndicator(canvas, 100, Color.LTGRAY);
                addIndicator(canvas, (float) bgDataList.get(bgDataList.size() - 1).low, getLowColor());
                addIndicator(canvas, (float) bgDataList.get(bgDataList.size() - 1).high, getHighColor());


            if(sharedPrefs.getBoolean("softRingHistory", true)){
                for(int i=bgDataList.size(); i > 0; i--) {
                    addReadingSoft(canvas, bgDataList.get(i - 1), i);
                }
            } else {
                for (int i = bgDataList.size(); i > 0; i--) {
                     addReading(canvas, bgDataList.get(i - 1), i);
                }
            }
            }
        }
    }
    public int holdInMemory() { return 6;}

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


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        prepareDrawTime();
        prepareLayout();
        invalidate();
    }

    private synchronized boolean isAnimated() {
        return isAnimated;
    }

    private synchronized void setIsAnimated(boolean isAnimated) {
        this.isAnimated = isAnimated;
    }

    void startAnimation() {
        Log.d("CircleWatchface", "start startAnimation");

        Thread animator = new Thread() {


            public void run() {
                //TODO:Wakelock?
                setIsAnimated(true);
                for (int i = 0; i <= 10 * 1000 / 30; i++) {
                    animationStep();
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setIsAnimated(false);
                prepareDrawTime();
                invalidate();
            }
        };

        animator.start();
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
            Log.d("CircleWatchface", "sgv level : " + getSgvLevel());
            setSgvString(dataMap.getString("sgvString"));
            Log.d("CircleWatchface", "sgv string : " + getSgvString());
            setDelta(dataMap.getString("delta"));
            setDatetime(dataMap.getDouble("timestamp"));
            addToWatchSet(dataMap);

            //start animation?
            //if (sharedPrefs.getBoolean("animation", false) && (getSgvString().equals("100") || getSgvString().equals("5.5") || getSgvString().equals("5,5"))) {

                startAnimation();

            //}

            prepareLayout();
            prepareDrawTime();

            invalidate();
            wakeLock.release();
        }
    }

    public synchronized void addToWatchSet(DataMap dataMap) {
        Log.d("CircleWatchface", "start addToWatchSet");
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            Log.d("addToWatchSet", "entries.size(): " + entries.size());

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

        Log.d("addToWatchSet", "start removing bgDataList.size(): " + bgDataList.size());
        HashSet removeSet = new HashSet();
        for (int i = 0; i < bgDataList.size(); i++) {

            if (bgDataList.get(i).timestamp < (new Date().getTime() - (1000 * 60 * 5 * holdInMemory()))) {
                removeSet.add(bgDataList.get(i));
                //bgDataList.remove(i);
                //Log.d("addToWatchSet", "removed one bgDataList.size(): " + bgDataList.size());

            } else {
                Log.d("Not Removed", " " + bgDataList.get(i).timestamp );
            }
        }
        bgDataList.removeAll(removeSet);
        Log.d("addToWatchSet", "after bgDataList.size(): " + bgDataList.size());
    }

    public int darken(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = darkenColor(red, fraction);
        green = darkenColor(green, fraction);
        blue = darkenColor(blue, fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private int darkenColor(int color, double fraction) {

        //if (sharedPrefs.getBoolean("dark", false)) {
            return (int) Math.max(color - (color * fraction), 0);
        //}

       // return (int)Math.min(color + (color * fraction), 255);
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


    public void addReadingSoft(Canvas canvas, BgWatchData entry, int i) {

        Log.d("CircleWatchface", "addReadingSoft");
        double size;

        int color = Color.LTGRAY;

        if (sharedPrefs.getBoolean("dark", false)) {
            color = Color.DKGRAY;
        }

        float offsetMultiplier = (((displaySize.x / 2f) - PADDING) / 12f);
        float offset = (float) Math.max(1, Math.ceil((new Date().getTime() - entry.timestamp) / (1000 * 60 * 5)));
        // TODO what if 2 readings are not exactly 5 minutes apart? How about:
        //float offset = (float) Math.max(1, Math.round((new Date().getTime() - entry.timestamp) / (1000f * 60 * 5)));

        if(entry.sgv > 100){
            size = (((entry.sgv - 100f) / 300f) * 225f) + 135;
        } else {
            size = ((entry.sgv / 100) * 135);
        }
        addArch(canvas, offset * offsetMultiplier + 10, color, (float) size);
        addArch(canvas, (float) size, offset * offsetMultiplier + 10, getBackgroundColor(), (float) (360 - size));
        addArch(canvas, (offset + .8f) * offsetMultiplier + 10, getBackgroundColor(), 360);
    }

    public void addReading(Canvas canvas, BgWatchData entry, int i) {
        Log.d("CircleWatchface", "addReading");

        double size;
        int color = Color.LTGRAY;
        int indicatorColor = Color.DKGRAY;

        if (sharedPrefs.getBoolean("dark", false)) {
            color = Color.DKGRAY;
            indicatorColor = Color.LTGRAY;
        }


        int barColor = Color.GRAY;
        if(entry.sgv >= entry.high) {
            indicatorColor = getHighColor();
            barColor = darken(getHighColor(), .5);
        } else if (entry.sgv <= entry.low) {
            indicatorColor = getLowColor();
            barColor =  darken(getLowColor(), .5);
        }
        float offsetMultiplier = (((displaySize.x / 2f) - PADDING) / 12f);
        float offset = (float) Math.max(1,Math.ceil((new Date().getTime() - entry.timestamp)/(1000 * 60 * 5)));
        if(entry.sgv > 100){
            size = (((entry.sgv - 100f) / 300f) * 225f) + 135;
        } else {
            size = ((entry.sgv / 100) * 135);
        }
        addArch(canvas, offset * offsetMultiplier + 11, barColor, (float) size - 2); // Dark Color Bar
        addArch(canvas, (float) size - 2, offset * offsetMultiplier + 11, indicatorColor, 2f); // Indicator at end of bar
        addArch(canvas, (float) size, offset * offsetMultiplier + 11, color, (float) (360f - size)); // Dark fill
        addArch(canvas, (offset + .8f) * offsetMultiplier + 11, getBackgroundColor(), 360);
    }


}
