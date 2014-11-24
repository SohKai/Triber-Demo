package com.brettsun.triber_demo.instagram.api;

import android.util.Log;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Error class for error responses coming from Instagram's API
 */
public final class InstagramError {
    public static final int NO_STATUS_CODE = -1;

    private static final String TAG = InstagramError.class.getSimpleName();
    private static final String META_KEY = "meta";
    private static final String ERROR_TYPE_KEY = "error_type";
    private static final String CODE_KEY = "code";
    private static final String ERROR_MESSAGE_KEY = "error_message";
    private static final String OAUTH_ERROR_TYPE_VALUE = "OAuthAccessTokenException";

    private String mErrorType;
    private String mErrorMessage;
    private int mStatusCode = NO_STATUS_CODE;

    /**
     * Parse a given top level JSON response from the API for any errors that are in the meta object
     * @param jsonResponse Top level JSON response from Instagram's API
     * @return An InstagramError with details if an error exists, otherwise null.
     */
    public static InstagramError parseFromApiResponse(final JSONObject jsonResponse) {
        InstagramError error = null;
        try {
            final JSONObject jsonMeta = jsonResponse.getJSONObject(META_KEY);
            int responseCode = jsonMeta.getInt(CODE_KEY);
            if (2 != (responseCode / 100)) {
                // Not successful return code in meta; found API error
                final String errorType = jsonMeta.optString(ERROR_TYPE_KEY, null);
                final String errorMessage = jsonMeta.optString(ERROR_MESSAGE_KEY, null);
                error = new InstagramError(responseCode, errorType, errorMessage);
            }
        } catch (JSONException jsonex) {
            Log.e(TAG, "API json response parsing failed at meta code verification: " + jsonex.getMessage());
            jsonex.printStackTrace();
            error = new InstagramError(InstagramError.NO_STATUS_CODE, "JSONException", jsonex.getMessage());
        }
        return error;
    }

    /**
     * Parse a VolleyError into an InstagramError. The two may not always map directly to each other,
     * so the returned InstagramError may not have all it's fields set.
     * @param error VolleyError to parse into an InstagramError
     * @return Always returns an InstagramError with details from the VolleyError.
     */
    public static InstagramError parseFromVolleyError(final VolleyError error) {
        int statusCode = InstagramError.NO_STATUS_CODE;
        if (null != error.networkResponse) {
            statusCode = error.networkResponse.statusCode;
            if (null != error.networkResponse.data) {
                // Try to parse the data into an Instagram API error
                try {
                    final String errorString = new String(error.networkResponse.data, "UTF-8");
                    final JSONObject errorJsonObj = new JSONObject(errorString);
                    return parseFromApiResponse(errorJsonObj);
                } catch (IOException ioex) {
                    Log.e(TAG, "Could not parse volley error data into String for conversion " +
                            "into InstagramApiError");
                    ioex.printStackTrace();
                } catch (JSONException jsonex) {
                    Log.e(TAG, "Could not parse volley error string into JSONObject for " +
                            "conversion into InstagramApiError");
                }
            }
        }
        return new InstagramError(statusCode, null, error.getMessage());
    }

    public String getErrorType() { return mErrorType; }
    public String getErrorMessage() { return mErrorMessage; }
    public int getStatusCode() { return mStatusCode; }

    /**
     * Check if this InstagramError was due to an OAuth token error.
     * @return True if the error was caused by an OAuth token error.
     */
    public boolean isOAuthInvalidError() {
        // OAuth errors are returned as 4xx status codes and have type 'OAuthAccessTokenException'
        return (4 == (mStatusCode / 100)) && OAUTH_ERROR_TYPE_VALUE.equals(mErrorType);
    }

    InstagramError(int code, String errorType, String errorMessage) {
        mStatusCode = code;
        mErrorType = errorType;
        mErrorMessage = errorMessage;
    }

}
