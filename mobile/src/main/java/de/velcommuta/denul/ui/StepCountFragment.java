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

import net.sqlcipher.Cursor;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import de.greenrobot.event.EventBus;
import de.velcommuta.denul.R;
import de.velcommuta.denul.db.StepLoggingContract;
import de.velcommuta.denul.event.DatabaseAvailabilityEvent;
import de.velcommuta.denul.event.ServiceReplyEvent;
import de.velcommuta.denul.event.ServiceRequestEvent;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;


/**
 * Fragment to display the step count
 */
public class StepCountFragment extends Fragment implements ServiceConnection {
    private static final String TAG = "StepCountFragment";

    // Instance variables
    private TextView mStepCountDisplay;
    private ProgressBar mProgressBar;

    // Database binder
    private DatabaseServiceBinder mBinder;

    // EventBus
    private EventBus mEventBus;

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
        mStepCountDisplay = (TextView)    v.findViewById(R.id.stepcount);
        mProgressBar      = (ProgressBar) v.findViewById(R.id.progressBar);

        mProgressBar.setMax(10000); // Max = Target number of 10k steps per day

        mEventBus = EventBus.getDefault();
        return v;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
        bindDbService();
        Log.d(TAG, "onResume: Registering with EventBus");
        mEventBus.register(this);
        mEventBus.post(new ServiceRequestEvent(ServiceRequestEvent.SERVICE_PEDOMETER, ServiceRequestEvent.REQUEST_UPDATE));
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unbinding and unregistering from eventbus");
        getActivity().unbindService(this);
        mEventBus.unregister(this);
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mBinder = (DatabaseServiceBinder) iBinder;
        Log.i(TAG, "onServiceConnected: Database binder received");
        DatabaseAvailabilityEvent ev = mEventBus.getStickyEvent(DatabaseAvailabilityEvent.class);
        if (ev.getStatus() == DatabaseAvailabilityEvent.OPENED) {
            onEvent(ev);
        }
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mBinder = null;
        Log.i(TAG, "onServiceDisconnected: Lost database binder");
    }


    /**
     * Check if the database is available
     * @return True if the database is available, false otherwise
     */
    private boolean isDbConnected() {
        return (mBinder != null && mBinder.isDatabaseOpen());
    }


    /**
     * Get todays total step count
     */
    private void getStepCountToday() {
        if (!isDbConnected()) {
            Log.e(TAG, "getStepCountToday: Database unavailable");
            return;
        }
        Cursor c = mBinder.query(StepLoggingContract.StepCountLog.TABLE_NAME,
                new String[] {StepLoggingContract.StepCountLog.COLUMN_TIME, StepLoggingContract.StepCountLog.COLUMN_VALUE},
                StepLoggingContract.StepCountLog.COLUMN_DATE + " LIKE ?",
                new String[] {formatDay(getTimestamp())},
                null,
                null,
                null);
        int total = 0;
        if (c.getCount() > 0) {

            while (c.moveToNext()) {
                total += c.getInt(c.getColumnIndexOrThrow(StepLoggingContract.StepCountLog.COLUMN_VALUE));
            }
        }
        c.close();
        mProgressBar.setProgress(total);
        mStepCountDisplay.setText("" + total);
    }


    /**
     * Get the current timestamp as a datetime object
     * @return The current timestamp, accurate to the hour
     */
    private DateTime getTimestamp() {
        return DateTime.now( DateTimeZone.getDefault() ).withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0);
    }


    /**
     * Get the date formatted for the provided DateTime object
     * @param dt DateTime object
     * @return A string representation of the date
     */
    private String formatDay(DateTime dt) {
        return dt.toString(DateTimeFormat.forPattern("dd/MM/yyyy"));
    }


    /**
     * Bind to the database service
     * @return True if binding is expected to be successful, false otherwise
     */
    private boolean bindDbService() {
        if (!DatabaseService.isRunning(getActivity())) {
            Log.e(TAG, "bindDbService: Database service not running");
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
     * Event handler for EventBus DatabaseAvailabilityEvents
     * @param ev The event
     */
    public void onEvent(DatabaseAvailabilityEvent ev) {
        if (ev.getStatus() == DatabaseAvailabilityEvent.STARTED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Service STARTED, binding");
            bindDbService();
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.OPENED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Database OPENED");
            if (mBinder != null) {
                getStepCountToday();
            }
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.CLOSED) {
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Database CLOSED");
        } else if (ev.getStatus() == DatabaseAvailabilityEvent.STOPPED) {
            getActivity().unbindService(this);
            Log.d(TAG, "onEvent(DatabaseAvailabilityEvent): Database STOPPED");
            mBinder = null;
        }
    }


    /**
     * EventHandler for EventBus ServiceReplyEvents
     * @param ev The event
     */
    @SuppressWarnings("unused")
    public void onEvent(ServiceReplyEvent ev) {
        if (ev.getService() != ServiceReplyEvent.SERVICE_PEDOMETER) {
            return;
        }
        if (ev.getReply() == ServiceReplyEvent.REPLY_UPDATE_COMPLETE) {
            if (isDbConnected()) {
                Log.d(TAG, "onEvent(ServiceReplyEvent): Grabbing the database updates");
                getStepCountToday();
            } else {
                Log.w(TAG, "onEvent(ServiceReplyEvent): Database was updated, but we have no binder :(");
            }
        }
    }
}
