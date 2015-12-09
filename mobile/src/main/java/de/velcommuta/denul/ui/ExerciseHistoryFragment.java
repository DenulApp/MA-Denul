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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.ExerciseListAdapter;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;


/**
 * Fragment to display the technology chooser for the Add Friend activity
 */
public class ExerciseHistoryFragment extends Fragment implements ServiceConnection,
                                                                 ExerciseListAdapter.OnItemClickListener {
    private static final String TAG = "ExercHist";

    private EmptyRecyclerView mRecyclerView;
    private DatabaseServiceBinder mDbBinder;
    private ExerciseListAdapter mAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static ExerciseHistoryFragment newInstance() {
        return new ExerciseHistoryFragment();
    }


    /**
     * Required empty constructor
     */
    public ExerciseHistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_exercise_list, container, false);
        mRecyclerView = (EmptyRecyclerView) v.findViewById(R.id.exclist_recycler);
        // Use linear layout manager
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager) manager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(manager);

        // Set up the emptyview (view that is shown if the database contains no friends)
        LinearLayout emptyview = (LinearLayout) v.findViewById(R.id.exc_list_empty);
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
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
        bindDbService();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = mAdapter.getPosition();
        switch (item.getItemId()) {
            case R.id.exercise_remove:
                Toast.makeText(getActivity(), "NotImplemented", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.exercise_share:
                Toast.makeText(getActivity(), "NotImplemented", Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }


    /**
     * Populate the RecyclerView
     */
    private void populateExerciseHistory() {
        // Load the list of friends from the database
        List<GPSTrack> list = mDbBinder.getGPSTracks();
        // Initialize FriendListCursorAdapter with the cursor
        mAdapter = new ExerciseListAdapter(getActivity(), this, list);
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
        populateExerciseHistory();
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
        Intent i = new Intent(getActivity(), ExerciseViewActivity.class);
        i.putExtra("track-id", mAdapter.getTrackAt(position).getID());
        startActivity(i);
    }
}
