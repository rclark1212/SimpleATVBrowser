package com.example.rclark.simpleatvbrowser;

/*
    Hack browser for ATV.
    Started with a phone/tablet project and just added the LEANBACK intents + some controller support.
    Redid into a second project "HackATVBrowser2" which starts with an ATV project (may make some difference)

 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rclark.simpleatvbrowser.data.FavContract;

import java.io.ByteArrayOutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements
        View.OnClickListener, SearchbarFragment.OnMainActivityCallbackListener, WebviewFragment.OnMainActivityCallbackListener {

    private View mvControls;

    //Variables for the history list
    ListPopupWindow mlpw;        //popup window
    List<String> m_urlist;      //history list
    private final static int MAX_HISTORY = 10;
    //Preference key for history
    private final static String HISTORY_LIST = "history_list";

    //Variables for the favorites list
    boolean mbFavoritesActive = false;

    //Fragments...
    SearchbarFragment mSearchFragment;
    WebviewFragment mWebFragment;
    FavoritesFragment mFavoritesFragment;

    private int mZoom = 0;      //used to track zoom status
    private final static float ZOOMIN_VALUE = 1.5f;             //change this constant to change the zoom step
    private final static float ZOOMOUT_VALUE = 1/ZOOMIN_VALUE;
    private final static int PAN_SCALE_FACTOR = 50;             //change this constant to change the pan speed
    private final static int ANIMTIME = 100;                    //100ms animations - speed is what we are after...

    //Prefix term for a google search
    private final static String GOOGLE_SEARCH = "http://www.google.com/#q=";

    //Some ordinal defines...
    protected static final int RESULT_SPEECH = 1;               //ordinal for our intent response
    protected static final int CALLBACK_LOAD_PAGE = 0;
    protected static final int CALLBACK_HIDE_BAR = 1;
    protected static final int CALLBACK_SHOW_BAR = 2;
    protected static final int CALLBACK_UPDATE_URL = 3;
    protected static final int CALLBACK_HIDE_KEYBOARD = 4;
    protected static final int CALLBACK_UPDATE_FAVORITE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init the history list...
        m_urlist = new ArrayList<String>();

        //get a view to searchbar
        mvControls = findViewById(R.id.searchbar_fragment);

        //Get the search fragment... (note - webview fragment dynamically loaded when needed)
        mSearchFragment = (SearchbarFragment) getFragmentManager().findFragmentById(R.id.searchbar_fragment);

        //and set up final initialization
        //set the initial edittext...
        mSearchFragment.updateEditBox("www.google.com");

        //and load initial webview
        loadPage();   //note - dynamically loads fragment if it does not exist...

        //set focus to voice input
        mSearchFragment.mvVoice.requestFocus();

        //hide keyboard
        hideKeyboard();

    }

    //Routine called by search bar fragment to request a page load
    public void onMainActivityCallback(int code) {
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
                mSearchFragment.mvHelp.requestFocus();
            }
        } else {
            if (mvControls.getVisibility() == View.VISIBLE) {
                mvControls.setVisibility(View.GONE);
            }
        }

        /* FIX animation below...
        if (show) {
            if (mvControls.getVisibility() != View.VISIBLE) {
                mvControls.animate().setDuration(ANIMTIME);
                //mView.animate().translationYBy(mvControls.getHeight());
                mvControls.animate().translationYBy(mvControls.getHeight()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mvControls.setVisibility(View.VISIBLE);
                    }
                });
            }
        } else {
            if (mvControls.getVisibility() == View.VISIBLE) {
                mvControls.animate().setDuration(ANIMTIME);
                //mView.animate().translationYBy(-mvControls.getHeight());
                mvControls.animate().translationYBy(-mvControls.getHeight()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mvControls.setVisibility(View.GONE);
                    }
                });
            }
        } */
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
        //Get the edit box text
        String url = mSearchFragment.getEditBox();
        String http = url;

        //Does it have a valid prefix? (note, this is assumptive code that http:// is in right spot)
        //if not, add it.
        if (!http.contains("http://")) {
            http = "http://" + url;
        }

        //note - hide keyboard if showing...
        hideKeyboard();

        //Now is this a valid http url?
        if (isValidUrl(http)) {
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
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*
        Check if this is a valid URL.
     */
    private boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
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
                    mWebFragment.mWView.setScrollY(0);      //reset to top of page...
                    //always show search bar if we go to info button...
                    showSearchBar(true);
                    mSearchFragment.mvHelp.requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                    bEatKey = true;
                    doVoiceSearch();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1) {
                    if (mZoom > 0) {
                        mZoom--;
                        mWebFragment.setZoom(ZOOMOUT_VALUE);
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1) {
                    mZoom++;
                    mWebFragment.setZoom(ZOOMIN_VALUE);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
                    bEatKey = true;
                    goBack();
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
            //Check that this is a move action (rather than hover which the RS mouse sends)
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getAxisValue(MotionEvent.AXIS_X);
                float y = event.getAxisValue(MotionEvent.AXIS_Y);

                //scale up the events...
                int scale = PAN_SCALE_FACTOR;

                int px = (int) (x * scale);
                int py = (int) (y * scale);

                //if we have movement, move...
                if ((px != 0) || (py != 0)) {
                    mWebFragment.setScroll(px, py);
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
        //set up the intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());

        //and kick it off
        try {
            startActivityForResult(intent, RESULT_SPEECH);
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
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //We got something. Go ahead and set the edit box and load the page.
                    mSearchFragment.updateEditBox(text.get(0));
                    loadPage();
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
                goBack();
                break;
            }
            case R.id.forward: {
                goForward();
                break;
            }
            case R.id.voice: {
                doVoiceSearch();;
                break;
            }
            case R.id.refresh: {
                //loadPage();
                mWebFragment.mWView.reload();
                break;
            }
            case R.id.help: {
                showHelp();
                break;
            }
            case R.id.dropdown: {
                popupList();
                break;
            }
            case R.id.home: {
                showFavorites();
                break;
            }
            case R.id.favorite: {
                updateFavorites(true);
                break;
            }
        }
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
                    contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB, getWebBytes());
                    getApplicationContext().getContentResolver().update(favoriteSearchUri, contentValues, null, null);
                }
            } else {
                //if not, insert it...
                ContentValues contentValues = new ContentValues();
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_URL, url);
                contentValues.put(FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB, getWebBytes());
                getApplicationContext().getContentResolver().insert(FavContract.FavoritesEntry.CONTENT_URI, contentValues);
                setFavoriteButton(true);
            }

            c.close();

        } else {
            //toggle off favorite for the currently selected favorites item
            //FIXME - todo
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
        boolean bret = false;

        if (mWebFragment != null) {
            if (mWebFragment.isVisible()) {
                bret = true;
            }
        }

        return bret;
    }


    private void showFavorites() {

        if (!mbFavoritesActive) {
            //show the favorites fragment!!!
            mbFavoritesActive = true;
            //create favorite fragment instance if it does not exist...
            if (mFavoritesFragment == null) {
                mFavoritesFragment = new FavoritesFragment();
            }

            //and replace web with this one...
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, mFavoritesFragment);

            transaction.commit();
        } else {
            //show web fragment
            mbFavoritesActive = false;

            //and replace web with this one...
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, mWebFragment);

            transaction.commit();

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

            //FIXME - use a custom layout that we override to get keyevents...
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
        }
    }


    /*
        Adds an url to our list...
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

        String title = getApplicationContext().getResources().getString(R.string.help_title);
        String msg = getApplicationContext().getResources().getString(R.string.help_msg);


        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue
                    }
                }).show();
    }

}
