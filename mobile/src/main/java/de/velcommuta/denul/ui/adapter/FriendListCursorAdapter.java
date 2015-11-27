package de.velcommuta.denul.ui.adapter;

import android.content.Context;
import net.sqlcipher.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.velcommuta.denul.R;

/**
 * Cursor adapter for a RecyclerView containing the friend list
 */
public class FriendListCursorAdapter extends CursorRecyclerViewAdapter<FriendListCursorAdapter.ViewHolder> {

    /**
     * Constructor
     * @param ctx A context object
     * @param cursor The cursor that contains the data we are interested in
     */
    public FriendListCursorAdapter(Context ctx, Cursor cursor) {
        super(ctx, cursor);
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, Cursor c) {
        Friend f = Friend.fromCursor(c);
        vh.mName.setText(f.getName());
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_list_item, parent, false);
        return new ViewHolder(item);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        /**
         * Constructor for the ViewHolder
         * @param v The view to hold
         */
        public ViewHolder(View v) {
            super(v);
            mName = (TextView) v.findViewById(R.id.friend_name);
        }
    }
}
