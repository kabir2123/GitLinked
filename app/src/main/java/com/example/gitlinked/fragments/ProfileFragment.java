package com.example.gitlinked.fragments;

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

import com.bumptech.glide.Glide;
import com.example.gitlinked.R;
import com.example.gitlinked.utils.Constants;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragment for displaying the current user's profile summary.
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(requireContext().getResources().getColor(R.color.primaryDark, null));
        layout.setPadding(48, 96, 48, 48);

        SharedPreferences prefs = requireContext().getSharedPreferences(Constants.PREF_NAME, 0);

        // Avatar
        CircleImageView avatar = new CircleImageView(requireContext());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(240, 240);
        avatar.setLayoutParams(avatarParams);
        avatar.setBorderWidth(6);
        avatar.setBorderColor(requireContext().getResources().getColor(R.color.accentCyan, null));

        String avatarUrl = prefs.getString(Constants.PREF_AVATAR_URL, "");
        if (!avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl).circleCrop().into(avatar);
        }
        layout.addView(avatar);

        // Username
        TextView tvUsername = new TextView(requireContext());
        tvUsername.setText(prefs.getString(Constants.PREF_USERNAME, "Developer"));
        tvUsername.setTextSize(24);
        tvUsername.setTextColor(requireContext().getResources().getColor(R.color.textPrimary, null));
        tvUsername.setPadding(0, 24, 0, 0);
        layout.addView(tvUsername);

        // Bio
        TextView tvBio = new TextView(requireContext());
        tvBio.setText(prefs.getString(Constants.PREF_USER_BIO, "Android Developer"));
        tvBio.setTextSize(14);
        tvBio.setTextColor(requireContext().getResources().getColor(R.color.textSecondary, null));
        tvBio.setPadding(0, 8, 0, 0);
        layout.addView(tvBio);

        // User ID
        TextView tvId = new TextView(requireContext());
        tvId.setText("ID: " + prefs.getString(Constants.PREF_USER_ID, ""));
        tvId.setTextSize(12);
        tvId.setTextColor(requireContext().getResources().getColor(R.color.textTertiary, null));
        tvId.setPadding(0, 16, 0, 0);
        layout.addView(tvId);

        return layout;
    }
}
