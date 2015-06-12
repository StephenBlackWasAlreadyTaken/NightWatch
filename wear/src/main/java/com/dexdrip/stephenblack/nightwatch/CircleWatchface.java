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
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;

import java.util.Calendar;


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


    private Point displaySize = new Point();
    private MessageReceiver messageReceiver = new MessageReceiver();

    private int sgvLevel = 0;
    private String sgvString = "999";
    private int batteryLevel = 0;
    private double datetime = 0;
    private String direction = "";
    private String delta = "";

    private View layoutView;
    private int specW;
    private int specH;
    private View myLayout;

    protected SharedPreferences sharedPrefs;


    @Override
    public void onCreate() {
        super.onCreate();

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
        ListenerService.requestData(this);
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
    protected void onDraw(Canvas canvas) {
        drawTime(canvas);
        myLayout.draw(canvas);

    }

    private void prepareLayout() {
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

        String minutes = "--\'";
        if (getDatetime() != 0) {
            minutes = ((int) Math.floor((System.currentTimeMillis() - getDatetime()) / 60000)) + "\'";
            ;
        }
        if (sharedPrefs.getBoolean("showAgo", true)) {
            ((TextView) myLayout.findViewById(R.id.agoString)).setVisibility(View.VISIBLE);
            ((TextView) myLayout.findViewById(R.id.agoString)).setText(minutes);
            ((TextView) myLayout.findViewById(R.id.agoString)).setTextColor(getTextColor());
        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            ((TextView) myLayout.findViewById(R.id.agoString)).setVisibility(View.GONE);
        }
        if (sharedPrefs.getBoolean("showDelta", true)) {
            ((TextView) myLayout.findViewById(R.id.deltaString)).setVisibility(View.VISIBLE);
            ((TextView) myLayout.findViewById(R.id.deltaString)).setText(getDelta());
            ((TextView) myLayout.findViewById(R.id.deltaString)).setTextColor(getTextColor());
        } else {
            //Also possible: View.INVISIBLE instead of View.GONE (no layout change)
            ((TextView) myLayout.findViewById(R.id.deltaString)).setVisibility(View.GONE);
        }
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
            /*Preparing the layout just on every minute tick:
            *  - hopefully better battery life
            *  - drawback: might update the minutes since last reading up to one minute late*/
            prepareLayout();
            prepareDrawTime();
            invalidate();  //redraw the time

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

    private String getDelta() {
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
            wakeLock.acquire(); //do we need this?

            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            setSgvLevel((int) dataMap.getLong("sgvLevel"));
            Log.d("CircleWatchface", "sgv level : " + getSgvLevel());

            setSgvString(dataMap.getString("sgvString"));
            setDelta(dataMap.getString("delta"));
            setDatetime(dataMap.getDouble("timestamp"));

            //start animation?
            if (sharedPrefs.getBoolean("animation", false) && (getSgvString().equals("100") || getSgvString().equals("5.5") || getSgvString().equals("5,5"))) {

                startAnimation();

            }

            wakeLock.release();
            prepareLayout();
            prepareDrawTime();
            invalidate();
        }
    }


}
