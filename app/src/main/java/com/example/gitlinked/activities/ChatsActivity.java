package com.example.gitlinked.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.gitlinked.R;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.database.GroupDao;
import com.example.gitlinked.database.MessageDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.GroupChat;
import com.example.gitlinked.models.GroupMessage;
import com.example.gitlinked.models.Message;
import com.example.gitlinked.models.User;
import com.example.gitlinked.realtime.FirebaseRealtimeRepository;
import com.example.gitlinked.utils.Constants;
import com.google.firebase.database.ChildEventListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Shows all conversations, pending invites, sent requests, and group chats.
 * Now also shows developers discovered via radar.
 */
public class ChatsActivity extends AppCompatActivity {

    private LinearLayout layoutContent;
    private String currentUserId;
    private ConnectionDao connectionDao;
    private MessageDao messageDao;
    private UserDao userDao;
    private GroupDao groupDao;
    private FirebaseRealtimeRepository realtimeRepository;
    private ChildEventListener connectionsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "");

        connectionDao = new ConnectionDao(this);
        messageDao = new MessageDao(this);
        userDao = new UserDao(this);
        groupDao = new GroupDao(this);
        realtimeRepository = new FirebaseRealtimeRepository(this);

        layoutContent = findViewById(R.id.layout_chats_content);

        // Create Group FAB
        ExtendedFloatingActionButton fabGroup = findViewById(R.id.fab_create_group);
        if (fabGroup != null) {
            fabGroup.setOnClickListener(v -> showCreateGroupDialog());
        }

        loadChatsAndInvites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRealtimeInviteSync();
        loadChatsAndInvites();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRealtimeInviteSync();
    }

    private void loadChatsAndInvites() {
        layoutContent.removeAllViews();

        // --- Section 0: Discovered Nearby (via Radar) ---
        List<User> allUsers = userDao.getAllUsers();
        List<User> discoveredNearby = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getId().equals(currentUserId)) continue;
            
            // Check if they are already connected or invited
            ConnectionRequest conn = connectionDao.getConnectionBetween(currentUserId, u.getId());
            if (conn != null) continue; // Already handled in other sections

            // Check if they were discovered via BLE (has address or was marked as discovered)
            if ((u.getBleDeviceAddress() != null && !u.getBleDeviceAddress().isEmpty()) 
                || (u.getBio() != null && u.getBio().contains("Discovered via"))) {
                discoveredNearby.add(u);
            }
        }

        if (!discoveredNearby.isEmpty()) {
            addSectionHeader("📡 Discovered Nearby");
            for (User u : discoveredNearby) {
                addDiscoveredItem(u);
            }
            addDivider();
        }

        // --- Section 1: Pending invites received ---
        List<ConnectionRequest> pendingInvites = connectionDao.getPendingInvitesFor(currentUserId);
        if (!pendingInvites.isEmpty()) {
            addSectionHeader("📩 Pending Invites");
            for (ConnectionRequest req : pendingInvites) {
                User fromUser = userDao.getUserById(req.getFromUserId());
                String name = fromUser != null ? fromUser.getUsername() : req.getFromUserId();
                String avatar = fromUser != null ? fromUser.getAvatarUrl() : "";
                addInviteItem(name, avatar, req);
            }
            addDivider();
        }

        // --- Section 2: Group Chats ---
        List<GroupChat> groups = groupDao.getGroupsForUser(currentUserId);
        if (!groups.isEmpty()) {
            addSectionHeader("👥 Group Chats");
            for (GroupChat group : groups) {
                GroupMessage lastMsg = groupDao.getLatestMessage(group.getId());
                String lastText = lastMsg != null ? lastMsg.getSenderName() + ": " + lastMsg.getContent()
                        : "No messages yet";
                String time = lastMsg != null ? new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(lastMsg.getTimestamp())) : "";
                addGroupChatItem(group, lastText, time);
            }
            addDivider();
        }

        // --- Section 3: Active chats (connected users) ---
        List<ConnectionRequest> allConnections = connectionDao.getAllConnectionsFor(currentUserId);
        List<ConnectionRequest> accepted = new ArrayList<>();
        List<ConnectionRequest> sentPending = new ArrayList<>();

        for (ConnectionRequest conn : allConnections) {
            if (conn.isAccepted()) {
                accepted.add(conn);
            } else if (conn.isPending() && conn.getFromUserId().equals(currentUserId)) {
                sentPending.add(conn);
            }
        }

        if (!accepted.isEmpty()) {
            addSectionHeader("💬 Direct Messages");
            for (ConnectionRequest conn : accepted) {
                String otherUserId = conn.getFromUserId().equals(currentUserId)
                        ? conn.getToUserId() : conn.getFromUserId();
                User otherUser = userDao.getUserById(otherUserId);
                String name = otherUser != null ? otherUser.getUsername() : otherUserId;
                String avatar = otherUser != null ? otherUser.getAvatarUrl() : "";

                List<Message> messages = messageDao.getMessagesBetween(currentUserId, otherUserId);
                String lastMsg = "Tap to start chatting";
                String time = "";
                if (!messages.isEmpty()) {
                    Message last = messages.get(messages.size() - 1);
                    lastMsg = last.getContent();
                    time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(new Date(last.getTimestamp()));
                }

                addChatItem(name, avatar, lastMsg, time, otherUserId);
            }
            addDivider();
        }

        // --- Section 4: Sent invites / waiting ---
        if (!sentPending.isEmpty()) {
            addSectionHeader("⏳ Sent Invites");
            for (ConnectionRequest conn : sentPending) {
                User toUser = userDao.getUserById(conn.getToUserId());
                String name = toUser != null ? toUser.getUsername() : conn.getToUserId();
                String avatar = toUser != null ? toUser.getAvatarUrl() : "";
                addSentInviteItem(name, avatar);
            }
        }

        // Empty state
        if (discoveredNearby.isEmpty() && pendingInvites.isEmpty() && accepted.isEmpty() && sentPending.isEmpty() && groups.isEmpty()) {
            addEmptyState();
        }
    }

    private void startRealtimeInviteSync() {
        if (!realtimeRepository.isAvailable() || connectionsListener != null) return;
        connectionsListener = realtimeRepository.observeConnectionsForUser(currentUserId, request -> {
            connectionDao.upsertFromRemote(
                    request.getFromUserId(),
                    request.getToUserId(),
                    request.getStatus(),
                    request.getTimestamp());
            runOnUiThread(this::loadChatsAndInvites);
        });
    }

    private void stopRealtimeInviteSync() {
        if (!realtimeRepository.isAvailable() || connectionsListener == null) return;
        realtimeRepository.removeConnectionsListener(connectionsListener);
        connectionsListener = null;
    }

    private void showCreateGroupDialog() {
        List<ConnectionRequest> allConns = connectionDao.getAllConnectionsFor(currentUserId);
        List<ConnectionRequest> accepted = new ArrayList<>();
        for (ConnectionRequest c : allConns) {
            if (c.isAccepted()) accepted.add(c);
        }

        if (accepted.isEmpty()) {
            Toast.makeText(this, "You need accepted connections first to create a group",
                    Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(48, 24, 48, 12);

        EditText etGroupName = new EditText(this);
        etGroupName.setHint("Group name");
        etGroupName.setTextColor(getResources().getColor(R.color.textPrimary, null));
        etGroupName.setHintTextColor(getResources().getColor(R.color.textTertiary, null));
        dialogLayout.addView(etGroupName);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("Select members:");
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(getResources().getColor(R.color.textSecondary, null));
        tvLabel.setPadding(0, 24, 0, 8);
        dialogLayout.addView(tvLabel);

        List<CheckBox> checkboxes = new ArrayList<>();
        List<String> userIds = new ArrayList<>();

        for (ConnectionRequest conn : accepted) {
            String otherUserId = conn.getFromUserId().equals(currentUserId)
                    ? conn.getToUserId() : conn.getFromUserId();

            if (userIds.contains(otherUserId)) continue;
            userIds.add(otherUserId);

            User user = userDao.getUserById(otherUserId);
            String name = user != null ? user.getUsername() : otherUserId;

            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setTextColor(getResources().getColor(R.color.textPrimary, null));
            cb.setTag(otherUserId);
            cb.setChecked(true);
            checkboxes.add(cb);
            dialogLayout.addView(cb);
        }

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("👥 Create Group Chat")
                .setView(dialogLayout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String groupName = etGroupName.getText().toString().trim();
                    if (groupName.isEmpty()) {
                        Toast.makeText(this, "Enter a group name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> selectedMembers = new ArrayList<>();
                    for (CheckBox cb : checkboxes) {
                        if (cb.isChecked()) {
                            selectedMembers.add((String) cb.getTag());
                        }
                    }

                    if (selectedMembers.isEmpty()) {
                        Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long groupId = groupDao.createGroup(groupName, currentUserId, selectedMembers);
                    Toast.makeText(this, "Group '" + groupName + "' created! 🎉",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, GroupChatActivity.class);
                    intent.putExtra("group_id", groupId);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== UI Builders ====================

    private void addSectionHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(18);
        header.setTextColor(getResources().getColor(R.color.textPrimary, null));
        header.setPadding(48, 32, 48, 16);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        layoutContent.addView(header);
    }

    private void addDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(48, 8, 48, 8);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
        layoutContent.addView(divider);
    }

    private void addDiscoveredItem(User user) {
        LinearLayout item = createItemLayout();

        CircleImageView avatar = createAvatar(user.getAvatarUrl(), R.color.accentCyan);
        item.addView(avatar);

        LinearLayout textLayout = createTextLayout();
        textLayout.addView(createNameText(user.getUsername()));

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Discovered nearby");
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(getResources().getColor(R.color.accentCyan, null));
        textLayout.addView(tvStatus);

        item.addView(textLayout);

        // Connect Button
        com.google.android.material.button.MaterialButton btnConnect =
                new com.google.android.material.button.MaterialButton(this);
        btnConnect.setText("Connect");
        btnConnect.setTextSize(11);
        btnConnect.setAllCaps(false);
        btnConnect.setMinimumWidth(0);
        btnConnect.setMinWidth(0);
        btnConnect.setPadding(24, 0, 24, 0);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 96);
        btnConnect.setLayoutParams(btnParams);
        btnConnect.setBackgroundColor(getResources().getColor(R.color.buttonPrimary, null));
        
        btnConnect.setOnClickListener(v -> {
            // Send invite
            connectionDao.sendRequest(currentUserId, user.getId());
            if (realtimeRepository.isAvailable()) {
                realtimeRepository.sendConnectionRequest(currentUserId, user.getId(), (success, error) -> { });
            }
            
            // Open Chat directly (as requested)
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, user.getId());
            intent.putExtra(Constants.EXTRA_USERNAME, user.getUsername());
            intent.putExtra(Constants.EXTRA_AVATAR_URL, user.getAvatarUrl());
            startActivity(intent);
            
            Toast.makeText(this, "Connecting with " + user.getUsername() + "...", Toast.LENGTH_SHORT).show();
        });
        
        item.addView(btnConnect);
        layoutContent.addView(item);
    }

    private void addGroupChatItem(GroupChat group, String lastMessage, String time) {
        LinearLayout item = createItemLayout();

        TextView icon = new TextView(this);
        icon.setText("👥");
        icon.setTextSize(28);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(120, 120);
        icon.setLayoutParams(iconParams);
        icon.setGravity(android.view.Gravity.CENTER);
        item.addView(icon);

        LinearLayout textLayout = createTextLayout();

        TextView tvName = createNameText(group.getName());
        textLayout.addView(tvName);

        TextView tvMessage = createSubText(lastMessage);
        textLayout.addView(tvMessage);

        TextView tvMembers = new TextView(this);
        tvMembers.setText(group.getMemberIds().size() + " members");
        tvMembers.setTextSize(11);
        tvMembers.setTextColor(getResources().getColor(R.color.accentPurple, null));
        textLayout.addView(tvMembers);

        item.addView(textLayout);

        if (time != null && !time.isEmpty()) {
            item.addView(createTimeText(time));
        }

        item.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupChatActivity.class);
            intent.putExtra("group_id", group.getId());
            startActivity(intent);
        });

        addRipple(item);
        layoutContent.addView(item);
    }

    private void addChatItem(String name, String avatarUrl, String lastMessage,
                              String time, String otherUserId) {
        LinearLayout item = createItemLayout();

        CircleImageView avatar = createAvatar(avatarUrl, R.color.accentCyan);
        item.addView(avatar);

        LinearLayout textLayout = createTextLayout();
        textLayout.addView(createNameText(name));
        textLayout.addView(createSubText(lastMessage));
        item.addView(textLayout);

        if (time != null && !time.isEmpty()) {
            item.addView(createTimeText(time));
        }

        item.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, otherUserId);
            intent.putExtra(Constants.EXTRA_USERNAME, name);
            intent.putExtra(Constants.EXTRA_AVATAR_URL, avatarUrl);
            startActivity(intent);
        });

        addRipple(item);
        layoutContent.addView(item);
    }

    private void addInviteItem(String name, String avatarUrl, ConnectionRequest request) {
        LinearLayout item = createItemLayout();

        CircleImageView avatar = createAvatar(avatarUrl, R.color.accentOrange);
        item.addView(avatar);

        LinearLayout textLayout = createTextLayout();
        textLayout.addView(createNameText(name));

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Wants to connect with you");
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(getResources().getColor(R.color.accentOrange, null));
        textLayout.addView(tvStatus);

        item.addView(textLayout);

        com.google.android.material.button.MaterialButton btnAccept =
                new com.google.android.material.button.MaterialButton(this);
        btnAccept.setText("Accept");
        btnAccept.setTextSize(11);
        btnAccept.setAllCaps(false);
        btnAccept.setMinimumWidth(0);
        btnAccept.setMinWidth(0);
        btnAccept.setPadding(24, 0, 24, 0);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 96);
        btnParams.setMarginEnd(8);
        btnAccept.setLayoutParams(btnParams);
        btnAccept.setBackgroundColor(getResources().getColor(R.color.accentGreen, null));
        btnAccept.setOnClickListener(v -> {
            connectionDao.acceptRequest(request.getId());
            if (realtimeRepository.isAvailable()) {
                realtimeRepository.updateConnectionStatusByUsers(
                        request.getFromUserId(),
                        request.getToUserId(),
                        ConnectionRequest.STATUS_ACCEPTED,
                        (success, error) -> { });
            }
            Toast.makeText(this, "Connected with " + name + "! 🎉", Toast.LENGTH_SHORT).show();
            loadChatsAndInvites();
        });
        item.addView(btnAccept);

        com.google.android.material.button.MaterialButton btnDecline =
                new com.google.android.material.button.MaterialButton(this);
        btnDecline.setText("✕");
        btnDecline.setTextSize(11);
        btnDecline.setAllCaps(false);
        btnDecline.setMinimumWidth(0);
        btnDecline.setMinWidth(0);
        btnDecline.setPadding(16, 0, 16, 0);
        LinearLayout.LayoutParams decParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 96);
        btnDecline.setLayoutParams(decParams);
        btnDecline.setBackgroundColor(getResources().getColor(R.color.surfaceDarkElevated, null));
        btnDecline.setOnClickListener(v -> {
            connectionDao.rejectRequest(request.getId());
            if (realtimeRepository.isAvailable()) {
                realtimeRepository.updateConnectionStatusByUsers(
                        request.getFromUserId(),
                        request.getToUserId(),
                        ConnectionRequest.STATUS_REJECTED,
                        (success, error) -> { });
            }
            loadChatsAndInvites();
        });
        item.addView(btnDecline);

        layoutContent.addView(item);
    }

    private void addSentInviteItem(String name, String avatarUrl) {
        LinearLayout item = createItemLayout();

        CircleImageView avatar = createAvatar(avatarUrl, R.color.textTertiary);
        item.addView(avatar);

        LinearLayout textLayout = createTextLayout();
        textLayout.addView(createNameText(name));

        TextView tvStatus = new TextView(this);
        tvStatus.setText("⏳ Waiting for response");
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(getResources().getColor(R.color.textTertiary, null));
        textLayout.addView(tvStatus);

        item.addView(textLayout);
        layoutContent.addView(item);
    }

    private void addEmptyState() {
        LinearLayout emptyLayout = new LinearLayout(this);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(android.view.Gravity.CENTER);
        emptyLayout.setPadding(48, 200, 48, 48);

        TextView emoji = new TextView(this);
        emoji.setText("💬");
        emoji.setTextSize(48);
        emoji.setGravity(android.view.Gravity.CENTER);
        emptyLayout.addView(emoji);

        TextView title = new TextView(this);
        title.setText("No conversations yet");
        title.setTextSize(18);
        title.setTextColor(getResources().getColor(R.color.textPrimary, null));
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 24, 0, 8);
        emptyLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Discover developers nearby and\nsend them an invite to connect!");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getResources().getColor(R.color.textSecondary, null));
        subtitle.setGravity(android.view.Gravity.CENTER);
        emptyLayout.addView(subtitle);

        layoutContent.addView(emptyLayout);
    }

    private LinearLayout createItemLayout() {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(48, 20, 48, 20);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return item;
    }

    private LinearLayout createTextLayout() {
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(32);
        textLayout.setLayoutParams(textParams);
        return textLayout;
    }

    private CircleImageView createAvatar(String url, int borderColorRes) {
        CircleImageView avatar = new CircleImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(120, 120);
        avatar.setLayoutParams(avatarParams);
        avatar.setBorderWidth(2);
        avatar.setBorderColor(getResources().getColor(borderColorRes, null));
        if (url != null && !url.isEmpty()) {
            Glide.with(this).load(url).circleCrop().into(avatar);
        } else {
            avatar.setImageResource(R.drawable.bg_circle_avatar);
        }
        return avatar;
    }

    private TextView createNameText(String name) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextSize(16);
        tv.setTextColor(getResources().getColor(R.color.textPrimary, null));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView createSubText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(getResources().getColor(R.color.textSecondary, null));
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return tv;
    }

    private TextView createTimeText(String time) {
        TextView tv = new TextView(this);
        tv.setText(time);
        tv.setTextSize(11);
        tv.setTextColor(getResources().getColor(R.color.textTertiary, null));
        return tv;
    }

    private void addRipple(LinearLayout item) {
        item.setClickable(true);
        item.setFocusable(true);
        android.content.res.TypedArray a = obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground});
        item.setForeground(a.getDrawable(0));
        a.recycle();
    }
}
