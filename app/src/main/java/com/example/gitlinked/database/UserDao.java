package com.example.gitlinked.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gitlinked.models.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDao {

    private final DBHelper dbHelper;

    public UserDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    public void insertOrUpdate(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_USER_ID, user.getId());
        values.put(DBHelper.COL_USERNAME, user.getUsername());
        values.put(DBHelper.COL_AVATAR_URL, user.getAvatarUrl());
        values.put(DBHelper.COL_BIO, user.getBio());
        values.put(DBHelper.COL_LANGUAGES, listToString(user.getLanguages()));
        values.put(DBHelper.COL_INTERESTS, listToString(user.getInterests()));
        values.put(DBHelper.COL_LATITUDE, user.getLatitude());
        values.put(DBHelper.COL_LONGITUDE, user.getLongitude());
        values.put(DBHelper.COL_BLE_ADDRESS, user.getBleDeviceAddress());

        db.insertWithOnConflict(DBHelper.TABLE_USERS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User getUserById(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_USERS, null,
                DBHelper.COL_USER_ID + "=?", new String[]{userId},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    public List<User> getAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<User> users = new ArrayList<>();
        Cursor cursor = db.query(DBHelper.TABLE_USERS, null,
                null, null, null, null, DBHelper.COL_USERNAME + " ASC");

        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        return users;
    }

    public void deleteUser(String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_USERS, DBHelper.COL_USER_ID + "=?",
                new String[]{userId});
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USER_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_USERNAME)));
        user.setAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_AVATAR_URL)));
        user.setBio(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_BIO)));
        user.setLanguages(stringToList(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LANGUAGES))));
        user.setInterests(stringToList(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_INTERESTS))));
        user.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LATITUDE)));
        user.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COL_LONGITUDE)));
        user.setBleDeviceAddress(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_BLE_ADDRESS)));
        return user;
    }

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }

    private List<String> stringToList(String str) {
        if (str == null || str.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }
}
