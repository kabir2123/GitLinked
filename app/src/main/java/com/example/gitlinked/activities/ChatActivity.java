package com.example.gitlinked.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gitlinked.R;
import com.example.gitlinked.adapters.ChatAdapter;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.database.MessageDao;
import com.example.gitlinked.models.Message;
import com.example.gitlinked.realtime.FirebaseRealtimeRepository;
import com.example.gitlinked.utils.Constants;
import com.google.firebase.database.ChildEventListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Chat screen with end-to-end encrypted messaging.
 * Messages are AES-encrypted before being stored in SQLite.
 * Chat is only accessible if the two users are connected (invite accepted).
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private CircleImageView imgChatAvatar;
    private TextView tvChatUsername;

    private ChatAdapter chatAdapter;
    private List<Message> messages = new ArrayList<>();
    private MessageDao messageDao;
    private ConnectionDao connectionDao;
    private FirebaseRealtimeRepository realtimeRepository;
    private ChildEventListener conversationListener;

    private String currentUserId;
    private String otherUserId;
    private String otherUsername;
    private String otherAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageDao = new MessageDao(this);
        connectionDao = new ConnectionDao(this);
        realtimeRepository = new FirebaseRealtimeRepository(this);

        // Get intent extras
        otherUserId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        otherUsername = getIntent().getStringExtra(Constants.EXTRA_USERNAME);
        otherAvatarUrl = getIntent().getStringExtra(Constants.EXTRA_AVATAR_URL);

        // Get current user
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(Constants.PREF_USER_ID, "demo_user");

        // Check connection status (allow demo users to chat freely)
        boolean isDemoUser = "demo_user".equals(currentUserId);
        boolean isMockOther = otherUserId != null && otherUserId.startsWith("mock_");
        boolean isConnected = connectionDao.areConnected(currentUserId, otherUserId);

        if (!isDemoUser && !isMockOther && !isConnected) {
            Toast.makeText(this, "You need to be connected to chat. Send an invite first!",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        loadMessages();

        // Only seed mock messages for demo mode
        if (isDemoUser || isMockOther) {
            seedMockMessages();
        }

        startRealtimeMessageSync();
    }

    private void initViews() {
        recyclerMessages = findViewById(R.id.recycler_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        imgChatAvatar = findViewById(R.id.img_chat_avatar);
        tvChatUsername = findViewById(R.id.tv_chat_username);

        // Set header
        tvChatUsername.setText(otherUsername != null ? otherUsername : "Developer");
        if (otherAvatarUrl != null && !otherAvatarUrl.isEmpty()) {
            Glide.with(this).load(otherAvatarUrl).circleCrop().into(imgChatAvatar);
        }

        // Back button
        findViewById(R.id.btn_chat_back).setOnClickListener(v -> finish());

        // RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(this, messages, currentUserId);
        recyclerMessages.setAdapter(chatAdapter);

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());

        // Send on Enter
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message message = new Message(currentUserId, otherUserId, text,
                System.currentTimeMillis());

        // Store encrypted in SQLite
        messageDao.insertMessage(message);

        if (realtimeRepository.isAvailable()) {
            realtimeRepository.sendDirectMessage(message, (success, error) -> {
                if (!success) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this,
                            "Failed to sync message: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        }

        // Add to UI
        chatAdapter.addMessage(message);
        recyclerMessages.scrollToPosition(messages.size() - 1);

        etMessage.setText("");

        // Simulate auto-reply after 1.5 seconds (only for mock users)
        if (otherUserId != null && otherUserId.startsWith("mock_")) {
            recyclerMessages.postDelayed(this::simulateReply, 1500);
        }
    }

    private void loadMessages() {
        if (otherUserId != null) {
            messages.clear();
            messages.addAll(messageDao.getMessagesBetween(currentUserId, otherUserId));
            if (chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    recyclerMessages.scrollToPosition(messages.size() - 1);
                }
            }
        }
    }

    private void startRealtimeMessageSync() {
        boolean canSync = realtimeRepository.isAvailable()
                && otherUserId != null
                && !otherUserId.isEmpty()
                && !"demo_user".equals(currentUserId)
                && !otherUserId.startsWith("mock_");
        if (!canSync || conversationListener != null) return;

        conversationListener = realtimeRepository.observeConversation(
                currentUserId,
                otherUserId,
                remoteMessage -> runOnUiThread(() -> {
                    boolean inserted = messageDao.insertMessageIfNotExists(remoteMessage);
                    if (!inserted) return;
                    chatAdapter.addMessage(remoteMessage);
                    recyclerMessages.scrollToPosition(messages.size() - 1);
                }));
    }

    private void stopRealtimeMessageSync() {
        if (!realtimeRepository.isAvailable() || conversationListener == null || otherUserId == null) return;
        realtimeRepository.removeConversationListener(currentUserId, otherUserId, conversationListener);
        conversationListener = null;
    }

    /**
     * Seed some mock messages if the conversation is empty (demo mode only).
     */
    private void seedMockMessages() {
        if (!messages.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Simulated conversation
        Message m1 = new Message(otherUserId, currentUserId,
                "Hey! Saw you're working on Android too. What stack are you using?",
                now - 300000);
        Message m2 = new Message(currentUserId, otherUserId,
                "Hi! Mostly Java with some Kotlin. You?",
                now - 240000);
        Message m3 = new Message(otherUserId, currentUserId,
                "Nice! I'm fully Kotlin + Compose now. Want to collaborate on something?",
                now - 180000);
        Message m4 = new Message(currentUserId, otherUserId,
                "Sounds great! I'm thinking about a dev tools app. Let's discuss!",
                now - 120000);

        messageDao.insertMessage(m1);
        messageDao.insertMessage(m2);
        messageDao.insertMessage(m3);
        messageDao.insertMessage(m4);

        // Reload
        loadMessages();
    }

    /**
     * Simulate an auto-reply from the other developer (mock users only).
     */
    private void simulateReply() {
        String[] replies = {
                "That sounds interesting! Let me check it out 🔍",
                "Great idea! I can contribute to the backend part 💻",
                "Let's set up a repo and get started! 🚀",
                "I'll share my GitHub link, check out my recent projects",
                "We should meet at the next dev meetup! 📍",
                "I've been working on something similar, let's merge efforts!",
                "Nice! What's your experience with BLE? I'm working on something similar."
        };

        String reply = replies[(int) (Math.random() * replies.length)];
        Message replyMsg = new Message(otherUserId, currentUserId, reply,
                System.currentTimeMillis());

        messageDao.insertMessage(replyMsg);
        chatAdapter.addMessage(replyMsg);
        recyclerMessages.scrollToPosition(messages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealtimeMessageSync();
    }
}
