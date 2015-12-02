package de.velcommuta.denul.ui;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

import de.velcommuta.denul.R;


/**
 * Fragment to display the step count
 */
public class FriendAddTechSelectionFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "FriendAddTech";

    private TechSelectionListener mListener;
    private LinearLayout mNearbyLayout;

    public static final int TECH_NEARBY = 1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static FriendAddTechSelectionFragment newInstance() {
        return new FriendAddTechSelectionFragment();
    }


    /**
     * Required empty constructor
     */
    public FriendAddTechSelectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_friend_tech_select, container, false);
        mNearbyLayout = (LinearLayout) v.findViewById(R.id.addfriend_step1_nearby_layout);
        mNearbyLayout.setOnClickListener(this);

        return v;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onClick(View view) {
        if (view == mNearbyLayout) {
            mListener.techSelection(TECH_NEARBY);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (TechSelectionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }



    /**
     * Interface for communication with the hosting activity
     */
    public interface TechSelectionListener {
        /**
         * Communicate a message to the hosting activity
         * @param tech Selected technology, as one of the TECH_* constants
         */
        void techSelection(int tech);
    }
}
