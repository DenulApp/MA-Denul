package de.velcommuta.denul.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.List;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.event.GPSLocationEvent;
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
    private LinearLayout mStatWindow;
    private Chronometer mChrono;
    private TextView mVelocity;
    private TextView mDistance;

    // State variables
    private int mLastCheckedIndex;
    private float mCurrentDistance;
    private float mCurrentVelocity;

    // Identifier Strings for Bundle
    public static final String VALUE_RUN_ACTIVE          = "value-run-active";
    public static final String VALUE_LAST_CHECKED_INDEX  = "value-last-checked-index";
    public static final String VALUE_CURRENT_DISTANCE    = "value-current-distance";
    public static final String VALUE_CURRENT_VELOCITY    = "value-current-velocity";
    public static final String VALUE_CURRENT_CHRONO_BASE = "value-current-chrono-base";
    public static final String VALUE_CURRENT_CHRONO_TEXT = "value-current-chrono-text";

    private OnFragmentInteractionListener mListener;


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
        mStartStopButton = (Button)       v.findViewById(R.id.actionbutton);
        mStatWindow      = (LinearLayout) v.findViewById(R.id.statwindow);
        mChrono          = (Chronometer)  v.findViewById(R.id.timer);
        mVelocity        = (TextView)     v.findViewById(R.id.speedfield);
        mDistance        = (TextView)     v.findViewById(R.id.distancefield);

        // Set up this fragment as the OnClickListener of the start/stop/reset button
        mStartStopButton.setOnClickListener(this);
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate: SavedInstanceState is not empty, restoring.");
            if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.stop_run))) {
                mLastCheckedIndex = savedInstanceState.getInt(VALUE_LAST_CHECKED_INDEX);
                mCurrentDistance = savedInstanceState.getFloat(VALUE_CURRENT_DISTANCE);
                mCurrentVelocity = savedInstanceState.getFloat(VALUE_CURRENT_VELOCITY);
                setButtonStateStarted();
                updateVelocityAndDistanceWidgets();
                mChrono.setBase(savedInstanceState.getLong(VALUE_CURRENT_CHRONO_BASE));
                mChrono.start();
                mStatWindow.setVisibility(View.VISIBLE);
            } else if (savedInstanceState.getString(VALUE_RUN_ACTIVE).equals(getString(R.string.reset_run))) {
                mLastCheckedIndex = savedInstanceState.getInt(VALUE_LAST_CHECKED_INDEX);
                mCurrentDistance = savedInstanceState.getFloat(VALUE_CURRENT_DISTANCE);
                mCurrentVelocity = savedInstanceState.getFloat(VALUE_CURRENT_VELOCITY);
                setButtonStateStopped();
                updateVelocityAndDistanceWidgets();
                mStatWindow.setVisibility(View.VISIBLE);
                mChrono.setText(savedInstanceState.getString(VALUE_CURRENT_CHRONO_TEXT));
            }
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
        // Theoretically, we would register with EventBus here. However, since we are using sticky
        // events, this could lead to race conditions between the EventBus and the MapFragment.
        // Hence, we register in the onMapReady function.
        // Register with EventBus, for the reasons outlined in the onStart-Method
        Log.i(TAG, "onMapReady: Registering with EventBus");
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
        // Save last seen index
        state.putInt(VALUE_LAST_CHECKED_INDEX, mLastCheckedIndex);
        // Save current distance and average velocity
        state.putFloat(VALUE_CURRENT_DISTANCE, mCurrentDistance);
        state.putFloat(VALUE_CURRENT_VELOCITY, mCurrentVelocity);
        if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
            state.putString(VALUE_CURRENT_CHRONO_TEXT, mChrono.getText().toString());
        } else {
            state.putLong(VALUE_CURRENT_CHRONO_BASE, mChrono.getBase());
        }
    }

    /////////// End of housekeeping functions
    @Override
    public void onClick(View v) {
        if (mStartStopButton.getText().equals(getString(R.string.start_run))) {
            // The user wants to start a run
            // Show the bar with the current information
            mStatWindow.setVisibility(LinearLayout.VISIBLE);
            // Set up button
            setButtonStateStarted();
            // Move the "my location" button of the map fragment out of the way of the information bar
            mMap.setPadding(0, 200, 0, 0);
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
            setButtonStateStopped();

            // Stop GPS tracking service
            Intent intent = new Intent(getActivity(), GPSTrackingService.class);
            getActivity().stopService(intent);

            Log.d(TAG, "onClick: Stopped run");

        } else if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
            // The user wants to reset the results of a run
            // Hide the information bar
            mStatWindow.setVisibility(LinearLayout.INVISIBLE);
            // Set up button
            setButtonStateReset();
            // Move the "my location" button back to its original location
            mMap.setPadding(0, 0, 0, 0);
            // Clear all markers and polylines
            mMap.clear();
            mPolyLine = null;
            mStartMarker = null;
            mCurrentVelocity = 0;
            mCurrentDistance = 0;
            mLastCheckedIndex = 0;

            Log.d(TAG, "onClick: Reset results");
        }
    }

    private void setButtonStateStarted() {
        // Set the new background color and text for the button
        mStartStopButton.setBackgroundColor(Color.parseColor("#F76F6F")); // FIXME Port to XML
        mStartStopButton.setText(getString(R.string.stop_run));
    }

    private void setButtonStateStopped() {
        // Update the text and color of the button
        mStartStopButton.setText(getString(R.string.reset_run));
        mStartStopButton.setBackgroundColor(Color.parseColor("#FF656BFF")); // FIXME Port to XML
    }

    private void setButtonStateReset() {
        // Change color and text of the button
        mStartStopButton.setBackgroundColor(Color.parseColor("#00D05D")); // FIXME Port to XML
        mStartStopButton.setText(R.string.start_run);
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
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .build();                   // Creates a CameraPosition from the builder

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        } catch (SecurityException e) {
            Log.e(TAG, "User rejected access to position data");
            // TODO Give indication to the user that GPS tracking will not work without the perm.
        }

    }

    /**
     * Event Handling function for EventBus. Called on Main (UI) Thread => Allows updating the UI
     * @param ev A GPSLocationEvent containing the current location and a timestamp
     */
    public void onEventMainThread(GPSLocationEvent ev) {
        Log.d(TAG, "onEventMainThread: Received update, updating map");
        if (mStartMarker == null) {
            Location start = ev.position.get(0);

            // Set icon for start of route
            IconGenerator ig = new IconGenerator(getActivity());
            ig.setStyle(IconGenerator.STYLE_BLUE);
            Bitmap startPoint = ig.makeIcon("Start");
            mStartMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                    .position(new LatLng(start.getLatitude(), start.getLongitude())));

        }
        if (mPolyLine == null) {
            // Set first element of polyline
            PolylineOptions poptions = new PolylineOptions();
            for (Location l : ev.position) {
                poptions.add(new LatLng(l.getLatitude(), l.getLongitude()));
            }
            mPolyLine = mMap.addPolyline(poptions);
        } else {
            // Update PolyLine with new points (can only be done through complete refresh, sadly)
            List<LatLng> points = mPolyLine.getPoints();
            for (int i=mLastCheckedIndex+1; i < ev.position.size(); i++) {
                Location element = ev.position.get(i);
                points.add(new LatLng(element.getLatitude(), element.getLongitude()));
            }
            mPolyLine.setPoints(points);
        }
        // Re-center the camera
        Location current = ev.position.get(ev.position.size() - 1);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(current.getLatitude(), current.getLongitude()))      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                .bearing(current.getBearing()) // Set bearing
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Update current distance
        for (int i=mLastCheckedIndex+1; i < ev.position.size(); i++) {
            mCurrentDistance = mCurrentDistance + ev.position.get(i).distanceTo(ev.position.get(i-1));
        }
        // Calculate average speed
        // Get elapsed time in minutes
        float elapsedSeconds = (SystemClock.elapsedRealtime() - mChrono.getBase()) / 1000.0f;
        // Divide distance in metres by elapsed minutes, multiply with 3.6 to get km/h
        mCurrentVelocity = (mCurrentDistance / elapsedSeconds) * 3.6f;

        // Update widgets to display the new values
        updateVelocityAndDistanceWidgets();

        // Update last checked index
        mLastCheckedIndex = ev.position.size()-1;
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
