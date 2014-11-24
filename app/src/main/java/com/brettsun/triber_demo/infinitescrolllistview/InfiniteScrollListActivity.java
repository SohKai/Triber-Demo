package com.brettsun.triber_demo.infinitescrolllistview;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;

/**
 * Base class for ListActivities who want to be able to scroll infinitely
 *
 * Would be nice to incorporate into a library module instead of directly into this app, but
 * Android Studio isn't being nice so this will have to do
 */
public abstract class InfiniteScrollListActivity extends ListActivity implements IInfiniteScrollListener {
    private static final String TAG = InfiniteScrollListActivity.class.getSimpleName();

    private View mFooterView;

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // We cannot do this in the onCreate method because we need to grab the ListView from the
        // inflated view, and a subclass will only call setContentView() after calling it's
        // super.onCreate().
        setupInfiniteScroll();
    }

    // Must be called before setListAdapter for the footer to show properly
    public void setLoadingFooter(View view) {
        mFooterView = view;
        getListView().addFooterView(mFooterView);
    }

    @Override
    public void setListAdapter(ListAdapter listAdapter) {
        Log.i(TAG, "InfiniteScrollAdapter set as list adapter");
        if (!canLoadMore()) {
            // No more data to load, so disable infinite scrolling
            disableInfiniteScroll();
        }
        super.setListAdapter(listAdapter);
    }

    protected void setupInfiniteScroll() {
        Log.i(TAG, "Infinite scroll enabled");
        final InfiniteScrollOnScrollListener scrollListener = new InfiniteScrollOnScrollListener(this);
        getListView().setOnScrollListener(scrollListener);
    }

    protected void disableInfiniteScroll() {
        // Remove the footer and scroll listener that will give end of list callbacks and
        getListView().removeFooterView(mFooterView);
        getListView().setOnScrollListener(null);
        Log.i(TAG, "Infinite scroll disabled");
    }

    protected abstract boolean canLoadMore();

}
