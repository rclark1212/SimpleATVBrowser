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

/*
    Simple (less simple now) browser for ATV.
    Started with a phone/tablet project and just added the LEANBACK intents + some controller support.
    Redid into a second project "HackATVBrowser2" which starts with an ATV project (may make some difference)
    Redid into this third project which started simple and got less simple.
    Yes - this file a bit big and messy. Really should split into utility routines.
    All button/action processing done in this routine.
    Three fragments used - searchbar is one. Then there is a container for either a webview or a browseview for favorites
 */

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.Toast;

import com.example.rclark.simpleatvbrowser.data.FavContract;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements
        View.OnClickListener, SearchbarFragment.OnMainActivityCallbackListener, WebviewFragment.OnMainActivityCallbackListener, FavoritesFragment.OnMainActivityCallbackListener {

    private View mvControls;

    //default home url
    private static String m_default_home = "www.google.com";

    //Variables for the history list
    ListPopupWindow mlpw;        //popup window
    List<String> m_urlist;      //history list
    private final static int MAX_HISTORY = 10;
    //Preference key for history
    private final static String HISTORY_LIST = "history_list";
    private static final String PREFS_HAS_RUN_ALREADY = "prefs_has_run_already";

    //Fragments...
    SearchbarFragment mSearchFragment;
    WebviewFragment mWebFragment;
    FavoritesFragment mFavoritesFragment;

    private int mZoom = 0;      //used to track zoom status
    private static float m_zoom_scale_factor = 1.5f;             //change this constant to change the zoom step
    private static float m_zoomout_scale_factor = 1/ m_zoom_scale_factor;
    private static int m_pan_scale_factor = 40;             //change this constant to change the pan speed

    //Prefix term for a google search (in case text entered not a url)
    private final static String GOOGLE_SEARCH = "http://www.google.com/#q=";

    //special case for when we re-enter intent from help page...
    private static String mPageHelpOverwrote = "";

    //semaphore for speech intent
    private boolean mbInSpeechIntent = false;

    //Some ordinal defines...
    protected static final int RESULT_SPEECH = 1967;               //ordinal for our intent response
    protected final static int PREFERENCE_REQUEST_CODE = 1968;

    protected static final int CALLBACK_LOAD_PAGE = 1969;
    protected static final int CALLBACK_HIDE_BAR = 1970;
    protected static final int CALLBACK_SHOW_BAR = 1971;
    protected static final int CALLBACK_UPDATE_URL = 1972;
    protected static final int CALLBACK_HIDE_KEYBOARD = 1973;
    protected static final int CALLBACK_UPDATE_FAVORITE = 1974;
    protected static final int CALLBACK_LOAD_FAVORITE = 1975;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean bOpenByIntent = false;          //indicate an intent other than launcher kicked us off
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init pan/zoom and homepage settings
        updatePanZoomHomeSettings();

        //init the history list...
        m_urlist = new ArrayList<String>();

        //get a view to searchbar
        mvControls = findViewById(R.id.searchbar_fragment);

        //Get the search fragment... (note - webview fragment dynamically loaded when needed)
        mSearchFragment = (SearchbarFragment) getFragmentManager().findFragmentById(R.id.searchbar_fragment);

        //and set up final initialization

        //set the initial edittext...
        mSearchFragment.updateEditBox(m_default_home);

        //initialize prefs...
        if (isRunningForFirstTime(getApplicationContext())) {
            //and init the preferences
            PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
        }

        //urp - lets see if we got started via an intent other than launcher here...
        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            //okay - could be a view intent, web search intent
            //if it is, grab the data and populate edit box with it
            if (receivedIntent.getAction().equals(Intent.ACTION_VIEW)) {
                mSearchFragment.updateEditBox(receivedIntent.getData().toString());
                bOpenByIntent = true;
            } else if (receivedIntent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
                //okay, now get the data...
                mSearchFragment.updateEditBox(receivedIntent.getStringExtra(SearchManager.QUERY));
                bOpenByIntent = true;
            }
        }

        //should we open favorites or main? depends on if there are favorites...
        loadPage();

        //set focus to voice input
        mSearchFragment.mvVoice.requestFocus();

        //hide keyboard
        hideKeyboard();

        //Only go to favorites screen on start if we were launched by launcher
        if ((favoriteCount() > 0) && !bOpenByIntent) {
            showFavorites(true);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        //called when we get re-entered. Specifically, help screen from preferences...
        if (intent != null) {
            if (intent.getAction().equals(Intent.ACTION_VIEW)) {
                mPageHelpOverwrote = mSearchFragment.getEditBox();
                mSearchFragment.updateEditBox(intent.getData().toString());
                loadPage();
            }
        }
    }


    //Routine called by search bar fragment to request a page load
    public void onMainActivityCallback(int code, String url) {
        //make this more generic. may need to do several things...
        if (code == CALLBACK_LOAD_PAGE) {
            loadPage();
        } else if (code == CALLBACK_HIDE_BAR) {
            showSearchBar(false);
        } else if (code == CALLBACK_SHOW_BAR) {
            showSearchBar(true);
        } else if (code == CALLBACK_UPDATE_URL) {
            mSearchFragment.updateEditBox(mWebFragment.getURL());
        } else if (code == CALLBACK_HIDE_KEYBOARD) {
            hideKeyboard();
        } else if (code == CALLBACK_UPDATE_FAVORITE) {
            setFavoriteButton(isFavorite(mWebFragment.getURL()));
            //and use this opportunity to enable/disable nav buttons
            mSearchFragment.mvBack.setEnabled(mWebFragment.mWView.canGoBack());
            mSearchFragment.mvForward.setEnabled(mWebFragment.mWView.canGoForward());
        } else if (code == CALLBACK_LOAD_FAVORITE) {
            //load a favorite
            //first, swap to webview
            showFavorites(false);
            //then update edit box
            mSearchFragment.updateEditBox(url);
            //then load page
            loadPage();
        }
    }

    /*
        Shows or hides the searchbar
        (probably should just subclass layout and override set visibility method but lazy...)
        Note that animation not coordinated. FIXME.
     */
    private void showSearchBar(boolean show) {

        if (show) {
            if (mvControls.getVisibility() != View.VISIBLE) {
                mvControls.setVisibility(View.VISIBLE);
                //and when you show searchbar (when it was invisible), put focus on help button
                mSearchFragment.mvSettings.requestFocus();
            }
        } else {
            if (mvControls.getVisibility() == View.VISIBLE) {
                mvControls.setVisibility(View.GONE);
            }
        }

        //If you want some nice animation... Do it here...
        //As speed is a virtue, don't bother with animatio at this point.
    }


    /*
        setFavoriteButton
        Sets favorite button to favorite if true, not favorite if false
     */
    private void setFavoriteButton(boolean bFavorite) {
        ImageButton b = (ImageButton) mSearchFragment.mvFavorite;
        if (bFavorite) {
            //change favorite icon to solid
            b.setImageResource(R.drawable.favorite);
        } else {
            //change to hollow
            b.setImageResource(R.drawable.notfavorite);
        }
    }


    /*
        Loads a web page from the URL/text that is in the edit box
        Note - dynamically loads fragment if it does not exist...
     */
    private void loadPage() {
        //two special cases we will make here...
        //if user types in http:// or https:// in front of page, we will assume it is real (and not search)
        //if the user types in "help", we will special case to help file
        //first, make sure to show web page when loading a page
        boolean bOverride = false;
        boolean bHelp = false;

        showFavorites(false);

        //Get the edit box text
        String url = mSearchFragment.getEditBox();
        String http = url;

        //is the url "help?"
        if (url.toLowerCase().equals("help")) {
            bHelp = true;
        }

        //if anyone tries to actually type in http:// or https://, try to load it
        if (url.length() > "https://".length()) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                bOverride = true;
            }
        }

        //Does it have a valid prefix? (note, this is assumptive code that http:// is in right spot)
        //if not, add it.
        if (!http.contains("http:/") && !http.contains("https:/")) {
            http = "http://" + url;
        }

        //note - hide keyboard if showing...
        hideKeyboard();

        if (bHelp) {
            //show help
            showHelp();
        } else if (isValidUrl(http) || bOverride) {         //Now is this a valid http url?
            //add this url to the list
            addToList(url);
            //if so, go ahead and load (and don't bother setting the edit text url with the full address)
            loadWebUrl(http, true);
        } else {
            //do a google search with the terms typed into the edit box...
            http = GOOGLE_SEARCH + url;
            loadWebUrl(http, false);
        }
    }

    /*
        loadWebUrl - loads the webview with supplied URL.
        Note - if webview fragment not created, will create it and will pass bundle to webview
     */
    void loadWebUrl(String http, boolean bEatUpdate) {

        if (mWebFragment == null) {
            mWebFragment = new WebviewFragment();
            Bundle args = new Bundle();
            args.putString(WebviewFragment.ARG_URL, http);
            if (bEatUpdate) {
                args.putInt(WebviewFragment.ARG_EATUPDATE, 1);
            } else {
                args.putInt(WebviewFragment.ARG_EATUPDATE, 0);
            }
            mWebFragment.setArguments(args);
            getFragmentManager().beginTransaction().add(R.id.fragment_container, mWebFragment).commit();
        } else {
            mWebFragment.loadURL(http, bEatUpdate);
        }
    }

    /*
        Hides on screen keyboard if showing
     */
    private void hideKeyboard() {
        if (!isKeyboardHidden()) {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }


    /*
        indicates if keyboard hidden or not
     */
    private boolean isKeyboardHidden() {
        //ugg - no android routine to do this...
        //so need to measure screen.
        boolean bret = true;

        //this really only an issue during webview...
        Rect r = new Rect();
        if (mWebFragment != null) {

            if (mWebFragment.mWView != null) {
                mWebFragment.mWView.getWindowVisibleDisplayFrame(r);

                //ratio if keyboard shown will be > 2:1
                if (r.height() > 0) {
                    if (r.width() >= (r.height() * 2)) {
                        bret = false;
                    }
                }
            }
        }
        return bret;
    }


    /*
        Check if this is a valid URL.
     */
    private boolean isValidUrl(String url) {
        //is it a url pattern match
        return (Patterns.WEB_URL.matcher(url).matches());
    }


    /*
        Handle the controller shortcuts for the buttons
        Essentially our input handler
        See the help file for controller button mappings...
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean bEatKey = false;    //some keys need to be eaten (voice search, back button)
        if (event != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y) {
                    if (isWebFragmentActive()) {
                        mWebFragment.mWView.setScrollY(0);      //reset to top of page...
                    }
                    //always show search bar if we go to info button...
                    showSearchBar(true);
                    mSearchFragment.mvSettings.requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    //special case first back key when entering to help page from settings...
                    //users expect back to take them to last web page.
                    if (mPageHelpOverwrote.length() > 0) {
                        mSearchFragment.updateEditBox(mPageHelpOverwrote);
                        mPageHelpOverwrote = "";
                        bEatKey = true;
                        loadPage();
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                    bEatKey = true;
                    doVoiceSearch();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1) {
                    if ((mZoom > 0) && isWebFragmentActive()) {
                        mZoom--;
                        mWebFragment.setZoom(m_zoomout_scale_factor);
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1) {
                    if (isWebFragmentActive()) {
                        mZoom++;
                        mWebFragment.setZoom(m_zoom_scale_factor);
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
                    if (isWebFragmentActive()) {
                        bEatKey = true;
                        goBack();
                    } else {
                        //swap to web fragment
                        showFavorites(false);
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    //okay - if user does a dpad up while on favorites page top row...
                    //pop cursor to home button
                    if (!isWebFragmentActive()) {
                        //showing favorites
                        if (mFavoritesFragment.getRow() == 0) {
                            //put focus on search bar home
                            mSearchFragment.mvHome.requestFocus();
                        }
                    }
                }
            }
        }

        //If we want to eat the key, return true here...
        if (bEatKey) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    /*
        Handle left stick controller move events for panning here
     */
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event != null) {
            //check that webfragment is visible
            if (isWebFragmentActive() && isKeyboardHidden()) {
                //Check that this is a move action (rather than hover which the RS mouse sends)
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float x = event.getAxisValue(MotionEvent.AXIS_X);
                    float y = event.getAxisValue(MotionEvent.AXIS_Y);

                    //scale up the events...
                    int scale = m_pan_scale_factor;

                    int px = (int) (x * scale);
                    int py = (int) (y * scale);

                    //if we have movement, move...
                    if ((px != 0) || (py != 0)) {
                        mWebFragment.setScroll(px, py);
                    }
                }
            }
        }

        return super.dispatchGenericMotionEvent(event);
    }


    /*
        Go back a web page
     */
    public void goBack() {
        mWebFragment.goBackWeb();
    }

    /*
    Go forward a web page
 */
    public void goForward() {
        mWebFragment.goForwardWeb();
    }


    /*
        Do a voice search entry for a web site (or for search)
        Use the standard android intent service for this. This routine kicks off the intent.
     */
    public void doVoiceSearch() {
        if (mbInSpeechIntent) {
            //already in an intent
            return;
        }
        //set up the intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());

        //and kick it off
        try {
            startActivityForResult(intent, RESULT_SPEECH);
            mbInSpeechIntent = true;
            //and if intent exists, clear out text box
            mSearchFragment.updateEditBox("");
        } catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(), "Oops - no voice to text service", Toast.LENGTH_LONG);
            t.show();
        }

        //intent call back will handle rest...
    }

    /*
        Intent callback service (used for voice search)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //get the voice search data back - this is only intent we are interested in...
        switch (requestCode) {
            case RESULT_SPEECH: {
                mbInSpeechIntent = false;
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //We got something. Go ahead and set the edit box and load the page.
                    mSearchFragment.updateEditBox(text.get(0));
                    loadPage();
                }
                break;
            }
            case PREFERENCE_REQUEST_CODE: {
                if (data != null) {
                    int prefResult = data.getIntExtra(SettingsActivity.PREF_RESULT_KEY, SettingsActivity.PREF_DO_NOTHING);

                    //And now process...
                    if ((prefResult & SettingsActivity.PREF_DO_SOMETHING) != 0) {
                        //update web...
                        mWebFragment.updateWebView(mWebFragment.mWView);

                        //update zoom/pan
                        updatePanZoomHomeSettings();
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //save any history
        saveHistory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //load any saved history
        loadHistory();
    }

    /*
        Save history to preferences
     */
    private void saveHistory() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> history = new HashSet<String>();

        for (int i = 0; i < m_urlist.size(); i++) {
            history.add(m_urlist.get(i));
        }

        SharedPreferences.Editor edit = pref.edit();
        edit.putStringSet(HISTORY_LIST, history);
        edit.commit();

    }

    /*
        Load history from preferences
     */
    private void loadHistory() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> history = pref.getStringSet(HISTORY_LIST, null);

        if (history != null) {
            //clear history first...
            m_urlist.clear();

            //and recover...
            for (String value: history) {
                m_urlist.add(value);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    /*
        Handle the onClick for searchbar buttons
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back: {
                hideKeyboard();
                showFavorites(false);
                goBack();
                break;
            }
            case R.id.forward: {
                hideKeyboard();
                showFavorites(false);
                goForward();
                break;
            }
            case R.id.voice: {
                hideKeyboard();
                doVoiceSearch();;
                break;
            }
            case R.id.refresh: {
                //loadPage();
                hideKeyboard();
                showFavorites(false);
                mWebFragment.mWView.reload();
                break;
            }
            case R.id.settings: {
                // Display the settings fragment
                hideKeyboard();
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), SettingsActivity.class);
                //and start for a result so that we can take care of any UI elements that we have to do when we get back...
                startActivityForResult(intent, PREFERENCE_REQUEST_CODE);
                break;
            }
            case R.id.dropdown: {
                hideKeyboard();
                popupList();
                break;
            }
            case R.id.home: {
                //if no favorites, go home
                hideKeyboard();
                if (favoriteCount() == 0) {
                    mSearchFragment.updateEditBox(m_default_home);
                    loadPage();
                } else {
                    //otherwise show favorites
                    showFavorites(isWebFragmentActive());    //toggles back and forth...
                }
                break;
            }
            case R.id.favorite: {
                hideKeyboard();
                if (isWebFragmentActive()) {
                    updateFavorites(true);
                    break;
                }
            }
        }
    }

    /*
        Returns number of favorites
     */
    int favoriteCount() {
        int iret = 0;

        Uri favoriteDB = FavContract.FavoritesEntry.CONTENT_URI;

        Cursor c = getApplicationContext().getContentResolver().query(favoriteDB, null, null, null, null);

        iret = c.getCount();

        c.close();

        return iret;
    }


    /*
        returns true if site is a favorite
    */
    boolean isFavorite(String url) {
        boolean bret = false;
        Uri favoriteDB = FavContract.FavoritesEntry.CONTENT_URI;

        //Now, search for the url
        Uri favoriteSearchUri = favoriteDB.buildUpon().appendPath(url).build();
        Cursor c = getApplicationContext().getContentResolver().query(favoriteSearchUri, null, null, null, null);

        //now check if it is stored in favorites already...
        if (c.getCount() > 0) {
            bret = true;
        }

        c.close();

        return bret;
    }

    /*
        Updates the content provider based upon the web url.
        if btoggle is true, will toggle favorite on/off.
        if btoggle is false, will only update thumb if favorite exists
     */
    private void updateFavorites(boolean btoggle) {
        //is web fragment visible?
        if (isWebFragmentActive()) {
            //get the URL...
            String url = mWebFragment.getURL();

            //Get the favorites DB reference...
            Uri favoriteDB = FavContract.FavoritesEntry.CONTENT_URI;

            //Now, search for the url
            Uri favoriteSearchUri = favoriteDB.buildUpon().appendPath(url).build();
            Cursor c = getApplicationContext().getContentResolver().query(favoriteSearchUri, null, null, null, null);

            //now check if it is stored in favorites already...
            if (c.getCount() > 0) {
                c.moveToFirst();
                if (btoggle) {
                    //if it is in favorites and we are toggling, delete
                    getApplicationContext().getContentResolver().delete(FavContract.FavoritesEntry.CONTENT_URI,
                            FavContract.FavoritesEntry.COLUMN_FAVORITES_URL + " = ?",
                            new String[] {url});
                    //finally, change icon to unfavorite
                    setFavoriteButton(false);
                } else {
                    //otherwise just update thumbnail
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_URL, url);
                    contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITE_TITLE, mWebFragment.mWView.getTitle());
                    contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITE_HTTP, mSearchFragment.getEditBox());
                    contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB, getWebBytes());
                    getApplicationContext().getContentResolver().update(favoriteSearchUri, contentValues, null, null);
                }
            } else {
                //if not, insert it...
                ContentValues contentValues = new ContentValues();
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_URL, url);
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITE_TITLE, mWebFragment.mWView.getTitle());
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITE_HTTP, mSearchFragment.getEditBox());
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB, getWebBytes());
                getApplicationContext().getContentResolver().insert(FavContract.FavoritesEntry.CONTENT_URI, contentValues);
                setFavoriteButton(true);
            }

            c.close();

        }
    }

    /*
        getWebBytes
        returns bytestream of web page image that can be put into content values blob
     */
    private byte[] getWebBytes() {
        //capture visible webview screen to bitmap
        Bitmap bm = Bitmap.createBitmap(mWebFragment.mWView.getWidth(), mWebFragment.mWView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        int height = bm.getHeight();
        canvas.drawBitmap(bm, 0, height, paint);
        mWebFragment.mWView.draw(canvas);

        //now convert to byte[]
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();

        return imageInByte;
    }


    /*
        Returns true if web fragment is active
     */
    private boolean isWebFragmentActive() {
        boolean bret = true;

        if (mWebFragment != null) {
            if (!mWebFragment.isVisible()) {
                bret = false;
            }
        }

        return bret;
    }


    /*
        Routine which swaps the fragments around to show either webview or favorites view.
        false = webview, true = favorites.
     */
    private void showFavorites(boolean bShow) {

        //create fragment if it does not exist
        //create favorite fragment instance if it does not exist...
        if (mFavoritesFragment == null) {
            mFavoritesFragment = new FavoritesFragment();
        }


        //check if fragment already visible...
        if (bShow) {
            //is fragment already visible?
            if (mFavoritesFragment.isVisible()) {
                //just return
                return;
            }

            //otherwise replace web with this one...
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, mFavoritesFragment);

            transaction.commit();

            getFragmentManager().executePendingTransactions();  //force the commit to take place

            //and when you show favorites, always make favorite button active
            setFavoriteButton(true);
        } else {
            //is web fragment already visible?
            if (isWebFragmentActive()) {
                return;
            }

            //otherwise show web fragment (if it has already been created - fix an issue on launch)
            if (mWebFragment != null) {
                //and replace web with this one...
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.fragment_container, mWebFragment);

                transaction.commit();

                getFragmentManager().executePendingTransactions();  //force the commit to take place

                //fix up favorites button
                setFavoriteButton(isFavorite(mWebFragment.getURL()));
            }
        }

    }

    /*
        Pop up list of last web sites visited
     */
    private void popupList() {

        //is there anything in the list?
        if (m_urlist.size() > 0) {

            String[] list = new String[m_urlist.size()];
            list = m_urlist.toArray(list);

            //create the window
            mlpw = new ListPopupWindow(this);

            mlpw.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
            mlpw.setAnchorView(mSearchFragment.mEdit);
            mlpw.setModal(true);

            mlpw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item = m_urlist.get(position);
                    //set the edit text and load the page (and dismiss)
                    mSearchFragment.updateEditBox(item);
                    loadPage();
                    mlpw.dismiss();
                }
            });

            mlpw.show();

            //set up a listener to exit popup if dpad right/left pressed
            ListView lv = (ListView) mlpw.getListView();
            if (lv != null) {
                lv.setOnKeyListener(new ListView.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT) || (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                            mlpw.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
    }


    /*
        Adds an url to our history list...
     */
    private void addToList(String url) {
        //first, does this url exist in the list?
        if (!m_urlist.contains(url)) {
            //not there. add it...
            m_urlist.add(0, url);
        } else {
            //if it is there, move it to top of list...
            int index = m_urlist.indexOf(url);
            if (index > 0) {
                m_urlist.remove(index);
                m_urlist.add(0, url);
            }
        }

        //and throw out those that exceed history
        if (m_urlist.size() > MAX_HISTORY) {
            m_urlist.remove(MAX_HISTORY);
        }
    }

    /*
        Throw up simple help dialog here
     */
    private void showHelp() {

        showFavorites(false);

        loadWebUrl("file:///android_asset/simple_browser_help.html", true);
    }

    public static boolean isRunningForFirstTime(Context ctx) {
        boolean bret = true;

        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bhasrun = pref.getBoolean(PREFS_HAS_RUN_ALREADY, false);

        if (bhasrun == true) {
            bret = false;
        }

        //And, if we are here, we have run for first time so mark preferences as such
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(PREFS_HAS_RUN_ALREADY, true);
        edit.commit();

        return bret;
    }

    public void updatePanZoomHomeSettings() {
        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String pan = pref.getString(getString(R.string.key_pan_speed), "1");
        String zoom = pref.getString(getString(R.string.key_zoom_speed), "1");

        m_default_home = pref.getString(getString(R.string.key_web_home), getString(R.string.web_home_default));

        //update pan
        if (pan.equals("0")) {
            m_pan_scale_factor = 20;
        } else if (pan.equals("2")) {
            m_pan_scale_factor = 75;
        } else {
            m_pan_scale_factor = 45;
        }

        //update zoom
        if (zoom.equals("0")) {
            m_zoom_scale_factor = 1.2f;
        } else if (zoom.equals("2")) {
            m_zoom_scale_factor = 1.8f;
        } else {
            m_zoom_scale_factor = 1.5f;
        }
        m_zoomout_scale_factor = 1/ m_zoom_scale_factor;
    }

}
