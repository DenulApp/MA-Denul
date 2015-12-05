package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;
import de.velcommuta.denul.ui.adapter.FriendListAdapter;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;

/**
 * Fragment containing the Friend List
 */
public class FriendListFragment extends Fragment {
    private static final String TAG = "FriendListFragment";

    private EmptyRecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    /**
     * Required empty constructor
     */
    public FriendListFragment() {
        // Required empty constructor
    }


    /**
     * Factory method
     * @return A FriendListFragment
     */
    public static FriendListFragment newInstance() {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tell the system that we want to add an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_friend_list, container, false);
        // Grab reference to RecyclerView
        mRecyclerView = (EmptyRecyclerView) v.findViewById(R.id.friendlist_recycler);
        // Use linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager) mLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Set up the emptyview (view that is shown if the database contains no friends)
        LinearLayout emptyview = (LinearLayout) v.findViewById(R.id.friendlist_empty);
        mRecyclerView.setEmptyView(emptyview);
        // Return the inflated view
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_friends, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_friend:
                // The "add friend" button was clicked
                Log.d(TAG, "onOptionsItemSelected: Add friend clicked");
                Intent intent = new Intent(getActivity(), FriendAddActivity.class);
                startActivity(intent);
                return true;
            default:
                // The clicked button was not our responsibility
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Get a database binder
        DatabaseServiceBinder binder = ((MainActivity) getActivity()).getDbBinder();
        // Load the list of friends from the database
        List<Friend> list = binder.getFriends();
        // Initialize FriendListCursorAdapter with the cursor
        FriendListAdapter ca = new FriendListAdapter(getActivity(), list);
        // Set the adapter for the RecyclerView
        mRecyclerView.setAdapter(ca);
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
