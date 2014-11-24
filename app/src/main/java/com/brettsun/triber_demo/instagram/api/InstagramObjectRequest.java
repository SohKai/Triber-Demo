package com.brettsun.triber_demo.instagram.api;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Instagram API request that will return a JSONArray back to the listener.
 * Most functionality is provided through the base class, InstagramRequest.
 */
public class InstagramObjectRequest extends InstagramRequest<JSONObject> {
    private static final String TAG = InstagramObjectRequest.class.getSimpleName();

    // For Parcelable
    public static final Creator<InstagramObjectRequest> CREATOR = new Creator<InstagramObjectRequest>() {
        @Override
        public InstagramObjectRequest createFromParcel(Parcel src) {
            return new InstagramObjectRequest(src.readInt(),       // method
                    src.readString(),    // paginationUrl
                    src.readString());   // url
        }
        @Override
        public InstagramObjectRequest[] newArray(int size) {
            return new InstagramObjectRequest[size];
        }
    };

    static InstagramObjectRequest send(final Context context, final int method, final String url,
                                      final JSONObject jsonPost, final InstagramListener<JSONObject> listener) {
        InstagramObjectRequest request = new InstagramObjectRequest(method);
        request.sendRequest(context, url, jsonPost, listener);
        return request;
    }

    protected JSONObject getData(JSONObject response) {
        try {
            return response.getJSONObject(DATA_KEY);
        } catch (JSONException jsonex) {
            Log.e(TAG, "API JSON object response parsing failed at obtaining data field: " + jsonex.getMessage());
            jsonex.printStackTrace();
        }
        return null;
    }

    private InstagramObjectRequest(final int method) {
        super(TAG, method, null, null);
    }

    private InstagramObjectRequest(final int method, final String paginationUrl, final String url) {
        super(TAG, method, paginationUrl, url);
    }


}
