package com.example.gitlinked.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gitlinked.models.GroupChat;
import com.example.gitlinked.models.GroupMessage;
import com.example.gitlinked.utils.EncryptionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for group chats, members, and group messages.
 */
public class GroupDao {

    private final DBHelper dbHelper;

    public GroupDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    // ==================== GROUP CHATS ====================

    /**
     * Create a new group chat.
     * @return the new group's row ID
     */
    public long createGroup(String name, String creatorId, List<String> memberIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_GROUP_NAME, name);
        values.put(DBHelper.COL_GROUP_CREATOR, creatorId);
        values.put(DBHelper.COL_GROUP_CREATED_AT, System.currentTimeMillis());

        long groupId = db.insert(DBHelper.TABLE_GROUPS, null, values);

        // Add creator as a member
        addMember(groupId, creatorId);

        // Add other members
        if (memberIds != null) {
            for (String memberId : memberIds) {
                addMember(groupId, memberId);
            }
        }

        return groupId;
    }

    /**
     * Get all groups a user belongs to.
     */
    public List<GroupChat> getGroupsForUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<GroupChat> groups = new ArrayList<>();

        // Query group IDs this user is a member of
        String query = "SELECT g.* FROM " + DBHelper.TABLE_GROUPS + " g "
                + "INNER JOIN " + DBHelper.TABLE_GROUP_MEMBERS + " gm "
                + "ON g." + DBHelper.COL_GROUP_ID + " = gm." + DBHelper.COL_GM_GROUP_ID
                + " WHERE gm." + DBHelper.COL_GM_USER_ID + " = ?"
                + " ORDER BY g." + DBHelper.COL_GROUP_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userId});

        while (cursor.moveToNext()) {
            GroupChat group = cursorToGroup(cursor);
            group.setMemberIds(getMembers(group.getId()));
            groups.add(group);
        }
        cursor.close();
        return groups;
    }

    /**
     * Get a specific group by ID.
     */
    public GroupChat getGroupById(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_GROUPS, null,
                DBHelper.COL_GROUP_ID + "=?", new String[]{String.valueOf(groupId)},
                null, null, null);

        GroupChat group = null;
        if (cursor.moveToFirst()) {
            group = cursorToGroup(cursor);
            group.setMemberIds(getMembers(groupId));
        }
        cursor.close();
        return group;
    }

    private GroupChat cursorToGroup(Cursor cursor) {
        GroupChat group = new GroupChat();
        group.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_ID)));
        group.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_NAME)));
        group.setCreatorId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_CREATOR)));
        group.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GROUP_CREATED_AT)));
        return group;
    }

    // ==================== MEMBERS ====================

    public void addMember(long groupId, String userId) {
        // Check if already a member
        if (isMember(groupId, userId)) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_GM_GROUP_ID, groupId);
        values.put(DBHelper.COL_GM_USER_ID, userId);
        db.insert(DBHelper.TABLE_GROUP_MEMBERS, null, values);
    }

    public boolean isMember(long groupId, String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_GROUP_MEMBERS, null,
                DBHelper.COL_GM_GROUP_ID + "=? AND " + DBHelper.COL_GM_USER_ID + "=?",
                new String[]{String.valueOf(groupId), userId},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public List<String> getMembers(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> members = new ArrayList<>();
        Cursor cursor = db.query(DBHelper.TABLE_GROUP_MEMBERS,
                new String[]{DBHelper.COL_GM_USER_ID},
                DBHelper.COL_GM_GROUP_ID + "=?", new String[]{String.valueOf(groupId)},
                null, null, null);
        while (cursor.moveToNext()) {
            members.add(cursor.getString(0));
        }
        cursor.close();
        return members;
    }

    // ==================== GROUP MESSAGES ====================

    /**
     * Send a message to a group. Encrypts before storage.
     */
    public long sendMessage(long groupId, String senderId, String senderName, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_GMSG_GROUP_ID, groupId);
        values.put(DBHelper.COL_GMSG_SENDER_ID, senderId);
        values.put(DBHelper.COL_GMSG_SENDER_NAME, senderName);
        values.put(DBHelper.COL_GMSG_CONTENT, content);
        values.put(DBHelper.COL_GMSG_TIMESTAMP, System.currentTimeMillis());

        // Encrypt the content
        try {
            String encrypted = EncryptionUtil.encrypt(content);
            values.put(DBHelper.COL_GMSG_ENCRYPTED, encrypted);
        } catch (Exception e) {
            values.put(DBHelper.COL_GMSG_ENCRYPTED, content);
        }

        return db.insert(DBHelper.TABLE_GROUP_MESSAGES, null, values);
    }

    /**
     * Get all messages for a group.
     */
    public List<GroupMessage> getMessages(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<GroupMessage> messages = new ArrayList<>();

        Cursor cursor = db.query(DBHelper.TABLE_GROUP_MESSAGES, null,
                DBHelper.COL_GMSG_GROUP_ID + "=?", new String[]{String.valueOf(groupId)},
                null, null, DBHelper.COL_GMSG_TIMESTAMP + " ASC");

        while (cursor.moveToNext()) {
            messages.add(cursorToMessage(cursor));
        }
        cursor.close();
        return messages;
    }

    /**
     * Get the latest message in a group.
     */
    public GroupMessage getLatestMessage(long groupId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_GROUP_MESSAGES, null,
                DBHelper.COL_GMSG_GROUP_ID + "=?", new String[]{String.valueOf(groupId)},
                null, null, DBHelper.COL_GMSG_TIMESTAMP + " DESC", "1");

        GroupMessage msg = null;
        if (cursor.moveToFirst()) {
            msg = cursorToMessage(cursor);
        }
        cursor.close();
        return msg;
    }

    private GroupMessage cursorToMessage(Cursor cursor) {
        GroupMessage msg = new GroupMessage();
        msg.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_ID)));
        msg.setGroupId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_GROUP_ID)));
        msg.setSenderId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_SENDER_ID)));
        msg.setSenderName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_SENDER_NAME)));
        msg.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_TIMESTAMP)));

        // Decrypt content
        String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_GMSG_CONTENT));
        msg.setContent(content);

        return msg;
    }
}
