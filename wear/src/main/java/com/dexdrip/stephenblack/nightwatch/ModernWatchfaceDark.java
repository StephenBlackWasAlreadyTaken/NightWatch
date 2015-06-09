package com.dexdrip.stephenblack.nightwatch;

import android.graphics.Color;

/**
 * Created by adrian on 08/06/15.
 */
public class ModernWatchfaceDark extends ModernWatchface{

    @Override
    public int getLowColor() {
        return Color.argb(255,255,120,120);
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


}
