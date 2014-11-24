package com.brettsun.triber_demo.instagram.api;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Instagram API request that will return a JSONArray back to the listener.
 * Most functionality is provided through the base class, InstagramRequest.
 */
public final class InstagramArrayRequest extends InstagramRequest<JSONArray> {
    private static final String TAG = InstagramArrayRequest.class.getSimpleName();

    // For Parcelable
    public static final Creator<InstagramArrayRequest> CREATOR = new Creator<InstagramArrayRequest>() {
        @Override
        public InstagramArrayRequest createFromParcel(Parcel src) {
            return new InstagramArrayRequest(src.readInt(),       // method
                                           src.readString(),    // paginationUrl
                                           src.readString());   // url
        }
        @Override
        public InstagramArrayRequest[] newArray(int size) {
            return new InstagramArrayRequest[size];
        }
    };

    static InstagramArrayRequest send(final Context context, final int method, final String url,
                                        final JSONObject jsonPost, final InstagramListener<JSONArray> listener) {
        InstagramArrayRequest request = new InstagramArrayRequest(method);
        request.sendRequest(context, url, jsonPost, listener);
        return request;
    }

    protected JSONArray getData(JSONObject response) {
        try {
            return response.getJSONArray(DATA_KEY);
        } catch (JSONException jsonex) {
            Log.e(TAG, "API JSON array response parsing failed at obtaining data field: " + jsonex.getMessage());
            jsonex.printStackTrace();
        }
        return null;
    }

    private InstagramArrayRequest(final int method) {
        super(TAG, method, null, null);
    }

    private InstagramArrayRequest(final int method, final String paginationUrl, final String url) {
        super(TAG, method, paginationUrl, url);
    }

}