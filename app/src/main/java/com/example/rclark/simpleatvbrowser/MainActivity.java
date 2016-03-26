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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ListPopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener {

    //Keep a copy of the views
    private WebView mView;
    private EditText mEdit;
    private View mvBack;
    private View mvVoice;
    private View mvRefresh;
    private View mvHelp;
    private View mvList;

    private View mvControls;

    ListPopupWindow mlpw;        //popup window
    List<String> m_urlist;
    private final static int MAX_HISTORY = 10;

    private int mZoom = 0;      //used to track zoom status
    private final static float ZOOMIN_VALUE = 1.5f;             //change this constant to change the zoom step
    private final static float ZOOMOUT_VALUE = 1/ZOOMIN_VALUE;
    private final static int PAN_SCALE_FACTOR = 50;             //change this constant to change the pan speed
    private final static int ANIMTIME = 100;                    //100ms animations - speed is what we are after...

    //Preference key for history
    private final static String HISTORY_LIST = "history_list";

    //The UA string to convince websites we are a desktop browser...(finding it does not really work though). FIXME
    private final static String UA_DESKTOP = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

    //Prefix term for a google search
    private final static String GOOGLE_SEARCH = "http://www.google.com/#q=";

    private boolean mbDontUpdate = false;                       //used to flag if we should update edit box URL with loaded URL

    protected static final int RESULT_SPEECH = 1;               //ordinal for our intent response

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get views
        mView = (WebView) findViewById(R.id.webview);
        mEdit = (EditText) findViewById(R.id.search);

        //Okay - some weirdness happening in button processing. Not getting onClick
        //events for imagebuttons. No idea why. Hack it up by looking for button A matching the view
        //and dispatch directly for now.
        mvBack = findViewById(R.id.back);
        mvVoice = findViewById(R.id.voice);
        mvRefresh = findViewById(R.id.refresh);
        mvHelp = findViewById(R.id.help);
        mvList = findViewById(R.id.dropdown);

        mvControls = findViewById(R.id.searchbar);

        //init the history list...
        m_urlist = new ArrayList<String>();

        //initialize the web view
        initWebView();

        //set the initial edittext...
        mEdit.setText("www.google.com");

        //and load initial webview
        loadPage();

        //set focus to voice input
        mvVoice.requestFocus();

        //hide keyboard
        hideKeyboard();


        //process edittext
        mEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))) {
                    //We got a done or enter. Go ahead and clean up the text box and load page...
                    cleanUpEdit();
                    loadPage();
                    return true;
                } else {
                    return false;
                }
            }
        });

        //process scroll events to show/hide searchbar
        mView.setOnScrollChangeListener(new WebView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int nx, int ny, int ox, int oy) {
                if (ny > 200) { //ut in a bit of a threshold...
                    showSearchBar(false);
                } else if (ny == 0) {
                    showSearchBar(true);
                }
            }
        });
    }

    /*
        This routine initializes the webview we use.
     */
    private void initWebView() {

        //Handle cookies here...
        //WARNING - this has security implications. We just globally enable cookies. Sites can read other sites cookies afaik
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);

        //Enable javascript
        WebSettings webSettings = mView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //Set up to support zoom...
        webSettings.setSupportZoom(true);

        //try to set the desktop mode for web page load...
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        //Don't use the zoom controls for ATV
        //webSettings.setBuiltInZoomControls(true);
        //webSettings.setDisplayZoomControls(false);

        //Set defalts for caching...
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        //Set UA string to try to get desktop sites (again, doesn't really work). FIXME
        webSettings.setUserAgentString(UA_DESKTOP);

        //Set webview to our overriden class...
        mView.setWebViewClient(new MyWebViewClient());
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
        Loads a web page from the URL/text that is in the edit box
     */
    private void loadPage() {
        //Get the edit box text
        String url = mEdit.getText().toString();

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
            //if so, go ahead and load (and don't bother setting the edit text url with the full address)
            mbDontUpdate = true;
            //add this url to the list
            addToList(url);
            mView.loadUrl(http);
        } else {
            //do a google search with the terms typed into the edit box...
            http = GOOGLE_SEARCH + url;
            mView.loadUrl(http);
        }
    }

    /*
        Hides on screen keyboard if showing
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
                    //always show search bar if we go to info button...
                    showSearchBar(true);
                    mvHelp.requestFocus();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                    bEatKey = true;
                    doVoiceSearch();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1) {
                    if (mZoom > 0) {
                        mZoom--;
                        mView.zoomBy(ZOOMOUT_VALUE);
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1) {
                    mZoom++;
                    mView.zoomBy(ZOOMIN_VALUE);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
                    bEatKey = true;
                    goBack();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A) {
                    //Ugg - ImageButtons not invoking onClick on ATV. No idea why at this point.
                    //So hack it up by actually processing in displatchKeyEvent. Ugg. Ugg. FIXME.
                    View v = getCurrentFocus();
                    if (v == mvBack) {
                        goBack();
                    } else if (v == mvVoice) {
                        doVoiceSearch();
                    } else if (v == mvRefresh) {
                        loadPage();
                    } else if (v == mvHelp) {
                        showHelp();
                    } else if (v == mvList) {
                        popupList();
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
                    mView.scrollBy(px, py);
                }
            }
        }

        return super.dispatchGenericMotionEvent(event);
    }


    /*
        Go back a web page
     */
    public void goBack() {
        mbDontUpdate = true;
        this.mView.goBack();
        mEdit.setText(mView.getUrl());
    }

    /*
        Do a voice search entry for a web site (or for search)
        Use the standard android intent service for this. This routine kicks off the intent.
     */
    public void doVoiceSearch() {
        //set up the intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

        //and kick it off
        try {
            startActivityForResult(intent, RESULT_SPEECH);
            //and if intent exists, clear out text box
            mEdit.setText("");
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
                    mEdit.setText(text.get(0));
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
        Handle the onClick.
        Ugg - for some reason, imagebuttons are not being recognized as clicks on ATV. So this routine
        never called. Handle it in the dispatch routine instead. And FIXME.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back: {
                goBack();
                break;
            }
            case R.id.voice: {
                doVoiceSearch();;
                break;
            }
            case R.id.refresh: {
                loadPage();
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
            mlpw.setAnchorView(mEdit);
            mlpw.setModal(true);
            mlpw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item = m_urlist.get(position);
                    //set the edit text and load the page (and dismiss)
                    mEdit.setText(item);
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

    /*
        Private webview class.
        We only override a couple methods.
        (1) shouldOverrideUrlLoading to keep user in this browser when clicking hyperlink (rather than triggering intent for system browser)
        (2) onPageFinished so we can update the url edit text box with the new url if the user clicks a hyperlink
     */
    private class MyWebViewClient extends WebViewClient {

        //want to keep user in this browser instance (and not fire intent for a general system browser)
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            return false;
        }

        //when you click page to page, want address bar to show new address
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            //update url text
            if (!mbDontUpdate) {
                mEdit.setText(url);
            } else {
                mbDontUpdate = false;
            }
        }
    }
}
