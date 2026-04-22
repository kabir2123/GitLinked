package com.example.gitlinked.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group chat with multiple members.
 */
public class GroupChat {
    private long id;
    private String name;
    private String creatorId;
    private long createdAt;
    private List<String> memberIds;

    public GroupChat() {
        this.memberIds = new ArrayList<>();
    }

    public GroupChat(String name, String creatorId, long createdAt) {
        this();
        this.name = name;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
}
