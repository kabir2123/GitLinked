package com.example.gitlinked.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gitlinked.R;
import com.example.gitlinked.activities.ChatActivity;
import com.example.gitlinked.database.MessageDao;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.Message;
import com.example.gitlinked.models.User;
import com.example.gitlinked.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing list of recent conversations.
 */
public class ChatFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Simple scrollable layout for conversations
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(requireContext().getResources().getColor(R.color.primaryDark, null));
        layout.setPadding(0, 0, 0, 0);

        // Header
        TextView header = new TextView(requireContext());
        header.setText("Messages");
        header.setTextSize(22);
        header.setTextColor(requireContext().getResources().getColor(R.color.textPrimary, null));
        header.setPadding(48, 48, 48, 24);
        layout.addView(header);

        // Load conversations
        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME, 0);
        String userId = prefs.getString(Constants.PREF_USER_ID, "");

        MessageDao messageDao = new MessageDao(requireContext());
        UserDao userDao = new UserDao(requireContext());
        List<Message> latestMessages = messageDao.getLatestMessages(userId);

        if (latestMessages.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No conversations yet.\nDiscover developers nearby and start chatting!");
            empty.setTextColor(requireContext().getResources().getColor(R.color.textSecondary, null));
            empty.setTextSize(14);
            empty.setPadding(48, 48, 48, 48);
            layout.addView(empty);
        } else {
            for (Message msg : latestMessages) {
                String otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
                User otherUser = userDao.getUserById(otherUserId);
                String displayName = otherUser != null ? otherUser.getUsername() : otherUserId;

                View item = createConversationItem(displayName, msg.getContent(),
                        msg.getTimestamp(), otherUserId,
                        otherUser != null ? otherUser.getAvatarUrl() : "");
                layout.addView(item);
            }
        }

        return layout;
    }

    private View createConversationItem(String name, String lastMessage, long timestamp,
                                         String otherUserId, String avatarUrl) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(48, 24, 48, 24);

        TextView tvName = new TextView(requireContext());
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTextColor(requireContext().getResources().getColor(R.color.textPrimary, null));
        item.addView(tvName);

        TextView tvMessage = new TextView(requireContext());
        tvMessage.setText(lastMessage);
        tvMessage.setTextSize(13);
        tvMessage.setTextColor(requireContext().getResources().getColor(R.color.textSecondary, null));
        tvMessage.setMaxLines(1);
        item.addView(tvMessage);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        TextView tvTime = new TextView(requireContext());
        tvTime.setText(sdf.format(new Date(timestamp)));
        tvTime.setTextSize(11);
        tvTime.setTextColor(requireContext().getResources().getColor(R.color.textTertiary, null));
        item.addView(tvTime);

        // Divider
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(requireContext().getResources().getColor(R.color.divider, null));

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(item);
        wrapper.addView(divider);

        wrapper.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, otherUserId);
            intent.putExtra(Constants.EXTRA_USERNAME, name);
            intent.putExtra(Constants.EXTRA_AVATAR_URL, avatarUrl);
            startActivity(intent);
        });

        return wrapper;
    }
}
