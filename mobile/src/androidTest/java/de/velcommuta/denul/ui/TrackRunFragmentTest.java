package de.velcommuta.denul.ui;
// FIXME Fix these tests

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hamcrest.Matchers;

import de.velcommuta.denul.R;
import de.velcommuta.denul.service.GPSTrackingService;

/**
 * Test class for the TrackRunFragment
 */
@LargeTest
public class TrackRunFragmentTest extends ActivityInstrumentationTestCase2<MainActivity> {
    // Activity
    Activity mMainActivity;

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

    /**
     * Public constructor
     * @param activityClass Class instance
     */
    @SuppressWarnings("unused")
    public TrackRunFragmentTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    /**
     * Required no-argument constructor
     */
    @SuppressWarnings("deprecation")
    public TrackRunFragmentTest() {
        super("de.velcommuta.denul.ui.MainActivity", MainActivity.class);
    }

    ///// Test lifecycle management
    @Override
    public void setUp() throws Exception {
        super.setUp();

        setActivityInitialTouchMode(true);

        mMainActivity = getActivity();

        DrawerActions.open();
        Espresso.onView(ViewMatchers.withId(R.id.nav_tracks)).perform(ViewActions.click());

        // Grab references to UI elements
        mStatWindow      = (LinearLayout) mMainActivity.findViewById(R.id.statwindow);
        mStatButtonPanel = (LinearLayout) mMainActivity.findViewById(R.id.stat_button_panel);
        mStatSaveWindow  = (LinearLayout) mMainActivity.findViewById(R.id.stat_save_panel);
        mStartStopButton = (Button)       mMainActivity.findViewById(R.id.actionbutton);
        mSaveRunButton   = (Button)       mMainActivity.findViewById(R.id.save_run_btn);
        mRunning         = (ImageButton)  mMainActivity.findViewById(R.id.btn_running);
        mCycling         = (ImageButton)  mMainActivity.findViewById(R.id.btn_cycling);
        mChrono          = (Chronometer)  mMainActivity.findViewById(R.id.timer);
        mVelocity        = (TextView)     mMainActivity.findViewById(R.id.speedfield);
        mDistance        = (TextView)     mMainActivity.findViewById(R.id.distancefield);
        mSessionName     = (EditText)     mMainActivity.findViewById(R.id.sessionname);
    }

    @Override
    public void tearDown() {
        // Kill off the service, if it is alive
        Intent intent = new Intent(getActivity(), GPSTrackingService.class);
        getActivity().stopService(intent);
    }

    ///// Actual test methods
    /**
     * Test the preconditions - All variables should be not null after the setUp has run.
     */
    public void testPreConditions() {
        assertNotNull(mStatWindow);
        assertNotNull(mStatButtonPanel);
        assertNotNull(mStatSaveWindow);
        assertNotNull(mStartStopButton);
        assertNotNull(mSaveRunButton);
        assertNotNull(mRunning);
        assertNotNull(mCycling);
        assertNotNull(mChrono);
        assertNotNull(mVelocity);
        assertNotNull(mDistance);
        assertNotNull(mSessionName);
    }

    /**
     * Test the presence in the layout of all elements
     */
    public void testLayoutPresence() {
        View decorView = mMainActivity.getWindow().getDecorView();
        // test presence in Layout Hierarchy
        ViewAsserts.assertOnScreen(decorView, mStatWindow);
        ViewAsserts.assertOnScreen(decorView, mStatButtonPanel);
        ViewAsserts.assertOnScreen(decorView, mStatSaveWindow);
        ViewAsserts.assertOnScreen(decorView, mStartStopButton);
        ViewAsserts.assertOnScreen(decorView, mSaveRunButton);
        ViewAsserts.assertOnScreen(decorView, mRunning);
        ViewAsserts.assertOnScreen(decorView, mCycling);
        ViewAsserts.assertOnScreen(decorView, mChrono);
        ViewAsserts.assertOnScreen(decorView, mVelocity);
        ViewAsserts.assertOnScreen(decorView, mDistance);
        ViewAsserts.assertOnScreen(decorView, mSessionName);
    }

    /**
     * Test the initial visibility of all Layout elements
     */
    public void testInitialVisibility() {
        assertStateTrackingReset();
    }

    /**
     * Test the visibility of layout elements while the tracking is running
     */
    public void testRunningVisibility() {
        // Click the StartStopButton
        TouchUtils.clickView(this, mStartStopButton);
        assertStateTrackingRunning();
    }

    /**
     * Test the visibility of layout elements while the tracking is stopped, but the results are not reset
     */
    public void testStoppedVisibility() {
        // Click the StartStopButton twice
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mStartStopButton);
        assertStateSaveButtonVisible();
    }

    /**
     * Test the visibility of layout elements while the tracking is stopped and the save save button was clicked once
     */
    public void testSaveButtonClickedVisibility() {
        // Click the StartStopButton twice
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mSaveRunButton);
        assertStateSaveUIVisible();
    }

    /**
     * Test the visibility of layout elements after the tracking was started, stopped, the save UI was shown, and the reset button was clicked
     */
    public void testResetAfterSaveStageOneVisibility() {
        // Click the StartStopButton twice
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mSaveRunButton);
        TouchUtils.clickView(this, mStartStopButton);
        assertStateTrackingReset();
    }

    /**
     * Test the visibility of layout elements after the tracking was started, stopped, the save UI was NOT shown, and the reset button was clicked
     */
    public void testResetVisibility() {
        // Click the StartStopButton twice
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mStartStopButton);
        TouchUtils.clickView(this, mStartStopButton);
        assertStateTrackingReset();
    }

    /**
     * Test if visibility is still okay after an orientation change, with nothing running
     */
    public void testInitialOrientationChange() {
        changeOrientation();
        assertStateTrackingReset();
    }

    /**
     * Test if visibility is still okay after an orientation change, with tracking running
     */
    public void testRunningOrientationChange() {
        testRunningVisibility();
        changeOrientation();
        assertStateTrackingRunning();
    }

    /**
     * Test if visibility is still okay after an orientation change, with tracking stopped but not reset
     */
    public void testStoppedOrientationChange() {
        testStoppedVisibility();
        changeOrientation();
        assertStateSaveButtonVisible();
    }

    /**
     * Test if visibility is still okay after an orientation change, with tracking stopped and save button clicked once
     */
    public void testSaveButtonClickedOrientationChange() {
        testSaveButtonClickedVisibility();
        changeOrientation();
        assertStateSaveUIVisible();
    }

    /**
     * Test if the values of the save UI are preserved during orientation change
     */
    public void testSaveUiStatePreservationOrientationChange() {
        testSaveButtonClickedVisibility();
        TouchUtils.clickView(this, mRunning);
        changeOrientation();
        assertTrue(mRunning.isSelected());
        assertFalse(mCycling.isSelected());
    }

    ///// Helper functions
    /**
     * Helper function. Asserts that the UI is in the following state:
     * - Tracking is not running
     * - Only the Start/Stop button is visible
     */
    private void assertStateTrackingReset() {
        assertEquals(mStatWindow.getVisibility(), View.INVISIBLE);
        assertEquals(mStatButtonPanel.getVisibility(), View.GONE);
        assertEquals(mStatSaveWindow.getVisibility(), View.GONE);
        // The following Views are set to visible even though they are not shown
        // This is because the Layout they are contained in is set to INVISIBLE or GONE
        assertEquals(mStartStopButton.getVisibility(), View.VISIBLE);
        assertEquals(mSaveRunButton.getVisibility(), View.VISIBLE);
        assertEquals(mRunning.getVisibility(), View.VISIBLE);
        assertEquals(mCycling.getVisibility(), View.VISIBLE);
        assertEquals(mChrono.getVisibility(), View.VISIBLE);
        assertEquals(mVelocity.getVisibility(), View.VISIBLE);
        assertEquals(mDistance.getVisibility(), View.VISIBLE);
        assertEquals(mSessionName.getVisibility(), View.VISIBLE);
    }

    /**
     * Helper method. Asserts the UI is in the following state:
     * - Tracking is running
     * - Stat window is visible
     * - Save session button and -UI is not visible
     */
    private void assertStateTrackingRunning() {
        // Check Visibilities
        assertEquals(mStatWindow.getVisibility(), View.VISIBLE);
        assertEquals(mStatButtonPanel.getVisibility(), View.GONE);
        assertEquals(mStatSaveWindow.getVisibility(), View.GONE);
        // The following Views are set to visible even though they are not shown
        // This is because the Layout they are contained in is set to INVISIBLE or GONE
        assertEquals(mStartStopButton.getVisibility(), View.VISIBLE);
        assertEquals(mSaveRunButton.getVisibility(), View.VISIBLE);
        assertEquals(mRunning.getVisibility(), View.VISIBLE);
        assertEquals(mCycling.getVisibility(), View.VISIBLE);
        assertEquals(mChrono.getVisibility(), View.VISIBLE);
        assertEquals(mVelocity.getVisibility(), View.VISIBLE);
        assertEquals(mDistance.getVisibility(), View.VISIBLE);
        assertEquals(mSessionName.getVisibility(), View.VISIBLE);
    }

    /**
     * Helper method. Asserts the UI is in the following state:
     * - Tracking is stopped, but not reset
     * - Save session button is shown
     * - Save session UI is shown
     */
    private void assertStateSaveUIVisible() {
        // Check Visibilities
        assertEquals(mStatWindow.getVisibility(), View.VISIBLE);
        assertEquals(mStatButtonPanel.getVisibility(), View.VISIBLE);
        assertEquals(mStatSaveWindow.getVisibility(), View.VISIBLE);
        // The following Views are set to visible even though they are not shown
        // This is because the Layout they are contained in is set to INVISIBLE or GONE
        assertEquals(mStartStopButton.getVisibility(), View.VISIBLE);
        assertEquals(mSaveRunButton.getVisibility(), View.VISIBLE);
        assertEquals(mRunning.getVisibility(), View.VISIBLE);
        assertEquals(mCycling.getVisibility(), View.VISIBLE);
        assertEquals(mChrono.getVisibility(), View.VISIBLE);
        assertEquals(mVelocity.getVisibility(), View.VISIBLE);
        assertEquals(mDistance.getVisibility(), View.VISIBLE);
        assertEquals(mSessionName.getVisibility(), View.VISIBLE);
    }

    /**
     * Helper method. Asserts the UI is in the following state:
     * - Tracking is stopped, but not reset
     * - Save session button is shown
     * - Save session UI is NOT shown
     */
    private void assertStateSaveButtonVisible() {
        // Check Visibilities
        assertEquals(mStatWindow.getVisibility(), View.VISIBLE);
        assertEquals(mStatButtonPanel.getVisibility(), View.VISIBLE);
        assertEquals(mStatSaveWindow.getVisibility(), View.GONE);
        // The following Views are set to visible even though they are not shown
        // This is because the Layout they are contained in is set to INVISIBLE or GONE
        assertEquals(mStartStopButton.getVisibility(), View.VISIBLE);
        assertEquals(mSaveRunButton.getVisibility(), View.VISIBLE);
        assertEquals(mRunning.getVisibility(), View.VISIBLE);
        assertEquals(mCycling.getVisibility(), View.VISIBLE);
        assertEquals(mChrono.getVisibility(), View.VISIBLE);
        assertEquals(mVelocity.getVisibility(), View.VISIBLE);
        assertEquals(mDistance.getVisibility(), View.VISIBLE);
        assertEquals(mSessionName.getVisibility(), View.VISIBLE);
    }

    /**
     * Force an orientation change to test teardown and re-creation of UI
     */
    private void changeOrientation() {
        if (mMainActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mMainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mMainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
}
