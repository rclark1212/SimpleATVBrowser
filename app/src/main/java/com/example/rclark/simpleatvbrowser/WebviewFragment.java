package com.example.rclark.simpleatvbrowser;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

    //The UA string to convince websites we are a desktop browser...(finding it does not really work though). FIXME
    private final static String UA_DESKTOP = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

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
        //webSettings.setBuiltInZoomControls(true);
        //webSettings.setDisplayZoomControls(false);

        //Set defalts for caching...
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        //Set UA string to try to get desktop sites (again, doesn't really work). FIXME
        webSettings.setUserAgentString(UA_DESKTOP);

        //Set webview to our overriden class...
        view.setWebViewClient(new MyWebViewClient());

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
                mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_URL, null);
            } else {
                mbDontUpdate = false;
            }

            //and update favorites
            mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_FAVORITE, null);
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
