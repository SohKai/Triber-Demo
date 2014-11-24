package com.brettsun.triber_demo.instagram.api;

import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.brettsun.triber_demo.instagram.InstagramUser;
import com.brettsun.triber_demo.oauth.OAuthError;
import com.brettsun.triber_demo.oauth.OAuthManager;
import com.brettsun.triber_demo.oauth.OAuthSettings;

import org.json.JSONObject;

/**
 * OAuth manager singleton for Instagram.
 * All OAuth authorization and logout should be made through this class
 */
public final class InstagramOAuthManager extends OAuthManager<InstagramOAuthUser> {
    private static final String TAG = InstagramOAuthManager.class.getSimpleName();
    private static InstagramOAuthManager mInstance;

    // Instagram Oauth API
    // Note that we use the implicit authentication because we should never ship our client secret
    // (http://instagram.com/developer/authentication/)
    private static final String OAUTH_ENDPOINT;     // initialized in static block
    private static final String OAUTH_REDIRECT_URL = "http://localhost/instagram_oauth_callback";
    private static final String OAUTH_SERVICE_NAME = "Instagram";
    private static final String ACCESS_TOKEN_PREFS_KEY = "com.brettsun.triber_demo.InstagramOauthManager.AccessTokenPrefs";

    // For parsing Instagram's OAuth response
    private static final String RESP_ACCESS_TOKEN_KEY = "access_token=";
    private static final String RESP_ERROR = "error";
    private static final String RESP_ERROR_REASON = "error_reason";
    private static final String RESP_ERROR_DESCRIPTION = "error_description";
    private static final String ERROR_USER_DENIED = "user_denied";

    static {
        final String oauthPath = "https://instagram.com/oauth/authorize/";
        final String oauthRedirectUriParam = "redirect_uri";
        final String oauthResponseParam = "response_type";
        final String oauthResponseKey = "token";
        final Uri.Builder oauthUriBuilder = Uri.parse(oauthPath).buildUpon();
        oauthUriBuilder.appendQueryParameter(oauthRedirectUriParam, OAUTH_REDIRECT_URL)
                       .appendQueryParameter(oauthResponseParam, oauthResponseKey);
        InstagramApi.appendClientIdParameterToUri(oauthUriBuilder);

        OAUTH_ENDPOINT = oauthUriBuilder.build().toString();
    }

    public static synchronized InstagramOAuthManager getInstance() {
        if (mInstance == null) {
            mInstance = new InstagramOAuthManager();
        }
        return mInstance;
    }

    /**
     * Authenticate the user through OAuth. Opens a dialog with a webview to Instagram's OAuth service.
     * @param fragManager FragmentManager of the current Activity to load the Dialog into
     */
    public void authenticate(final FragmentManager fragManager, final TokenListener listener) {
        final OAuthSettings settings = new OAuthSettings(OAUTH_ENDPOINT, OAUTH_REDIRECT_URL, OAUTH_SERVICE_NAME);
        handleAuthentication(fragManager, settings, listener);
    }

    /**
     * Check if the OAuthError was from the user denying access
     */
    public boolean didUserDeny(final OAuthError error) {
        return null != error && error.errorReason.equals(ERROR_USER_DENIED);
    }

    /**
     * Get the currently authenticated user's details through the API.
     * Returns a wrapped instance of InstagramUser to the listener that also provides the user's
     * access token
     */
    public void getUser(final Context context, final UserListener<InstagramOAuthUser> listener) {
        InstagramApi.sendUserSelfDetailsRequest(context, getAccessToken(), new InstagramListener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                Log.i(TAG, "JSON request for logged in user succeeded");
                listener.onSuccess(new InstagramOAuthUser(getAccessToken(),
                                            InstagramUser.parseUserFromJson(response)));
            }
            @Override
            public void onErrorResponse(final InstagramError error) {
                listener.onError(new OAuthError(error.getStatusCode(), error.getErrorType(),
                                        error.getErrorType(), error.getErrorMessage()));
            }
        });
    }

    protected void parseResponse(final String url, final TokenListener listener) {
        final Uri uri = Uri.parse(url);
        final String accessFragment = uri.getFragment();
        if (null != accessFragment && accessFragment.startsWith(RESP_ACCESS_TOKEN_KEY)) {
            // Has the access token parameter
            final String accessToken = accessFragment.substring(RESP_ACCESS_TOKEN_KEY.length());
            storeLoggedInUser(accessToken);
            listener.onSuccess(accessToken);
        } else {
            // Otherwise it must be an error response
            final String error = uri.getQueryParameter(RESP_ERROR);
            final String errorReason = uri.getQueryParameter(RESP_ERROR_REASON);
            final String description = uri.getQueryParameter(RESP_ERROR_DESCRIPTION);
            listener.onError(new OAuthError(OAuthError.NO_STATUS_CODE, error, errorReason, description));
        }
    }

    private InstagramOAuthManager() {
        super(TAG, ACCESS_TOKEN_PREFS_KEY, OAUTH_SERVICE_NAME);
    }
}
