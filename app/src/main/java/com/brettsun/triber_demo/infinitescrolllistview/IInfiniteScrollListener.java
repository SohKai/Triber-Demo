package com.brettsun.triber_demo.infinitescrolllistview;

/**
 * Cloned from https://github.com/sedenardi/InfiniteScrollListView
 * Would be nice to incorporate into a library module instead of directly into this app, but
 * Android Studio isn't being nice  so this will have to do
 */
public interface IInfiniteScrollListener {
    public void endIsNear();

    // Item visibility code
    public void onScrollCalled(int firstVisibleItem, int visibleItemCount, int totalItemCount);
}
