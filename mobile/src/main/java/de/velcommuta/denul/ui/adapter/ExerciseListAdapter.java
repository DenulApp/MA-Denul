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
import de.velcommuta.denul.data.Friend;
import de.velcommuta.denul.data.GPSTrack;

/**
 * RecyclerView adapter for the exercise list
 *
 * The OnClick and OnLongClick implementation is adapted from
 * http://stackoverflow.com/a/27945635/1232833
 * Context menu implementation adapted from
 * http://stackoverflow.com/a/27886458/1232833
 */
public class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListAdapter.ViewHolder> {
    private List<GPSTrack> mTracks;
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
        private TextView mEntryTitle;
        private TextView mEntrySubtitle;
        private ImageView mEntryIcon;

        /**
         * ViewHoldere constructor holding the Reference to a view
         * @param itemView The View to hold
         */
        public ViewHolder(View itemView) {
            super(itemView);
            mHeldView = itemView;
            mEntryTitle = (TextView) itemView.findViewById(R.id.exc_list_item_title);
            mEntrySubtitle = (TextView) itemView.findViewById(R.id.exc_list_item_subtitle);
            mEntryIcon = (ImageView) itemView.findViewById(R.id.exc_list_item_icon);
        }


        /**
         * Display a Friend in the held view
         * @param track The {@link GPSTrack} to display
         */
        public void display(GPSTrack track) {
            mEntryTitle.setText(track.getSessionName());
            mEntrySubtitle.setText("Here be subtitles");
            switch (track.getModeOfTransportation()) {
                case GPSTrack.VALUE_RUNNING:
                    mEntryIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_running));
                    break;
                case GPSTrack.VALUE_CYCLING:
                    mEntryIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_cycling));
                    break;
                default:
                    Log.w("ViewHolder", "display: Unknown Mode of transportation");
                    mEntryIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_running));
            }
        }


        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            Log.d("ViewHolder", "onCreateContextMenu");
            MenuInflater inflater = new MenuInflater(mContext);
            // TODO
            inflater.inflate(R.menu.context_friendlist, contextMenu);
            // MenuInfo is null
        }
    }


    /**
     * Constructor, being passed the dataset to be displayed
     * @param tracks A List of {@link GPSTrack} objects to display
     * @param frag The fragment embedding this List
     * @param ctx A Context
     */
    public ExerciseListAdapter(Context ctx, Fragment frag, List<GPSTrack> tracks) {
        mTracks = tracks;
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
        holder.display(mTracks.get(position));
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
        return mTracks.size();
    }


    /**
     * Get the Friend at the specified position in the List
     * @param position The position
     * @return The friend at that position
     */
    public GPSTrack getTrackAt(int position) {
        return mTracks.get(position);
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
