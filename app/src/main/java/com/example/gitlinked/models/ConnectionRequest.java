package com.example.gitlinked.models;

/**
 * Represents a connection/invite request between two users.
 * Status flow: PENDING → ACCEPTED / REJECTED
 */
public class ConnectionRequest {
    private long id;
    private String fromUserId;
    private String toUserId;
    private String status; // "pending", "accepted", "rejected"
    private long timestamp;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    public ConnectionRequest() {}

    public ConnectionRequest(String fromUserId, String toUserId, String status, long timestamp) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
}
