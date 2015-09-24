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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

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
    private static final String TAG = "TrackRunFragment";

    private MapFragment mMapFragment;
    private GoogleMap mMap;

    private Button mStartStopButton;
    private LinearLayout mStatWindow;
    private Chronometer mChrono;
    private TextView mVelocity;
    private TextView mDistance;

    private OnFragmentInteractionListener mListener;

    private Polyline mPolyLine;


    // TODO Lifecycle management
    ////////// Housekeeping functions (onCreate etc)
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TrackRunFragment.
     */
    public static TrackRunFragment newInstance() {
        TrackRunFragment fragment = new TrackRunFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
        mMapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.gmaps);
        mMapFragment.getMapAsync(this);

        // Grab references to UI elements
        mStartStopButton = (Button)       v.findViewById(R.id.actionbutton);
        mStatWindow      = (LinearLayout) v.findViewById(R.id.statwindow);
        mChrono          = (Chronometer)  v.findViewById(R.id.timer);
        mVelocity        = (TextView)     v.findViewById(R.id.speedfield);
        mDistance        = (TextView)     v.findViewById(R.id.distancefield);

        // Set up this fragment as the OnClickListener of the start/stop/reset button
        mStartStopButton.setOnClickListener(this);

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

    /////////// End of housekeeping functions
    @Override
    public void onClick(View v) {
        if (mStartStopButton.getText().equals(getString(R.string.start_run))) {
            // The user wants to start a run
            // Show the bar with the current information
            mStatWindow.setVisibility(LinearLayout.VISIBLE);
            // Set the new background color and text for the button
            mStartStopButton.setBackgroundColor(Color.parseColor("#F76F6F")); // FIXME Port to XML
            mStartStopButton.setText(getString(R.string.stop_run));
            // Move the "my location" button of the map fragment out of the way of the information bar
            mMap.setPadding(0, 200, 0, 0);
            // Reset the timer and start it
            mChrono.setBase(SystemClock.elapsedRealtime());
            mChrono.start();

            // Start GPS tracking service
            Intent intent = new Intent(getActivity(), GPSTrackingService.class);
            getActivity().startService(intent);

            Log.d(TAG, "onClick: Started run");

        } else if (mStartStopButton.getText().equals(getString(R.string.stop_run))) {
            // The user wants to stop a run
            // Stop the clock
            mChrono.stop();
            // Update the text and color of the button
            mStartStopButton.setText(getString(R.string.reset_run));
            mStartStopButton.setBackgroundColor(Color.parseColor("#FF656BFF")); // FIXME Port to XML

            // Stop GPS tracking service
            Intent intent = new Intent(getActivity(), GPSTrackingService.class);
            getActivity().stopService(intent);

            Log.d(TAG, "onClick: Stopped run");

        } else if (mStartStopButton.getText().equals(getString(R.string.reset_run))) {
            // The user wants to reset the results of a run
            // Hide the information bar
            mStatWindow.setVisibility(LinearLayout.INVISIBLE);
            // Change color and text of the button
            mStartStopButton.setBackgroundColor(Color.parseColor("#00D05D")); // FIXME Port to XML
            mStartStopButton.setText(R.string.start_run);
            // Move the "my location" button back to its original location
            mMap.setPadding(0, 0, 0, 0);
            // Clear all markers and polylines
            mMap.clear();
            mPolyLine = null;

            Log.d(TAG, "onClick: Reset run results");
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
        if (ev.isInitial && ev.position.size() == 1) {
            // Set icon for start of route
            IconGenerator ig = new IconGenerator(getActivity());
            ig.setStyle(IconGenerator.STYLE_BLUE);
            Bitmap startPoint = ig.makeIcon("Start");
            mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                    .position(ev.position.get(0)));
        }
        if (mPolyLine == null) {
            // Set first element of polyline
            PolylineOptions poptions = new PolylineOptions();
            for (LatLng l : ev.position) {
                poptions.add(l);
            }
            mPolyLine = mMap.addPolyline(poptions);
        } else {
            // Update PolyLine with new points (can only be done through complete refresh, sadly)
            mPolyLine.setPoints(ev.position);
        }

        // TODO Center Camera on new position / setup camera to follow user
        // TODO Calculate and update distance
        // TODO Calculate and update speed
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
