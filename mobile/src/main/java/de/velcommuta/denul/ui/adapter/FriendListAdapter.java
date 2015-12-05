package de.velcommuta.denul.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.velcommuta.denul.R;

/**
 * RecyclerView adapter for the friendlist
 */
public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    private List<Friend> mFriends;
    protected Context mContext;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mHeldView;
        private TextView mNameView;
        private ImageView mVerificationView;

        /**
         * ViewHoldere constructor holding the Reference to a view
         * @param itemView The View to hold
         */
        public ViewHolder(View itemView) {
            super(itemView);
            mHeldView = itemView;
            mNameView = (TextView) itemView.findViewById(R.id.friend_list_item_text);
            mVerificationView = (ImageView) itemView.findViewById(R.id.friend_list_item_verification);
        }


        /**
         * Display a Friend in the held view
         * @param friend The friend to display
         */
        public void display(Friend friend) {
            mNameView.setText(friend.getName());
            if (friend.getVerified() == Friend.UNVERIFIED) {
                mVerificationView.getDrawable().setTint(mContext.getResources().getColor(android.R.color.holo_orange_light));
            } else if (friend.getVerified() == Friend.VERIFIED_OK) {
                mVerificationView.getDrawable().setTint(mContext.getResources().getColor(android.R.color.holo_green_light));
            } else {
                mVerificationView.getDrawable().setTint(mContext.getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }


    /**
     * Constructor, being passed the dataset to be displayed
     * @param friends A List of {@link Friend} objects to display
     * @param ctx A Context
     */
    public FriendListAdapter(Context ctx, List<Friend> friends) {
        mFriends = friends;
        mContext = ctx;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.friend_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.display(mFriends.get(position));
    }


    @Override
    public int getItemCount() {
        return mFriends.size();
    }
}
