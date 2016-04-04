package com.example.rclark.simpleatvbrowser;

import android.app.Activity;
import android.app.Fragment;
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

    private WebView mWView;
    private boolean mbDontUpdate;

    //The UA string to convince websites we are a desktop browser...(finding it does not really work though). FIXME
    private final static String UA_DESKTOP = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

    OnMainActivytCallbackListener mCallback;
    //Put in an interface for container activity to implement so that fragment can deliver messages
    public interface OnMainActivytCallbackListener {
        //called by SearchbarFragment when a url is selected
        public void onMainActivityCallback(int code);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View retView = inflater.inflate(R.layout.webview_fragment, container, false);

        if (retView != null) {
            mWView = (WebView) retView.findViewById(R.id.webview);

            //initialize the web view
            initWebView(mWView);

            //process scroll events to show/hide searchbar
            mWView.setOnScrollChangeListener(new WebView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int nx, int ny, int ox, int oy) {
                    if (ny > 200) { //ut in a bit of a threshold...
                        mCallback.onMainActivityCallback(MainActivity.CALLBACK_HIDE_BAR);
                    } else if (ny == 0) {
                        mCallback.onMainActivityCallback(MainActivity.CALLBACK_SHOW_BAR);
                    }
                }
            });
        }

        return retView;
    }

    //
    //  Set the callback
    //
    @Override
    public void onAttach(Activity activity) {   //FIXME
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        // housekeeping function
        try {
            mCallback = (OnMainActivytCallbackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMainActivytCallbackListener");
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
                mCallback.onMainActivityCallback(MainActivity.CALLBACK_UPDATE_URL);
            } else {
                mbDontUpdate = false;
            }
        }
    }

}
