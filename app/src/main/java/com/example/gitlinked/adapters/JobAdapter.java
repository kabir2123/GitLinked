package com.example.gitlinked.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitlinked.R;
import com.example.gitlinked.models.Job;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {

    private final Context context;
    private List<Job> jobs;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Job job, int position);
        void onApplyClick(Job job, int position);
    }

    public JobAdapter(Context context, List<Job> jobs) {
        this.context = context;
        this.jobs = jobs;
    }

    public void setOnJobClickListener(OnJobClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = jobs.get(position);

        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvLocation.setText(job.getLocation());
        holder.tvType.setText(job.getType());
        holder.tvSalary.setText(job.getSalary());
        holder.tvPostedDate.setText("Posted " + job.getPostedDate());

        // Set type badge color
        switch (job.getType()) {
            case "Full-time":
                holder.tvType.setBackgroundResource(R.drawable.bg_chip);
                break;
            case "Freelance":
                holder.tvType.setBackgroundResource(R.drawable.bg_chip);
                break;
            case "Gig":
                holder.tvType.setBackgroundResource(R.drawable.bg_chip);
                break;
        }

        // Skills chips
        holder.chipGroupSkills.removeAllViews();
        if (job.getSkills() != null) {
            for (String skill : job.getSkills()) {
                Chip chip = new Chip(context);
                chip.setText(skill);
                chip.setChipBackgroundColorResource(R.color.chipBackground);
                chip.setTextColor(context.getResources().getColor(R.color.chipText, null));
                chip.setClickable(false);
                chip.setTextSize(11);
                holder.chipGroupSkills.addView(chip);
            }
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onJobClick(job, position);
        });

        holder.btnApply.setOnClickListener(v -> {
            if (listener != null) listener.onApplyClick(job, position);
        });
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    public void updateData(List<Job> newJobs) {
        this.jobs = newJobs;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvLocation, tvType, tvSalary, tvPostedDate;
        ChipGroup chipGroupSkills;
        View btnApply;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_job_title);
            tvCompany = itemView.findViewById(R.id.tv_company);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvType = itemView.findViewById(R.id.tv_type);
            tvSalary = itemView.findViewById(R.id.tv_salary);
            tvPostedDate = itemView.findViewById(R.id.tv_posted_date);
            chipGroupSkills = itemView.findViewById(R.id.chip_group_skills);
            btnApply = itemView.findViewById(R.id.btn_apply);
        }
    }
}
