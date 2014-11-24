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
import java.util.Date;
import java.util.List;

/**
 * Instagram media model.
 * Media can refer to either pictures or videos, but for now, this only handles
 * pictures and the screenshot of the video.
 *
 * Always uses standard resolution as pictures to display.
 */
public class InstagramMedia implements Parcelable {
    private static final String TAG = InstagramUser.class.getSimpleName();
    // For JSON response parsing
    private static final String CAPTION_KEY = "caption";
    private static final String CAPTION_TEXT_KEY = "text";
    private static final String CREATED_TIME_KEY = "created_time";
    private static final String FROM_USER_KEY = "user";
    private static final String IMAGES_KEY = "images";
    private static final String IMAGES_STANDARD_RES_KEY = "standard_resolution";
    private static final String IMAGES_URL_KEY = "url";
    private static final String LIKES_KEY = "likes";
    private static final String LIKES_COUNT_KEY = "count";

    // For Parcelable
    public static final Creator<InstagramMedia> CREATOR = new Creator<InstagramMedia>() {
        @Override
        public InstagramMedia createFromParcel(Parcel src) {
            final String caption = src.readString();

            final List<InstagramComment> comments = new ArrayList<InstagramComment>();
            src.readTypedList(comments, InstagramComment.CREATOR);

            final Date createdTime = new Date(src.readLong());
            final InstagramUser fromUser = src.readParcelable(InstagramUser.class.getClassLoader());
            final String imageUrl = src.readString();
            final int likeCount = src.readInt();

            return new InstagramMedia(caption, comments, createdTime, fromUser, imageUrl, likeCount);
        }
        @Override
        public InstagramMedia[] newArray(int size) {
            return new InstagramMedia[size];
        }
    };

    private final String mCaption;
    private final List<InstagramComment> mComments;
    private final Date mCreatedTime;
    private final InstagramUser mFromUser;
    private final String mImageUrl;
    private final int mLikeCount;

    /**
     * Parse a single InstagramMedia instance from a JSONObject.
     * @param mediaJson JSONObject representing a single media item
     */
    public static InstagramMedia parseMediaFromJson(final JSONObject mediaJson) {
        if (mediaJson != null) {
            try {
                // Image url is mandatory for us to process this media
                final String imageUrl = mediaJson.getJSONObject(IMAGES_KEY)
                                            .getJSONObject(IMAGES_STANDARD_RES_KEY)
                                            .getString(IMAGES_URL_KEY);
                // So is the user who posted the media
                final InstagramUser fromUser = InstagramUser.parseUserFromJson(mediaJson.getJSONObject(FROM_USER_KEY));

                // Optional fields
                final List<InstagramComment> comments = InstagramComment.parseCommentListFromMedia(mediaJson);
                // Java expects the Unix time to be in milliseconds
                final Date createdTime = new Date(mediaJson.optLong(CREATED_TIME_KEY, 0) * 1000);
                Date aTime = new Date(0);

                String caption = "";
                if (!mediaJson.isNull(CAPTION_KEY)) {
                    final JSONObject captionJson = mediaJson.getJSONObject(CAPTION_KEY);
                    caption = captionJson.optString(CAPTION_TEXT_KEY, "");
                }

                int likeCount = 0;
                if (!mediaJson.isNull(LIKES_KEY)) {
                    final JSONObject likesJson = mediaJson.getJSONObject(LIKES_KEY);
                    likeCount = likesJson.optInt(LIKES_COUNT_KEY, 0);
                }

                return new InstagramMedia(caption, comments, createdTime, fromUser, imageUrl, likeCount);
            } catch (JSONException jsonex) {
                Log.e(TAG, "Media parsing failed due to: " + jsonex.getMessage());
                jsonex.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Parse an array of InstagramMedia from a JSONArray.
     * @param jsonArray JSONArray representing an array of media items
     */
    public static List<InstagramMedia> parseMediaListFromJson(final JSONArray jsonArray) {
        final List<InstagramMedia> mediaList = new ArrayList<InstagramMedia>();
        if (null != jsonArray) {
            for (int ii = 0; ii < jsonArray.length(); ++ii) {
                try {
                    final JSONObject mediaJson = jsonArray.getJSONObject(ii);
                    final InstagramMedia media = InstagramMedia.parseMediaFromJson(mediaJson);
                    if (null != media) {
                        mediaList.add(media);
                    }
                } catch (JSONException jsonex) {
                    Log.e(TAG, "Media list parsing failed at array index: " + ii + " due to: " + jsonex.getMessage());
                    jsonex.printStackTrace();
                }
            }
        }
        return mediaList;
    }

    public String getCaption() { return mCaption; }
    public List<InstagramComment> getComments() { return mComments; }
    public Date getCreatedTime() { return mCreatedTime; }
    public InstagramUser getFromUser() { return mFromUser; }
    public String getImageUrl() { return mImageUrl; }
    public int getLikeCount() { return mLikeCount; }

    /**
     * Convenience function for loading the media image.
     * Sets a default display in case the load fails.
     */
    public void loadImage(final Context context, final NetworkImageView view) {
        view.setDefaultImageResId(R.drawable.ig_image_default);
        view.setImageUrl(mImageUrl, VolleyHandler.getInstance(context).getImageLoader());
    }

    private InstagramMedia(final String caption, final List<InstagramComment> comments,
                           final Date createdTime, final InstagramUser fromUser,
                           final String imageUrl, final int likeCount) {
        mCaption = caption;
        mComments = comments;
        mCreatedTime = createdTime;
        mFromUser = fromUser;
        mImageUrl = imageUrl;
        mLikeCount = likeCount;
    }

    // For Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCaption);
        dest.writeTypedList(mComments);
        dest.writeLong(mCreatedTime.getTime());
        dest.writeParcelable(mFromUser, flags);
        dest.writeString(mImageUrl);
        dest.writeInt(mLikeCount);
    }

}
