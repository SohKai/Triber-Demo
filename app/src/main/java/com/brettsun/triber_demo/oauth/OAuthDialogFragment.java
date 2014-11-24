package com.brettsun.triber_demo.oauth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * OAuth authentication dialog. Holds a webview that will catch redirects and return responses from
 * the OAuth service to the OAuthManagers.
 *
 * Should only be used from an OAuthManager.
 */
public class OAuthDialogFragment extends DialogFragment {
    private static final String TAG = OAuthDialogFragment.class.getSimpleName();
    private static final String OAUTH_SETTINGS_KEY = "com.brettsun.triber_demo.OAuthSettings";

    // Listener interface for the dialog handler to respond to authentication replies or webview failures
    interface OAuthDialogListener {
        void onReply(final String redirectrl);
        void onError(final int errorCode, final String description, final String failingUrl);
    }

    private OAuthDialogListener mListener;

    /**
     * Factory for creating the OAuthDialog fragment so we can pass in our OAuth service settings.
     * Should only be used by an OAuthManager.
     * @param settings Settings for the OAuth dialog, such as endpoint, redirectUrl, etc.
     */
    static OAuthDialogFragment newInstance(final OAuthSettings settings, final OAuthDialogListener listener) {
        Log.i(TAG, "Creating new OAuth fragment for endpoint: " + settings.getOAuthEndpoint());
        OAuthDialogFragment dialogFrag = new OAuthDialogFragment();
        dialogFrag.setListener(listener);

        // Give the dialog our OAuth settings
        Bundle args = new Bundle();
        args.putParcelable(OAUTH_SETTINGS_KEY, settings);
        dialogFrag.setArguments(args);

        return dialogFrag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final OAuthSettings settings = getArguments().getParcelable(OAUTH_SETTINGS_KEY);
        WebView oAuthWebView = createWebViewFromSettings(settings);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Log into " + settings.getService())
                     .setView(oAuthWebView);

        return dialogBuilder.create();
    }

    private WebView createWebViewFromSettings(final OAuthSettings settings) {
        // Create webview with this override to allow the keyboard to appear
        // See second answer of http://stackoverflow.com/questions/4200259/tapping-form-field-in-webview-does-not-show-soft-keyboard/4977247#4977247
        WebView webView = new WebView(getActivity()) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true;
            }
        };

        // Set a WebViewClient to handle the redirect url
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(settings.getRedirectUrl())) {
                    Log.i(TAG, "OAuth service replied on redirect url: " + settings.getRedirectUrl());
                    if (null != mListener) {
                        // Let the listener handle how to parse the url, as different services
                        // may have their own implementations
                        mListener.onReply(url);
                    }

                    // Dismiss the dialog and stop the webview from loading the url
                    dismiss();
                    return true;
                }

                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.i(TAG, "OAuth webview received error code: " + errorCode + " with description: " + description
                                + " on failing url: " + failingUrl);
                if (mListener != null) {
                    mListener.onError(errorCode, description, failingUrl);
                }
                // Dismiss the dialog when the request fails so the listener can choose how to respond
                dismiss();
            }
        });

        Log.i(TAG, "Loading OAuth endpoint: " + settings.getOAuthEndpoint());
        webView.loadUrl(settings.getOAuthEndpoint());
        return webView;
    }

    private void setListener(final OAuthDialogListener listener) {
        mListener = listener;
    }

}
