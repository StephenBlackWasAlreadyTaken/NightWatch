package com.dexdrip.stephenblack.nightwatch;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;
import com.ustwo.clockwise.WatchFace;
import com.ustwo.clockwise.WatchFaceTime;
import com.ustwo.clockwise.WatchShape;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import lecho.lib.hellocharts.view.LineChartView;


public abstract class ModernWatchface extends WatchFace {
    public final float PADDING = 20f;
    public final float CIRCLE_WIDTH = 6f;
    public final int BIG_HAND_WIDTH = 16;
    public final int SMALL_HAND_WIDTH = 8;
    public final int NEAR = 2; //how near do the hands have to be to activate overlapping mode
    public final boolean ALWAYS_HIGHLIGT_SMALL = false;

    private Point displaySize = new Point();
    private MessageReceiver messageReceiver = new MessageReceiver();

    private int svgLevel = 0;
    private int batteryLevel = 0;
    private int bgLevel = 0;
    private double datetime = 0;
    private String direction = "";

    private View layoutView;
    private int specW;
    private int specH;
    private View myLayout;


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

        //register Message Receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter(Intent.ACTION_SEND));


        //TODO: Try to get a layout to work:
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myLayout = inflater.inflate(R.layout.modern_layout, null);
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

        myLayout.measure(specW, specH);
        myLayout.layout(0, 0, myLayout.getMeasuredWidth(),
                myLayout.getMeasuredHeight());
        canvas.drawColor(Color.BLACK);
        myLayout.draw(canvas);
        drawTime(canvas);
        myLayout.draw(canvas);

    }

    private void drawTime(Canvas canvas) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 12;
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        float angleBig = (((hour + minute / 60f) / 12f * 360) - 90 - BIG_HAND_WIDTH / 2f + 360) % 360;
        float angleSMALL = ((minute / 60f * 360) - 90 - SMALL_HAND_WIDTH / 2f + 360) % 360;


        int color = 0;
        switch (getSvgLevel()) {
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
        ;


        Log.d("ModernWatchface", "angleBig : " + angleBig);
        Paint circlePaint = new Paint();
        circlePaint.setColor(color);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(CIRCLE_WIDTH);
        circlePaint.setAntiAlias(true);

        //delete Canvas
        canvas.drawColor(getBackgroundColor());

        //draw circle
        RectF rect = new RectF(PADDING, PADDING, (float) (displaySize.x - PADDING), (float) (displaySize.y - PADDING));
        canvas.drawArc(rect, 0, 360, false, circlePaint);

        //"remove" hands from circle
        float plus = CIRCLE_WIDTH / 2f; // delete more to fight antializing artifacts
        circlePaint.setColor(getBackgroundColor());
        circlePaint.setStrokeWidth(CIRCLE_WIDTH + 2 * plus);
        canvas.drawArc(rect, angleBig, (float) BIG_HAND_WIDTH, false, circlePaint);
        canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);



        if (ALWAYS_HIGHLIGT_SMALL || areOverlapping(angleSMALL,  angleSMALL + SMALL_HAND_WIDTH + NEAR ,angleBig,angleBig + BIG_HAND_WIDTH + NEAR) ){
            //add small hand with extra
            circlePaint.setStrokeWidth(CIRCLE_WIDTH + 2 * plus);
            circlePaint.setColor(color);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);

            //remove inner part of hands
            circlePaint.setColor(getBackgroundColor());
            circlePaint.setStrokeWidth(CIRCLE_WIDTH);
            canvas.drawArc(rect, angleBig, (float) BIG_HAND_WIDTH, false, circlePaint);
            canvas.drawArc(rect, angleSMALL, (float) SMALL_HAND_WIDTH, false, circlePaint);
        }
    }

    private boolean areOverlapping(float aBegin, float aEnd, float bBegin, float bEnd){
        return
                aBegin<=bBegin && aEnd>=bBegin ||
                        aBegin<=bBegin && (bEnd>360) && bEnd%360 > aBegin ||
                        bBegin<=aBegin && bEnd>=aBegin ||
                        bBegin<=aBegin && aEnd>360 && aEnd%360 > bBegin;
    }

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        invalidate();  //redraw the time
    }

    /*Some methods to implement by child classes*/
    public abstract int getLowColor();

    public abstract int getInRangeColor();

    public abstract int getHighColor();

    public abstract int getBackgroundColor();


    //getters & setters

    private synchronized int getSvgLevel() {
        return svgLevel;
    }

    private synchronized void setSvgLevel(int svgLevel) {
        this.svgLevel = svgLevel;
    }

    private synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    private synchronized void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    private synchronized int getBgLevel() {
        return bgLevel;
    }

    private synchronized void setBgLevel(int bgLevel) {
        this.bgLevel = bgLevel;
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

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire();
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));
            setSvgLevel((int) dataMap.getLong("sgvLevel"));
            Log.d("ModernWatchface", "svg level : " + getSvgLevel());
            invalidate();
            wakeLock.release();
        }
    }


}
