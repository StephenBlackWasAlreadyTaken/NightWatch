package com.dexdrip.stephenblack.nightwatch.alerts;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import com.dexdrip.stephenblack.nightwatch.activities.BaseActivity;
import com.dexdrip.stephenblack.nightwatch.R;
import com.dexdrip.stephenblack.nightwatch.model.UserError;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephenblack on 8/3/15.
 */
public class ErrorsActivity extends BaseActivity {
    public static final String MENU_NAME = "Errors";
    private CheckBox highCheckboxView;
    private CheckBox mediumCheckboxView;
    private CheckBox lowCheckboxView;
    private ListView errorList;
    private List<UserError> errors;
    private ErrorListAdapter adapter;

    @Override
    public String getMenuName() {
        return MENU_NAME;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_errors;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        highCheckboxView = (CheckBox) findViewById(R.id.highSeverityCheckbox);
        mediumCheckboxView = (CheckBox) findViewById(R.id.midSeverityCheckbox);
        lowCheckboxView = (CheckBox) findViewById(R.id.lowSeverityCheckBox);

        highCheckboxView.setOnClickListener(checkboxListener);
        mediumCheckboxView.setOnClickListener(checkboxListener);
        lowCheckboxView.setOnClickListener(checkboxListener);

        updateErrors();
        errorList = (ListView) findViewById(R.id.errorList);
        adapter = new ErrorListAdapter(getApplicationContext(), errors);
        errorList.setAdapter(adapter);
    }

    private View.OnClickListener checkboxListener = new View.OnClickListener() {
        public void onClick(View v) {
            updateErrors();
            adapter.notifyDataSetChanged();
        }
    };

    public void updateErrors() {
        List<Integer> severitiesList = new ArrayList<>();
        if (highCheckboxView.isChecked()) severitiesList.add(3);
        if (mediumCheckboxView.isChecked()) severitiesList.add(2);
        if (lowCheckboxView.isChecked()) severitiesList.add(1);
        if(errors == null) {
            errors = UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()]));
        } else {
            errors.clear();
            errors.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
        }
    }
}
