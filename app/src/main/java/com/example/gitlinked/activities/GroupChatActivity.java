package com.example.gitlinked.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gitlinked.R;
import com.example.gitlinked.database.GroupDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.GroupChat;
import com.example.gitlinked.models.GroupMessage;
import com.example.gitlinked.models.User;
import com.example.gitlinked.utils.Constants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Group chat activity — messaging with multiple connected developers.
 */
public class GroupChatActivity extends AppCompatActivity {

    private LinearLayout layoutMessages;
    private ScrollView scrollMessages;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private TextView tvGroupName, tvMemberCount;

    private GroupDao groupDao;
    private UserDao userDao;
    private long groupId;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        groupDao = new GroupDao(this);
        userDao = new UserDao(this);

        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "");
        currentUsername = prefs.getString(Constants.PREF_USERNAME, "You");

        groupId = getIntent().getLongExtra("group_id", -1);
        if (groupId == -1) {
            Toast.makeText(this, "Invalid group", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadGroupInfo();
        loadMessages();
    }

    private void initViews() {
        layoutMessages = findViewById(R.id.layout_group_messages);
        scrollMessages = findViewById(R.id.scroll_group_messages);
        etMessage = findViewById(R.id.et_group_message);
        btnSend = findViewById(R.id.btn_group_send);
        tvGroupName = findViewById(R.id.tv_group_name);
        tvMemberCount = findViewById(R.id.tv_group_member_count);

        findViewById(R.id.btn_group_back).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadGroupInfo() {
        GroupChat group = groupDao.getGroupById(groupId);
        if (group == null) {
            finish();
            return;
        }

        tvGroupName.setText(group.getName());

        List<String> members = group.getMemberIds();
        StringBuilder memberNames = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            User user = userDao.getUserById(members.get(i));
            if (user != null) {
                if (memberNames.length() > 0) memberNames.append(", ");
                memberNames.append(user.getUsername());
            }
        }
        tvMemberCount.setText(members.size() + " members: " + memberNames);
    }

    private void loadMessages() {
        layoutMessages.removeAllViews();

        List<GroupMessage> messages = groupDao.getMessages(groupId);

        if (messages.isEmpty()) {
            addSystemMessage("Group created! Say hello to everyone 👋");
            return;
        }

        for (GroupMessage msg : messages) {
            addMessageBubble(msg);
        }

        scrollToBottom();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        groupDao.sendMessage(groupId, currentUserId, currentUsername, text);

        // Add bubble
        GroupMessage msg = new GroupMessage(groupId, currentUserId, currentUsername,
                text, System.currentTimeMillis());
        addMessageBubble(msg);

        etMessage.setText("");
        scrollToBottom();
    }

    private void addMessageBubble(GroupMessage msg) {
        boolean isSent = msg.isSentByUser(currentUserId);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(32, 12, 32, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = isSent ? Gravity.END : Gravity.START;
        params.setMargins(isSent ? 100 : 0, 4, isSent ? 0 : 100, 4);
        bubble.setLayoutParams(params);

        bubble.setBackgroundResource(isSent ? R.drawable.bg_message_sent : R.drawable.bg_message_received);

        // Sender name (not shown for own messages)
        if (!isSent) {
            TextView tvSender = new TextView(this);
            tvSender.setText(msg.getSenderName() != null ? msg.getSenderName() : "Unknown");
            tvSender.setTextSize(11);
            tvSender.setTextColor(getResources().getColor(R.color.accentCyan, null));
            tvSender.setTypeface(null, android.graphics.Typeface.BOLD);
            bubble.addView(tvSender);
        }

        // Message content
        TextView tvContent = new TextView(this);
        tvContent.setText(msg.getContent());
        tvContent.setTextSize(14);
        tvContent.setTextColor(getResources().getColor(R.color.textPrimary, null));
        bubble.addView(tvContent);

        // Timestamp
        TextView tvTime = new TextView(this);
        tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(msg.getTimestamp())));
        tvTime.setTextSize(10);
        tvTime.setTextColor(getResources().getColor(R.color.textTertiary, null));
        tvTime.setGravity(isSent ? Gravity.END : Gravity.START);
        bubble.addView(tvTime);

        layoutMessages.addView(bubble);
    }

    private void addSystemMessage(String text) {
        TextView tvSystem = new TextView(this);
        tvSystem.setText(text);
        tvSystem.setTextSize(13);
        tvSystem.setTextColor(getResources().getColor(R.color.textTertiary, null));
        tvSystem.setGravity(Gravity.CENTER);
        tvSystem.setPadding(16, 32, 16, 32);
        layoutMessages.addView(tvSystem);
    }

    private void scrollToBottom() {
        scrollMessages.post(() -> scrollMessages.fullScroll(View.FOCUS_DOWN));
    }
}
