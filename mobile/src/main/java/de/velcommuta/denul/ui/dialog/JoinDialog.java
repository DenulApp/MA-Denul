package de.velcommuta.denul.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

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


    /**
     * Show a dialog to confirm joining a study that could not be verified
     * @param act The calling activity
     * @param callback The callback to notify if the join was confirmed
     */
    public static void showConfirmUnverifiedDialog(final Activity act, final OnJoinCallback callback) {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        // Set the values, build and show the dialog
        builder.setTitle("Verification failed")
                // TODO Improve wording of the message
                .setMessage("The study could not be verified - this means that it may not have been created by the claimed authors. Do you want to join it anyway?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callback.onJoinConfirmed();
                    }
                })
                .setNegativeButton("No", null)
                // TODO addNeutralButton with more information
                .create().show();
    }

    public interface OnJoinCallback {
        /**
         * Called to notify the callback that the join was confirmed
         */
        void onJoinConfirmed();
    }
}
