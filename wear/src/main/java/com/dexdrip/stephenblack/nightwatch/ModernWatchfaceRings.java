package com.dexdrip.stephenblack.nightwatch;

import android.graphics.Canvas;
import android.graphics.Color;

/**
 * Created by stephenblack on 6/13/15.
 */
public class ModernWatchfaceRings extends CircleWatchface{

    /*@Override
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

        //TODO: adjust according to screen size!
        return 6;
    }

    @Override
    public String getMinutes() {
        //TODO: disable in baseclass according to settings
        return "";
    }

    @Override
    public String getDelta() {
        //TODO: disable in baseclass according to settings
        return "";
    }*/

    @Override
    public void drawOtherStuff(Canvas canvas) {
        //Perfect low and High indicators
        if(bgDataList.size() > 0) {
            addIndicator(canvas, 100, Color.LTGRAY);
            addIndicator(canvas, (float) bgDataList.get(0).low, getLowColor());
            addIndicator(canvas, (float) bgDataList.get(0).high, getHighColor());
        }
        for(int i=bgDataList.size(); i > 0; i--) {
            addReading(canvas, bgDataList.get(i - 1), i);
        }
    }

}
