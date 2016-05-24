package com.production.rclark.simpleatvbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by rclark on 4/28/16.
 */
public class SettingsActivity extends Activity {

    public static boolean mUpdateSomething = false;
    public static final String PREF_RESULT_KEY = "pref_result";
    public static final int PREF_DO_NOTHING = 0;
    public static final int PREF_DO_SOMETHING = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

    @Override
    public void finishAfterTransition() {
        //Save off what we should do...
        Intent data = new Intent();
        int retflags = PREF_DO_NOTHING;

        if (mUpdateSomething) {
            retflags |= PREF_DO_SOMETHING;
        }
        //shove the flags into return intent...
        data.putExtra(PREF_RESULT_KEY, retflags);
        setResult(RESULT_OK, data);

        super.finishAfterTransition();
    }
}
