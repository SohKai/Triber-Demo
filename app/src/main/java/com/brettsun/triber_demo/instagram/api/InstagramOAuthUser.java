package com.brettsun.triber_demo.instagram.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.brettsun.triber_demo.instagram.InstagramUser;

/**
 * Wrapped version of InstagramUser for the currently authenticated user.
 * Provides the access token of the user.
 */
public class InstagramOAuthUser implements Parcelable {

    // For Parcelable
    public static final Creator<InstagramOAuthUser> CREATOR = new Creator<InstagramOAuthUser>() {
        @Override
        public InstagramOAuthUser createFromParcel(Parcel src) {
            return new InstagramOAuthUser(src.readString(),     // accessToken
                    (InstagramUser) src.readParcelable(InstagramUser.class.getClassLoader()));  // user
        }
        @Override
        public InstagramOAuthUser[] newArray(int size){
            return new InstagramOAuthUser[size];
        }
    };

    private final String mAccessToken;
    private final InstagramUser mInstagramUser;

    public String getAccessToken() { return mAccessToken; }
    public InstagramUser getInstagramUser() { return mInstagramUser; }

    InstagramOAuthUser(final String accessToken, final InstagramUser instagramUser) {
        mAccessToken = accessToken;
        mInstagramUser = instagramUser;
    }

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAccessToken);
        dest.writeParcelable(mInstagramUser, flags);
    }
}
