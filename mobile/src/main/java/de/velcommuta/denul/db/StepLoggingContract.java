package de.velcommuta.denul.db;

import android.provider.BaseColumns;

/**
 * Contract file for the SQLite-Tables for step counter logging
 */
public class StepLoggingContract {
    /**
     * Required empty constructor
     */
    public StepLoggingContract() {}

    public static abstract class StepCountLog implements BaseColumns {
        // Table name
        public static final String TABLE_NAME = "step_count";

        // Columns
        public static final String COLUMN_DATE  = "recording_date";
        public static final String COLUMN_TIME  = "recording_time";
        public static final String COLUMN_VALUE = "recording_steps";
        // Owner - references Friend table, or set to -1 if the user is the owner
        public static final String COLUMN_OWNER = "recording_owner";
        public static final String COLUMN_SHARE_ID = "share_id";
    }
}
