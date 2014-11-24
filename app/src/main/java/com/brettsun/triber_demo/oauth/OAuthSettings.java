package com.brettsun.triber_demo.oauth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Settings passed to the OAuthDialog to configure its webview for the OAuth service we give it.
 */
public class OAuthSettings implements Parcelable {

    // For Parcelable
    public static final Creator<OAuthSettings> CREATOR = new Creator<OAuthSettings>() {
        @Override
        public OAuthSettings createFromParcel(Parcel src) {
            return new OAuthSettings(src.readString(),      // oAuthEndpoint
                                     src.readString(),      // redirectUrl
                                     src.readString());     // service
        }

        @Override
        public OAuthSettings[] newArray(int size) {
            return new OAuthSettings[size];
        }
    };

    private final String mOAuthEndpoint;
    private final String mRedirectUrl;
    private final String mService;

    public OAuthSettings(final String oAuthEndpoint, final String redirectUrl, final String service) {
        mOAuthEndpoint = oAuthEndpoint;
        mRedirectUrl = redirectUrl;
        mService = service;
    }

    public String getOAuthEndpoint() { return mOAuthEndpoint; }
    public String getRedirectUrl() { return mRedirectUrl; }
    public String getService() { return mService; }

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOAuthEndpoint);
        dest.writeString(mRedirectUrl);
        dest.writeString(mService);
    }

}
