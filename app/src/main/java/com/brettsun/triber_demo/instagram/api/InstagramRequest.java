package com.brettsun.triber_demo.instagram.api;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.brettsun.triber_demo.VolleyHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base type of the Instagram API request classes.
 * Similar to how Volley has its JsonObjectRequest and JsonArrayRequest derive from a generic
 * JsonRequest, we do the same for Instagram's API request classes so clients can obtain either
 * JSONObjects or JSONArrays back to parse.
 *
 * API responses are preprocessed for errors in the meta object and to automatically register
 * pagination on the request for future loading. In this model, a single InstagramRequest is
 * responsible for its entire set of pages.
 *
 * Tags all volley requests with the given context to allow for cancellation when the context
 * is destroyed.
 *
 * @param <T> Must be either JSONObject or JSONArray
 */
public abstract class InstagramRequest<T> implements Parcelable {
    private static final String PAGINATION_KEY = "pagination";
    private static final String NEXT_URL_KEY = "next_url";
    private static final int PAGINATION_ITEM_COUNT = 10;
    protected static final String DATA_KEY = "data";

    private final String TAG;
    private final int mMethod;
    private String mPaginationUrl;
    private String mUrl;
    private JSONObject mJsonPost;
    private InstagramListener<T> mListener;

    /**
     * Check if current request has pagination.
     */
    public boolean hasPagination() {
        return null != mPaginationUrl;
    }

    /**
     * Request next page of data in the set this InstagramRequest is responsible for
     */
    public void requestNextPage(final Context context, final JSONObject jsonPost,
                                    final InstagramListener<T> listener) {
        Log.i(TAG, "Requesting next page: " + mPaginationUrl);
        final Uri.Builder pageUriBuilder = Uri.parse(mPaginationUrl).buildUpon();
        InstagramApi.appendCountParameterToUri(pageUriBuilder, PAGINATION_ITEM_COUNT);

        // Clear pagination so clients do not think we have more pages before we load the next page
        mPaginationUrl = null;
        sendRequest(context, pageUriBuilder.build().toString(), jsonPost, listener);
    }

    /**
     * Retry the previous request with the same parameters
     * Note that the access parameter will be updated automatically in case the OAuth token
     * is expired or revoked.
     */
    public void retry(final Context context) {
        // Update the API access parameter in case we failed due to an invalid OAuth key
        final String updatedUrl = InstagramApi.updateAccessParameter(mUrl);
        Log.i(TAG, "Retrying last request with: " + updatedUrl);
        sendRequest(context, updatedUrl, mJsonPost, mListener);
    }

    protected InstagramRequest(final String tag, final int method,
                                    final String paginationUrl, final String url) {
        TAG = tag;
        mMethod = method;
        mPaginationUrl = paginationUrl;
        mUrl = url;
    }

    protected void sendRequest(final Context context, final String url, final JSONObject jsonPost,
                                    final InstagramListener<T> listener) {
        mUrl = url;
        mJsonPost = jsonPost;
        mListener = listener;
        JsonObjectRequest request = makeRequest(url, jsonPost, listener);

        request.setTag(context);       // Use the given context as the tag to allow for cancellation
        VolleyHandler volleyHandler = VolleyHandler.getInstance(context);
        volleyHandler.addToRequestQueue(request);
        Log.i(TAG, "Requesting API: " + url);
    }

    private JsonObjectRequest makeRequest(final String url, final JSONObject jsonPost,
                                          final InstagramListener<T> listener) {
        // Wrap the original listener so we can preprocess the API response
        return new JsonObjectRequest(mMethod, url, jsonPost,
                new ListenerWrapper(listener), new ErrorListenerWrapper(listener));
    }

    protected abstract T getData(JSONObject response);

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMethod);
        dest.writeString(mPaginationUrl);
        dest.writeString(mUrl);
    }

    // Wrapper listeners so we can do some preprocessing on the response before giving it to the client
    // Checks for errors, registers pagination, and then strips out just the data field to give
    // back to the client.
    private class ListenerWrapper implements Response.Listener<JSONObject> {
        private final InstagramListener<T> mListener;

        ListenerWrapper(final InstagramListener<T> listener) { mListener = listener; }

        @Override
        public void onResponse(final JSONObject response) {
            InstagramError apiError = InstagramError.parseFromApiResponse(response);
            if (null != apiError) {
                // Meta does not indicate success; respond with error
                mListener.onErrorResponse(apiError);
                return;
            }
            mPaginationUrl = getPaginationUrl(response);
            mListener.onResponse(getData(response));
        }

        private String getPaginationUrl(final JSONObject jsonResponse) {
            try {
                if (jsonResponse.has(PAGINATION_KEY)) {
                    final JSONObject jsonPagination = jsonResponse.getJSONObject(PAGINATION_KEY);
                    if (jsonPagination.has(NEXT_URL_KEY)) {
                        return jsonPagination.getString(NEXT_URL_KEY);
                    }
                }
            } catch (JSONException jsonex) {
                Log.e(TAG, "API json response parsing failed at obtaining pagination: " + jsonex.getMessage());
                jsonex.printStackTrace();
            }
            return null;
        }
    }

    private class ErrorListenerWrapper implements Response.ErrorListener {
        private final InstagramListener<T> mErrorListener;
        ErrorListenerWrapper(final InstagramListener<T> errorListener) { mErrorListener = errorListener; }

        @Override
        public void onErrorResponse(final VolleyError error) {
            mErrorListener.onErrorResponse(InstagramError.parseFromVolleyError(error));
        }
    }

}