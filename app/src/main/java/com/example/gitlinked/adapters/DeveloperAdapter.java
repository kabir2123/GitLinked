package com.example.gitlinked.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gitlinked.R;
import com.example.gitlinked.database.ConnectionDao;
import com.example.gitlinked.models.ConnectionRequest;
import com.example.gitlinked.models.MatchResult;
import com.example.gitlinked.utils.Constants;
import com.example.gitlinked.utils.MatchUtils;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DeveloperAdapter extends RecyclerView.Adapter<DeveloperAdapter.ViewHolder> {

    private final Context context;
    private final List<MatchResult> matchResults;
    private OnDeveloperClickListener listener;
    private final ConnectionDao connectionDao;
    private final String currentUserId;

    public interface OnDeveloperClickListener {
        void onDeveloperClick(MatchResult match, int position);
        void onConnectClick(MatchResult match, int position);
    }

    public DeveloperAdapter(Context context, List<MatchResult> matchResults) {
        this.context = context;
        this.matchResults = matchResults;
        this.connectionDao = new ConnectionDao(context);
        this.currentUserId = context.getSharedPreferences(Constants.PREF_NAME,
                Context.MODE_PRIVATE).getString(Constants.PREF_USER_ID, "");
    }

    public void setOnDeveloperClickListener(OnDeveloperClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_developer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchResult match = matchResults.get(position);

        holder.tvUsername.setText(match.getUser().getUsername());
        holder.tvBio.setText(match.getUser().getBio());
        holder.tvMatchPercent.setText(match.getMatchPercentage() + "%");
        holder.tvMatchPercent.setTextColor(Color.parseColor(
                MatchUtils.getMatchColor(match.getMatchPercentage())));

        // Match progress bar
        holder.progressMatch.setProgress(match.getMatchPercentage());

        // Common languages
        String matchSummary = match.getMatchSummary();
        holder.tvMatchSummary.setText(matchSummary.isEmpty() ? "Discovered nearby" : matchSummary);

        // Online status
        holder.viewOnlineStatus.setVisibility(
                match.getUser().isOnline() ? View.VISIBLE : View.GONE);

        // Load avatar
        if (match.getUser().getAvatarUrl() != null && !match.getUser().getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(match.getUser().getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.bg_circle_avatar)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.bg_circle_avatar);
        }

        // Update button text based on connection status
        updateConnectButton(holder.btnConnect, match.getUser().getId());

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeveloperClick(match, position);
        });

        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) listener.onConnectClick(match, position);
        });
    }

    /**
     * Update the connect button text/state based on connection status.
     */
    private void updateConnectButton(MaterialButton button, String otherUserId) {
        ConnectionRequest conn = connectionDao.getConnectionBetween(currentUserId, otherUserId);

        if (conn == null) {
            button.setText("Connect");
            button.setEnabled(true);
            button.setAlpha(1f);
            button.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.buttonPrimary)));
        } else if (conn.isAccepted()) {
            button.setText("Chat");
            button.setEnabled(true);
            button.setAlpha(1f);
            button.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.accentCyan)));
        } else if (conn.isPending()) {
            if (conn.getFromUserId().equals(currentUserId)) {
                button.setText("Sent");
                button.setEnabled(false);
                button.setAlpha(0.6f);
            } else {
                button.setText("Accept");
                button.setEnabled(true);
                button.setAlpha(1f);
                button.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.buttonPrimary)));
            }
        }
    }

    @Override
    public int getItemCount() {
        return matchResults.size();
    }

    public void updateData(List<MatchResult> newData) {
        matchResults.clear();
        matchResults.addAll(newData);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername, tvBio, tvMatchPercent, tvMatchSummary;
        ProgressBar progressMatch;
        View viewOnlineStatus;
        MaterialButton btnConnect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvBio = itemView.findViewById(R.id.tv_bio);
            tvMatchPercent = itemView.findViewById(R.id.tv_match_percent);
            tvMatchSummary = itemView.findViewById(R.id.tv_match_summary);
            progressMatch = itemView.findViewById(R.id.progress_match);
            viewOnlineStatus = itemView.findViewById(R.id.view_online_status);
            btnConnect = itemView.findViewById(R.id.btn_connect);
        }
    }
}
