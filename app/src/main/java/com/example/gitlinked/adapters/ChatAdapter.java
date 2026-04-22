package com.example.gitlinked.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitlinked.R;
import com.example.gitlinked.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final Context context;
    private final List<Message> messages;
    private final String currentUserId;

    public ChatAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isSentByUser(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isSent = message.isSentByUser(currentUserId);

        // Show correct bubble, hide the other
        holder.layoutSent.setVisibility(isSent ? View.VISIBLE : View.GONE);
        holder.layoutReceived.setVisibility(isSent ? View.GONE : View.VISIBLE);

        if (isSent) {
            holder.tvSentMessage.setText(message.getContent());
            holder.tvSentTime.setText(formatTime(message.getTimestamp()));
        } else {
            holder.tvReceivedMessage.setText(message.getContent());
            holder.tvReceivedTime.setText(formatTime(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSent, layoutReceived;
        TextView tvSentMessage, tvSentTime;
        TextView tvReceivedMessage, tvReceivedTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layout_sent);
            layoutReceived = itemView.findViewById(R.id.layout_received);
            tvSentMessage = itemView.findViewById(R.id.tv_sent_message);
            tvSentTime = itemView.findViewById(R.id.tv_sent_time);
            tvReceivedMessage = itemView.findViewById(R.id.tv_received_message);
            tvReceivedTime = itemView.findViewById(R.id.tv_received_time);
        }
    }
}
