package com.andrewvora.apps.planforatlanta.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.andrewvora.apps.planforatlanta.R;
import com.andrewvora.apps.planforatlanta.fragments.ScheduleFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by root on 6/2/16.
 * @author faytxzen
 */
public class ScheduleActivity extends BaseActivity {
    @BindView(R.id.toolbar) Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        ButterKnife.bind(this);

        // configure the ActionBar
        setSupportActionBar(mToolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_schedule_activity);
        }

        // inflate the ScheduleFragment
        Fragment scheduleFragment =
                getFragmentManager().findFragmentByTag(ScheduleFragment.TAG);

        if(scheduleFragment == null) {
            scheduleFragment = new ScheduleFragment();
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, scheduleFragment, ScheduleFragment.TAG)
                .commit();
    }
}
