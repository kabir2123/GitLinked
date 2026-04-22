package com.example.gitlinked.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitlinked.R;
import com.example.gitlinked.adapters.JobAdapter;
import com.example.gitlinked.database.JobDao;
import com.example.gitlinked.models.Job;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Displays job listings with type-based filtering (All, Full-time, Freelance, Gig).
 */
public class JobsActivity extends AppCompatActivity {

    private RecyclerView recyclerJobs;
    private ChipGroup chipGroupFilter;
    private JobAdapter jobAdapter;
    private JobDao jobDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs);

        jobDao = new JobDao(this);
        jobDao.seedMockData(); // Ensure mock data exists

        initViews();
        loadJobs(null); // Load all jobs
    }

    private void initViews() {
        recyclerJobs = findViewById(R.id.recycler_jobs);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        recyclerJobs.setLayoutManager(new LinearLayoutManager(this));

        // Filter chip listeners
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                loadJobs(null);
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_all) {
                loadJobs(null);
            } else if (checkedId == R.id.chip_fulltime) {
                loadJobs("Full-time");
            } else if (checkedId == R.id.chip_freelance) {
                loadJobs("Freelance");
            } else if (checkedId == R.id.chip_gig) {
                loadJobs("Gig");
            }
        });
    }

    private void loadJobs(String type) {
        List<Job> jobs;
        if (type != null) {
            jobs = jobDao.getJobsByType(type);
        } else {
            jobs = jobDao.getAllJobs();
        }

        if (jobAdapter == null) {
            jobAdapter = new JobAdapter(this, jobs);
            jobAdapter.setOnJobClickListener(new JobAdapter.OnJobClickListener() {
                @Override
                public void onJobClick(Job job, int position) {
                    Toast.makeText(JobsActivity.this,
                            job.getTitle() + " at " + job.getCompany(),
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onApplyClick(Job job, int position) {
                    Toast.makeText(JobsActivity.this,
                            "Application sent for " + job.getTitle() + "! 🎉",
                            Toast.LENGTH_SHORT).show();
                }
            });
            recyclerJobs.setAdapter(jobAdapter);
        } else {
            jobAdapter.updateData(jobs);
        }
    }
}
