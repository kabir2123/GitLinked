package com.example.gitlinked.models;

/**
 * A message within a group chat.
 */
public class GroupMessage {
    private long id;
    private long groupId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;

    public GroupMessage() {}

    public GroupMessage(long groupId, String senderId, String senderName,
                        String content, long timestamp) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSentByUser(String userId) {
        return senderId != null && senderId.equals(userId);
    }
}
