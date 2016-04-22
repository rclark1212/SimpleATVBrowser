/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.rclark.simpleatvbrowser.data.FavContract;

import java.util.ArrayList;

/**
 * Created by rclark on 4/4/2016.
 * Taken from google android tv sample code and modified...
 */
public class FavoritesFragment extends BrowseFragment {
    private static final String TAG = "FavoritesFragment";
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayList<ObjectDetail> mObjects;
    private int mRow;

    public final static int MAX_COLUMNS = 4;

    OnMainActivityCallbackListener mCallback;
    //Put in an interface for container activity to implement so that fragment can deliver messages
    public interface OnMainActivityCallbackListener {
        //called by FavoritesFragment when a url is selected
        public void onMainActivityCallback(int code, String url);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        setupUIElements();

        loadData();

        setupEventListeners();
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
        Returns the row
     */
    public int getRow() {
        return mRow;
    }

    /*
        Loads data from content provider into the browsefragment
     */
    private void loadData() {

        /*
            GRRR - hate doing this.
            Really should be a content adapter (CursorObjectAdapter).
            The problem is that the browsefragment is too rigid. There is no way to control the length of
            the rows if using cursor adapter. So for this application, it kind of sucks.
            Tried a gridview and while it works, it obviously is not built for TV (then again, browser is not).
            But really want to use a BrowseFragment. And I want to use the standard one - not a modified one that might break in future.
            So lets hold our noses and manually mod the rows to keep row length sane and do it by _uggg_ _uggg_ _uggg_ preloading data
            out of the content provider.
            Only saving grace is there is no way CP is being updated behind our back. So functionality should not suffer.
         */
        //create backing array
        mObjects = new ArrayList<ObjectDetail>();

        //Get the favorites DB reference...
        Uri favoriteDB = FavContract.FavoritesEntry.CONTENT_URI;

        Cursor c = getActivity().getContentResolver().query(favoriteDB, null, null, null, null);

        //now just suck up the data...
        if (c.getCount() > 0) {
            c.moveToFirst();

            while (!c.isAfterLast()) {
                //create the backing object
                ObjectDetail od = new ObjectDetail();

                //grab the data
                od.url = c.getString(c.getColumnIndex(FavContract.FavoritesEntry.COLUMN_FAVORITES_URL));
                od.title = c.getString(c.getColumnIndex(FavContract.FavoritesEntry.COLUMN_FAVORITE_TITLE));
                od.httpedit = c.getString(c.getColumnIndex(FavContract.FavoritesEntry.COLUMN_FAVORITE_HTTP));
                byte[] blob = c.getBlob(c.getColumnIndex(FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB));
                Bitmap bitMapImage = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                od.thumb = new BitmapDrawable(bitMapImage);

                //add the object
                mObjects.add(od);

                //and go to next
                c.moveToNext();
            }
        }

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        //and shove it into object adapter - ugg...
        for (int row = 0; row <= (mObjects.size()/MAX_COLUMNS); row++) {
            HeaderItem header = new HeaderItem(row, "");
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            for (int col = 0; col < MAX_COLUMNS; col ++) {
                int i = row*MAX_COLUMNS + col;
                if (i >= mObjects.size()) {
                    break;
                }
                listRowAdapter.add(mObjects.get(i));
            }
            //add row header and listRowAdapter
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(mRowsAdapter);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.favorites)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        //FIXME - this is not working
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        //setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));

        //setBadgeDrawable(getResources().getDrawable(R.drawable.www));
    }


    private void setupEventListeners() {

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ObjectDetail) {
                ObjectDetail favorite = (ObjectDetail) item;
                Log.d(TAG, "Item: " + item.toString());
                //kick off the browse
                mCallback.onMainActivityCallback(MainActivity.CALLBACK_LOAD_FAVORITE, favorite.url);
                /* intent example code...
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle); */

            } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
            }
        }
    }

    /*
        Use to track selected row
     */
    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mRow = (int) row.getHeaderItem().getId();
        }
    }

}
