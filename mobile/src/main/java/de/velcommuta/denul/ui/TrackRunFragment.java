package de.velcommuta.denul.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.List;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.db.LocationLoggingContract;
import de.velcommuta.denul.event.DatabaseResultEvent;
import de.velcommuta.denul.event.GPSLocationEvent;
import de.velcommuta.denul.event.GPSTrackEvent;
import de.velcommuta.denul.service.GPSTrackingService;
import de.velcommuta.denul.service.PedometerService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackRunFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackRunFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    // Debugging Tag
    private static final String TAG = "TrackRunFragment";

    private GoogleMap mMap;
    private Polyline mPolyLine;
    private Marker mStartMarker;

    // GUI elements
    private Button mStartStopButton;
    private Button mSaveRunButton;
    private LinearLayout mStatButtonPanel;
    private LinearLayout mStatWindow;
    private LinearLayout mStatSaveWindow;
    private Chronometer mChrono;
    private TextView mVelocity;
    private TextView mDistance;
    private EditText mSessionName;
    private ImageButton mRunning;
    private ImageButton mCycling;

    // State variables
    private int mLastCheckedIndex;
    private float mCurrentDistance;
    private float mCurrentVelocity;

    private EventBus mEventBus;
    // Identifier Strings for Bundle
    public static final String VALUE_RUN_ACTIVE              = "value-run-active";
    public static final String VALUE_CURRENT_CHRONO_BASE     = "value-current-chrono-base";
    public static final String VALUE_CURRENT_CHRONO_TEXT     = "value-current-chrono-text";
    public static final String VALUE_STAT_SAVE_WINDOW_HEIGHT = "value-stat-save-window-height";
    public static final String VALUE_STAT_SAVE_ACTIVE        = "value-stat-save-active";
    public static final String VALUE_STAT_SAVE_SELECTED      = "value-stat-save-selected";
    public static final String VALUE_STAT_SAVE_TITLE         = "value-stat-save-title";

    public static final int VALUE_RUNNING = 0;
    public static final int VALUE_CYCLING = 1;

    // Annoying variables which we have to keep track of because android is buggy
    private int mStatSaveWindowHeight;
    private int mTopPadding = 0;

    /**
     * Factory method to create new instances of this fragment
     * @return an instance of the fragment
     */
    public static TrackRunFragment newInstance() {
        return new TrackRunFragment();
    }

    /**
     * Required empty public constructor
     */
    public TrackRunFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_track_run, container, false);

        // Get a reference to the Map fragment and perform an async. initialization
        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.gmaps);
        mapFragment.getMapAsync(this);

        // Grab references to UI elements
        mStatWindow      = (LinearLayout) v.findViewById(R.id.statwindow);
        mStatButtonPanel = (LinearLayout) v.findViewById(R.id.stat_button_panel);
        mStatSaveWindow  = (LinearLayout) v.findViewById(R.id.stat_save_panel);
        mStartStopButton = (Button)       v.findViewById(R.id.actionbutton);
        mSaveRunButton   = (Button)       v.findViewById(R.id.save_run_btn);
        mRunning         = (ImageButton)  v.findViewById(R.id.btn_running);
        mCycling         = (ImageButton)  v.findViewById(R.id.btn_cycling);
        mChrono          = (Chronometer)  v.findViewById(R.id.timer);
        mVelocity        = (TextView)     v.findViewById(R.id.speedfield);
        mDistance        = (TextView)     v.findViewById(R.id.distancefield);
        mSessionName     = (EditText)     v.findViewById(R.id.sessionname);

        // Set up this fragment as the OnClickListener of the start/stop/reset button
        mStartStopButton.setOnClickListener(this);
        mSaveRunButton.setOnClickListener(this);
        mCycling.setOnClickListener(this);
        mRunning.setOnClickListener(this);

        // FIXME Test call
        Intent intent = new Intent(getActivity(), PedometerService.class);
        getActivity().startService(intent);


        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: Restore - SavedInstanceState is not empty, restoring.");
            //noinspection ConstantConditions
            if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.stop_run))) {
                Log.d(TAG, "onCreate: Restore - Ongoing session, restoring state");
                setButtonStateStarted(false);
                mChrono.setBase(savedInstanceState.getLong(VALUE_CURRENT_CHRONO_BASE));
                mChrono.start();
                showStatPanel(false);
            } else //noinspection ConstantConditions
                if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.reset_run))) {
                    Log.d(TAG, "onCreate: Restore - Stopped, but saved session, restoring state");
                    setButtonStateStopped(false);
                    // markFinalPosition();
                    showStatPanel(false);
                    //(false);
                    mChrono.setText(savedInstanceState.getString(VALUE_CURRENT_CHRONO_TEXT));
            }
            mStatSaveWindowHeight = savedInstanceState.getInt(VALUE_STAT_SAVE_WINDOW_HEIGHT);
            if (savedInstanceState.getBoolean(VALUE_STAT_SAVE_ACTIVE)) {
                Log.d(TAG, "onCreate: Restore - SavedInstanceState says SaveSessionPanel is active. Restoring");
                showSaveSessionPanel(false);
                mSessionName.setText(savedInstanceState.getString(VALUE_STAT_SAVE_TITLE));
                if (savedInstanceState.getInt(VALUE_STAT_SAVE_SELECTED) == VALUE_RUNNING) {
                    setRunningButton();
                } else if (savedInstanceState.getInt(VALUE_STAT_SAVE_SELECTED) == VALUE_CYCLING) {
                    setCyclingButton();
                } // TODO Add new buttons here
            } else {
                Log.d(TAG, "onCreate: Restore - SaveSessionPanel was not active, setting it to GONE");
                mStatSaveWindow.setVisibility(View.GONE);
            }
        } else {
            // Remember the height of the mStatSaveWindow and set it to View.GONE
            Log.d(TAG, "onCreate: Restore - No SavedSessionState found");
            Log.d(TAG, "onCreate: Restore - Getting height of mStatSaveWindow");
            mStatSaveWindow.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mStatSaveWindowHeight = mStatSaveWindow.getHeight();

                            mStatSaveWindow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            mStatSaveWindow.setVisibility(View.GONE);
                        }
                    }
            );
            if (isMyServiceRunning()) {
                Log.d(TAG, "onCreate: Restore - Service is running, restoring state");
                setButtonStateStarted(false);
                showStatPanel(false);
                GPSLocationEvent ev = EventBus.getDefault().getStickyEvent(GPSLocationEvent.class);
                mChrono.setBase(ev.getChronoBase());
                mChrono.start();
            } else {
                Log.d(TAG, "onCreate: Restore - No service running");
            }
        }
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: Registering with EventBus");
        if (mEventBus == null) {
            mEventBus = EventBus.getDefault();
            mEventBus.register(this);
        }
    }

    @Override
    public void onStop() {
        // Unregister from EventBus
        Log.d(TAG, "onStop: Unregister from EventBus");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Log.d(TAG, "onSaveInstanceState: We've been asked to save instance state. Complying");
        // Save if we are currently tracking a run
        state.putString(VALUE_RUN_ACTIVE, mStartStopButton.getText().toString());
        if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
            state.putString(VALUE_CURRENT_CHRONO_TEXT, mChrono.getText().toString());
        } else {
            state.putLong(VALUE_CURRENT_CHRONO_BASE, mChrono.getBase());
        }
        state.putInt(VALUE_STAT_SAVE_WINDOW_HEIGHT, mStatSaveWindowHeight);
        if (mStatSaveWindow.getVisibility() == View.GONE) {
            state.putBoolean(VALUE_STAT_SAVE_ACTIVE, false);
        } else {
            state.putBoolean(VALUE_STAT_SAVE_ACTIVE, true);
            if (mCycling.isSelected()) {
                state.putInt(VALUE_STAT_SAVE_SELECTED, VALUE_CYCLING);
            } else if (mRunning.isSelected()) {
                state.putInt(VALUE_STAT_SAVE_SELECTED, VALUE_RUNNING);
            } else {
                state.putInt(VALUE_STAT_SAVE_SELECTED, -1);
            }

            state.putString(VALUE_STAT_SAVE_TITLE, mSessionName.getText().toString());
        }
    }

    /////////// End of housekeeping functions
    @Override
    public void onClick(View v) {
        if (v.equals(mStartStopButton)) {
            if (mStartStopButton.getText().equals(getString(R.string.start_run))) {
                // The user wants to start a run
                startTracking();
            } else if (mStartStopButton.getText().equals(getString(R.string.stop_run))) {
                // The user wants to stop a run
                stopTracking();
            } else if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
                // The user wants to reset the results of a run
                resetTrackingState();
            }
        } else if (v.equals(mSaveRunButton)){
            // The "Save run" button was clicked
            if (!isSaveStatWindowVisible()) {
                showSaveSessionPanel(true);
            } else {
                int mode = getSelectedModeOfTransportation();
                if (mode != -1) {
                    GPSLocationEvent gpsloc = EventBus.getDefault().getStickyEvent(GPSLocationEvent.class);
                    GPSTrackEvent ev = new GPSTrackEvent(
                            gpsloc.getPosition(),
                            mSessionName.getText().toString(),
                            mode);
                    EventBus.getDefault().post(ev);
                }
            }
        } else if (v.equals(mRunning)) {
            setRunningButton();
        } else if (v.equals(mCycling)) {
            setCyclingButton();
        }
    }

    /**
     * Start Tracking
     */
    private void startTracking() {
        // Show the bar with the current information
        showStatPanel(true);
        // Set up button
        setButtonStateStarted(true);
        // Reset the timer and start it
        mChrono.setBase(SystemClock.elapsedRealtime());
        mChrono.start();

        // Start GPS tracking service
        Intent intent = new Intent(getActivity(), GPSTrackingService.class);
        getActivity().startService(intent);
        // TODO Convert to foreground service w/ notification

        Log.d(TAG, "startTracking: Started run");
    }

    /**
     * Stop tracking, retain state
     */
    private void stopTracking() {
        // Stop the clock
        mChrono.stop();
        // Set up button
        setButtonStateStopped(true);

        // Stop GPS tracking service
        Intent intent = new Intent(getActivity(), GPSTrackingService.class);
        getActivity().stopService(intent);

        // FIXME debugging call
        intent = new Intent(getActivity(), PedometerService.class);
        getActivity().stopService(intent);

        // Mark final position during the run
        markFinalPosition();

        Log.d(TAG, "stopTracking: Stopped run");
    }

    /**
     * Reset all the tracking state
     */
    private void resetTrackingState() {
        // Hide the information bar
        hideStatPanel(true);
        // Set up button
        setButtonStateReset(true);
        // Clear all markers and polylines
        mMap.clear();
        mPolyLine = null;
        mStartMarker = null;
        mCurrentVelocity = 0;
        mCurrentDistance = 0;
        mLastCheckedIndex = 0;

        Log.d(TAG, "resetTrackingState: Reset results");
    }

    /**
     * Clear all button highlights in preparation for new ones
     */
    private void clearAllHighlights() {
        mRunning.setSelected(false);
        mCycling.setSelected(false);
        mRunning.clearColorFilter();
        mCycling.clearColorFilter();
        mRunning.setAlpha(1.0f);
        mCycling.setAlpha(1.0f);
        mRunning.setElevation(4.0f);
        mCycling.setElevation(4.0f);
    }
    /**
     * Visually highlight the "cycling" button
     */
    private void setCyclingButton() {
        clearAllHighlights();
        mCycling.setSelected(true);
        mCycling.setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        mRunning.setAlpha(0.3f);
        mCycling.setElevation(6.0f);
    }

    /**
     * Visually Highlight the "running" button
     */
    private void setRunningButton() {
        clearAllHighlights();
        mRunning.setSelected(true);
        mRunning.setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        mCycling.setAlpha(0.3f);
        mRunning.setElevation(6.0f);
    }

    /**
     * Returns the selected mode of transportation
     * @return Returns the selected mode of transportation (as defined in LocationLoggingContract.LocationSessions,
     *         or -1, if none is selected.
     */
    private int getSelectedModeOfTransportation() {
        if (mRunning.isSelected()) {
            return LocationLoggingContract.LocationSessions.VALUE_RUNNING;
        } else if (mCycling.isSelected()) {
            return LocationLoggingContract.LocationSessions.VALUE_CYCLING;
        } else {
            // Nothing is selected :(
            Toast.makeText(getActivity(), "Please select a mode of transportation", Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    /**
     * Sets the button state up to show that tracking is currently active
     * @param animated True if the state change should be animated, false if not
     */
    private void setButtonStateStarted(boolean animated) {
        if (animated) {
            Integer colorOld = ContextCompat.getColor(getActivity(), R.color.start_green);
            Integer colorNew = ContextCompat.getColor(getActivity(), R.color.stop_red);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorOld, colorNew);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mStartStopButton.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
            colorAnimation.start();
            mStartStopButton.setText(R.string.stop_run);
        } else {
            // Set the new background color and text for the button
            mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.stop_red));
            mStartStopButton.setText(R.string.stop_run);
            // mStatWindow.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the button state up to show that tracking has been stopped
     * @param animated True if the state change should be animated, false if not
     */
    private void setButtonStateStopped(boolean animated) {
        // Move the "my position" button out of the way

        setMapPadding(350);

        if (animated) {
            Integer colorOld = ContextCompat.getColor(getActivity(), R.color.stop_red);
            Integer colorNew = ContextCompat.getColor(getActivity(), R.color.reset_blue);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorOld, colorNew);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mStartStopButton.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
            colorAnimation.start();

            mStatButtonPanel.setVisibility(View.VISIBLE);
            mStatButtonPanel.setTranslationY(-(mStatButtonPanel.getHeight()+mStatWindow.getHeight()));
            mStatButtonPanel.setAlpha(0.0f);
            mStatButtonPanel.setTranslationZ(-1.0f);
            mStatButtonPanel.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .translationZ(mStatWindow.getTranslationZ())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mStatButtonPanel.setAlpha(1.0f);
                            mStatButtonPanel.setTranslationZ(mStatWindow.getTranslationZ());
                        }
                    });
            mStartStopButton.setText(R.string.reset_run);
        } else {
            // Update the text and color of the button
            mStartStopButton.setText(R.string.reset_run);
            mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.reset_blue));
            mStatButtonPanel.setTranslationY(0);
            mStatButtonPanel.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sets the button state up to show that the tracking results have been discarded.
     * @param animated True if the change should be animated, false if not
     */
    private void setButtonStateReset(boolean animated) {
        if (animated) {
            Integer colorOld = ContextCompat.getColor(getActivity(), R.color.reset_blue);
            Integer colorNew = ContextCompat.getColor(getActivity(), R.color.start_green);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorOld, colorNew);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mStartStopButton.setBackgroundColor((Integer) animator.getAnimatedValue());
                }

            });
            colorAnimation.start();
            mStartStopButton.setText(R.string.start_run);
        } else {
            // Change color and text of the button
            mStartStopButton.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.start_green));
            mStartStopButton.setText(R.string.start_run);
        }

    }

    /**
     * Show the statistics panel (topmost panel)
     * @param animated True if the change should be animated, false if not
     */
    private void showStatPanel(boolean animated) {
        // Move the "my location" button of the map fragment out of the way of the information bar
        setMapPadding(200);

        if (animated) {
            mStatWindow.setTranslationY(-mStatWindow.getHeight());
            mStatWindow.setVisibility(View.VISIBLE);
            mStatWindow.setAlpha(0.0f);

            mStatWindow.animate()
                    .translationY(0)
                    .alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mStatWindow.setAlpha(1.0f);
                        }
                    });
        } else {
            mStatWindow.setTranslationY(0);
            mStatWindow.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide the statistics (topmost) panel
     * @param animated True if the change should be animated, false if not
     */
    private void hideStatPanel(boolean animated) {
        // Move the "my location" button of the map fragment back to its old position
        setMapPadding(0);
        if (animated) {
            mStatWindow.animate()
                    .translationY(-mStatWindow.getHeight())
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mStatWindow.setVisibility(View.INVISIBLE);
                            mStatWindow.setAlpha(1.0f);
                        }
                    });
        } else {
            mStatWindow.setVisibility(View.INVISIBLE);
        }
        hideStatButtonPanel(animated);
        hideSaveSessionPanel(animated);
    }

    /**
     * Hide the panel with the "save stats"-button
     * @param animated True if the change should be animated, false if not
     */
    private void hideStatButtonPanel(boolean animated) {
        if(animated) {
            mStatButtonPanel.animate()
                    .translationY(-(mStatButtonPanel.getHeight() + mStatWindow.getHeight() + mStatSaveWindow.getHeight()))
                    .alpha(0.0f)
                    .translationZ(-1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mStatButtonPanel.setVisibility(View.GONE);
                        }
                    });
        } else {
            mStatButtonPanel.setVisibility(View.GONE);
        }
    }

    /**
     * Show the panel with the options for saving the session
     * @param animated True if the change should be animated, false if not
     */
    private void showSaveSessionPanel(boolean animated) {
        if (animated) {
            mStatSaveWindow.setVisibility(View.VISIBLE);

            mStatButtonPanel.setTranslationY(-mStatSaveWindowHeight);
            mStatButtonPanel.animate()
                    .translationY(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mStatButtonPanel.setTranslationY(0);
                        }
                    });
        } else {
            mStatSaveWindow.setVisibility(View.VISIBLE);
        }
        // TODO Default to last used or default mode of transportation, if it is ever implemented
    }

    /**
     * Hide the panel with the options for saving the session
     * @param animated True if the change should be animated, false if not
     * @param retainButton True if the save session button should be retained, false otherwise
     */
    private void hideSaveSessionPanel(boolean animated, boolean retainButton) {
        if (animated) {
            if (retainButton) {
                mStatButtonPanel.animate()
                        .translationY(-mStatSaveWindowHeight)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mStatSaveWindow.setVisibility(View.GONE);
                                mStatButtonPanel.setTranslationY(0);
                            }
                        });
            } else {
                mStatSaveWindow.animate()
                        .translationY(-(mStatButtonPanel.getHeight() + mStatSaveWindow.getHeight()))
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mStatSaveWindow.setTranslationY(0);
                                mStatSaveWindow.setAlpha(1.0f);
                                mStatSaveWindow.setVisibility(View.GONE);
                            }
                        });
            }
        } else {
            mStatSaveWindow.setVisibility(View.GONE);
        }
        clearAllHighlights();
    }

    /**
     * Hides the panel with the options for saving the session
     * @param animated True if change should be animated, false otherwise
     */
    private void hideSaveSessionPanel(boolean animated) {
        hideSaveSessionPanel(animated, false);
    }

    /**
     * Check if the Save stat window is visible
     * @return True if it is visible, false otherwise
     */
    private boolean isSaveStatWindowVisible() {
        return mStatSaveWindow.getVisibility() == View.VISIBLE;
    }

    /**
     * Set the map padding, influencing the position of the "my position" button in the map fragment
     * @param top Top padding, as described in the Documentation of GoogleMap.setPadding
     */
    private void setMapPadding(int top) {
        // TODO Is it possible to animate this?
        if (mMap != null) {
            mMap.setPadding(0, top, 0, 0);
        } else {
            mTopPadding = top;
        }
    }

    @Override
    /**
     * Called once the map has finished loading
     */
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        try {
            // Source for this block of code: Modification of the following two StackExchange posts:
            // http://stackoverflow.com/a/20930874/1232833
            // http://stackoverflow.com/a/14511032/1232833

            // Get location from GPS if it's available
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Location wasn't found, check the next most accurate place for the current location
            if (myLocation == null) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                // Finds a provider that matches the criteria
                String provider = lm.getBestProvider(criteria, true);
                // Use the provider to get the last known location
                myLocation = lm.getLastKnownLocation(provider);
            }
            if (myLocation != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))      // Sets the center of the map to location user
                        .zoom(17)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

        } catch (SecurityException e) {
            Log.e(TAG, "User rejected access to position data");
            // TODO Give indication to the user that GPS tracking will not work without the perm.
        }
        // If the tracking is stopped (but not reset), we are returning from a prior instance.
        // Since it is not possible to store the PolyLine in the Bundle, we re-request the last
        // location update to take care of redrawing everything
        if (isStopped() || isRunning()) {
            Log.d(TAG, "OnMapReady: Requesting sticky event to redraw path");
            mLastCheckedIndex = 0;
            mCurrentDistance = 0;
            mCurrentVelocity = 0;
            GPSLocationEvent ev = EventBus.getDefault().getStickyEvent(GPSLocationEvent.class);
            if (ev != null) {
                onEventMainThread(ev);
            }
            if (isStopped()) {
                markFinalPosition();
            }
        }

        if (mTopPadding != 0) {
            mMap.setPadding(0, mTopPadding, 0, 0);
        }
    }

    /**
     * Event Handling function for EventBus. Called on Main (UI) Thread => Allows updating the UI
     * @param ev A GPSLocationEvent containing the current location and a timestamp
     */
    public void onEventMainThread(GPSLocationEvent ev) {
        Log.d(TAG, "onEventMainThread: Received update, updating map");
        if (mStartMarker == null) {
            Location start = ev.getPosition().get(0);

            // Set icon for start of route
            IconGenerator ig = new IconGenerator(getActivity());
            ig.setStyle(IconGenerator.STYLE_GREEN);
            Bitmap startPoint = ig.makeIcon("Start");
            mStartMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                    .position(new LatLng(start.getLatitude(), start.getLongitude())));

        }
        if (mPolyLine == null) {
            // Set first element of polyline
            PolylineOptions poptions = new PolylineOptions();
            for (Location l : ev.getPosition()) {
                poptions.add(new LatLng(l.getLatitude(), l.getLongitude()));
            }
            mPolyLine = mMap.addPolyline(poptions);
        } else {
            // Update PolyLine with new points (can only be done through complete refresh, sadly)
            List<LatLng> points = mPolyLine.getPoints();
            for (int i=mLastCheckedIndex+1; i < ev.getPosition().size(); i++) {
                Location element = ev.getPosition().get(i);
                points.add(new LatLng(element.getLatitude(), element.getLongitude()));
            }
            mPolyLine.setPoints(points);
        }
        // Re-center the camera
        Location current = ev.getPosition().get(ev.getPosition().size() - 1);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(current.getLatitude(), current.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(current.getBearing()) // Set bearing
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Update current distance
        for (int i = mLastCheckedIndex + 1; i < ev.getPosition().size(); i++) {
            float newDistance = mCurrentDistance + ev.getPosition().get(i).distanceTo(ev.getPosition().get(i - 1));
            // Check if we crossed an interval where we want to set a bubble on the map
            if (mCurrentDistance % 1000 > newDistance % 1000) {
                int kilometres = (int) newDistance / 1000;
                LatLng pin = mPolyLine.getPoints().get(i);
                IconGenerator ig = new IconGenerator(getActivity());
                ig.setStyle(IconGenerator.STYLE_BLUE);
                Bitmap startPoint = ig.makeIcon(kilometres + " km");
                mStartMarker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                        .position(pin));
            }
            mCurrentDistance = newDistance;
        }
        // Calculate average speed
        // Get elapsed time in minutes
        float elapsedSeconds = (SystemClock.elapsedRealtime() - mChrono.getBase()) / 1000.0f;
        // Divide distance in metres by elapsed minutes, multiply with 3.6 to get km/h
        mCurrentVelocity = (mCurrentDistance / elapsedSeconds) * 3.6f;

        // Update widgets to display the new values
        updateVelocityAndDistanceWidgets();


        // Update last checked index
        mLastCheckedIndex = ev.getPosition().size()-1;
    }

    /**
     * Set a marker at the final position of the route that was just tracked.
     */
    private void markFinalPosition() {
        // Get final position
        LatLng finalPos = mPolyLine.getPoints().get(mPolyLine.getPoints().size()-1);
        // Get Icon Generator
        IconGenerator ig = new IconGenerator(getActivity());
        // Set up style
        ig.setStyle(IconGenerator.STYLE_RED);
        Bitmap startPoint = ig.makeIcon("Finish");
        // Create marker
        mStartMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                .position(finalPos));
    }

    /**
     * Updates the widgets for velocity and distance in the stat panel
     */
    private void updateVelocityAndDistanceWidgets() {
        // Update Velocity and Distance widgets
        if (mCurrentDistance < 1000) {
            mDistance.setText(String.format(getString(R.string.distance_m), (int) mCurrentDistance));
        } else {
            mDistance.setText(String.format(getString(R.string.distance_km), mCurrentDistance / 1000.0f));
        }
        mVelocity.setText(String.format(getString(R.string.velocity_kmh), mCurrentVelocity));
    }

    /**
     * Check if the current state of the tracking system is reset (no data available)
     * @return True if tracking is not running and no data is available, false otherwise
     */
    @SuppressWarnings("unused")
    private boolean isReset() {
        return mStartStopButton.getText().equals(getString(R.string.start_run));
    }

    /**
     * Check if the system is currently tracking
     * @return True if the system is actively tracking, false otherwise
     */
    private boolean isRunning() {
        return mStartStopButton.getText().equals(getString(R.string.stop_run));
    }

    /**
     * Checks if the GPSTrackingService is running
     * @return True if the service is running, false otherwise
     */
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("de.velcommuta.denul.service.GPSTrackingService".equals(service.service.getClassName())) { // TODO Update if
                return true; // Package name matches, our service is running
            }
        }
        return false; // No matching package name found => Our service is not running
    }

    /**
     * Check if the system has stopped tracking, but is still retaining the data
     * @return True if the system is no longer tracking, but still has data available, false otherwise
     */
    private boolean isStopped() {
        return mStartStopButton.getText().equals(getString(R.string.reset_run));
    }


    /**
     * EventBus function to perform Database interactions in an AsyncThread (in the background)
     * @param ev Event containing the track and other information that should be saved to the database
     */
    @SuppressWarnings("unused")
    public void onEventAsync(GPSTrackEvent ev) {
        // Get a handler on the database
        SQLiteDatabase db = ((MainActivity)getActivity()).getLocationDatabaseHandler();
        if (db == null) {
            EventBus.getDefault().post(new DatabaseResultEvent("Database not open"));
            return;
        }

        // Ensure that the event is sane
        if (ev.getPosition().size() != ev.getPosition().size()) {
            EventBus.getDefault().post(new DatabaseResultEvent("Position list has different size than timestsamp list - aborting"));
            return;
        }

        // Start a transaction to get an all-or-nothing write to the database
        db.beginTransaction();
        // Write new database entry with metadata for the track
        ContentValues metadata = new ContentValues();

        metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_START, ev.getPosition().get(0).getTime());
        metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_SESSION_END, ev.getPosition().get(ev.getPosition().size() - 1).getTime());
        metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_NAME, ev.getSessionName());
        metadata.put(LocationLoggingContract.LocationSessions.COLUMN_NAME_MODE, ev.getModeOfTransportation());

        long rowid = db.insert(LocationLoggingContract.LocationSessions.TABLE_NAME, null, metadata);

        // Write the individual steps in the track
        for (int i = 0; i < ev.getPosition().size(); i++) {
            // Prepare ContentValues object
            ContentValues entry = new ContentValues();
            // Get Location object
            Location cLoc = ev.getPosition().get(i);
            // Set values for ContentValues
            entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_SESSION, rowid);
            entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_LAT, cLoc.getLatitude());
            entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_LONG, cLoc.getLongitude());
            entry.put(LocationLoggingContract.LocationLog.COLUMN_NAME_TIMESTAMP, cLoc.getTime());
            // Save ContentValues into Database
            db.insert(LocationLoggingContract.LocationLog.TABLE_NAME, null, entry);
        }
        // Finish transaction
        db.endTransaction();
        // Notify main thread
        EventBus.getDefault().post(new DatabaseResultEvent(getString(R.string.save_success)));
    }

    /**
     * Display error or success messages from the database client in a Toast
     * @param ev The event with the message
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(DatabaseResultEvent ev) {
        Toast.makeText(getActivity(), ev.getMessage(), Toast.LENGTH_SHORT).show();
        if (ev.getMessage().equals(getString(R.string.save_success))) {
            hideSaveSessionPanel(true, true);
        }
    }
}
