package com.example.gitlinked.realtime;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.Message;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Realtime cross-device sync for invites and direct messages.
 * Local SQLite remains source for rendering; this layer mirrors data across phones.
 */
public class FirebaseRealtimeRepository {

    private static final String TAG = "FirebaseRealtimeRepo";
    private static final String PATH_CONNECTIONS = "connectionRequests";
    private static final String PATH_MESSAGES = "messages";

    public interface OnComplete {
        void onComplete(boolean success, @Nullable String error);
    }

    public interface OnConnectionEvent {
        void onConnection(ConnectionRequest request);
    }

    public interface OnMessageEvent {
        void onMessage(Message message);
    }

    private final DatabaseReference rootRef;
    private final boolean available;

    public FirebaseRealtimeRepository(Context context) {
        FirebaseApp app = null;
        if (FirebaseApp.getApps(context).isEmpty()) {
            app = FirebaseApp.initializeApp(context);
        } else {
            app = FirebaseApp.getInstance();
        }

        if (app == null) {
            available = false;
            rootRef = null;
            Log.w(TAG, "Firebase not configured. Realtime sync disabled.");
            return;
        }

        available = true;
        rootRef = FirebaseDatabase.getInstance(app).getReference();
    }

    public boolean isAvailable() {
        return available;
    }

    public void sendConnectionRequest(String fromUserId, String toUserId, OnComplete callback) {
        if (!available) {
            callback.onComplete(false, "Firebase unavailable");
            return;
        }

        String key = rootRef.child(PATH_CONNECTIONS).push().getKey();
        if (key == null) {
            callback.onComplete(false, "Failed to create invite key");
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put("fromUserId", fromUserId);
        payload.put("toUserId", toUserId);
        payload.put("status", ConnectionRequest.STATUS_PENDING);
        payload.put("timestamp", now);
        payload.put("pairKey", getPairKey(fromUserId, toUserId));

        rootRef.child(PATH_CONNECTIONS).child(key).setValue(payload)
                .addOnSuccessListener(v -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void updateConnectionStatusByUsers(String fromUserId, String toUserId, String status,
                                              OnComplete callback) {
        if (!available) {
            callback.onComplete(false, "Firebase unavailable");
            return;
        }

        String pairKey = getPairKey(fromUserId, toUserId);
        rootRef.child(PATH_CONNECTIONS)
                .orderByChild("pairKey")
                .equalTo(pairKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    DataSnapshot latest = null;
                    long maxTs = -1L;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Long ts = child.child("timestamp").getValue(Long.class);
                        if (ts != null && ts > maxTs) {
                            maxTs = ts;
                            latest = child;
                        }
                    }
                    if (latest == null) {
                        callback.onComplete(false, "No connection request found");
                        return;
                    }
                    latest.getRef().child("status").setValue(status)
                            .addOnSuccessListener(v -> callback.onComplete(true, null))
                            .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public ChildEventListener observeConnectionsForUser(String userId, OnConnectionEvent callback) {
        if (!available) return null;

        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, @Nullable String previousChildName) {
                emitConnection(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, @Nullable String previousChildName) {
                emitConnection(snapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Connection listener cancelled: " + error.getMessage());
            }

            private void emitConnection(DataSnapshot snapshot) {
                String from = snapshot.child("fromUserId").getValue(String.class);
                String to = snapshot.child("toUserId").getValue(String.class);
                if (from == null || to == null) return;
                if (!userId.equals(from) && !userId.equals(to)) return;

                ConnectionRequest req = new ConnectionRequest();
                req.setFromUserId(from);
                req.setToUserId(to);
                req.setStatus(valueOr(snapshot.child("status").getValue(String.class),
                        ConnectionRequest.STATUS_PENDING));
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                req.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                callback.onConnection(req);
            }
        };

        rootRef.child(PATH_CONNECTIONS).addChildEventListener(listener);
        return listener;
    }

    public void removeConnectionsListener(ChildEventListener listener) {
        if (!available || listener == null) return;
        rootRef.child(PATH_CONNECTIONS).removeEventListener(listener);
    }

    public void sendDirectMessage(Message message, OnComplete callback) {
        if (!available) {
            callback.onComplete(false, "Firebase unavailable");
            return;
        }

        String conversationId = getConversationId(message.getSenderId(), message.getReceiverId());
        String key = rootRef.child(PATH_MESSAGES).child(conversationId).push().getKey();
        if (key == null) {
            callback.onComplete(false, "Failed to create message key");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", message.getSenderId());
        payload.put("receiverId", message.getReceiverId());
        payload.put("content", message.getContent());
        payload.put("timestamp", message.getTimestamp());

        rootRef.child(PATH_MESSAGES).child(conversationId).child(key).setValue(payload)
                .addOnSuccessListener(v -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public ChildEventListener observeConversation(String userA, String userB, OnMessageEvent callback) {
        if (!available) return null;
        String conversationId = getConversationId(userA, userB);
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshotToMessage(snapshot);
                if (message != null) callback.onMessage(message);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Message listener cancelled: " + error.getMessage());
            }
        };
        rootRef.child(PATH_MESSAGES).child(conversationId).addChildEventListener(listener);
        return listener;
    }

    public void removeConversationListener(String userA, String userB, ChildEventListener listener) {
        if (!available || listener == null) return;
        String conversationId = getConversationId(userA, userB);
        rootRef.child(PATH_MESSAGES).child(conversationId).removeEventListener(listener);
    }

    private Message snapshotToMessage(DataSnapshot snapshot) {
        String sender = snapshot.child("senderId").getValue(String.class);
        String receiver = snapshot.child("receiverId").getValue(String.class);
        String content = snapshot.child("content").getValue(String.class);
        Long ts = snapshot.child("timestamp").getValue(Long.class);
        if (sender == null || receiver == null || content == null || ts == null) return null;
        return new Message(sender, receiver, content, ts);
    }

    private String getConversationId(String userA, String userB) {
        return userA.compareTo(userB) < 0 ? userA + "__" + userB : userB + "__" + userA;
    }

    private String getPairKey(String userA, String userB) {
        return getConversationId(userA, userB);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
