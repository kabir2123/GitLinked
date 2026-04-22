package com.example.gitlinked.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gitlinked.models.ConnectionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for connection/invite requests between users.
 */
public class ConnectionDao {

    private final DBHelper dbHelper;

    public ConnectionDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * Send a new connection request (invite).
     */
    public long sendRequest(String fromUserId, String toUserId) {
        // Check if request already exists in either direction
        ConnectionRequest existing = getRequest(fromUserId, toUserId);
        if (existing != null) return existing.getId();

        existing = getRequest(toUserId, fromUserId);
        if (existing != null) return existing.getId();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_CONN_FROM, fromUserId);
        values.put(DBHelper.COL_CONN_TO, toUserId);
        values.put(DBHelper.COL_CONN_STATUS, ConnectionRequest.STATUS_PENDING);
        values.put(DBHelper.COL_CONN_TIMESTAMP, System.currentTimeMillis());

        return db.insert(DBHelper.TABLE_CONNECTIONS, null, values);
    }

    /**
     * Accept a connection request.
     */
    public void acceptRequest(long requestId) {
        updateStatus(requestId, ConnectionRequest.STATUS_ACCEPTED);
    }

    /**
     * Reject a connection request.
     */
    public void rejectRequest(long requestId) {
        updateStatus(requestId, ConnectionRequest.STATUS_REJECTED);
    }

    /**
     * Upsert remote connection state into local DB.
     */
    public void upsertFromRemote(String fromUserId, String toUserId, String status, long timestamp) {
        ConnectionRequest existing = getConnectionBetween(fromUserId, toUserId);
        if (existing == null) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DBHelper.COL_CONN_FROM, fromUserId);
            values.put(DBHelper.COL_CONN_TO, toUserId);
            values.put(DBHelper.COL_CONN_STATUS, status);
            values.put(DBHelper.COL_CONN_TIMESTAMP, timestamp);
            db.insert(DBHelper.TABLE_CONNECTIONS, null, values);
            return;
        }

        // Ignore stale updates so realtime listener ordering can't regress state.
        if (existing.getTimestamp() > timestamp) {
            return;
        }
        if (existing.getTimestamp() == timestamp) {
            if (status.equals(existing.getStatus())) return;

            // Allow only forward transitions at equal timestamp:
            // pending -> accepted/rejected. Prevent accepted/rejected -> pending regressions.
            if (ConnectionRequest.STATUS_ACCEPTED.equals(existing.getStatus())
                    && !ConnectionRequest.STATUS_ACCEPTED.equals(status)) {
                return;
            }
            if (ConnectionRequest.STATUS_REJECTED.equals(existing.getStatus())
                    && ConnectionRequest.STATUS_PENDING.equals(status)) {
                return;
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_CONN_STATUS, status);
        values.put(DBHelper.COL_CONN_TIMESTAMP, timestamp);
        db.update(DBHelper.TABLE_CONNECTIONS, values,
                DBHelper.COL_CONN_ID + "=?", new String[]{String.valueOf(existing.getId())});
    }

    private void updateStatus(long requestId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_CONN_STATUS, status);
        db.update(DBHelper.TABLE_CONNECTIONS, values,
                DBHelper.COL_CONN_ID + "=?", new String[]{String.valueOf(requestId)});
    }

    /**
     * Get the connection request between two users (in either direction).
     */
    public ConnectionRequest getRequest(String userId1, String userId2) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DBHelper.COL_CONN_FROM + "=? AND " + DBHelper.COL_CONN_TO + "=?";

        Cursor cursor = db.query(DBHelper.TABLE_CONNECTIONS, null,
                selection, new String[]{userId1, userId2},
                null, null, DBHelper.COL_CONN_TIMESTAMP + " DESC", "1");

        ConnectionRequest request = null;
        if (cursor.moveToFirst()) {
            request = cursorToRequest(cursor);
        }
        cursor.close();
        return request;
    }

    /**
     * Get connection between two users in either direction.
     */
    public ConnectionRequest getConnectionBetween(String userId1, String userId2) {
        ConnectionRequest req = getRequest(userId1, userId2);
        if (req != null) return req;
        return getRequest(userId2, userId1);
    }

    /**
     * Check if two users are connected (request accepted).
     */
    public boolean areConnected(String userId1, String userId2) {
        ConnectionRequest req = getConnectionBetween(userId1, userId2);
        return req != null && req.isAccepted();
    }

    /**
     * Get all pending invites received by a user.
     */
    public List<ConnectionRequest> getPendingInvitesFor(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ConnectionRequest> requests = new ArrayList<>();

        Cursor cursor = db.query(DBHelper.TABLE_CONNECTIONS, null,
                DBHelper.COL_CONN_TO + "=? AND " + DBHelper.COL_CONN_STATUS + "=?",
                new String[]{userId, ConnectionRequest.STATUS_PENDING},
                null, null, DBHelper.COL_CONN_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            requests.add(cursorToRequest(cursor));
        }
        cursor.close();
        return requests;
    }

    /**
     * Get all connections (any status) involving a user.
     */
    public List<ConnectionRequest> getAllConnectionsFor(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ConnectionRequest> requests = new ArrayList<>();

        String selection = "(" + DBHelper.COL_CONN_FROM + "=? OR " + DBHelper.COL_CONN_TO + "=?)";
        Cursor cursor = db.query(DBHelper.TABLE_CONNECTIONS, null,
                selection, new String[]{userId, userId},
                null, null, DBHelper.COL_CONN_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            requests.add(cursorToRequest(cursor));
        }
        cursor.close();
        return requests;
    }

    private ConnectionRequest cursorToRequest(Cursor cursor) {
        ConnectionRequest req = new ConnectionRequest();
        req.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_CONN_ID)));
        req.setFromUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CONN_FROM)));
        req.setToUserId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CONN_TO)));
        req.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_CONN_STATUS)));
        req.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_CONN_TIMESTAMP)));
        return req;
    }
}
