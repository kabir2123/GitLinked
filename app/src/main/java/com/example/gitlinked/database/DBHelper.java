package com.example.gitlinked.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.gitlinked.utils.Constants;

public class DBHelper extends SQLiteOpenHelper {

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_AVATAR_URL = "avatar_url";
    public static final String COL_BIO = "bio";
    public static final String COL_LANGUAGES = "languages";
    public static final String COL_INTERESTS = "interests";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_BLE_ADDRESS = "ble_address";

    // Messages table
    public static final String TABLE_MESSAGES = "messages";
    public static final String COL_MSG_ID = "msg_id";
    public static final String COL_SENDER_ID = "sender_id";
    public static final String COL_RECEIVER_ID = "receiver_id";
    public static final String COL_CONTENT = "content";
    public static final String COL_ENCRYPTED_CONTENT = "encrypted_content";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_IS_READ = "is_read";

    // Jobs table
    public static final String TABLE_JOBS = "jobs";
    public static final String COL_JOB_ID = "job_id";
    public static final String COL_TITLE = "title";
    public static final String COL_COMPANY = "company";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_LOCATION = "location";
    public static final String COL_TYPE = "type";
    public static final String COL_SKILLS = "skills";
    public static final String COL_POSTED_DATE = "posted_date";
    public static final String COL_SALARY = "salary";

    // Events table
    public static final String TABLE_EVENTS = "events";
    public static final String COL_EVENT_ID = "event_id";
    public static final String COL_EVENT_TITLE = "event_title";
    public static final String COL_EVENT_DESC = "event_desc";
    public static final String COL_EVENT_LOCATION = "event_location";
    public static final String COL_EVENT_DATE = "event_date";
    public static final String COL_ATTENDEES = "attendees";
    public static final String COL_MAX_ATTENDEES = "max_attendees";
    public static final String COL_ORGANIZER = "organizer";
    public static final String COL_CHECKED_IN = "checked_in";

    // Connections (invites) table
    public static final String TABLE_CONNECTIONS = "connections";
    public static final String COL_CONN_ID = "conn_id";
    public static final String COL_CONN_FROM = "from_user_id";
    public static final String COL_CONN_TO = "to_user_id";
    public static final String COL_CONN_STATUS = "status";
    public static final String COL_CONN_TIMESTAMP = "conn_timestamp";

    // Group chats table
    public static final String TABLE_GROUPS = "group_chats";
    public static final String COL_GROUP_ID = "group_id";
    public static final String COL_GROUP_NAME = "group_name";
    public static final String COL_GROUP_CREATOR = "creator_id";
    public static final String COL_GROUP_CREATED_AT = "created_at";

    // Group members table
    public static final String TABLE_GROUP_MEMBERS = "group_members";
    public static final String COL_GM_ID = "gm_id";
    public static final String COL_GM_GROUP_ID = "group_id";
    public static final String COL_GM_USER_ID = "user_id";

    // Group messages table
    public static final String TABLE_GROUP_MESSAGES = "group_messages";
    public static final String COL_GMSG_ID = "gmsg_id";
    public static final String COL_GMSG_GROUP_ID = "group_id";
    public static final String COL_GMSG_SENDER_ID = "sender_id";
    public static final String COL_GMSG_SENDER_NAME = "sender_name";
    public static final String COL_GMSG_CONTENT = "content";
    public static final String COL_GMSG_ENCRYPTED = "encrypted_content";
    public static final String COL_GMSG_TIMESTAMP = "timestamp";

    private static final String CREATE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COL_USER_ID + " TEXT PRIMARY KEY, "
            + COL_USERNAME + " TEXT NOT NULL, "
            + COL_AVATAR_URL + " TEXT, "
            + COL_BIO + " TEXT, "
            + COL_LANGUAGES + " TEXT, "
            + COL_INTERESTS + " TEXT, "
            + COL_LATITUDE + " REAL DEFAULT 0, "
            + COL_LONGITUDE + " REAL DEFAULT 0, "
            + COL_BLE_ADDRESS + " TEXT)";

    private static final String CREATE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + " ("
            + COL_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_SENDER_ID + " TEXT NOT NULL, "
            + COL_RECEIVER_ID + " TEXT NOT NULL, "
            + COL_CONTENT + " TEXT, "
            + COL_ENCRYPTED_CONTENT + " TEXT, "
            + COL_TIMESTAMP + " INTEGER NOT NULL, "
            + COL_IS_READ + " INTEGER DEFAULT 0)";

    private static final String CREATE_JOBS = "CREATE TABLE " + TABLE_JOBS + " ("
            + COL_JOB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_TITLE + " TEXT NOT NULL, "
            + COL_COMPANY + " TEXT, "
            + COL_DESCRIPTION + " TEXT, "
            + COL_LOCATION + " TEXT, "
            + COL_TYPE + " TEXT, "
            + COL_SKILLS + " TEXT, "
            + COL_POSTED_DATE + " TEXT, "
            + COL_SALARY + " TEXT)";

    private static final String CREATE_EVENTS = "CREATE TABLE " + TABLE_EVENTS + " ("
            + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_EVENT_TITLE + " TEXT NOT NULL, "
            + COL_EVENT_DESC + " TEXT, "
            + COL_EVENT_LOCATION + " TEXT, "
            + COL_EVENT_DATE + " TEXT, "
            + COL_ATTENDEES + " INTEGER DEFAULT 0, "
            + COL_MAX_ATTENDEES + " INTEGER DEFAULT 0, "
            + COL_ORGANIZER + " TEXT, "
            + COL_CHECKED_IN + " INTEGER DEFAULT 0)";

    private static final String CREATE_CONNECTIONS = "CREATE TABLE " + TABLE_CONNECTIONS + " ("
            + COL_CONN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_CONN_FROM + " TEXT NOT NULL, "
            + COL_CONN_TO + " TEXT NOT NULL, "
            + COL_CONN_STATUS + " TEXT NOT NULL DEFAULT 'pending', "
            + COL_CONN_TIMESTAMP + " INTEGER NOT NULL)";

    private static final String CREATE_GROUPS = "CREATE TABLE " + TABLE_GROUPS + " ("
            + COL_GROUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_GROUP_NAME + " TEXT NOT NULL, "
            + COL_GROUP_CREATOR + " TEXT NOT NULL, "
            + COL_GROUP_CREATED_AT + " INTEGER NOT NULL)";

    private static final String CREATE_GROUP_MEMBERS = "CREATE TABLE " + TABLE_GROUP_MEMBERS + " ("
            + COL_GM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_GM_GROUP_ID + " INTEGER NOT NULL, "
            + COL_GM_USER_ID + " TEXT NOT NULL)";

    private static final String CREATE_GROUP_MESSAGES = "CREATE TABLE " + TABLE_GROUP_MESSAGES + " ("
            + COL_GMSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_GMSG_GROUP_ID + " INTEGER NOT NULL, "
            + COL_GMSG_SENDER_ID + " TEXT NOT NULL, "
            + COL_GMSG_SENDER_NAME + " TEXT, "
            + COL_GMSG_CONTENT + " TEXT, "
            + COL_GMSG_ENCRYPTED + " TEXT, "
            + COL_GMSG_TIMESTAMP + " INTEGER NOT NULL)";

    private static DBHelper instance;

    private DBHelper(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_MESSAGES);
        db.execSQL(CREATE_JOBS);
        db.execSQL(CREATE_EVENTS);
        db.execSQL(CREATE_CONNECTIONS);
        db.execSQL(CREATE_GROUPS);
        db.execSQL(CREATE_GROUP_MEMBERS);
        db.execSQL(CREATE_GROUP_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JOBS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONNECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_MESSAGES);
        onCreate(db);
    }
}
