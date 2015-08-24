package com.dexdrip.stephenblack.nightwatch.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.dexdrip.stephenblack.nightwatch.Constants;
import com.dexdrip.stephenblack.nightwatch.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by adrian on 30/06/15.
 */
public class FirstPageFragment extends Fragment {

    private View myView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("DrawStats", "FirstPageFragment onCreateView");

        myView = inflater.inflate(
                R.layout.stats_general, container, false);

        (new CalculationThread(myView, getActivity().getApplicationContext())).start();

        return getView();
    }

    @Nullable
    @Override
    public View getView() {
        return myView;
    }

    private class CalculationThread extends Thread {

        private final View localView;
        private final Context context;

        public CalculationThread(View myView, Context context) {
            this.localView = myView;
            this.context = context;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DrawStats", "FirstPageFragment CalculationThread started");

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

            if (context == null) {
                Log.d("DrawStats", "FirstPageFragment context == null, do not calculate if fragment is not attached");
                return;
            }

            List<BgReadingStats> bgList = DBSearchUtil.getReadings();


            //Ranges
            long aboveRange = 0;
            long belowRange = 0;
            long inRange = 0;

            double high = Double.parseDouble(settings.getString("highValue", "170"));
            double low = Double.parseDouble(settings.getString("lowValue", "70"));


            for (BgReadingStats reading : bgList){
                if (reading.calculated_value>high){
                    aboveRange++;
                } else if (reading.calculated_value < low){
                    belowRange++;
                } else {
                    inRange++;
                }
            }


            long total = aboveRange + belowRange + inRange;

            if (total == 0) {
                total = Long.MAX_VALUE;
            }

            TextView rangespercent = (TextView) localView.findViewById(R.id.textView_ranges_percent);
            TextView rangesabsolute = (TextView) localView.findViewById(R.id.textView_ranges_absolute);

            updateText(localView, rangespercent, inRange * 100 / total + "%/" + aboveRange * 100 / total + "%/" + belowRange * 100 / total + "%");
            updateText(localView, rangesabsolute, inRange + "/" + aboveRange + "/" + belowRange);

            Collections.sort(bgList, new Comparator<BgReadingStats>() {
                @Override
                public int compare(BgReadingStats lhs, BgReadingStats rhs) {

                   return Double.compare(lhs.calculated_value, rhs.calculated_value);
                }
            });
            if (bgList.size() > 0) {
                double median = bgList.get(bgList.size() / 2).calculated_value;
                TextView medianView = (TextView) localView.findViewById(R.id.textView_median);

                if (mgdl) {
                    updateText(localView, medianView, Math.round(median * 10) / 10d + " mg/dl");

                } else {
                    updateText(localView, medianView, Math.round(median * Constants.MGDL_TO_MMOLL * 100) / 100d + " mmol/l");

                }

                double mean = 0;
                double len = bgList.size();
                double stdev = 0;
                for (BgReadingStats bgr : bgList) {
                    mean += bgr.calculated_value / len;
                }

                TextView meanView = (TextView) localView.findViewById(R.id.textView_mean);
                //update mean
                if (mgdl) {
                    updateText(localView, meanView, (Math.round(mean * 10) / 10d) + " mg/dl");
                } else {
                    updateText(localView, meanView, (Math.round(mean * Constants.MGDL_TO_MMOLL * 100) / 100d) + " mmol/l");

                }
                //update A1c
                TextView a1cView = (TextView) localView.findViewById(R.id.textView_a1c);
                int a1c_ifcc = (int) Math.round(((mean + 46.7) / 28.7 - 2.15) * 10.929);
                double a1c_dcct = Math.round(10 * (mean + 46.7) / 28.7) / 10d;
                updateText(localView, a1cView, a1c_ifcc + " mmol/mol\n" + a1c_dcct + "%");


                for (BgReadingStats bgr : bgList) {
                    stdev += (bgr.calculated_value - mean) * (bgr.calculated_value - mean) / len;
                }
                stdev = Math.sqrt(stdev);
                TextView stdevView = (TextView) localView.findViewById(R.id.textView_stdev);
                if (mgdl) {
                    updateText(localView, stdevView, (Math.round(stdev * 10) / 10d) + " mg/dl");
                } else {
                    updateText(localView, stdevView, (Math.round(stdev * Constants.MGDL_TO_MMOLL * 100) / 100d) + " mmol/l");
                }
            }
        }

        private void updateText(final View localView, final TextView tv, final String s) {
            Log.d("DrawStats", "updateText: " + s);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    //Adrian: after screen rotation it might take some time to attach the view to the window
                    //Wait up to 3 seconds for this to happen.
                    int i = 0;
                    while (localView.getHandler() == null && i < 10) {
                        i++;
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                        }

                    }

                    if (localView.getHandler() == null) {
                        Log.d("DrawStats", "no Handler found - stopping to update view");
                        return;
                    }


                    boolean success = localView.post(new Runnable() {
                        @Override
                        public void run() {
                            tv.setText(s);
                            Log.d("DrawStats", "setText actually called: " + s);

                        }
                    });
                    Log.d("DrawStats", "updateText: " + s + " success: " + success);
                }
            });
            thread.start();

        }

    }

}
