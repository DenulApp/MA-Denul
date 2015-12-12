package de.velcommuta.denul.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
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
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.ui.adapter.SocialStreamAdapter;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;
import de.velcommuta.denul.util.ShareManager;


/**
 * A fragment displaying the social stream of the user
 */
public class StartScreenFragment extends Fragment implements ServiceConnection,
        SocialStreamAdapter.OnItemClickListener,
        ShareManager.ShareManagerCallback {
    private static final String TAG = "StartScreenFragment";

    private DatabaseServiceBinder mBinder;
    private EmptyRecyclerView mRecycler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment StartScreenFragment.
     */
    public static StartScreenFragment newInstance() {
        return new StartScreenFragment();
    }

    public StartScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false); // TODO Add options menu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_start_screen, container, false);
        setHasOptionsMenu(true);
        mRecycler = (EmptyRecyclerView) v.findViewById(R.id.socialstream_recycler);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager) mLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecycler.setLayoutManager(mLayoutManager);
        mBinder = ((MainActivity) getActivity()).getDbBinder();
        registerForContextMenu(mRecycler);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_social, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // The "add friend" button was clicked
                Log.d(TAG, "onOptionsItemSelected: Refresh requested");
                ShareManager.RetrieveWithProgress update = new ShareManager().new RetrieveWithProgress(this, mBinder);
                update.execute(mBinder.getFriends());
                return true;
            default:
                // The clicked button was not our responsibility
                return super.onOptionsItemSelected(item);
        }
    }

    public void onPause() {
        super.onPause();
        getActivity().unbindService(this);
    }

    public void onResume() {
        super.onResume();
        bindDbService();
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


    /**
     * Populate the social stream
     */
    private void populateSocialStream() {
        List<Shareable> tracks = new LinkedList<>();
        for (GPSTrack track : mBinder.getGPSTracks()) {
            tracks.add(track);
        }
        RecyclerView.Adapter adapter = new SocialStreamAdapter(getActivity(), this, tracks, mBinder);
        mRecycler.setAdapter(adapter);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: New service connection received");
        mBinder = (DatabaseServiceBinder) iBinder;
        // TODO Debugging code, move to passphrase activity once it is added
        if (!mBinder.isDatabaseOpen()) {
            mBinder.openDatabase("VerySecureHardcodedPasswordOlolol123");
        }
        // Populate the list of friends
        populateSocialStream();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i(TAG, "onServiceDisconnected: Lost DB binder");
        mBinder = null;
    }


    @Override
    public void onItemClicked(int position) {
        Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onShareStatusUpdate(int status) {
        Toast.makeText(getActivity(), "" + status, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onShareFinished(boolean success) {
        if (success) {
            Toast.makeText(getActivity(), "Refresh complete", Toast.LENGTH_SHORT).show();
            populateSocialStream();
        }
        else Toast.makeText(getActivity(), "Refresh failed", Toast.LENGTH_SHORT).show();
    }
}
