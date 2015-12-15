package de.velcommuta.denul.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.util.ShareManager;

/**
 * Activity to show details about a specific track
 */
public class ExerciseViewActivity extends AppCompatActivity implements ServiceConnection, OnMapReadyCallback {
    private static final String TAG = "ExerciseViewActivity";

    private DatabaseServiceBinder mDbBinder;
    private GPSTrack mTrack;
    private int mTrackId;

    private TextView mTrackTitle;
    private TextView mTrackDate;
    private TextView mTrackDistance;
    private TextView mOwner;
    private ImageView mTrackMode;
    private GoogleMap mMap;

    private Marker mStartMarker;
    private Marker mEndMarker;
    private Polyline mPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_show);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        requestDatabaseBinder();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            mTrackId = b.getInt("track-id");
        } else {
            Log.e(TAG, "onCreate: No Bundle passed, returning");
            finish();
        }
        mTrackTitle = (TextView) findViewById(R.id.exc_view_title);
        mTrackDate = (TextView) findViewById(R.id.exc_view_date);
        mTrackDistance = (TextView) findViewById(R.id.exc_view_distance);
        mTrackMode = (ImageView) findViewById(R.id.exc_view_mode);
        mOwner = (TextView) findViewById(R.id.exc_view_owner);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mTrack.getOwner() == -1) {
            getMenuInflater().inflate(R.menu.activity_exercise_view, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_exercise_view_shared, menu);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_delete:
                askDeleteConfirm();
                return true;
            case R.id.action_rename:
                performRename();
                return true;
            case R.id.action_share:
                performShare();
                return true;
        }
        return false;
    }


    /**
     * Ask the user to confirm the deletion request, and perform the deletion if it was confirmed
     */
    private void askDeleteConfirm() {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the values, build and show the dialog
        builder.setMessage("Delete this exercise?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mDbBinder.deleteGPSTrack(mTrack);
                        Toast.makeText(ExerciseViewActivity.this, "Exercise deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .create().show();
    }


    /**
     * Rename the Exercise and write the updated friend to the database
     */
    private void performRename() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText newName = new EditText(this);
        newName.setText(mTrack.getSessionName());
        builder.setView(newName);
        builder.setTitle("Enter a new Name:");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selectedName = newName.getText().toString().trim();
                if (selectedName.equals("")) {
                    Toast.makeText(ExerciseViewActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                // if the name has not changed, do nothing
                if (selectedName.equals(mTrack.getSessionName())) return;
                // Name is available. Update Track object
                mDbBinder.renameGPSTrack(mTrack, selectedName);
                // Reload
                loadTrackInformation();
            }
        });
        // Set cancel buttel
        builder.setNegativeButton("Cancel", null);
        // Create and show the dialog
        builder.create().show();
    }


    /**
     * Ask the user with whom he wants to share the data, and perform the actual sharing
     */
    private void performShare() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Create a ListView to display the Friendlist
        View dialog = getLayoutInflater().inflate(R.layout.dialog_share, null);
        final ListView lv = (ListView) dialog.findViewById(R.id.share_menu_friendlist);
        // Set the ListView to allow multiple selections
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        // Load the friend list from the database
        final List<Friend> friendlist = mDbBinder.getFriends();
        // Set up the adapter
        lv.setAdapter(new ArrayAdapter<Friend>(this, android.R.layout.simple_list_item_multiple_choice, friendlist));
        // Pre-check those friends that have already received the share
        List<Friend> sharedFriends = mDbBinder.getShareRecipientsForShareable(mTrack);
        for (Friend f : sharedFriends) {
            lv.setItemChecked(sharedFriends.indexOf(f), true);
        }
        // Populate the list of share granularity options
        final Spinner granularitySpinner = (Spinner) dialog.findViewById(R.id.share_menu_granularity);
        final ArrayAdapter<CharSequence> granularityAdapter = ArrayAdapter.createFromResource(this, mTrack.getGranularityDescriptor(), android.R.layout.simple_list_item_1);
        granularitySpinner.setAdapter(granularityAdapter);
        // Set the description
        final EditText description = (EditText) dialog.findViewById(R.id.share_menu_description);
        if (mTrack.getDescription() != null) {
            description.setText(mTrack.getDescription());
        }
        int granularity = mDbBinder.getShareGranularity(mTrack);
        if (granularity != -1) {
            granularitySpinner.setSelection(granularity);
            granularitySpinner.setEnabled(false);
            description.setEnabled(false);
        }
        // Set the finished List as the view of the AlertDialog
        builder.setView(dialog);
        // Set title
        builder.setTitle("Select a friend:");
        // Set "OK" button with callback
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int selected) {
                // Apply the description
                mTrack.setDescription(description.getText().toString().trim());
                mDbBinder.updateShareableDescription(mTrack);
                // Read out which friends have been selected
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                List<Friend> rcpt = new LinkedList<>();
                for (int i = 0; i<checked.size(); i++) {
                    if (checked.valueAt(i))
                        rcpt.add(friendlist.get(checked.keyAt(i)));
                }
                // Check if any have been selected
                if (rcpt.size() > 0) {
                    int granularity = granularitySpinner.getSelectedItemPosition();
                    // Prepare a ShareWithProgress AsyncTask with nested Callback
                    // TODO Proper callback and progress bar?
                    ShareManager.ShareWithProgress m = new ShareManager().new ShareWithProgress(mDbBinder, new ShareManager.ShareManagerCallback() {
                        @Override
                        public void onShareStatusUpdate(int status) {
                            // Toast.makeText(ExerciseViewActivity.this, ""+status, Toast.LENGTH_SHORT).show();
                        }


                        @Override
                        public void onShareFinished(boolean success) {
                            if (success)
                                Toast.makeText(ExerciseViewActivity.this, "Done", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(ExerciseViewActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, granularity, mTrack);
                    m.execute(rcpt);
                } else {
                    Toast.makeText(ExerciseViewActivity.this, "No one selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set cancel buttel
        builder.setNegativeButton("Cancel", null);
        // Create and show the dialog
        builder.create().show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    /**
     * Request a binder to the database service
     */
    private void requestDatabaseBinder() {
        if (!DatabaseService.isRunning(this)) {
            Log.w(TAG, "bindDbService: Trying to bind to a non-running database service. Aborting");
        }
        Intent intent = new Intent(this, DatabaseService.class);
        if (!bindService(intent, this, 0)) {
            Log.e(TAG, "bindDbService: An error occured during binding :(");
        } else {
            Log.d(TAG, "bindDbService: Database service binding request sent");
        }
    }


    /**
     * Load the track information and display it
     */
    private void loadTrackInformation() {
        // Load track
        mTrack = mDbBinder.getGPSTrackById(mTrackId);
        if (mTrack.getPosition().size() != 0) {
            // Get a reference to the Map fragment and perform an async. initialization
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.exc_view_gmap);
            mapFragment.getMapAsync(this);
        } else {
            // TODO Choose a different background instead of loading the map, as we do not have a path to display
        }
        // Set title
        mTrackTitle.setText(mTrack.getSessionName());
        // Set date
        mTrackDate.setText(DateTimeFormat.shortDateTime().print(new LocalDateTime(mTrack.getTimestamp(), DateTimeZone.forID(mTrack.getTimezone()))));
        float distance = mTrack.getDistance();
        if (distance < 1000.0f) {
            mTrackDistance.setText(String.format(getString(R.string.distance_m), (int) distance));
        } else {
            mTrackDistance.setText(String.format(getString(R.string.distance_km), (int) distance / 1000.0f));
        }
        switch (mTrack.getModeOfTransportation()) {
            case GPSTrack.VALUE_RUNNING:
                mTrackMode.setImageDrawable(getResources().getDrawable(R.drawable.ic_running));
                break;
            case GPSTrack.VALUE_CYCLING:
                mTrackMode.setImageDrawable(getResources().getDrawable(R.drawable.ic_cycling));
                break;
            default:
                Log.w(TAG, "loadTrackInformation: Unknown Mode of transportation");
                mTrackMode.setImageDrawable(getResources().getDrawable(R.drawable.ic_running));
        }
        if (mMap != null) drawPath();
        if (mTrack.getOwner() != -1) {
            mOwner.setVisibility(View.VISIBLE);
            mOwner.setText("Shared by: " + mDbBinder.getFriendById(mTrack.getOwner()).getName());
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
        loadTrackInformation();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: Lost DB service");
        mDbBinder = null;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Disable display of device location
        mMap.setMyLocationEnabled(false);
        if (mTrack != null) drawPath();
    }


    /**
     * Draw the path in the {@link GPSTrack} object
     */
    private void drawPath() {
        // if the map has already been drawn to, just return
        if ((mStartMarker != null && mEndMarker != null && mPolyline != null) || mTrack.getPosition().size() == 0) return;
        // Draw start marker
        Location start = mTrack.getPosition().get(0);
        // Set icon for start of route
        IconGenerator ig = new IconGenerator(this);
        ig.setStyle(IconGenerator.STYLE_GREEN);
        Bitmap startPoint = ig.makeIcon("Start");
        mStartMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(startPoint))
                .position(new LatLng(start.getLatitude(), start.getLongitude())));

        // Draw polyline and prepare LatLngBounds object for later camera zoom
        PolylineOptions poptions = new PolylineOptions();
        LatLngBounds.Builder latlngbounds = new LatLngBounds.Builder();
        for (Location l : mTrack.getPosition()) {
            poptions.add(new LatLng(l.getLatitude(), l.getLongitude()));
            latlngbounds.include(new LatLng(l.getLatitude(), l.getLongitude()));
        }
        mPolyline = mMap.addPolyline(poptions);

        // Get final position
        Location finalPos = mTrack.getPosition().get(mTrack.getPosition().size()-1);
        // Set up style
        ig.setStyle(IconGenerator.STYLE_RED);
        Bitmap endPoint = ig.makeIcon("Finish");
        // Create marker
        mEndMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(endPoint))
                .position(new LatLng(finalPos.getLatitude(), finalPos.getLongitude())));

        // Move the camera to show the whole path
        // Code credit: http://stackoverflow.com/a/14828739/1232833
        int padding = 100;
        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latlngbounds.build(), padding);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.animateCamera(cu);
            }
        });
    }
}
