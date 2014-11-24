package com.brettsun.triber_demo.instagram;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.brettsun.triber_demo.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapter class for handling InstagramMedia items
 */
public final class InstagramMediaAdapter extends BaseAdapter {
    // Colour to use to style a username in a caption or comment text.
    // It would be nice to use values in the colors.xml file, but Android requires us to grab a
    // resource handler through a context first to obtain those values. So we stick with this
    // constant for now as we only need colors here. In the future, it may be worthwhile to
    // make a utility class holding color values.
    private static final String USERNAME_STYLED_COLOR = "#B20000";

    private List<InstagramMedia> mMedia;
    private Context mContext;

    public InstagramMediaAdapter(Context context, List<InstagramMedia> media) {
        mContext = context;
        mMedia = media;
    }

    /**
     * Add new items into the current adapter.
     * @param newMedia new InstagramMedia items to be added to the adapter
     */
    public void addItems(Collection<InstagramMedia> newMedia) {
        if (!newMedia.isEmpty()) {
            mMedia.addAll(newMedia);
            notifyDataSetChanged();
        }
    }

    /**
     * Clear all items from the current adapter
     */
    public void clear() {
        mMedia = new ArrayList<InstagramMedia>(0);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mMedia.size();
    }

    @Override
    public InstagramMedia getItem(int position) {
        return mMedia.get(position);
    }

    /**
     * Return all items from the current adapter
     */
    public List<InstagramMedia> getItems() {
        return mMedia;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // Check first if we're recycling a view from before due to scrolling
        if (null == convertView || null == convertView.getTag()) {
            // Otherwise inflate a new view
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            rowView = inflater.inflate(R.layout.media_list_item, parent, false);

            final MediaItemViewHolder holder = new MediaItemViewHolder();
            holder.caption = (TextView) rowView.findViewById(R.id.media_caption);
            holder.commentSection = (LinearLayout) rowView.findViewById(R.id.media_comment_section);
            holder.dateCreated = (TextView) rowView.findViewById(R.id.media_date);
            holder.image = (NetworkImageView) rowView.findViewById(R.id.media_image);
            holder.likeCount = (TextView) rowView.findViewById(R.id.media_like_count);

            rowView.setTag(holder);
        }

        initializeListItem(position, rowView);
        return rowView;
    }

    /**
     * Replace current items in the adapter with the new ones
     * @param newMedia new items that will replace the current ones
     */
    public void updateItems(List<InstagramMedia> newMedia) {
        mMedia = newMedia;
        notifyDataSetChanged();
    }

    private Context getContext() { return mContext; }

    private void initializeListItem(final int position, final View rowView) {
        final InstagramMedia media = mMedia.get(position);
        final MediaItemViewHolder viewHolder = (MediaItemViewHolder) rowView.getTag();

        // Load image
        media.loadImage(getContext(), viewHolder.image);

        // Set text
        viewHolder.caption.setText(Html.fromHtml(media.getFromUser().getUsernameHtmlStyled(USERNAME_STYLED_COLOR) +
                                                " " + media.getCaption()));

        viewHolder.likeCount.setText(Integer.toString(media.getLikeCount()));
        if (0 != media.getCreatedTime().getTime()) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
            viewHolder.dateCreated.setText(dateFormat.format(media.getCreatedTime()));
        }

        // Append comments, if any, to be below the caption
        inflateComments(viewHolder, media);
    }

    private void inflateComments(final MediaItemViewHolder holder, final InstagramMedia media) {
        // Make sure to remove previous comments if we are using a recycled view
        if (holder.commentSection.getChildCount() > 0) {
            holder.commentSection.removeAllViews();
        }
        for (InstagramComment comment : media.getComments()) {
            final TextView commentText = new TextView(getContext());
            commentText.setText(Html.fromHtml(comment.getFromUser().getUsernameHtmlStyled(USERNAME_STYLED_COLOR) +
                                                " " + comment.getText()));

            commentText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            commentText.setTextAppearance(getContext(), R.style.MediaCaptionText);
            holder.commentSection.addView(commentText);
        }
    }

    // ViewHolder pattern for media list items.
    private static class MediaItemViewHolder {
        TextView caption;
        LinearLayout commentSection;
        TextView dateCreated;
        NetworkImageView image;
        TextView likeCount;
    }

}