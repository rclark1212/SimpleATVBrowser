package com.example.rclark.simpleatvbrowser;

import android.content.Intent;
import android.graphics.Movie;
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

import java.util.ArrayList;

/**
 * Created by rclar on 4/4/2016.
 */
public class FavoritesFragment extends BrowseFragment {
    private static final String TAG = "FavoritesFragment";
    private ArrayObjectAdapter mRowsAdapter;
    public final static int MAX_COLUMNS = 5;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        setupUIElements();

        hackloadData();

        setupEventListeners();
    }

    private void hackloadData() {

        //hack in some data...
        ObjectDetail od1 = new ObjectDetail();
        ObjectDetail od2 = new ObjectDetail();
        od1.url = "www.nvidia.com";
        od2.url = "www.google.com";

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        HeaderItem header = new HeaderItem(0, "");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        listRowAdapter.add(od1);
        listRowAdapter.add(od2);
        mRowsAdapter.add(new ListRow(header, listRowAdapter));

        setAdapter(mRowsAdapter);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.favorites)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }


    private void setupEventListeners() {

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ObjectDetail) {
                ObjectDetail favorite = (ObjectDetail) item;
                Log.d(TAG, "Item: " + item.toString());
                /* kick off the browse...
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

}
