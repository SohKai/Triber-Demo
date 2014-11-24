package com.brettsun.triber_demo.instagram;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.volley.toolbox.NetworkImageView;
import com.brettsun.triber_demo.R;
import com.brettsun.triber_demo.VolleyHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class InstagramUser implements Parcelable {
    private static final String TAG = InstagramUser.class.getSimpleName();
    // For JSON response parsing
    private static final String BIO_KEY = "bio";
    private static final String FULL_NAME_KEY = "full_name";
    private static final String ID_KEY = "id";
    private static final String PROFILE_PICTURE_URL_KEY = "profile_picture";
    private static final String USERNAME_KEY = "username";

    // For Parcelable
    public static final Creator<InstagramUser> CREATOR = new Creator<InstagramUser>() {
        @Override
        public InstagramUser createFromParcel(Parcel src) {
            return new InstagramUser(src.readString(),      // bio
                                     src.readString(),      // fullName
                                     src.readInt(),         // id
                                     src.readString(),      // profilePictureUrl
                                     src.readString());     // username
        }
        @Override
        public InstagramUser[] newArray(int size) {
            return new InstagramUser[size];
        }
    };

    private final String mBio;
    private final String mFullName;
    private final int mId;
    private final String mProfilePictureUrl;
    private final String mUserName;

    /**
     * Parse a single InstagramUser from a JSONObject.
     * @param userJson JSONObject representing a single user
     */
    public static InstagramUser parseUserFromJson(final JSONObject userJson) {
        if (null != userJson) {
            try {
                // We must have the username and id
                final int id = userJson.getInt(ID_KEY);
                final String username = userJson.getString(USERNAME_KEY);

                // The other fields are optional to us
                final String bio = userJson.optString(BIO_KEY, "");
                final String fullName = userJson.optString(FULL_NAME_KEY, "");
                final String profilePictureUrl = userJson.optString(PROFILE_PICTURE_URL_KEY, null);

                return new InstagramUser(bio, fullName, id, profilePictureUrl, username);
            } catch (JSONException jsonex) {
                Log.e(TAG, "User parsing failed due to: " + jsonex.getMessage());
                jsonex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Parse an array of InstagramUsers from a JSONArray.
     * @param jsonArray JSONArray representing an array of users
     */
    public static List<InstagramUser> parseUserListFromJson(final JSONArray jsonArray) {
        final List<InstagramUser> userList = new ArrayList<InstagramUser>();
        if (null != jsonArray) {
            for (int ii = 0; ii < jsonArray.length(); ++ii) {
                try {
                    final JSONObject userJson = jsonArray.getJSONObject(ii);
                    final InstagramUser user = InstagramUser.parseUserFromJson(userJson);
                    if (null != user) {
                        userList.add(user);
                    }
                } catch (JSONException jsonex) {
                    Log.e(TAG, "User list parsing failed at array index: " + ii + " due to: " + jsonex.getMessage());
                    jsonex.printStackTrace();
                }
            }
        }
        return userList;
    }

    public String getBio() { return mBio; }
    public String getFullName() { return mFullName; }
    public int getId() { return mId; }
    public String getProfilePictureUrl() { return mProfilePictureUrl; }
    public String getUsername() { return mUserName; }

    /**
     * Styles the user name using HTML formatting for color and bold. Requires the client to use
     * Html.fromHtml on the string before setting it in a TextView.
     * @param color Color to be passed in
     * @return HTML styled string
     */
    public String getUsernameHtmlStyled(final String color) {
        return "<b><font color='" + color + "'>" + mUserName + "</font></b>";
    }

    public void loadProfilePic(final Context context, final NetworkImageView view) {
        view.setDefaultImageResId(R.drawable.ig_user_default_profile);
        if (null != mProfilePictureUrl) {
            view.setImageUrl(mProfilePictureUrl, VolleyHandler.getInstance(context).getImageLoader());
        }
    }

    // Users must be created by parsing through the JSON returned from the API
    private InstagramUser(final String bio, final String fullName, final int id,
                          final String profilePictureUrl, final String username) {
        mBio = bio;
        mFullName = fullName;
        mId = id;
        mProfilePictureUrl = profilePictureUrl;
        mUserName = username;
    }

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBio);
        dest.writeString(mFullName);
        dest.writeInt(mId);
        dest.writeString(mProfilePictureUrl);
        dest.writeString(mUserName);
    }

}
