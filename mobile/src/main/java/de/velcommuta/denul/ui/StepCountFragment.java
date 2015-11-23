package de.velcommuta.denul.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.velcommuta.denul.R;
import de.velcommuta.denul.service.PedometerService;
import de.velcommuta.denul.service.PedometerServiceBinder;
import de.velcommuta.denul.service.UpdateListener;


/**
 * Fragment to display the step count
 */
public class StepCountFragment extends Fragment implements ServiceConnection, UpdateListener {
    private static final String TAG = "StepCountFragment";

    // Instance variables
    private TextView mStepCountDisplay;
    private ProgressBar mProgressBar;

    // Database binder
    private PedometerServiceBinder mBinder;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static StepCountFragment newInstance() {
        return new StepCountFragment();
    }


    /**
     * Required empty constructor
     */
    public StepCountFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_step_count, container, false);
        mStepCountDisplay = (TextView) v.findViewById(R.id.stepcount);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        // TODO Find a way to overlay a checkmark on the progressbar to show if the daily goal was achieved (overlays?)

        mProgressBar.setMax(10000); // Max = Target number of 10k steps per day

        return v;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Getting binding");
        bindPedometerService();
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unbinding and unregistering listeners");
        if (mBinder != null) {
            mBinder.removeUpdateListener(this);
            getActivity().unbindService(this);
        }
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mBinder = (PedometerServiceBinder) iBinder;
        Log.i(TAG, "onServiceConnected: Pedometer binder received");
        mBinder.addUpdateListener(this);
        updateUI();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mBinder = null;
        Log.i(TAG, "onServiceDisconnected: Lost Pedometer binder");
    }


    /**
     * Bind to the Pedometer service
     */
    private void bindPedometerService() {
        if (!PedometerService.isRunning(getActivity())) {
            Log.e(TAG, "bindPedometerService: PedometerService not running");
            return;
        }
        Intent intent = new Intent(getActivity(), PedometerService.class);
        getActivity().bindService(intent, this, 0);
    }


    /**
     * Get todays total step count
     */
    private void updateUI() {
        if (mBinder == null) {
            Log.e(TAG, "updateUI: Binder is null, aborting");
            return;
        }
        int total = mBinder.getSumToday();
        mProgressBar.setProgress(total);
        mStepCountDisplay.setText("" + total);
    }


    /**
     * Update listener for the pedometer service
     */
    @Override
    public void update() {
        updateUI();
    }
}
