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

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.StudyListAdapter;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;
import de.velcommuta.denul.util.StudyManager;


/**
 * Fragment to display the List of Studies offered on the server
 */
public class StudyListFragment extends Fragment implements ServiceConnection,
                                                           StudyListAdapter.OnItemClickListener,
                                                           StudyManager.StudyManagerCallback {
    private static final String TAG = "StudyListF";

    private EmptyRecyclerView mRecyclerView;
    private DatabaseServiceBinder mDbBinder;
    private StudyListAdapter mAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of a StudyListFragment.
     */
    public static StudyListFragment newInstance() {
        return new StudyListFragment();
    }


    /**
     * Required empty constructor
     */
    public StudyListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_study_list, container, false);
        mRecyclerView = (EmptyRecyclerView) v.findViewById(R.id.studylist_recycler);
        // Use linear layout manager
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager) manager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(manager);

        // Set up the emptyview (view that is shown if the database contains no friends)
        LinearLayout emptyview = (LinearLayout) v.findViewById(R.id.study_list_empty);
        mRecyclerView.setEmptyView(emptyview);
        // Register RecyclerView for the context menu
        registerForContextMenu(mRecyclerView);
        // Return the inflated view
        return v;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Announce that we have an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_studies, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // The "add friend" button was clicked
                Log.d(TAG, "onOptionsItemSelected: Refresh requested");
                StudyManager.retrieveStudies(mDbBinder, this);
                return true;
            default:
                // The clicked button was not our responsibility
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
        bindDbService();
    }


    /**
     * Populate the RecyclerView
     */
    private void populateStudyList() {
        // Load the list of friends from the database
        List<StudyRequest> list = mDbBinder.getStudyRequests();
        // Initialize FriendListCursorAdapter with the cursor
        mAdapter = new StudyListAdapter(getActivity(), this, list);
        // Set the adapter for the RecyclerView
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(this);
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: New service connection received");
        mDbBinder = (DatabaseServiceBinder) iBinder;
        // TODO Debugging code, move to passphrase activity once it is added
        if (!mDbBinder.isDatabaseOpen()) {
            mDbBinder.openDatabase("VerySecureHardcodedPasswordOlolol123");
        }
        // Populate the track list
        populateStudyList();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(TAG, "onServiceDisconnected: Lost DB binder");
        mDbBinder = null;
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
    public void onItemClicked(int position) {
        // TODO
        Intent i = new Intent(getActivity(), StudyViewActivity.class);
        i.putExtra("study-id", mAdapter.getStudyAt(position).id);
        startActivity(i);
    }


    @Override
    public void onUpdateFinished() {
        populateStudyList();
    }
}
