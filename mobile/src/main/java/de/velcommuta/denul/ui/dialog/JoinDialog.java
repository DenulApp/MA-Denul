package de.velcommuta.denul.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import de.velcommuta.denul.service.DatabaseServiceBinder;

/**
 * Helper class to display a dialog for joining studies
 */
public class JoinDialog {
    /**
     * Show a dialog to ask if something should really be joined
     * @param act Calling activity
     * @param callback Callback to be notified if the join was confirmed
     */
    public static void showJoinDialog(final Activity act, final OnJoinCallback callback) {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        // Set the values, build and show the dialog
        builder.setTitle("Join Study?")
                .setMessage("You will automatically upload the data requested by the study once it is collected. You can leave the study at any time.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.onJoinConfirmed();
                    }
                })
                .setNegativeButton("No", null)
                .create().show();
    }

    public interface OnJoinCallback {
        /**
         * Called to notify the callback that the requested deletion was confirmed and performed
         */
        void onJoinConfirmed();
    }
}
