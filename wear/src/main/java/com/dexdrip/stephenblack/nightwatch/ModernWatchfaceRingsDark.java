package com.dexdrip.stephenblack.nightwatch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Date;

/**
 * Created by stephenblack on 6/13/15.
 */
public class ModernWatchfaceRingsDark extends CircleWatchface{

    /* @Override
    public int getLowColor() {
        return Color.argb(255, 255, 120, 120);
    }

    @Override
    public int getInRangeColor() {
        return Color.argb(255,120,255,120);

    }

    @Override
    public int getHighColor() {
        return Color.argb(255,255,255,120);

    }

    @Override
    public int getBackgroundColor() {
        return Color.BLACK;
    }

    @Override
    public int getTextColor() {
        return Color.WHITE;
    }

    @Override
    public int holdInMemory() {
        return 6;
    }

    @Override
    public String getMinutes() {
        return "";
    }

    @Override
    public String getDelta() {
        return "";
    }

    */
    @Override
    public void drawOtherStuff(Canvas canvas) {
        //Perfect low and High indicators
        if(bgDataList.size() > 0) {
            addIndicator(canvas, 100, Color.GRAY);
            addIndicator(canvas, (float) bgDataList.get(0).low, Color.GRAY);
            addIndicator(canvas, (float) bgDataList.get(0).high, Color.GRAY);
        }
        for(int i=bgDataList.size(); i > 0; i--) {
            addReadingSoft(canvas, bgDataList.get(i - 1), i);
        }
    }


}
