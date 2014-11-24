package com.brettsun.triber_demo.instagram;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Instagram comment model.
 * Clients should not be able to interact directly with this class since comments are always tied
 * to a media item.
 */
final class InstagramComment implements Parcelable {
    private static final String TAG = InstagramComment.class.getSimpleName();
    // For JSON parsing
    private static final String COMMENT_KEY = "comments";
    private static final String COMMENT_COUNT_KEY = "count";
    private static final String COMMENT_DATA_KEY = "data";
    private static final String COMMENT_TEXT_KEY = "text";
    private static final String COMMENT_FROM_USER_KEY = "from";

    // Keep a static empty list instance to use for all empty comments
    private static final ArrayList<InstagramComment> EMPTY_COMMENT_LIST = new ArrayList<InstagramComment>(0);

    // For Parcelable
    public static final Creator<InstagramComment> CREATOR = new Creator<InstagramComment>() {
        @Override
        public InstagramComment createFromParcel(Parcel src) {
            InstagramUser fromUser = src.readParcelable(InstagramUser.class.getClassLoader());
            return new InstagramComment(fromUser,               // fromUser
                                        src.readString());      // text
        }
        @Override
        public InstagramComment[] newArray(int size) {
            return new InstagramComment[size];
        }
    };

    private final InstagramUser mFromUser;
    private final String mText;

    // Parse a media JSONObject for its list of comments (if any exist)
    // Comments are always associated with their media, so they should only be created through this
    static List<InstagramComment> parseCommentListFromMedia(final JSONObject mediaJson) {
        try {
            if (null != mediaJson && !mediaJson.isNull(COMMENT_KEY)) {
                final JSONObject commentRootJson = mediaJson.getJSONObject(COMMENT_KEY);
                final int commentCount = commentRootJson.optInt(COMMENT_COUNT_KEY, 0);
                if (commentCount > 0) {
                    return InstagramComment.parseCommentListFromJson(commentRootJson.getJSONArray(COMMENT_DATA_KEY));
                }
            }
        } catch (JSONException jsonex) {
            Log.e(TAG, "Comment list parsing failed due to: " + jsonex.getMessage());
            jsonex.printStackTrace();
        }
        // If parsing failed, return an empty comment list
        return EMPTY_COMMENT_LIST;
    }

    InstagramUser getFromUser() { return mFromUser; }
    String getText() { return mText; }

    // Here, we don't expect anyone else to parse a comment using this, so we let
    // the list parser handle any exceptions that may occur
    private static InstagramComment parseCommentFromJson(final JSONObject commentJson) throws JSONException {
        if (null != commentJson) {
            final InstagramUser fromUser = InstagramUser.parseUserFromJson(commentJson.getJSONObject(COMMENT_FROM_USER_KEY));
            final String text = commentJson.optString(COMMENT_TEXT_KEY, "");
            return new InstagramComment(fromUser, text);
        }
        return null;
    }

    private static List<InstagramComment> parseCommentListFromJson(final JSONArray jsonArray) {
        final List<InstagramComment> commentList = new ArrayList<InstagramComment>();
        if (null != jsonArray) {
            for (int ii = 0; ii < jsonArray.length(); ++ii) {
                try {
                    final JSONObject commentJson = jsonArray.getJSONObject(ii);
                    final InstagramComment comment = InstagramComment.parseCommentFromJson(commentJson);
                    if (null != comment) {
                        commentList.add(comment);
                    }
                } catch (JSONException jsonex) {
                    Log.e(TAG, "Comment list parsing failed at array index: " + ii + " due to: " + jsonex.getMessage());
                    jsonex.printStackTrace();
                }
            }
        }
        return (commentList.size()) > 0 ? commentList : EMPTY_COMMENT_LIST;
    }

    private InstagramComment(final InstagramUser fromUser, final String text) {
        mFromUser = fromUser;
        mText = text;
    }

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mFromUser, flags);
        dest.writeString(mText);
    }

}
