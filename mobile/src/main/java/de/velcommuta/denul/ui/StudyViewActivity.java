package de.velcommuta.denul.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.StudyRequest;
import de.velcommuta.denul.service.DatabaseService;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.util.StudyManager;

/**
 * Activity to show details about a specific user
 */
public class StudyViewActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "StudyViewAct";

    private DatabaseServiceBinder mDbBinder;
    private long mStudyId;
    private StudyRequest mStudy;

    private TextView mStudyName;
    private TextView mStudyInstitution;
    private TextView mStudyDescription;
    private TextView mStudyPurpose;
    private TextView mStudyProcedures;
    private TextView mStudyBenefits;
    private TextView mStudyRisks;
    private TextView mStudyConflictsOfInterest;
    private TextView mStudyConfidentiality;
    private TextView mStudyParticipation;
    private TextView mStudyRights;
    private TextView mStudyURL;
    private TextView mStudyInvestigators;
    private TextView mStudyDataRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_show);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        requestDatabaseBinder();
        Bundle b = getIntent().getExtras();
        if (b != null) {
            mStudyId = b.getLong("study-id");
        } else {
            Log.e(TAG, "onCreate: No Bundle passed, returning");
            finish();
        }
        mStudyName = (TextView) findViewById(R.id.study_name);
        mStudyInstitution = (TextView) findViewById(R.id.study_institution);
        mStudyDescription = (TextView) findViewById(R.id.study_description);
        mStudyPurpose = (TextView) findViewById(R.id.study_purpose);
        mStudyProcedures = (TextView) findViewById(R.id.study_procedures);
        mStudyBenefits = (TextView) findViewById(R.id.study_benefits);
        mStudyRisks = (TextView) findViewById(R.id.study_risks);
        mStudyConflictsOfInterest = (TextView) findViewById(R.id.study_conflicts);
        mStudyConfidentiality = (TextView) findViewById(R.id.study_confidentiality);
        mStudyParticipation = (TextView) findViewById(R.id.study_participation);
        mStudyRights = (TextView) findViewById(R.id.study_rights);
        mStudyURL = (TextView) findViewById(R.id.study_url);
        mStudyInvestigators = (TextView) findViewById(R.id.study_investigators);
        mStudyDataRequests = (TextView) findViewById(R.id.study_datarequests);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.activity_friend_view, menu);
        // TODO
        return true;
    }

    /**
     * Load the information about the friend from the database and display it
     */
    private void loadStudyInformation() {
        mStudy = mDbBinder.getStudyRequestByID(mStudyId);
        mStudyName.setText(mStudy.name);
        mStudyInstitution.setText(format(getString(R.string.study_institution) + " ", mStudy.institution));
        mStudyDescription.setText(format(getString(R.string.study_description), mStudy.description));
        mStudyPurpose.setText(format(getString(R.string.study_purpose), mStudy.purpose));
        mStudyProcedures.setText(format(getString(R.string.study_procedures), mStudy.procedures));
        mStudyBenefits.setText(format(getString(R.string.study_benefits), mStudy.benefits));
        mStudyRisks.setText(format(getString(R.string.study_risks), mStudy.risks));
        mStudyConflictsOfInterest.setText(format(getString(R.string.study_conflicts), mStudy.conflicts));
        mStudyConfidentiality.setText(format(getString(R.string.study_confidentiality), mStudy.confidentiality));
        mStudyParticipation.setText(format(getString(R.string.study_participation), mStudy.participationAndWithdrawal));
        mStudyRights.setText(format(getString(R.string.study_rights), mStudy.rights));
        mStudyURL.setText(format(getString(R.string.study_url) + " ", mStudy.webpage));
        StringBuilder investigators = new StringBuilder();
        for (StudyRequest.Investigator inv : mStudy.investigators) {
            if (investigators.length() != 0) investigators.append("\r\n");
            investigators.append("- ");
            investigators.append(inv.name);
            investigators.append(" (");
            investigators.append(inv.group);
            investigators.append(" - ");
            investigators.append(inv.institution);
            investigators.append(") - ");
            investigators.append(inv.position);
        }
        mStudyInvestigators.setText(format(getString(R.string.study_investigators), investigators.toString()));
        StringBuilder datarequests = new StringBuilder();
        for (StudyRequest.DataRequest req : mStudy.requests) {
            if (datarequests.length() != 0) datarequests.append("\r\n");
            datarequests.append("- ");
            datarequests.append(req.toString());
        }
        mStudyDataRequests.setText(format(getString(R.string.study_datarequests), datarequests.toString()));
    }


    /**
     * Helper function to format two strings to show the first one in bold, the second one as regular text
     * @param bold The bold part
     * @param normal the normal part
     * @return The formatted SpannableStringBuilder
     */
    private SpannableStringBuilder format(String bold, String normal) {
        SpannableStringBuilder rv = new SpannableStringBuilder(bold + normal);
        rv.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return rv;
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


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: New service connection received");
        mDbBinder = (DatabaseServiceBinder) iBinder;
        // TODO Debugging code, move to passphrase activity once it is added
        if (!mDbBinder.isDatabaseOpen()) {
            mDbBinder.openDatabase("VerySecureHardcodedPasswordOlolol123");
        }
        loadStudyInformation();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: Lost DB service");
        mDbBinder = null;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO
        switch (item.getItemId()) {
            default:
                break;
        }
        return false;
    }
}
