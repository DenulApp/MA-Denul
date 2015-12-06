package de.velcommuta.denul.ui.adapter;

import android.app.Fragment;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.velcommuta.denul.R;

/**
 * RecyclerView adapter for the friendlist
 *
 * The OnClick and OnLongClick implementation is adapted from
 * http://stackoverflow.com/a/27945635/1232833
 * Context menu implementation adapted from
 * http://stackoverflow.com/a/27886458/1232833
 */
public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    private List<Friend> mFriends;
    protected Context mContext;
    private Fragment mFragment;
    private int mPosition;

    public interface OnItemClickListener {
        /**
         * Called when an item is clicked
         * @param position Position of the item in the list
         */
        void onItemClicked(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener{
        protected View mHeldView;
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
            mHeldView.setOnCreateContextMenuListener(this);
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


        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            Log.d("ViewHolder", "onCreateContextMenu");
            MenuInflater inflater = new MenuInflater(mContext);
            inflater.inflate(R.menu.context_friendlist, contextMenu);
            // MenuInfo is null
        }
    }


    /**
     * Constructor, being passed the dataset to be displayed
     * @param friends A List of {@link Friend} objects to display
     * @param frag The fragment embedding this List
     * @param ctx A Context
     */
    public FriendListAdapter(Context ctx, Fragment frag, List<Friend> friends) {
        mFriends = friends;
        mFragment = frag;
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.display(mFriends.get(position));
        holder.mHeldView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((OnItemClickListener) mFragment).onItemClicked(position);
            }
        });
        holder.mHeldView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(holder.getAdapterPosition());
                return false;
            }
        });
    }


    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.mHeldView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }


    @Override
    public int getItemCount() {
        return mFriends.size();
    }


    /**
     * Get the current position (helper for the Context menu implementation)
     * @return Current position
     */
    public int getPosition() {
        return mPosition;
    }


    /**
     * Set the current position (helper for the Context menu implementation)
     * @param pos Position
     */
    private void setPosition(int pos) {
        mPosition = pos;
    }
}
