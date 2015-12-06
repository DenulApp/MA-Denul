package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.Toast;

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.Friend;
import de.velcommuta.denul.ui.adapter.FriendListAdapter;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;
import de.velcommuta.denul.util.FriendManagement;

/**
 * Fragment containing the Friend List
 */
public class FriendListFragment extends Fragment implements ServiceConnection,
                                                            FriendListAdapter.OnItemClickListener {
    private static final String TAG = "FriendListFragment";

    private EmptyRecyclerView mRecyclerView;
    private FriendListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private DatabaseServiceBinder mDbBinder;

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
        // Register RecyclerView for the context menu
        registerForContextMenu(mRecyclerView);
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
        // Get a database binder (once it is connected, the UI will be filled)
        bindDbService();
    }

    public void onPause() {
        super.onPause();
        // Disconnect from the database service
        getActivity().unbindService(this);
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * Get a binder to the database service
     * @return true if the request was sent successfully, false otherwise
     */
    private boolean bindDbService() {
        if (!DatabaseService.isRunning(getActivity())) {
            Log.w(TAG, "bindDbService: Trying to bind to a non-running database service. Aborting");
            return false;
        }
        Intent intent = new Intent(getActivity(), DatabaseService.class);
        if (!getActivity().bindService(intent, this, 0)) {
            Log.e(TAG, "bindDbService: An error occured during binding :(");
            return false;
        } else {
            Log.d(TAG, "bindDbService: Database service binding request sent");
            return true;
        }
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: New service connection received");
        mDbBinder = (DatabaseServiceBinder) iBinder;
        // TODO Debugging code, move to passphrase activity once it is added
        if (!mDbBinder.isDatabaseOpen()) {
            mDbBinder.openDatabase("VerySecureHardcodedPasswordOlolol123");
        }
        // Populate the list of friends
        populateFriendList();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(TAG, "onServiceDisconnected: Lost DB binder");
        mDbBinder = null;
    }


    /**
     * Fill the friendlist in the UI from the database
     */
    private void populateFriendList() {
        // Load the list of friends from the database
        List<Friend> list = mDbBinder.getFriends();
        // Initialize FriendListCursorAdapter with the cursor
        mAdapter = new FriendListAdapter(getActivity(), this, list);
        // Set the adapter for the RecyclerView
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    public void onItemClicked(int position) {
        Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = mAdapter.getPosition();
        switch (item.getItemId()) {
            case R.id.friend_remove:
                FriendManagement.deleteFriend(mAdapter.getFriendAt(position), mDbBinder);
                Toast.makeText(getActivity(), "Friend deleted", Toast.LENGTH_SHORT).show();
                populateFriendList();
                return true;
        }
        return super.onContextItemSelected(item);
    }
}
