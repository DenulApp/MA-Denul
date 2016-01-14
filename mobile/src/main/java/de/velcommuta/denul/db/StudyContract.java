package de.velcommuta.denul.db;

import android.provider.BaseColumns;

/**
 * Database contract for Study information
 */
public class StudyContract {
    public static abstract class Studies implements BaseColumns {
        public static final String TABLE_NAME = "studies";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_INSTITUTION = "institution";
        public static final String COLUMN_WEB = "webpage";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_PURPOSE = "purpose";
        public static final String COLUMN_PROCEDURES = "procedures";
        public static final String COLUMN_RISKS = "risks";
        public static final String COLUMN_BENEFITS = "benefits";
        public static final String COLUMN_PAYMENT = "payment";
        public static final String COLUMN_CONFLICTS = "conflicts";
        public static final String COLUMN_CONFIDENTIALITY = "confidentiality";
        public static final String COLUMN_PARTICIPATION = "participationAndWithdrawal";
        public static final String COLUMN_RIGHTS = "rights";
        public static final String COLUMN_VERIFICATION = "verification";
        public static final String COLUMN_PUBKEY = "pubkey";
        public static final String COLUMN_KEYALGO = "keyalgo";
        public static final String COLUMN_KEX = "kex";
        public static final String COLUMN_KEXALGO = "kexalgo";
        public static final String COLUMN_QUEUE = "queue";
        public static final String COLUMN_PARTICIPATING = "participating";
        public static final String COLUMN_KEY_IN = "key_in";
        public static final String COLUMN_CTR_IN = "ctr_in";
        public static final String COLUMN_KEY_OUT = "key_out";
        public static final String COLUMN_CTR_OUT = "ctr_out";
    }

    public static abstract class Investigators implements BaseColumns{
        public static final String TABLE_NAME = "Investigators";

        public static final String COLUMN_STUDY = "study";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_INSTITUTION = "institution";
        public static final String COLUMN_GROUP = "wg"; // Working group, as GROUP is a keyword in SQL
        public static final String COLUMN_POSITION = "pos";
    }

    public static abstract class DataRequests implements BaseColumns {
        public static final String TABLE_NAME = "DataRequests";

        public static final String COLUMN_STUDY = "study";
        public static final String COLUMN_DATATYPE = "datatype";
        public static final String COLUMN_GRANULARITY = "granularity";
        public static final String COLUMN_FREQUENCY = "frequency";
    }

    public static abstract class DataShare implements BaseColumns {
        public static final String TABLE_NAME = "StudyDataShare";

        public static final String COLUMN_DATATYPE = "datatype";
        public static final String COLUMN_GRANULARITY = "granularity";
        public static final String COLUMN_SHAREABLE_ID = "shr_id";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_IDENT = "identifier";
        public static final String COLUMN_REVOKE = "revocation";
    }

    public static abstract class StudyShare implements BaseColumns {
        public static final String TABLE_NAME = "StudyKeyShare";

        public static final String COLUMN_DATASHARE = "datashare_id";
        public static final String COLUMN_STUDYID = "study_id";
        public static final String COLUMN_IDENTIFIER = "ident";
        public static final String COLUMN_REVOCATION = "revoke";
    }
}
