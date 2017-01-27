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

package com.production.rclark.simpleatvbrowser;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * Created by rclark on 3/30/2016.
 */
public class WebviewFragment extends Fragment {

    private final static int HIDE_SEARCH_AT_YSCROLL = 200;
    public final static String ARG_URL = "url";
    public final static String ARG_EATUPDATE = "update";

    public WebView mWView;
    private boolean mbDontUpdate;
    private Bundle webViewBundle;
    private String mNewHTML;

    //The UA string to convince websites we are a desktop browser...(finding it does not really work though). FIXME
    //private final static String UA_DESKTOP = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

    OnMainActivityCallbackListener mCallback;
    //Put in an interface for container activity to implement so that fragment can deliver messages
    public interface OnMainActivityCallbackListener {
        //called by WebviewFragment when a url is selected
        public void onMainActivityCallback(int code, String url);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View retView = inflater.inflate(R.layout.webview_fragment, container, false);

        mWView = (WebView) retView.findViewById(R.id.webview);

        //initialize the web view
        initWebView(mWView);

        //process scroll events to show/hide searchbar
        mWView.setOnScrollChangeListener(new WebView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int nx, int ny, int ox, int oy) {
                if (ny > HIDE_SEARCH_AT_YSCROLL) { //put in a bit of a threshold...
                    mCallback.onMainActivityCallback(MainActivity.CALLBACK_HIDE_BAR, null);
                } else if (ny == 0) {
                    mCallback.onMainActivityCallback(MainActivity.CALLBACK_SHOW_BAR, null);
                }
            }
        });

        //Now check args... (we pass initial web site to load as arguments when webview not created yet).
        //Note - we only want to do this for the first load (use webViewBundle == null to tell us it is the first load)
        if ((getArguments() != null) && (webViewBundle == null)) {
            if (getArguments().containsKey(ARG_URL)) {
                String http = getArguments().getString(ARG_URL);
                int eatUpdate = getArguments().getInt(ARG_EATUPDATE);
                if (eatUpdate != 0) {
                    loadURL(http, true);
                } else {
                    loadURL(http, false);
                }
            }
        }

        return retView;
    }

    //
    //  Set the callback
    //
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

    //
    //  Use onPause to save the webview state
    //
    @Override
    public void onPause() {
        super.onPause();
        webViewBundle = new Bundle();
        mWView.saveState(webViewBundle);
    }

    //
    //  Use onActivityCreated to restore the state
    //
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (webViewBundle != null) {
            mWView.restoreState(webViewBundle);
        }
    }


    //
    //  Zoom function
    //
    public void setZoom(float zoom) {
        mWView.zoomBy(zoom);
    }

    //
    //  Scroll function
    //
    public void setScroll(int x, int y) {
        mWView.scrollBy(x, y);
    }

    //
    //  Go back function
    //
    public void goBackWeb() {
        mbDontUpdate = false;   //we want to update post load...
        mWView.goBack();
    }

    //
    //  Go back function
    //
    public void goForwardWeb() {
        mbDontUpdate = false;   //we want to update post load...
        mWView.goForward();
    }

    //
    //  Get the URL
    //
    public String getURL() {
        return mWView.getUrl();
    }

    //
    // Routine to load the webview
    //
    public void loadURL(String url, boolean bUpdateEditBox) {
        mbDontUpdate = bUpdateEditBox;
        mWView.loadUrl(url);
    }

    /**
     * Break the settings call into 2. Fixed settings we set in initWebView.
     * These settings may get updated due to preference changes.
     * @param view
     */
    public void updateWebView(WebView view){

        if (view != null) {

            WebSettings webSettings = view.getSettings();

            if (webSettings != null) {

                //load the prefs...
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean bAllowFileDB = pref.getBoolean(getResources().getString(R.string.key_enable_file_access), true);

                //more settings for HTML5
                webSettings.setDatabaseEnabled(bAllowFileDB);
                webSettings.setAllowContentAccess(bAllowFileDB);
                webSettings.setAllowFileAccess(bAllowFileDB);
                webSettings.setAllowFileAccessFromFileURLs(bAllowFileDB);
                webSettings.setAllowUniversalAccessFromFileURLs(bAllowFileDB);
                webSettings.setGeolocationEnabled(bAllowFileDB);
                PackageManager pm = getActivity().getPackageManager();
                String pkgname = getActivity().getPackageName();
                try {
                    PackageInfo pi = pm.getPackageInfo(pkgname, 0);
                    webSettings.setGeolocationDatabasePath(pi.applicationInfo.dataDir);
                    webSettings.setAppCachePath(pi.applicationInfo.dataDir);
                    webSettings.setAppCacheEnabled(bAllowFileDB);
                } catch (PackageManager.NameNotFoundException e) {
                    //should log error...
                }

                //Set UA string to try to get desktop sites (again, doesn't really work).
                String UAString = pref.getString(getResources().getString(R.string.key_ua_string), getString(R.string.ua_string_default));
                if (UAString.length() == 0) {
                    UAString = getString(R.string.ua_string_default);
                }
                webSettings.setUserAgentString(UAString);
            }
        }
    }

    /*
    This routine initializes the webview we use.
    */
    private void initWebView(WebView view) {

        //Handle cookies here...
        //WARNING - this has security implications. We just globally enable cookies. Sites can read other sites cookies afaik
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);

        //Enable javascript
        WebSettings webSettings = view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccess(true);

        //Set up to support zoom...
        webSettings.setSupportZoom(true);

        //try to set the desktop mode for web page load...
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        //Don't use the zoom controls for ATV
        //webSettings.setDisplayZoomControls(false);

        //Try some settings for html5 video per web
        webSettings.setBuiltInZoomControls(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setSaveFormData(true);

        //Set defalts for caching...
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        //Set webview to our overriden class...
        view.setWebViewClient(new MyWebViewClient());
        //view.setWebChromeClient(new MyWebChromeClient());

        //finally, set scroll bars...
        view.setVerticalScrollBarEnabled(true);
        view.setScrollbarFadingEnabled(true);

        //and set up our custom interface (can process html loaded code)
        view.addJavascriptInterface(new InjectJavaScriptInterface(), "HTMLOUT");

        //now the rest of the settings
        updateWebView(view);
    }

    /*
        Private JavaScript interface for post processing (as needed)
        see http://stackoverflow.com/questions/5264162/how-to-retrieve-html-content-from-webview-as-a-string
     */
    private class InjectJavaScriptInterface
    {

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html)
        {

            /*
            int currentIndex = 0;
            int newIndex = 0;
            int count = 0;
            String find = "<div class=\"title";
            String replacefmt = "<div tabindex=\"%d\" class=\"title";

            Log.d("JScript Injector","html length start " + html.length());
            // process the html as needed by the app
            while ((newIndex = html.indexOf(find,currentIndex)) > 0) {
                count++;
                String replace = String.format(replacefmt, count);
                html = html.replaceFirst(find, replace);
                currentIndex = newIndex + replace.length();
                if (currentIndex >= html.length()) {
                    break;
                }
            }
            Log.d("JScript Injector", "Found/replaced " + count + " instances");
            Log.d("JScript Injector","html length end " + html.length());
            if (count > 0) {
                //set up to reload page...
                mNewHTML = html;
            } else {
                mNewHTML = null;
            }
            */

        }
    }


    /*
    Private webview class.
    We only override a couple methods.
    (1) shouldOverrideUrlLoading to keep user in this browser when clicking hyperlink (rather than triggering intent for system browser)
    (2) onPageFinished so we can update the url edit text box with the new url if the user clicks a hyperlink
 */
    private class MyWebViewClient extends WebViewClient {

        private final static String YOUTUBE_ID_PARSE = "watch%3Fv%3D";
        private final static String YOUTUBE_SPECIAL_CASE = "http://www.youtube.com/?tab";

        //want to keep user in this browser instance (and not fire intent for a general system browser)
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            //Update this to launch youtube URLs in the native youtube app
            if (isYoutubeUrl(url)) {
                // youtube URL
                if (launchYoutubeUrl(url)) {
                    return true;
                }
            }
            return false;
        }

        //tries to launch youtube by recovering watch id in url and sending intent.
        //else will just return false to load within webview
        private boolean launchYoutubeUrl(String url) {
            boolean bret = false;
            //special case youtube from google web page...
            if (url.startsWith(YOUTUBE_SPECIAL_CASE)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
                startActivity(intent);
                return true;
            }

            //try to get the ID...
            int index = url.lastIndexOf(YOUTUBE_ID_PARSE);

            if (((index + YOUTUBE_ID_PARSE.length()) < url.length() && (index > 0))) {
                String watchend = url.substring(index + YOUTUBE_ID_PARSE.length());

                //build id string by walking down string to first non-alpha char
                int i = 0;
                String id = "";
                char[] buffer = new char[1];

                while (i < watchend.length()) {
                    watchend.getChars(i, i+1, buffer, 0);
                    if (isNumberLetter(buffer[0])) {
                        id = id + watchend.substring(i, i+1);
                    }
                    else {
                        break;
                    }
                    i++;
                }

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
                    startActivity(intent);
                    bret = true;
                } catch (ActivityNotFoundException ex) {
                    //give up and load web page - or we could load youtube...
                    bret = false;
                }
            }
            return bret;
        }

        //returns true if letter or number
        boolean isNumberLetter(char c) {

            //is it a number?
            if ((c >= '0') && (c <= '9')) {
                return true;
            }

            //upper case?
            if ((c >= 'A') && (c <= 'Z')) {
                return true;
            }

            //lower case
            if ((c >= 'a') && (c <= 'z')) {
                return true;
            }

            return false;
        }


        //returns true if youtube site...
        private boolean isYoutubeUrl(String url) {
            //take a look at prefs...
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean bUseYoutube = pref.getBoolean(getString(R.string.key_enable_youtube_apk), true);

            if (!bUseYoutube) {
                return false;
            }

            return (url.contains("youtube:") || url.contains("www.youtube.com") || url.contains("m.youtube.com"));
        }

        //when you click page to page, want address bar to show new address
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            //update url text
            if (!mbDontUpdate) {
                mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_URL, null);
            } else {
                mbDontUpdate = false;
            }

            //and update favorites
            mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_FAVORITE, null);

            //mWView.loadUrl("$(\":input\").each(function (i) { $(this).attr('tabindex', i + 1); });");
            /*
            //and do any object injection... Note - revert to get rid of all...
            mWView.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            mWView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mNewHTML != null) {
                        Log.d("Webview", "reloading webview");
                        mWView.loadData(mNewHTML, "text/html", "UTF-8");
                    }
                }
            }, 1000);
            if (mNewHTML != null) {
                Log.d("Webview", "reloading webview in 1 second");
            }*/
        }

        //when you click page to page, want address bar to show new address
        //onpagefinished takes too long... So try updating on the start
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            //update url text (clear don't update flag on finish)
            if (!mbDontUpdate) {
                mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_URL, null);
            }

            //and update favorites
            mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_FAVORITE, null);
        }

    }

}
