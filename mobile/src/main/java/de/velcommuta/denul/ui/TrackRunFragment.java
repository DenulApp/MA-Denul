package de.velcommuta.denul.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TrackRunFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
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
    private LinearLayout mStatContainer;
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

    // Identifier Strings for Bundle
    public static final String VALUE_RUN_ACTIVE              = "value-run-active";
    public static final String VALUE_CURRENT_CHRONO_BASE     = "value-current-chrono-base";
    public static final String VALUE_CURRENT_CHRONO_TEXT     = "value-current-chrono-text";
    public static final String VALUE_STAT_SAVE_WINDOW_HEIGHT = "value-stat-save-window-height";
    public static final String VALUE_STAT_SAVE_ACTIVE        = "value-stat-save-active";
    public static final String VALUE_STAT_SAVE_SELECTED      = "value-stat-save-selected";
    public static final String VALUE_STAT_SAVE_TITLE         = "value-stat-save-title";

    private OnFragmentInteractionListener mListener;

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
        mStatContainer   = (LinearLayout) v.findViewById(R.id.stat_window_container);
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

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: SavedInstanceState is not empty, restoring.");
            if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.stop_run))) {
                setButtonStateStarted(false);
                mChrono.setBase(savedInstanceState.getLong(VALUE_CURRENT_CHRONO_BASE));
                mChrono.start();
                showStatPanel(false);
            } else if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.reset_run))) {
                setButtonStateStopped(false);
                // markFinalPosition();
                showStatPanel(false);
                mChrono.setText(savedInstanceState.getString(VALUE_CURRENT_CHRONO_TEXT));
            }
            mStatSaveWindowHeight = savedInstanceState.getInt(VALUE_STAT_SAVE_WINDOW_HEIGHT);
            if (savedInstanceState.getBoolean(VALUE_STAT_SAVE_ACTIVE)) {
                showSaveSessionPanel(false);
                mSessionName.setText(savedInstanceState.getString(VALUE_STAT_SAVE_TITLE));
                // TODO Restore selections on buttons
            } else {
                mStatSaveWindow.setVisibility(View.GONE);
            }
        } else {
            // Remember the height of the mStatSaveWindow and set it to View.GONE
            mStatSaveWindow.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mStatSaveWindowHeight = mStatSaveWindow.getHeight();
                            Log.d(TAG, "onCreate: mStatSaveWindowHeight = " + mStatSaveWindowHeight);

                            mStatSaveWindow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            mStatSaveWindow.setVisibility(View.GONE);
                        }
                    }
            );
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: Registering with EventBus");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        // Unregister from EventBus
        Log.d(TAG, "onStop: Unregister from EventBus");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
            state.putInt(VALUE_STAT_SAVE_SELECTED, 0); // FIXME Proper values
            state.putString(VALUE_STAT_SAVE_TITLE, mSessionName.getText().toString());
        }
    }

    /////////// End of housekeeping functions
    @Override
    public void onClick(View v) {
        if (v.equals(mStartStopButton)) {
            if (mStartStopButton.getText().equals(getString(R.string.start_run))) {
                // The user wants to start a run
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

                Log.d(TAG, "onClick: Started run");

            } else if (mStartStopButton.getText().equals(getString(R.string.stop_run))) {
                // The user wants to stop a run
                // Stop the clock
                mChrono.stop();
                // Set up button
                setButtonStateStopped(true);

                // Stop GPS tracking service
                Intent intent = new Intent(getActivity(), GPSTrackingService.class);
                getActivity().stopService(intent);

                // Mark final position during the run
                markFinalPosition();

                Log.d(TAG, "onClick: Stopped run");

            } else if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
                // The user wants to reset the results of a run
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

                Log.d(TAG, "onClick: Reset results");
            }
        } else {
            showSaveSessionPanel(true);
            /*
            GPSLocationEvent gpsloc = EventBus.getDefault().getStickyEvent(GPSLocationEvent.class);
            GPSTrackEvent ev = new GPSTrackEvent(
                    gpsloc.getPosition(),
                    "Test");
            EventBus.getDefault().post(ev);
            */
        }
    }

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
            mStatButtonPanel.setVisibility(View.VISIBLE);
        }
    }

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
            mStatButtonPanel.setVisibility(View.VISIBLE);
        }
    }

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

    private void showSaveSessionPanel(boolean animated) {
        if (animated) {
            mStatSaveWindow.setVisibility(View.VISIBLE);

            mStatButtonPanel.setTranslationY(-mStatSaveWindowHeight);
            mStatButtonPanel.animate()
                    .translationY(0);
        } else {
            mStatSaveWindow.setVisibility(View.VISIBLE);
        }
    }

    private void hideSaveSessionPanel(boolean animated) {
        if (animated) {
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
        } else {
            mStatSaveWindow.setVisibility(View.GONE);
        }
    }

    private void setMapPadding(int top) {
        // TODO Is it possible to animate this?
        if (mMap != null) {
            mMap.setPadding(0, top, 0, 0);
        } else {
            mTopPadding = top;
        }
    }

    @Override
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

    private void updateVelocityAndDistanceWidgets() {
        // Update Velocity and Distance widgets
        if (mCurrentDistance < 1000) {
            mDistance.setText(String.format(getString(R.string.distance_m), (int) mCurrentDistance));
        } else {
            mDistance.setText(String.format(getString(R.string.distance_km), mCurrentDistance / 1000.0f));
        }
        mVelocity.setText(String.format(getString(R.string.velocity_kmh), mCurrentVelocity));
    }

    private boolean isReset() {
        return mStartStopButton.getText().equals(getString(R.string.start_run));
    }

    private boolean isRunning() {
        return mStartStopButton.getText().equals(getString(R.string.stop_run));
    }

    private boolean isStopped() {
        return mStartStopButton.getText().equals(getString(R.string.reset_run));
    }


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
        EventBus.getDefault().post(new DatabaseResultEvent("Track saved"));
    }

    /**
     * Display error or success messages from the database client in a Toast
     * @param ev The event with the message
     */
    public void onEventMainThread(DatabaseResultEvent ev) {
        Toast.makeText(getActivity(), ev.getMessage(), Toast.LENGTH_SHORT).show();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
}
