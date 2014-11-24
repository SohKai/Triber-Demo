package com.brettsun.triber_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.Volley;

/**
 * Utility singleton for accessing Volley
 * Adapted from http://developer.android.com/training/volley/requestqueue.html
 */
public final class VolleyHandler {
    private static VolleyHandler mInstance;
    private static Context mAppContext;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    public static synchronized VolleyHandler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyHandler(context);
        }
        return mInstance;
    }

    public void cancelAll(Context context) {
        // Cancel all requests made from this context
        if (mRequestQueue != null) { mRequestQueue.cancelAll(context); }
    }

    public ImageLoader getImageLoader() {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(getRequestQueue(), new LruBitmapCache(mAppContext));
        }
        return mImageLoader;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mAppContext);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    private VolleyHandler(Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mAppContext = context.getApplicationContext();
        mRequestQueue = getRequestQueue();
        mImageLoader = getImageLoader();
    }

    // In memory LRU cache for the ImageLoader
    // Adapted from http://developer.android.com/training/volley/request.html
    private static class LruBitmapCache extends LruCache<String, Bitmap>
            implements ImageCache {

        public LruBitmapCache(int maxSize) {
            super(maxSize);
        }

        public LruBitmapCache(Context ctx) {
            this(getCacheSize(ctx));
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }

        @Override
        public Bitmap getBitmap(String url) {
            return get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }

        // Returns a cache size equal to approximately three screens worth of images.
        public static int getCacheSize(Context ctx) {
            final DisplayMetrics displayMetrics = ctx.getResources().
                    getDisplayMetrics();
            final int screenWidth = displayMetrics.widthPixels;
            final int screenHeight = displayMetrics.heightPixels;
            // 4 bytes per pixel
            final int screenBytes = screenWidth * screenHeight * 4;

            return screenBytes * 3;
        }
    }
}