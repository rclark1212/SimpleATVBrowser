/*
 * Copyright (C) 2016 Richard Clark
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.rclark.simpleatvbrowser;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by rclark on 3/30/2016.
 * Implements the searchbar for the browser.
 */
public class SearchbarFragment extends Fragment {

    //Keep a copy of the views
    public EditText mEdit;
    public View mvBack;
    public View mvForward;
    public View mvVoice;
    public View mvRefresh;
    public View mvSettings;
    public View mvList;
    public View mvFavorite;
    public View mvHome;

    //note that button processing happens in mainactivity.

    OnMainActivityCallbackListener mCallback;
    //Put in an interface for container activity to implement so that fragment can deliver messages
    public interface OnMainActivityCallbackListener {
        //called by SearchbarFragment when a url is selected
        public void onMainActivityCallback(int code, String url);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View retView = inflater.inflate(R.layout.searchbar_fragment, container, false);

        //Get views
        mEdit = (EditText) retView.findViewById(R.id.search);
        mvBack = retView.findViewById(R.id.back);
        mvVoice = retView.findViewById(R.id.voice);
        mvRefresh = retView.findViewById(R.id.refresh);
        mvSettings = retView.findViewById(R.id.settings);
        mvList = retView.findViewById(R.id.dropdown);
        mvFavorite = retView.findViewById(R.id.favorite);
        mvForward = retView.findViewById(R.id.forward);
        mvHome = retView.findViewById(R.id.home);

        //process edittext
        mEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))) {
                    //We got a done or enter. Go ahead and clean up the text box and load page...
                    cleanUpEdit();
                    mCallback.onMainActivityCallback(MainActivity.CALLBACK_LOAD_PAGE, null);
                    return true;
                } else {
                    return false;
                }
            }
        });

        return retView;
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(ctx);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        // housekeeping function
        try {
            mCallback = (OnMainActivityCallbackListener) ctx;
        } catch (ClassCastException e) {
            throw new ClassCastException(ctx.toString()
                    + " must implement OnMainActivityCallbackListener");
        }
    }

    /*
    Remove spaces after periods for text strings that have 2 . in them.
    Android keyboards have a really irritating habit of inserting spaces after period. This is not due to edit
    text control. This is a keyboard issue. So fix this here. (look to see if you have a web address and remove space).
    2 . = web address
 */
    private void cleanUpEdit() {
        String input = mEdit.getText().toString();
        String web = "";
        int count = 0;

        for (int i = 0; i < input.length(); i++) {
            if (input.substring(i,i+1).equals(".")) {
                count++;
            }

            if (!input.substring(i,i+1).equals(" ")) {
                web = web + input.substring(i,i+1);
            }
        }

        if (count == 2) {
            mEdit.setText(web);
        }

    }

    //
    //  Update the edit box text...
    //
    public void updateEditBox(String edit) {
        mEdit.setText(edit);
    }

    //
    //  Get the edit box text///
    //
    public String getEditBox() {
        return mEdit.getText().toString();
    }

}
