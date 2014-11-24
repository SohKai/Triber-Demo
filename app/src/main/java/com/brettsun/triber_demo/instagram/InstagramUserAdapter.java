package com.brettsun.triber_demo.instagram;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.brettsun.triber_demo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for handling InstagramUsers
 */
public final class InstagramUserAdapter extends BaseAdapter {
    private List<InstagramUser> mUsers;
    private Context mContext;

    public InstagramUserAdapter(Context context, List<InstagramUser> users) {
        mContext = context;
        mUsers = users;
    }

    /**
     * Clear all users from the current adapter
     */
    public void clear() {
        mUsers = new ArrayList<InstagramUser>(0);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mUsers.size();
    }

    @Override
    public InstagramUser getItem(int position) {
        return mUsers.get(position);
    }

    /**
     * Return all users from the current adapter
     */
    public List<InstagramUser> getItems() {
        return mUsers;
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
            rowView = inflater.inflate(R.layout.user_list_item, parent, false);

            UserItemViewHolder holder = new UserItemViewHolder();
            holder.bio = (TextView) rowView.findViewById(R.id.user_bio);
            holder.fullName = (TextView) rowView.findViewById(R.id.user_full_name);
            holder.profilePic = (NetworkImageView) rowView.findViewById(R.id.user_profile_pic);
            holder.username = (TextView) rowView.findViewById(R.id.user_username);

            rowView.setTag(holder);
        }

        initializeListItem(position, rowView);
        return rowView;
    }

    /**
     * Replace current users in the adapter with the new ones
     * @param newUsers new users that will replace the current ones
     */
    public void updateItems(List<InstagramUser> newUsers) {
        mUsers = newUsers;
        notifyDataSetChanged();
    }

    private Context getContext() { return mContext; }

    private void initializeListItem(final int position, final View rowView) {
        final InstagramUser user = mUsers.get(position);
        final UserItemViewHolder holder = (UserItemViewHolder) rowView.getTag();

        // Load profile pic
        user.loadProfilePic(getContext(), holder.profilePic);

        // Set user info text
        holder.bio.setText(user.getBio());
        holder.fullName.setText(user.getFullName());
        holder.username.setText(user.getUsername());
    }

    // ViewHolder pattern for user list items.
    private static class UserItemViewHolder {
        TextView bio;
        TextView fullName;
        NetworkImageView profilePic;
        TextView username;
    }

}
