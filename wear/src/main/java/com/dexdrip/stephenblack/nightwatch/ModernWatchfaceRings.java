package com.dexdrip.stephenblack.nightwatch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Date;

/**
 * Created by stephenblack on 6/13/15.
 */
public class ModernWatchfaceRings extends ModernWatchface{

    @Override
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

    @Override
    public void drawOtherStuff(Canvas canvas) {
        //Perfect low and High indicators
        if(bgDataList.size() > 0) {
            addIndicator(canvas, 100, Color.LTGRAY);
            addIndicator(canvas, (float) bgDataList.get(0).low, getLowColor());
            addIndicator(canvas, (float) bgDataList.get(0).high, getHighColor());
        }
        for(int i=bgDataList.size(); i > 0; i--) {
            addReading2(canvas, bgDataList.get(i - 1), i);
        }
    }

    public void addReading2(Canvas canvas, BgWatchData entry, int i) {
        double size;
        int color = Color.DKGRAY;
        int indicatorColor = Color.LTGRAY;
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
