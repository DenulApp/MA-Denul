package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.velcommuta.denul.R;


/**
 * Fragment to display the step count
 */
public class FriendAddFragment extends Fragment {
    private static final String TAG = "FriendAddFragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static FriendAddFragment newInstance() {
        return new FriendAddFragment();
    }


    /**
     * Required empty constructor
     */
    public FriendAddFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_friend_1, container, false);

        return v;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

}
