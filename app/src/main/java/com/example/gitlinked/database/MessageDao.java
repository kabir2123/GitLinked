package com.example.gitlinked.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gitlinked.models.Message;
import com.example.gitlinked.utils.EncryptionUtil;

import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    private final DBHelper dbHelper;

    public MessageDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * Insert a message. Content is automatically encrypted before storage.
     */
    public long insertMessage(Message message) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_SENDER_ID, message.getSenderId());
        values.put(DBHelper.COL_RECEIVER_ID, message.getReceiverId());
        values.put(DBHelper.COL_CONTENT, message.getContent());
        // Encrypt the content before storing
        values.put(DBHelper.COL_ENCRYPTED_CONTENT,
                EncryptionUtil.encrypt(message.getContent()));
        values.put(DBHelper.COL_TIMESTAMP, message.getTimestamp());
        values.put(DBHelper.COL_IS_READ, message.isRead() ? 1 : 0);

        return db.insert(DBHelper.TABLE_MESSAGES, null, values);
    }

    /**
     * Insert only when not already present (used for Firebase sync de-dup).
     */
    public boolean insertMessageIfNotExists(Message message) {
        if (messageExists(message)) return false;
        insertMessage(message);
        return true;
    }

    /**
     * Get all messages between two users, sorted by time.
     */
    public List<Message> getMessagesBetween(String userId1, String userId2) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Message> messages = new ArrayList<>();

        String selection = "(" + DBHelper.COL_SENDER_ID + "=? AND " + DBHelper.COL_RECEIVER_ID + "=?) OR ("
                + DBHelper.COL_SENDER_ID + "=? AND " + DBHelper.COL_RECEIVER_ID + "=?)";
        String[] args = {userId1, userId2, userId2, userId1};

        Cursor cursor = db.query(DBHelper.TABLE_MESSAGES, null,
                selection, args, null, null,
                DBHelper.COL_TIMESTAMP + " ASC");

        while (cursor.moveToNext()) {
            messages.add(cursorToMessage(cursor));
        }
        cursor.close();
        return messages;
    }

    /**
     * Get the last message for each conversation (for chat list).
     */
    public List<Message> getLatestMessages(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Message> messages = new ArrayList<>();

        String query = "SELECT * FROM " + DBHelper.TABLE_MESSAGES
                + " WHERE " + DBHelper.COL_SENDER_ID + "=? OR " + DBHelper.COL_RECEIVER_ID + "=?"
                + " ORDER BY " + DBHelper.COL_TIMESTAMP + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{userId, userId});
        List<String> seenConversations = new ArrayList<>();

        while (cursor.moveToNext()) {
            Message msg = cursorToMessage(cursor);
            String otherUser = msg.getSenderId().equals(userId)
                    ? msg.getReceiverId() : msg.getSenderId();
            if (!seenConversations.contains(otherUser)) {
                seenConversations.add(otherUser);
                messages.add(msg);
            }
        }
        cursor.close();
        return messages;
    }

    /**
     * Mark messages as read.
     */
    public void markAsRead(String senderId, String receiverId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_IS_READ, 1);

        db.update(DBHelper.TABLE_MESSAGES, values,
                DBHelper.COL_SENDER_ID + "=? AND " + DBHelper.COL_RECEIVER_ID + "=?",
                new String[]{senderId, receiverId});
    }

    private boolean messageExists(Message message) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_MESSAGES,
                new String[]{DBHelper.COL_MSG_ID},
                DBHelper.COL_SENDER_ID + "=? AND "
                        + DBHelper.COL_RECEIVER_ID + "=? AND "
                        + DBHelper.COL_TIMESTAMP + "=? AND "
                        + DBHelper.COL_CONTENT + "=?",
                new String[]{
                        message.getSenderId(),
                        message.getReceiverId(),
                        String.valueOf(message.getTimestamp()),
                        message.getContent()
                },
                null, null, null, "1");
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private Message cursorToMessage(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_MSG_ID)));
        message.setSenderId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_SENDER_ID)));
        message.setReceiverId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_RECEIVER_ID)));

        // Decrypt the content when reading
        String encrypted = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ENCRYPTED_CONTENT));
        if (encrypted != null && !encrypted.isEmpty()) {
            message.setContent(EncryptionUtil.decrypt(encrypted));
        } else {
            message.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CONTENT)));
        }

        message.setEncryptedContent(encrypted);
        message.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_TIMESTAMP)));
        message.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_IS_READ)) == 1);
        return message;
    }
}
