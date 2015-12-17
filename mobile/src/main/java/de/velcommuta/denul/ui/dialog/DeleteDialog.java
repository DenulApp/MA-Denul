package de.velcommuta.denul.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.service.DatabaseServiceBinder;
import de.velcommuta.denul.util.FriendManager;
import de.velcommuta.denul.util.ShareManager;

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
                        // Revoke shareable on server
                        // TODO should this be optional? Should the user be warned about this behaviour?
                        ShareManager.revokeShareable(shareable, binder);
                        // Locally delete shareable
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


    /**
     * Show a deletion dialog for a {@link Friend}
     * @param act The calling activity
     * @param binder An open {@link DatabaseServiceBinder}
     * @param friend The {@link Friend} to delete
     * @param callback The Callback to notify on successful deletion
     */
    public static void showDeleteDialog(final Activity act, final DatabaseServiceBinder binder, final Friend friend, final OnDeleteCallback callback) {
        // Prepare a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        // Set the values, build and show the dialog
        builder.setMessage("Delete this contact?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FriendManager.deleteFriend(friend, binder);
                        Toast.makeText(act, "Contact deleted", Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onDeleted();
                        }
                    }
                })
                .setNegativeButton("No", null)
                .create().show();
    }


    /**
     * Show a deletion dialog for a {@link Friend}
     * @param act The calling activity
     * @param binder An open {@link DatabaseServiceBinder}
     * @param friend The {@link Friend} to delete
     */
    public static void showDeleteDialog(final Activity act, final DatabaseServiceBinder binder, final Friend friend) {
        showDeleteDialog(act, binder, friend, null);
    }

    public interface OnDeleteCallback {
        /**
         * Called to notify the callback that the requested deletion was confirmed and performed
         */
        void onDeleted();
    }
}
