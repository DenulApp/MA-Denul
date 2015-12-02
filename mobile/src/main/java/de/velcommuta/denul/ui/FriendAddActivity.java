package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import de.velcommuta.denul.R;

/**
 * Activity containing the flow for adding a new friend
 */
public class FriendAddActivity extends AppCompatActivity implements FriendAddTechSelectionFragment.TechSelectionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);
        loadTechSelectFragment();
    }


    /**
     * Load the tech selection fragment
     */
    private void loadTechSelectFragment() {
        Fragment fragment = FriendAddTechSelectionFragment.newInstance();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.friend_add_container, fragment)
                .commit();
    }


    @Override
    public void techSelection(int tech) {
        if (tech == FriendAddTechSelectionFragment.TECH_NEARBY) {
            Toast.makeText(FriendAddActivity.this, "Nearby selected", Toast.LENGTH_SHORT).show();
        }
    }
}
