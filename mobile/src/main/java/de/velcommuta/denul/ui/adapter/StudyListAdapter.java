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
import android.widget.TextView;

import java.util.List;

import de.velcommuta.denul.R;
import de.velcommuta.denul.data.StudyRequest;

/**
 * RecyclerView adapter for the study list
 *
 * The OnClick and OnLongClick implementation is adapted from
 * http://stackoverflow.com/a/27945635/1232833
 * Context menu implementation adapted from
 * http://stackoverflow.com/a/27886458/1232833
 */
public class StudyListAdapter extends RecyclerView.Adapter<StudyListAdapter.ViewHolder> {
    private List<StudyRequest> mStudies;
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        protected View mHeldView;
        private TextView mEntryTitle;
        private TextView mEntrySubtitle;


        /**
         * ViewHoldere constructor holding the Reference to a view
         * @param itemView The View to hold
         */
        public ViewHolder(View itemView) {
            super(itemView);
            mHeldView = itemView;
            mEntryTitle = (TextView) itemView.findViewById(R.id.study_list_item_title);
            mEntrySubtitle = (TextView) itemView.findViewById(R.id.study_list_item_subtitle);
        }


        /**
         * Display a Friend in the held view
         * @param req The {@link StudyRequest} to display
         */
        public void display(StudyRequest req) {
            mEntryTitle.setText(req.name);
            // Display date of entry
            mEntrySubtitle.setText(req.institution);
        }
    }


    /**
     * Constructor, being passed the dataset to be displayed
     * @param studies A List of {@link StudyRequest} objects to display
     * @param frag The fragment embedding this List
     * @param ctx A Context
     */
    public StudyListAdapter(Context ctx, Fragment frag, List<StudyRequest> studies) {
        mStudies = studies;
        mFragment = frag;
        mContext = ctx;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.study_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.display(mStudies.get(position));
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
        return mStudies.size();
    }


    /**
     * Get the StudyRequest at the specified position in the List
     * @param position The position
     * @return The StudyRequest at that position
     */
    public StudyRequest getStudyAt(int position) {
        return mStudies.get(position);
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
