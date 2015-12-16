package de.velcommuta.denul.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.service.DatabaseServiceBinder;

/**
 * Helper class to display a dialog for deleting items from the database
 */
public class DeleteDialog {
    /**
     * Show a shareable deletion dialog, potentially closing the calling activity after deletion
     * @param act Calling activity
     * @param binder Open DatabaseServiceBinder
     * @param shareable The shareable to delete
     * @param callback Callback to be notified if the deletion was confirmed
     */
    public static void showDeleteDialog(final Activity act, final DatabaseServiceBinder binder, final Shareable shareable, final OnDeleteCallback callback) {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        // Set the values, build and show the dialog
        builder.setMessage("Delete this exercise?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        binder.deleteShareable(shareable);
                        Toast.makeText(act, "Exercise deleted", Toast.LENGTH_SHORT).show();
                        if (callback != null)
                            callback.onDeleted();
                    }
                })
                .setNegativeButton("No", null)
                .create().show();
    }

    /**
     * Show a shareable deletion dialog, leaving the calling activity open afterwards
     * @param act Calling activity
     * @param binder Open DatabaseServiceBinder
     * @param shareable The shareable to delete
     */
    public static void showDeleteDialog(final Activity act, final DatabaseServiceBinder binder, final Shareable shareable) {
        showDeleteDialog(act, binder, shareable, null);
    }

    public interface OnDeleteCallback {
        /**
         * Called to notify the callback that the requested deletion was confirmed and performed
         */
        void onDeleted();
    }
}
