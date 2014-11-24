package com.brettsun.triber_demo.infinitescrolllistview;

import android.widget.AbsListView;

/**
 * Cloned from https://github.com/sedenardi/InfiniteScrollListView
 * Would be nice to incorporate into a library module instead of directly into this app, but
 * Android Studio isn't being nice so this will have to do
 *
 * Slight modifications made to allow the scroll offset to be taken in as a parameter and
 * removed the checking function for new loads
 */
public class InfiniteScrollOnScrollListener implements AbsListView.OnScrollListener {

    private static final int DEFAULT_SCROLL_OFFSET = 2;
    private IInfiniteScrollListener listener;
    private final int scrollOffset;

    public InfiniteScrollOnScrollListener(IInfiniteScrollListener listener) {
        this(listener, DEFAULT_SCROLL_OFFSET);
    }

    public InfiniteScrollOnScrollListener(IInfiniteScrollListener listener, int scrollOffset) {
        this.listener = listener;
        this.scrollOffset = scrollOffset;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount - (firstVisibleItem + 1 + visibleItemCount) < scrollOffset &&
                visibleItemCount < totalItemCount) {
            listener.endIsNear();
        }

        // Item visibility code
        listener.onScrollCalled(firstVisibleItem, visibleItemCount, totalItemCount);
    }

}
