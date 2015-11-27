package de.velcommuta.denul.ui;

import android.app.Fragment;
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
import android.widget.TextView;

import net.sqlcipher.Cursor;

import de.velcommuta.denul.R;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.FriendListCursorAdapter;
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

        DatabaseServiceBinder binder = ((MainActivity) getActivity()).getDbBinder();
        Cursor c = binder.getFriends();
        FriendListCursorAdapter ca = new FriendListCursorAdapter(getActivity(), c);
        mRecyclerView.setAdapter(ca);
        // TODO Update the text and design of the "here be nothing" message
        TextView emptyview = (TextView) v.findViewById(R.id.friendlist_empty);
        mRecyclerView.setEmptyView(emptyview);
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
                Log.d(TAG, "onOptionsItemSelected: Add friend clicked");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }
}
