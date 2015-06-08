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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
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
    public final int NEAR = 4; //how near do the hands have to be to activate overlapping mode

    private Point displaySize = new Point();

    @Override
    public void onCreate() {
        super.onCreate();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        display.getSize(displaySize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawTime(canvas);
    }


    private void drawTime(Canvas canvas) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 12;
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        float angleBig = (((hour + minute / 60f) / 12f * 360) - 90 - BIG_HAND_WIDTH / 2f + 360) % 360;
        float angleSMALL = ((minute / 60f * 360) - 90 - SMALL_HAND_WIDTH / 2f + 360) % 360;

        int color = getLowColor();


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

        if ((angleSMALL + SMALL_HAND_WIDTH + NEAR) % 360 >= angleBig
                && angleSMALL <= (angleBig + BIG_HAND_WIDTH + NEAR) % 360) {

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

    @Override
    protected void onTimeChanged(WatchFaceTime oldTime, WatchFaceTime newTime) {
        invalidate();  //redraw the time
    }

    /*Some methods to implement by child classes*/
    public abstract int getLowColor();

    public abstract int getBackgroundColor();

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE, null);
            wl.acquire();
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));




            wl.release();
/*

            if (layoutSet) {
                wakeLock.acquire(50);
                sgvLevel = dataMap.getLong("sgvLevel");
                batteryLevel = dataMap.getInt("batteryLevel");
                datetime = dataMap.getDouble("timestamp");

                mSgv.setText(dataMap.getString("sgvString"));

                if(ageLevel()<=0) {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    mSgv.setPaintFlags(mSgv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(ModernWatchface.this);
                mTime.setText(timeFormat.format(System.currentTimeMillis()));
                mTimestamp.setText(readingAge());

                mDirection.setText(dataMap.getString("slopeArrow"));
                mUploaderBattery.setText("Uploader: " + dataMap.getString("battery") + "%");
                mDelta.setText(dataMap.getString("delta"));

                mTimestamp.setText(readingAge());
                if (chart != null) {
                    addToWatchSet(dataMap);
                    setupCharts();
                }
                mRelativeLayout.measure(specW, specH);
                mRelativeLayout.layout(0, 0, mRelativeLayout.getMeasuredWidth(),
                        mRelativeLayout.getMeasuredHeight());
                invalidate();
            } else {
                Log.d("ERROR: ", "DATA IS NOT YET SET");
            }*/
        }
    }


}
