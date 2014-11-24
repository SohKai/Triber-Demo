package com.brettsun.triber_demo.instagram.api;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Instagram API handler
 *
 * All requests to the Instagram API should be made through this class.
 * InstagramRequests are returned back to the client for pagination and retries.
 */
public final class InstagramApi {
    // API keys
    private static final String INSTAGRAM_CLIENT_ID = "211558673bd34c35a0b428609b98dfd7";

    // API endpoints
    private static final String USER_QUERY_ENDPOINT = "https://api.instagram.com/v1/users/search";
    private static final String USER_RECENT_MEDIA_ENDPOINT = "https://api.instagram.com/v1/users/USER_ID_PARAM/media/recent/";
    private static final String USER_SELF_DETAILS_ENDPOINT = "https://api.instagram.com/v1/users/self";

    // API parameters
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String CLIENT_ID_KEY = "client_id";
    private static final String COUNT_PARAM_KEY = "count";
    private static final int COUNT_DEFAULT_VALUE = 5;
    private static final int COUNT_INFINITE_VALUE = -1;
    private static final String USER_ID_PARAM_KEY = "USER_ID_PARAM";
    private static final String USER_QUERY_PARAM_KEY = "q";

    /***** Send API requests *****/
    /**
     * Request a user search based on the query string given.
     * @param query Search query for users
     */
    public static InstagramArrayRequest sendSearchRequest(final Context context,
                    final String query, final InstagramListener<JSONArray> listener) {
        // It turns out Instagram's user search API doesn't support pagination...
        // We'll pull as much as we can, but the API will only give us at most 50
        // responses anyway
        final String apiUrl = createUserSearchUrl(query, COUNT_INFINITE_VALUE);
        return InstagramArrayRequest.send(context, Request.Method.GET, apiUrl, null, listener);
    }

    /**
     * Request recent media uploads by the given user
     * @param userId Id of user to to pull recent uploads from
     */
    public static InstagramArrayRequest sendRecentMediaRequest(final Context context,
                    final int userId, final InstagramListener<JSONArray> listener) {
        final String apiUrl = createRecentMediaUrl(userId, COUNT_DEFAULT_VALUE);
        return InstagramArrayRequest.send(context, Request.Method.GET, apiUrl, null, listener);
    }

    /**
     * Request user details of the authenticated user
     * @param accessToken OAuth access token of the authenticated user
     */
    public static InstagramObjectRequest sendUserSelfDetailsRequest(final Context context,
                    final String accessToken, final InstagramListener<JSONObject> listener) {
        final String apiUrl = createUserSelfDetailsUrl(accessToken);
        return InstagramObjectRequest.send(context, Request.Method.GET, apiUrl, null, listener);
    }

    /***** Build URLs for the APIs *****/
    static String createUserSearchUrl(final String query, final int itemCount) {
        final Uri.Builder uriBuilder = Uri.parse(USER_QUERY_ENDPOINT).buildUpon();
        uriBuilder.appendQueryParameter(USER_QUERY_PARAM_KEY, query);
        appendCountParameterToUri(uriBuilder, itemCount);
        appendAccessParameterToUri(uriBuilder);
        return uriBuilder.build().toString();
    }

    static String createRecentMediaUrl(final int userId, final int itemCount) {
        final String userUrl = USER_RECENT_MEDIA_ENDPOINT.replace(USER_ID_PARAM_KEY, Integer.toString(userId));
        final Uri.Builder uriBuilder = Uri.parse(userUrl).buildUpon();
        appendCountParameterToUri(uriBuilder, itemCount);
        appendAccessParameterToUri(uriBuilder);
        return uriBuilder.build().toString();
    }

    static String createUserSelfDetailsUrl(final String accessToken) {
        final Uri.Builder uriBuilder = Uri.parse(USER_SELF_DETAILS_ENDPOINT).buildUpon();
        uriBuilder.appendQueryParameter(ACCESS_TOKEN_KEY, accessToken);
        return uriBuilder.build().toString();
    }

    /***** Append parameters to the API Urls *****/
    static void appendCountParameterToUri(final Uri.Builder uriBuilder, final int itemCount) {
        // If we do not append a count key, the API will give us back as much data as it can
        if (COUNT_INFINITE_VALUE != itemCount) {
            uriBuilder.appendQueryParameter(COUNT_PARAM_KEY, Integer.toString(itemCount));
        }
    }

    /**
     * Append an API access parameter to the URL builder.
     * We can use either our client_id or an authenticated user's access_token as the access
     * parameter.
     *
     * Once a user has logged in, prefer to use the oauth access_token associated with their
     * session than the Instagram client_id for this app
     */
    static void appendAccessParameterToUri(final Uri.Builder uriBuilder) {
        final String oauthToken = InstagramOAuthManager.getInstance().getAccessToken();
        if (null != oauthToken) {
            uriBuilder.appendQueryParameter(ACCESS_TOKEN_KEY, oauthToken);
        } else {
            appendClientIdParameterToUri(uriBuilder);
        }
    }

    static void appendClientIdParameterToUri(final Uri.Builder uriBuilder) {
        uriBuilder.appendQueryParameter(CLIENT_ID_KEY, INSTAGRAM_CLIENT_ID);
    }

    /**
     * Update the given url to use the currently available access parameter.
     * This is useful in cases where a user may have logged out during pagination or had their
     * OAuth token revoked.
     * @param oldUrl URL to update with the currently available access parameter
     */
    static String updateAccessParameter(final String oldUrl) {
        final Uri uri = Uri.parse(oldUrl);
        final Uri.Builder newUrlBuilder = uri.buildUpon();
        newUrlBuilder.clearQuery();

        // Add each parameter except for the access parameters
        for (String paramName : uri.getQueryParameterNames()) {
            if (!(paramName.equals(ACCESS_TOKEN_KEY) || paramName.equals(CLIENT_ID_KEY))) {
                newUrlBuilder.appendQueryParameter(paramName, uri.getQueryParameter(paramName));
            }
        }

        appendAccessParameterToUri(newUrlBuilder);
        return newUrlBuilder.build().toString();
    }

}
