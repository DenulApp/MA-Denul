package de.velcommuta.denul.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.util.ShareManager;

/**
 * Utility class to display a sharing dialog
 */
public class ShareDialog {
    /**
     * Show a share dialog for a {@link Shareable}
     * @param act The calling {@link Activity}
     * @param binder An open {@link DatabaseServiceBinder}
     * @param shareable The {@link Shareable} that should be shared
     */
    public static void showShareDialog(final Activity act, final DatabaseServiceBinder binder, final Shareable shareable) {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        // Create a ListView to display the Friendlist
        View dialog = act.getLayoutInflater().inflate(R.layout.dialog_share, null);
        final ListView lv = (ListView) dialog.findViewById(R.id.share_menu_friendlist);
        // Set the ListView to allow multiple selections
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        // Load the friend list from the database
        final List<Friend> friendlist = binder.getFriends();
        // Set up the adapter
        lv.setAdapter(new ArrayAdapter<>(act, android.R.layout.simple_list_item_multiple_choice, friendlist));
        // Pre-check those friends that have already received the share
        List<Friend> sharedFriends = binder.getShareRecipientsForShareable(shareable);
        for (Friend f : sharedFriends) {
            lv.setItemChecked(sharedFriends.indexOf(f), true);
        }
        // Populate the list of share granularity options
        final Spinner granularitySpinner = (Spinner) dialog.findViewById(R.id.share_menu_granularity);
        final ArrayAdapter<CharSequence> granularityAdapter = ArrayAdapter.createFromResource(act, shareable.getGranularityDescriptor(), android.R.layout.simple_list_item_1);
        granularitySpinner.setAdapter(granularityAdapter);
        // Set the description
        final EditText description = (EditText) dialog.findViewById(R.id.share_menu_description);
        if (shareable.getDescription() != null) {
            description.setText(shareable.getDescription());
        }
        int granularity = binder.getShareGranularity(shareable);
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
                // Apply the description, if it changed
                if (shareable.getDescription() == null || !(shareable.getDescription().equals(description.getText().toString().trim()))) {
                    shareable.setDescription(description.getText().toString().trim());
                    binder.updateShareableDescription(shareable);
                }
                // Read out which friends have been selected
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                List<Friend> rcpt = new LinkedList<>();
                for (int i = 0; i < checked.size(); i++) {
                    if (checked.valueAt(i))
                        rcpt.add(friendlist.get(checked.keyAt(i)));
                }
                // Check if any have been selected
                if (rcpt.size() > 0) {
                    int granularity = granularitySpinner.getSelectedItemPosition();
                    // Prepare a ShareWithProgress AsyncTask with nested Callback
                    ShareManager.ShareWithProgress m = new ShareManager().new ShareWithProgress(binder, new ShareManager.ShareManagerCallback() {
                        @Override
                        public void onShareStatusUpdate(int status) {
                            // Toast.makeText(ExerciseViewActivity.this, ""+status, Toast.LENGTH_SHORT).show();
                        }


                        @Override
                        public void onShareFinished(boolean success) {
                            if (success)
                                Toast.makeText(act, "Done", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(act, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, granularity, shareable);
                    m.execute(rcpt);
                } else {
                    Toast.makeText(act, "No one selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set cancel buttel
        builder.setNegativeButton("Cancel", null);
        // Create and show the dialog
        builder.create().show();
    }
}
