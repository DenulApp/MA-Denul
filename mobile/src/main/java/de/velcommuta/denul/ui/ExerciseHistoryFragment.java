package de.velcommuta.denul.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.velcommuta.denul.R;
import de.velcommuta.denul.ui.view.EmptyRecyclerView;


/**
 * Fragment to display the technology chooser for the Add Friend activity
 */
public class ExerciseHistoryFragment extends Fragment {
    private static final String TAG = "ExercHist";

    private EmptyRecyclerView mRecycler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StepCountFragment.
     */
    public static ExerciseHistoryFragment newInstance() {
        return new ExerciseHistoryFragment();
    }


    /**
     * Required empty constructor
     */
    public ExerciseHistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_exercise_list, container, false);
        mRecycler = (EmptyRecyclerView) v.findViewById(R.id.exclist_recycler);
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
