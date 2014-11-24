package com.brettsun.triber_demo;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.brettsun.triber_demo.infinitescrolllistview.InfiniteScrollListActivity;
import com.brettsun.triber_demo.instagram.InstagramMedia;
import com.brettsun.triber_demo.instagram.InstagramMediaAdapter;
import com.brettsun.triber_demo.instagram.InstagramUser;
import com.brettsun.triber_demo.instagram.api.InstagramApi;
import com.brettsun.triber_demo.instagram.api.InstagramArrayRequest;
import com.brettsun.triber_demo.instagram.api.InstagramError;
import com.brettsun.triber_demo.instagram.api.InstagramListener;
import com.brettsun.triber_demo.instagram.api.InstagramOAuthManager;
import com.brettsun.triber_demo.oauth.OAuthError;
import com.brettsun.triber_demo.oauth.OAuthManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public final class RecentMediaActivity extends InfiniteScrollListActivity {
    public static final String SELECTED_USER_KEY = "com.brettsun.triber_demo.RecentMedia.SelectedUser";

    private static final String TAG = RecentMediaActivity.class.getSimpleName();
    private static final String SAVED_LISTVIEW_STATE_KEY = "com.brettsun.triber_demo.RecentMedia.SavedListViewState";
    private static final String SAVED_RECENT_MEDIA_API_KEY = "com.brettsun.triber_demo.RecentMedia.SavedRecentMediaApi";
    private static final String SAVED_RECENT_MEDIA_RESULTS_KEY = "com.brettsun.triber_demo.RecentMedia.SavedRecentMediaResults";

    private InstagramArrayRequest mApiRequest;
    private View mEmptyRetryView;
    private View mFooterView;
    private InstagramMediaAdapter mListAdapter;
    private InstagramUser mSelectedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_media);

        if (getIntent().hasExtra(SELECTED_USER_KEY)) {
            mSelectedUser = getIntent().getParcelableExtra(SELECTED_USER_KEY);
        }
        restoreMediaApi(savedInstanceState);
        restoreMediaResults(savedInstanceState);
        setupActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recent_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_recent_media_toggle_login) {
            // Switch the behaviour of this menu item depending on if there's a logged in user or not
            if (InstagramOAuthManager.getInstance().hasLoggedInUser()) {
                logout();
            } else {
                login();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Toggle this menu item depending on if the user is logged in or not
        MenuItem item = menu.findItem(R.id.action_recent_media_toggle_login);
        if (InstagramOAuthManager.getInstance().hasLoggedInUser()) {
            item.setTitle(R.string.action_logout);
        } else {
            item.setTitle(R.string.action_login);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current Api call so we can maintain pagination
        if (null != mApiRequest) {
            outState.putParcelable(SAVED_RECENT_MEDIA_API_KEY, mApiRequest);
        }

        // Save the current media results
        if (null != mListAdapter) {
            final List<InstagramMedia> mediaList = mListAdapter.getItems();
            outState.putParcelableArray(SAVED_RECENT_MEDIA_RESULTS_KEY,
                    mediaList.toArray(new InstagramMedia[mediaList.size()]));
        }

        // Save the listview's state
        if (null != getListView()) {
            outState.putParcelable(SAVED_LISTVIEW_STATE_KEY, getListView().onSaveInstanceState());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Cancelling all volley requests associated with this activity");
        VolleyHandler.getInstance(this).cancelAll(this);
    }

    private void setupActivity() {
        initializeUserTopBar();
        initializeLoadingFooter();
        setEmptyListRetryListener();
        // Make sure we have obtained a selected user from the intent, otherwise we won't be able
        // to make API calls
        if (null != mSelectedUser) {
            displayUserOnTopBar();
            // Start querying for recent image uploads from the user if we didn't restore a previous API request
            if (null == mApiRequest) {
                requestRecentMedia();
            }
        } else {
            Log.e(TAG, "No selected user found in RecentMedia activity");
            Toast.makeText(getApplicationContext(), R.string.no_selected_user_text, Toast.LENGTH_SHORT)
                 .show();
        }
    }

    private void displayUserOnTopBar() {
        if (null != mSelectedUser) {
            TextView username = (TextView) findViewById(R.id.recent_user_username);
            NetworkImageView profilePic = (NetworkImageView) findViewById(R.id.recent_user_profile_pic);

            username.setText(mSelectedUser.getUsername());
            mSelectedUser.loadProfilePic(this, profilePic);
        }
    }

    private void setEmptyListRetryListener() {
        mEmptyRetryView = findViewById(R.id.media_empty_retry_button);
        mEmptyRetryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryApiRequest();
                mEmptyRetryView.setVisibility(View.GONE);
            }
        });
    }

    private void initializeLoadingFooter() {
        mFooterView = LayoutInflater.from(this).inflate(R.layout.list_loading_item, getListView(), false);
        setLoadingFooter(mFooterView);
    }

    private void initializeUserTopBar() {
        NetworkImageView profilePic = (NetworkImageView) findViewById(R.id.recent_user_profile_pic);
        profilePic.setDefaultImageResId(R.drawable.ig_user_default_profile);
    }

    private void login() {
        InstagramOAuthManager.getInstance().authenticate(getFragmentManager(), new OAuthManager.TokenListener() {
            @Override
            public void onSuccess(final String accessToken) {
                Log.i(TAG, "Successfully authenticated user through OAuth");
                // Change the menu to have logout instead of login
                invalidateOptionsMenu();
            }

            @Override
            public void onError(final OAuthError error) {
                Log.i(TAG, "Failed OAuth authentication with errorCode: " + error.errorCode +
                        ", reason: " + error.errorReason + ", and description: " + error.description);
                if (!InstagramOAuthManager.getInstance().didUserDeny(error)) {
                    Log.i(TAG, "User denied OAuth authentication");
                    Toast.makeText(getApplicationContext(),
                            R.string.login_failed_text, Toast.LENGTH_SHORT)
                         .show();
                }
            }
        });
    }

    private void logout() {
        InstagramOAuthManager.getInstance().logout();
        // Change the menu to have login instead of logout
        invalidateOptionsMenu();
    }

    private void restoreMediaApi(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            mApiRequest = savedInstanceState.getParcelable(SAVED_RECENT_MEDIA_API_KEY);
            if (null != mApiRequest) {
                Log.i(TAG, "Restored saved media api request");
            }
        }
    }

    private void restoreMediaResults(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            final Parcelable[] savedMediaParcels = savedInstanceState.getParcelableArray(SAVED_RECENT_MEDIA_RESULTS_KEY);
            if (null != savedMediaParcels && savedMediaParcels.length > 0) {
                final List<InstagramMedia> media = new ArrayList<InstagramMedia>(savedMediaParcels.length);
                for (Parcelable parcel : savedMediaParcels) {
                    media.add((InstagramMedia) parcel);
                }
                setupMediaListView(media);
                Log.i(TAG, "Restored saved media items to ListView");
            }

            final Parcelable savedListViewState = savedInstanceState.getParcelable(SAVED_LISTVIEW_STATE_KEY);
            if (null != savedListViewState) {
                getListView().onRestoreInstanceState(savedListViewState);
                Log.i(TAG, "Restored saved ListView state");
            }
        }
    }

    private void requestRecentMedia() {
        final int userId = mSelectedUser.getId();
        Log.i(TAG, "Initial request for media of user: " + userId);
        mApiRequest = InstagramApi.sendRecentMediaRequest(this, userId, new InstagramListener<JSONArray>() {
            @Override
            public void onResponse(final JSONArray response) {
                // JSON request succeeded, parse it for media
                Log.i(TAG, "JSON request for media succeeded");
                List<InstagramMedia> media = InstagramMedia.parseMediaListFromJson(response);
                setupMediaListView(media);
            }
            @Override
            public void onErrorResponse(final InstagramError error) {
                Log.i(TAG, "JSON request for media failed. Received status code: "
                        + error.getStatusCode() + " with message: " + error.getErrorMessage());
                if (error.isOAuthInvalidError()) {
                    Log.i(TAG, "Request failed due to invalid OAuth.");
                    logout();
                }

                showRetryButton();
                Toast.makeText(getApplicationContext(), R.string.media_request_failed, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void retryApiRequest() {
        Log.i(TAG, "Retrying last media request...");
        mApiRequest.retry(this);
    }

    private void setupMediaListView(List<InstagramMedia> media) {
        Log.i(TAG, "Setting up list with parsed media");
        mListAdapter = new InstagramMediaAdapter(this, media);
        setListAdapter(mListAdapter);
    }

    private void showRetryButton() {
        if (null == mListAdapter || mListAdapter.isEmpty()) {
            // No items in the list yet, so we show the id:empty retry button
            Log.i(TAG, "Showing the retry view from id:empty");
            mEmptyRetryView.setVisibility(View.VISIBLE);
        } else {
            // Items in the list, so we we failed loading the next page of results
            // Repurpose the footer view into a retry button
            Log.i(TAG, "Showing the retry view as the ListView footer");
            final TextView footerText = (TextView) mFooterView.findViewById(R.id.list_loading_text);
            footerText.setText(R.string.network_retry_button);
            mFooterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFooterView.setOnClickListener(null);
                    retryApiRequest();
                    footerText.setText(R.string.footer_loading_text);
                }
            });
        }
    }

    /***** For infinite scrolling *****/
    protected boolean canLoadMore() {
        // If the request exists and there are additional pages, we can load more
        return null != mApiRequest && mApiRequest.hasPagination();
    }

    // IInfiniteScrollListener implementation
    @Override
    public void endIsNear() {
        Log.i(TAG, "End of list reached");
        if(canLoadMore()) {
            Log.i(TAG, "Loading more pages of data");
            mApiRequest.requestNextPage(getListView().getContext(), null, new InstagramListener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    Log.i(TAG, "JSON request for additional page of media succeeded");
                    List<InstagramMedia> media = InstagramMedia.parseMediaListFromJson(response);
                    mListAdapter.addItems(media);

                    // If there are no more pages after this load, disable infinite scrolling
                    if (!canLoadMore()) {
                        disableInfiniteScroll();
                    }
                }
                @Override
                public void onErrorResponse(final InstagramError error) {
                    Log.i(TAG, "JSON request additional page of media failed. Received status code: "
                            + error.getStatusCode() + " with message: " + error.getErrorMessage());
                    if (error.isOAuthInvalidError()) { logout(); }
                    showRetryButton();
                    Toast.makeText(getApplicationContext(), R.string.media_page_request_failed, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    @Override
    public void onScrollCalled(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

}
