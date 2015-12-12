package de.velcommuta.denul.ui.adapter;

import android.app.Fragment;
import android.content.Context;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.GPSTrack;
import de.velcommuta.denul.data.Shareable;
import de.velcommuta.denul.service.DatabaseServiceBinder;

/**
 * RecyclerView adapter for the friendlist
 *
 * The OnClick and OnLongClick implementation is adapted from
 * http://stackoverflow.com/a/27945635/1232833
 * Context menu implementation adapted from
 * http://stackoverflow.com/a/27886458/1232833
 */
public class SocialStreamAdapter extends RecyclerView.Adapter<SocialStreamAdapter.ViewHolder> {
    private List<Shareable> mShares;
    protected DatabaseServiceBinder mBinder;
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
        private TextView mNameViewTrailer;
        private ImageView mIconView;
        private FrameLayout mIllustration;
        private TextView mDateView;
        private TextView mFurtherInfoView;

        /**
         * ViewHoldere constructor holding the Reference to a view
         * @param itemView The View to hold
         */
        public ViewHolder(View itemView) {
            super(itemView);
            mHeldView = itemView;
            mNameView = (TextView) itemView.findViewById(R.id.stream_name);
            mNameViewTrailer = (TextView) itemView.findViewById(R.id.stream_name_trailer);
            mIconView = (ImageView) itemView.findViewById(R.id.stream_icon);
            mIllustration = (FrameLayout) itemView.findViewById(R.id.stream_image_layout);
            mDateView = (TextView) itemView.findViewById(R.id.stream_date);
            mFurtherInfoView = (TextView) itemView.findViewById(R.id.stream_moreinfo);
            mHeldView.setOnCreateContextMenuListener(this);
        }


        /**
         * Display a shareable in the held view
         * @param share The shareable to display
         */
        public void display(Shareable share) {
            switch (share.getType()) {
                case Shareable.SHAREABLE_TRACK:
                    displayGPSTrack((GPSTrack) share);
                    break;
                // TODO Add further stuff here
            }
        }


        /**
         * Display the data from a GPSTrack object
         * @param track The track to display
         */
        private void displayGPSTrack(GPSTrack track) {
            // TODO Display path on Lite googlemap (non-interactive)
            if (track.getOwner() != -1) {
                mNameView.setText(mBinder.getFriendById(track.getOwner()).getName());
                mNameViewTrailer.setText("shared '" + track.getSessionName() + "'");
            } else {
                mNameView.setText("You");
                mNameViewTrailer.setText("recorded '" + track.getSessionName() + "'");
            }
            switch (track.getModeOfTransportation()) {
                case GPSTrack.VALUE_RUNNING:
                    mIconView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_running));
                    break;
                case GPSTrack.VALUE_CYCLING:
                    mIconView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_cycling));
                    break;
                default:
                    Log.w("ViewHolder", "display: Unknown Mode of transportation");
                     mIconView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_running));
            }
            mDateView.setText(DateTimeFormat.shortDateTime().print(new LocalDateTime(track.getTimestamp(), DateTimeZone.forID(track.getTimezone()))));
            float distance = track.getDistance();
            if (distance < 1000.0f) {
                mFurtherInfoView.setText(String.format(mContext.getString(R.string.distance_m), (int) distance));
            } else {
                mFurtherInfoView.setText(String.format(mContext.getString(R.string.distance_km), (int) distance / 1000.0f));
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            Log.d("ViewHolder", "onCreateContextMenu");
            MenuInflater inflater = new MenuInflater(mContext);
            inflater.inflate(R.menu.context_friendlist, contextMenu);
            // TODO Switch out context menu - do I even need one?
            // MenuInfo is null
        }
    }


    /**
     * Constructor, being passed the dataset to be displayed
     * @param shares A List of {@link Shareable} objects to display
     * @param frag The fragment embedding this List
     * @param ctx A Context
     * @param binder A {@link DatabaseServiceBinder}
     */
    public SocialStreamAdapter(Context ctx, Fragment frag, List<Shareable> shares, DatabaseServiceBinder binder) {
        mShares = shares;
        mFragment = frag;
        mContext = ctx;
        mBinder = binder;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.social_stream_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.display(mShares.get(position));
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
        return mShares.size();
    }


    /**
     * Get the Friend at the specified position in the List
     * @param position The position
     * @return The friend at that position
     */
    public Shareable getShareableAt(int position) {
        return mShares.get(position);
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
