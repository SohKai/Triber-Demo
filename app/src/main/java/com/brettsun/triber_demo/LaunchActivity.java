package com.brettsun.triber_demo;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.toolbox.NetworkImageView;
import com.brettsun.triber_demo.instagram.InstagramUser;
import com.brettsun.triber_demo.instagram.InstagramUserAdapter;
import com.brettsun.triber_demo.instagram.api.InstagramApi;
import com.brettsun.triber_demo.instagram.api.InstagramError;
import com.brettsun.triber_demo.instagram.api.InstagramListener;
import com.brettsun.triber_demo.instagram.api.InstagramOAuthManager;
import com.brettsun.triber_demo.instagram.api.InstagramOAuthUser;
import com.brettsun.triber_demo.oauth.OAuthError;
import com.brettsun.triber_demo.oauth.OAuthManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public final class LaunchActivity extends ListActivity {
    private static final String TAG = LaunchActivity.class.getSimpleName();
    private static final String SAVED_LISTVIEW_STATE_KEY = "com.brettsun.triber_demo.Launch.SavedListViewState";
    private static final String SAVED_LOGGED_IN_USER_KEY = "com.brettsun.triber_demo.Launch.SavedLoggedInUser";
    private static final String SAVED_SEARCH_RESULTS_KEY = "com.brettsun.triber_demo.Launch.SavedSearchResults";

    private InstagramOAuthUser mLoggedInUser;
    private InstagramUserAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        SharedPrefUtils.init(this);     // Initialize the shared pref utils with the application context
        restoreLoginState(savedInstanceState);
        restoreSearchResultState(savedInstanceState);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        // We must check for login state on every resume as the user could have logged out elsewhere
        // in the app and returned to a resumed instance of this
        maintainLoginView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently logged in user's details
        outState.putParcelable(SAVED_LOGGED_IN_USER_KEY, mLoggedInUser);

        // Save the current search results
        if (null != mListAdapter) {
            final List<InstagramUser> searchList = mListAdapter.getItems();
            outState.putParcelableArray(SAVED_SEARCH_RESULTS_KEY,
                    searchList.toArray(new InstagramUser[searchList.size()]));
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

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Log.i(TAG, "Clicked user at position: " + position + " with item id: " + id);

        InstagramUser selectedUser = (InstagramUser) listView.getItemAtPosition(position);
        Intent mediaActivityIntent = new Intent(this, RecentMediaActivity.class);
        // Send the selected user so RecentMediaActivity can show him
        mediaActivityIntent.putExtra(RecentMediaActivity.SELECTED_USER_KEY, selectedUser);
        Log.i(TAG, "Starting RecentMedia activity");
        startActivity(mediaActivityIntent);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void login() {
        InstagramOAuthManager.getInstance().authenticate(getFragmentManager(), new OAuthManager.TokenListener() {
            @Override
            public void onSuccess(final String accessToken) {
                Log.i(TAG, "Successfully authenticated user through OAuth");
                maintainLoginView();
            }

            @Override
            public void onError(final OAuthError error) {
                Log.i(TAG, "Failed OAuth authentication with errorCode: " + error.errorCode +
                        ", reason: " + error.errorReason + ", and description: " + error.description);
                if (!InstagramOAuthManager.getInstance().didUserDeny(error)) {
                    Log.i(TAG, "User denied OAuth authentication");
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.login_failed_text, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void logout() {
        InstagramOAuthManager.getInstance().logout();
        mLoggedInUser = null;
        toggleLoggedInView(false);
    }

    private void maintainLoginView() {
        final String accessToken = InstagramOAuthManager.getInstance().getAccessToken();
        if (null != accessToken) {
            if (null != mLoggedInUser && accessToken.equals(mLoggedInUser.getAccessToken())) {
                // We're logged in and already have the correct user, so we can immediately display him
                toggleLoggedInView(true);
            } else {
                // Logged in but do not have the user, so we need to get him from the API
                InstagramOAuthManager.getInstance().getUser(this,
                        new OAuthManager.UserListener<InstagramOAuthUser>() {
                    @Override
                    public void onSuccess(final InstagramOAuthUser user) {
                        mLoggedInUser = user;
                        toggleLoggedInView(true);
                    }
                    @Override
                    public void onError(final OAuthError error) {
                        Log.i(TAG, "Could not get logged in user details with error code: " +
                                        error.errorCode + " and reason: " + error.errorReason);
                        logout();
                    }
                });
            }
            return;
        }
        // Not logged in at all
        toggleLoggedInView(false);
    }

    private void restoreLoginState(Bundle savedInstanceState) {
        // If found saved user, restore it
        if (null != savedInstanceState) {
            mLoggedInUser = savedInstanceState.getParcelable(SAVED_LOGGED_IN_USER_KEY);
        }
    }

    private void restoreSearchResultState(Bundle savedInstanceState) {
        // If found search results, restore them
        if (null != savedInstanceState) {
            final Parcelable[] savedSearchParcels = savedInstanceState.getParcelableArray(SAVED_SEARCH_RESULTS_KEY);
            if (null != savedSearchParcels && savedSearchParcels.length > 0) {
                // Recreate the list of search results from Parcelables
                final List<InstagramUser> users = new ArrayList<InstagramUser>(savedSearchParcels.length);
                for (Parcelable parcel : savedSearchParcels) {
                    users.add((InstagramUser) parcel);
                }
                setupUserListView(users);
                Log.i(TAG, "Restored saved searches to ListView");
            }

            // Restore the state of the ListView
            final Parcelable savedListViewState = savedInstanceState.getParcelable(SAVED_LISTVIEW_STATE_KEY);
            if (null != savedListViewState) {
                getListView().onRestoreInstanceState(savedListViewState);
                Log.i(TAG, "Restored saved ListView state");
            }
        }
    }

    /**
     * Unfortunately Instagram's user search API does not support pagination and will return at
     * most 50 results for any query. Because of this, we don't bother with implementing an
     * infinite scroll and just list every user the API gives us for the query
     */
    private void searchForUsers(final String query) {
        // Make sure the user gives us something to search with
        if (query.isEmpty()) {
            Log.i(TAG, "Ignored empty search query from user");
            return;
        }

        Log.i(TAG, "Searching for users with query: " + query);
        InstagramApi.sendSearchRequest(this, query, new InstagramListener<JSONArray>() {
            @Override
            public void onResponse(final JSONArray response) {
                // JSON request succeeded, parse it for users
                Log.i(TAG, "JSON request for users succeeded");
                List<InstagramUser> users = InstagramUser.parseUserListFromJson(response);
                setupUserListView(users);
            }

            @Override
            public void onErrorResponse(final InstagramError error) {
                Log.i(TAG, "JSON request for paged media failed. Received status code: "
                        + error.getStatusCode() + " with message: " + error.getErrorMessage());
                if (error.isOAuthInvalidError()) {
                    Log.i(TAG, "Request failed due to invalid OAuth.");
                    logout();
                }

                // Clear the previous search results on an error
                if (null != mListAdapter) {
                    mListAdapter.clear();
                }
                Toast.makeText(getApplicationContext(), R.string.search_failed_text, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void setupListeners() {
        final EditText searchText = (EditText) findViewById(R.id.search_search_box);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                // search_search_box has imeOptions="actionSearch"
                if (EditorInfo.IME_ACTION_SEARCH == actionId) {
                    searchForUsers(v.getText().toString());
                    hideKeyboard(searchText);
                    handled = true;
                }
                return handled;
            }
        });

        final ImageButton searchButton = (ImageButton) findViewById(R.id.search_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForUsers(searchText.getText().toString());
                hideKeyboard(searchText);
            }
        });

        final Button loginButton = (Button) findViewById(R.id.search_login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        final View logoutButton = findViewById(R.id.search_logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void setListUsers(final List<InstagramUser> users) {
        if (null == mListAdapter) {
            mListAdapter = new InstagramUserAdapter(this, users);
        } else {
            mListAdapter.updateItems(users);
        }
    }

    private void setupUserListView(final List<InstagramUser> users) {
        Log.i(TAG, "Setting up list with parsed users");
        setListUsers(users);
        setListAdapter(mListAdapter);
    }

    // Toggle the login / logout buttons that are inside the search_login_user_switcher ViewSwitcher
    private void toggleLoggedInView(boolean loggedIn) {
        final ViewSwitcher loginViewSwitcher = (ViewSwitcher) findViewById(R.id.search_login_user_switcher);
        final int curItem = loginViewSwitcher.getDisplayedChild();
        // Unfortunately the ViewSwitcher loops around the views it contains instead of stopping at the start
        // or end view. This means that we need to make sure our ViewSwitcher is in the correct position
        // before we move it.
        // Only if it shows the login view (curItem == 0) do we move it to the logged in view (curItem == 1),
        // and vice-versa
        if (loggedIn && 0 == curItem && null != mLoggedInUser) {
            final NetworkImageView profilePic = (NetworkImageView) findViewById(R.id.search_logged_in_profile_pic);
            mLoggedInUser.getInstagramUser().loadProfilePic(this, profilePic);

            loginViewSwitcher.showNext();
        } else if (!loggedIn && 1 == curItem) {
            loginViewSwitcher.showPrevious();
        }
    }

}
