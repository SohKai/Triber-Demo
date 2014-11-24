package com.brettsun.triber_demo.oauth;

import android.app.FragmentManager;
import android.content.Context;
import android.util.Log;

import com.brettsun.triber_demo.SharedPrefUtils;

/**
 * Generic OAuth manager implementation. Handles OAuth authentication through a webview inside a dialog.
 * Currently is tied to this application's default shared preferences to store the authenticated user's
 * access tokens, but could allow subclasses to choose their own storage types.
 *
 * @param <T> Type of user that this OAuth manager handles. This will be the type returned to the client
 *            on getUser();
 */
public abstract class OAuthManager<T> {
    private static final String OAUTH_DIALOG_TAG = "com.brettsun.triber_demo.OAuthDialog";

    private final String TAG;
    private final String mAccessTokenPrefsKey;
    private final String mServiceName;

    private String mAccessToken;

    // Listener interface for clients to respond to authentication success or failures
    public interface TokenListener {
        public void onSuccess(final String accessToken);
        public void onError(final OAuthError error);
    }

    // Listener interface for clients to get the currently authenticated user
    public interface UserListener<T> {
        public void onSuccess(final T user);
        public void onError(final OAuthError error);
    }

    /**
     * Return the current access token
     * @return The current access token. If there is no authenticated user, returns null.
     */
    public String getAccessToken() {
        if (null == mAccessToken) {
            mAccessToken = SharedPrefUtils.getString(mAccessTokenPrefsKey, null);
        }
        return mAccessToken;
    }

    /**
     * Check if there is currently an authenticated user
     */
    public boolean hasLoggedInUser() {
        return (null != mAccessToken) || SharedPrefUtils.contains(mAccessTokenPrefsKey);
    }

    /**
     * Logs the authenticated user, if any, out.
     */
    public void logout() {
        // To logout we just need to remove the access token we have from shared preferences
        SharedPrefUtils.remove(mAccessTokenPrefsKey);
        mAccessToken = null;
        Log.i(TAG, "Logged user out of " + mServiceName);
    }

    protected OAuthManager(final String tag, final String accessTokenPrefsKey, final String serviceName) {
        TAG = tag;
        mAccessTokenPrefsKey = accessTokenPrefsKey;
        mServiceName = serviceName;
    }

    protected void handleAuthentication(final FragmentManager fragManager, final OAuthSettings settings,
                                        final TokenListener listener) {
        final String curAccessToken = getAccessToken();
        if (null != curAccessToken) {
            listener.onSuccess(curAccessToken);
        } else {
            createOAuthDialog(fragManager, settings, listener);
        }
    }

    protected void storeLoggedInUser(final String accessToken) {
        SharedPrefUtils.putString(mAccessTokenPrefsKey, accessToken);
        mAccessToken = accessToken;
        Log.i(TAG, "Logged user into: " + mServiceName);
    }

    private void createOAuthDialog(final FragmentManager fragManager, final OAuthSettings settings,
                                   final TokenListener listener) {
        Log.i(TAG, "Showing new OAuthDialog");

        /**
         * Wrap the given client listener with the dialog's, so we can let the OAuthManager parse
         * the response itself.
         * This is necessary as different OAuth services may have different implementations that
         * have their own response formats for successes and errors.
         */
        OAuthDialogFragment oAuthDialog = OAuthDialogFragment.newInstance(settings,
                new OAuthDialogFragment.OAuthDialogListener() {
                    @Override
                    public void onReply(String url) {
                        // Let the subclass handle evaluating the response
                        parseResponse(url, listener);
                    }

                    @Override
                    public void onError(int errorCode, String description, String failingUrl) {
                        final String errorReason = "Webview failed to load: " + failingUrl;
                        listener.onError(new OAuthError(errorCode, errorReason, errorReason, description));
                    }
                });
        oAuthDialog.show(fragManager, OAUTH_DIALOG_TAG);
    }

    public abstract void authenticate(final FragmentManager fragManager, final TokenListener listener);
    public abstract boolean didUserDeny(final OAuthError error);
    public abstract void getUser(final Context context, final UserListener<T> listener);
    protected abstract void parseResponse(final String url, final TokenListener listener);

}
